package com.github.t1.deployer.model;

import com.github.t1.problem.*;
import com.google.common.collect.ImmutableMap;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.util.ArrayList;
import java.util.*;
import java.util.function.*;
import java.util.regex.*;

import static com.github.t1.problem.WebException.*;
import static java.util.Arrays.*;
import static java.util.Locale.*;
import static java.util.stream.Collectors.*;
import static javax.ws.rs.core.Response.Status.*;

@Slf4j
public class Variables {
    @SneakyThrows(UnknownHostException.class)
    public static String hostName() { return InetAddress.getLocalHost().getHostName().split("\\.")[0]; }

    private static final Pattern VAR = Pattern.compile("\\$\\{([^}]*)\\}");
    private static final Pattern VARIABLE_VALUE = Pattern.compile("[- ._a-zA-Z0-9]{1,256}");

    private final ImmutableMap<String, String> variables;

    @SuppressWarnings("unchecked")
    public Variables() { this(ImmutableMap.copyOf((Map<String, String>) (Map) System.getProperties())); }

    public Variables(ImmutableMap<String, String> variables) { this.variables = variables; }


    /**
     * Reads from the reader, removes all comments (starting with a `#`), and replaces all variables
     * (starting with `${` and ending with `}` - may be escaped with a second `$`, i.e. `$${a}` will be replaced by `${a}`).
     *
     * @implNote We load the entire text from the source reader instead of streaming it, as these are never really big.
     */
    @SneakyThrows(IOException.class)
    public Reader resolve(Reader reader) {
        StringBuilder out = new StringBuilder();
        String line;
        BufferedReader buffered = buffered(reader);
        while ((line = buffered.readLine()) != null) {
            if (line.contains("#"))
                line = line.substring(0, line.indexOf('#'));
            Matcher matcher = VAR.matcher(line);
            int tail = 0;
            while (matcher.find()) {
                out.append(line.substring(tail, matcher.start()));
                if (matcher.start() > 0 && line.charAt(matcher.start() - 1) == '$') {
                    // +1 to skip the var-$ as we already copied the escape-$
                    out.append(line.substring(matcher.start() + 1, matcher.end()));
                } else {
                    String expression = matcher.group(1);
                    String value = resolveExpression(expression);
                    if (value == null)
                        throw new UnresolvedVariableException(expression);
                    out.append(value);
                }
                tail = matcher.end();
            }
            out.append(line.substring(tail));
            out.append('\n');
        }
        return new StringReader(out.toString());
    }

    public String resolveExpression(String expression) {
        Resolver resolver = new OrResolver(expression);
        if (resolver.isMatch())
            return resolver.getValue();
        log.trace("unresolved expression [{}]", expression);
        return null;
    }

    public boolean contains(String name) { return variables.containsKey(name); }

    private abstract static class Resolver {
        @Getter protected boolean match;
        @Getter protected String value;

        @Override public String toString() { return getClass().getSimpleName() + ":" + match + ":" + value; }
    }

    private static final List<Class<? extends Resolver>> RESOLVERS
            = asList(LiteralResolver.class, FunctionResolver.class, VariableResolver.class);

    private class OrResolver extends Resolver {
        public OrResolver(String expression) {
            for (String key : split(expression, " or ")) {
                log.trace("try to resolve variable expression [{}]", key);
                for (Class<? extends Resolver> resolverType : RESOLVERS) {
                    Resolver resolver = create(resolverType, key);
                    if (resolver.isMatch()) {
                        this.match = true;
                        this.value = resolver.getValue();
                        return;
                    }
                }
            }
            this.match = false;
            this.value = null;
        }

        @NotNull public Resolver create(Class<? extends Resolver> type, String key) {
            try {
                return type.getConstructor(Variables.class, String.class).newInstance(Variables.this, key);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException)
                    throw (RuntimeException) e.getCause();
                throw new RuntimeException("can't create " + type.getName() + " for key [" + key + "]");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("can't create " + type.getName() + " for key [" + key + "]");
            }
        }
    }

    private static final Pattern LITERAL = Pattern.compile("«(\\V*)»");

    private class LiteralResolver extends Resolver {
        private final Matcher matcher;

        public LiteralResolver(String key) {
            this.matcher = LITERAL.matcher(key);
            this.match = matcher.matches();
            this.value = match ? matcher.group(1) : null;
        }
    }

    private static final Pattern VARIABLE_NAME = Pattern.compile("[-._a-zA-Z0-9]{1,256}");

    private class VariableResolver extends Resolver {
        private final String key;
        private final Matcher matcher;
        private final String variableName;

        public VariableResolver(String key) {
            this.key = key;
            this.matcher = VARIABLE_NAME.matcher(key);
            this.match = matcher.matches();
            this.variableName = match ? matcher.group() : null;
            this.value = match ? resolve() : null;
        }

        private String resolve() {
            if (!variables.containsKey(variableName)) {
                log.trace("undefined variable [{}]", key);
                this.match = false;
                return null;
            }
            log.trace("did resolve [{}]", variableName);
            return checkValue(variableName, variables.get(variableName));
        }
    }

    private static final Pattern FUNCTION = Pattern.compile("(?<name>" + VARIABLE_NAME + ")" + "(\\((?<body>.*)\\))");

    private class FunctionResolver extends Resolver {
        private final Matcher matcher;
        private final String functionName;
        private final List<Supplier<String>> params;

        public FunctionResolver(String expression) {
            this.matcher = FUNCTION.matcher(expression);
            this.match = matcher.matches();
            this.functionName = match ? matcher.group("name") : null;
            this.params = match ? params() : null;
            this.value = match ? resolve() : null;
        }

        private List<Supplier<String>> params() {
            return split(matcher.group("body"), ",")
                    .stream()
                    .map(String::trim)
                    .map(expression -> (Supplier<String>) () -> {
                        OrResolver resolver = new OrResolver(expression);
                        return resolver.isMatch() ? resolver.getValue() : null;
                    })
                    .collect(toList());
        }

        private String resolve() {
            log.trace("found function name [{}] with {} params", functionName, params.size());
            switch (functionName + "#" + params.size()) {
            case "hostName#0":
                return hostName();
            case "domainName#0":
                return domainName();
            case "toUpperCase#1":
                return apply1(s -> s.toUpperCase(US));
            case "toLowerCase#1":
                return apply1(s -> s.toLowerCase(US));
            case "regex#2":
                return apply2(this::regex);
            default:
                throw badRequest(
                        "undefined variable function with " + params.size() + " params: [" + functionName + "]");
            }
        }

        private String apply1(Function<String, String> function) {
            return param(0).map(function).orElseGet(this::fail);
        }

        private String apply2(BiFunction<String, String, String> function) {
            Optional<String> param0 = param(0);
            Optional<String> param1 = param(1);
            return param0.isPresent() && param1.isPresent() ? function.apply(param0.get(), param1.get()) : fail();
        }

        private Optional<String> param(int index) { return Optional.ofNullable(params.get(index).get()); }

        @SneakyThrows(UnknownHostException.class)
        private String domainName() {
            String[] split = InetAddress.getLocalHost().getHostName().split("\\.", 2);
            return (split.length == 2) ? split[1] : null;
        }

        private String regex(String text, String pattern) {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            return matcher.matches() ? matcher.group(1) : fail();
        }

        private String fail() {
            match = false;
            return null;
        }
    }

    private static List<String> split(String expression, String pattern) {
        List<String> list = new ArrayList<>();
        String current = "";
        if (!expression.isEmpty())
            for (String split : expression.split(pattern)) {
                current += split;
                if (count('(', current) == count(')', current)) {
                    list.add(current);
                    current = "";
                } else {
                    current += pattern;
                }
            }
        return list;
    }

    private static int count(char c, String string) {
        int n = -1, offset = -1;
        do {
            ++n;
            offset = string.indexOf(c, offset + 1);
        } while (offset >= 0);
        return n;
    }

    private static String checkValue(String expression, String value) {
        if (value != null && !VARIABLE_VALUE.matcher(value).matches())
            throw badRequest("invalid character in variable value for [" + expression + "]");
        return value;
    }

    private BufferedReader buffered(Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    }

    public Variables with(String key, String value) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder().putAll(this.variables);
        if (this.variables.containsKey(key))
            throw badRequest("Variable named [" + key + "] already set. It's not allowed to overwrite.");
        builder.put(key, value);
        return new Variables(builder.build());
    }

    public Variables withAll(Map<String, String> variables) {
        if (variables == null || variables.isEmpty())
            return this;
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder().putAll(this.variables);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (this.variables.containsKey(key))
                throw badRequest("Variable named [" + key + "] already set. It's not allowed to overwrite.");
            builder.put(key, entry.getValue());
        }
        return new Variables(builder.build());
    }

    @ReturnStatus(BAD_REQUEST)
    public static class UnresolvedVariableException extends WebApplicationApplicationException {
        @Getter private final String expression;

        protected UnresolvedVariableException(String expression) {
            super("unresolved variable expression: " + expression);
            this.expression = expression;
        }
    }
}
