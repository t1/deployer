package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.github.t1.deployer.tools.*;
import com.github.t1.problem.*;
import com.google.common.collect.ImmutableMap;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

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
import static lombok.AccessLevel.*;

@Slf4j
@RequiredArgsConstructor
public class Expressions {
    private static final Pattern NAME_TOKEN = Pattern.compile("[-._a-zA-Z0-9]{1,256}");

    @Value
    @NoArgsConstructor(access = PRIVATE, force = true)
    @JsonSerialize(using = ToStringSerializer.class)
    public static class VariableName {
        private static String checked(String value) {
            if (!NAME_TOKEN.matcher(value).matches())
                throw new IllegalArgumentException("invalid variable name [" + value + "]");
            return value;
        }

        @NonNull String value;

        @JsonCreator public VariableName(@NonNull String value) { this.value = checked(value); }

        @Override public String toString() { return value; }
    }

    @SneakyThrows(UnknownHostException.class)
    public static String hostName() { return InetAddress.getLocalHost().getHostName().split("\\.")[0]; }

    @SneakyThrows(UnknownHostException.class)
    public static String domainName() {
        String[] split = InetAddress.getLocalHost().getHostName().split("\\.", 2);
        return (split.length == 2) ? split[1] : null;
    }

    private static final Pattern VAR = Pattern.compile("\\$\\{([^}]*)\\}");
    private static final Pattern VARIABLE_VALUE = Pattern.compile("[- ._a-zA-Z0-9?*:|\\\\{}()\\[\\]]{1,256}");

    private final ImmutableMap<VariableName, String> variables;
    private final RootBundleConfig rootBundle;
    private final KeyStoreConfig keyStore;

    public Expressions() { this(ImmutableMap.copyOf(systemProperties()), null, null); }

    private static Map<VariableName, String> systemProperties() {
        return System.getProperties().stringPropertyNames().stream()
                     .filter(name -> NAME_TOKEN.matcher(name).matches())
                     .collect(toMap(VariableName::new, System::getProperty));
    }


    /**
     * Replaces all variables (starting with `${` and ending with `}` - may be escaped with a second `$`,
     * i.e. `$${a}` will be replaced by `${a}`.
     */
    public String resolve(String line, String alternative) {
        StringBuilder out = new StringBuilder();
        if (line.contains("#"))
            line = line.substring(0, line.indexOf('#'));
        Matcher matcher = VAR.matcher(line);
        int tail = 0;
        boolean hasNullValue = false;
        while (matcher.find()) {
            out.append(line.substring(tail, matcher.start()));
            if (matcher.start() > 0 && line.charAt(matcher.start() - 1) == '$') {
                // +1 to skip the var-$ as we already copied the escape-$
                out.append(line.substring(matcher.start() + 1, matcher.end()));
            } else {
                String expression = matcher.group(1);
                if (alternative != null)
                    expression += " or " + alternative;
                Resolver resolver = resolver(expression);
                if (!resolver.matches())
                    throw new UnresolvedVariableException(expression);
                if (resolver.getValue() == null)
                    hasNullValue = true;
                else
                    out.append(resolver.getValue());
            }
            tail = matcher.end();
        }
        out.append(line.substring(tail));
        return (hasNullValue && out.length() == 0) ? null : out.toString();
    }

    public Resolver resolver(CharSequence expression) { return new OrResolver(expression); }

    public boolean contains(VariableName name) { return variables.containsKey(name); }

    public abstract static class Resolver {
        protected boolean match;
        @Getter protected String value;

        protected void setNoMatch() { set(Optional.empty()); }

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        protected void set(Optional<String> value) { set(value.isPresent(), value.orElse(null)); }

        protected void set(boolean match, Supplier<String> value) { set(match, match ? value.get() : null); }

        protected void set(boolean match, String value) {
            this.match = match;
            this.value = value;
        }

        @Override public String toString() { return getClass().getSimpleName() + ":" + value; }

        public boolean matches() { return match; }

        public String getOrElseThrow(Supplier<? extends RuntimeException> thrower) {
            if (match)
                return value;
            throw thrower.get();
        }

        public String getValueOr(String alternative) { return match ? value : alternative; }
    }

    private static final List<Class<? extends Resolver>> RESOLVERS = asList(
            NullResolver.class,
            BooleanResolver.class,
            SwitchResolver.class,
            LiteralResolver.class,
            FunctionResolver.class,
            VariableResolver.class,
            RootBundleResolver.class);

    private class OrResolver extends Resolver {
        public OrResolver(CharSequence expression) {
            for (String subExpression : split(expression, " or ")) {
                log.trace("try to resolve variable expression [{}]", subExpression);
                for (Class<? extends Resolver> resolverType : RESOLVERS) {
                    Resolver resolver = create(resolverType, subExpression);
                    if (resolver.matches()) {
                        set(true, resolver.getValue());
                        return;
                    }
                }
            }
            setNoMatch();
        }

        public Resolver create(Class<? extends Resolver> type, String subExpression) {
            try {
                return type.getConstructor(Expressions.class, String.class)
                           .newInstance(Expressions.this, subExpression);
            } catch (InvocationTargetException e) {
                //noinspection ChainOfInstanceofChecks
                if (e.getCause() instanceof Error)
                    throw (Error) e.getCause();
                if (e.getCause() instanceof RuntimeException)
                    throw (RuntimeException) e.getCause();
                throw new RuntimeException(
                        "can't create " + type.getName() + " for expression [" + subExpression + "]", e);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(
                        "can't create " + type.getName() + " for expression [" + subExpression + "]", e);
            }
        }
    }


    private class NullResolver extends Resolver {
        public NullResolver(String expression) {
            this.match = "null".equals(expression);
            this.value = null;
        }
    }


    private class BooleanResolver extends Resolver {
        public BooleanResolver(String expression) {
            set("true".equals(expression) || "false".equals(expression), expression);
        }
    }


    private class SwitchResolver extends Resolver {
        public SwitchResolver(String expression) {
            set(expression.startsWith("switch"), () -> doSwitch(expression.substring(6)));
        }

        private String doSwitch(String expression) {
            String head = findBrackets("()", expression)
                    .orElseThrow(() -> new IllegalArgumentException("unmatched brackets for switch statement"));
            String value = resolver(head).getOrElseThrow(() ->
                    new IllegalArgumentException("no variable defined in switch header: '" + head + "'"));
            String body = expression.substring(head.length() + 2, expression.length());
            int i = body.indexOf(" " + value + ":");
            if (i < 0)
                throw new IllegalArgumentException("no case label for '" + value + "' in switch statement");
            String rest = body.substring(i + value.length() + 2, body.length()).trim();
            return findBrackets("«»", rest)
                    .orElseThrow(() -> new IllegalArgumentException("unmatched brackets for switch literal"));
        }
    }

    private static Optional<String> findBrackets(String type, String text) {
        assert type.length() == 2;
        char open = type.charAt(0);
        char close = type.charAt(1);

        if (text.indexOf(open) != 0)
            return Optional.empty();

        int nesting = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == open)
                nesting++;
            else if (c == close)
                nesting--;
            if (nesting < 0)
                throw new IllegalArgumentException("mismatched closing brackets");
            if (nesting == 0)
                return Optional.of(text.substring(1, i));
        }
        return Optional.empty();
    }


    private class LiteralResolver extends Resolver {
        public LiteralResolver(String expression) { set(findBrackets("«»", expression)); }
    }


    private class VariableResolver extends Resolver {
        public VariableResolver(String expression) {
            Matcher matcher = NAME_TOKEN.matcher(expression);
            this.match = matcher.matches();
            if (match) {
                VariableName variableName = new VariableName(matcher.group());
                if (variables.containsKey(variableName)) {
                    log.trace("did resolve [{}]", variableName);
                    this.value = resolve(variables.get(variableName), "null");
                    if (value != null && !VARIABLE_VALUE.matcher(value).matches())
                        throw badRequest("invalid character in variable value for [" + variableName + "]");
                } else {
                    log.trace("undefined variable [{}]", expression);
                    setNoMatch();
                }
            } else {
                setNoMatch();
            }
        }
    }


    private static final String ROOT_BUNDLE = "root-bundle:";
    private static final ImmutableMap<String, Function<RootBundleConfig, String>> BUNDLE = ImmutableMap.of(
            "group-id", c -> (c.getGroupId() == null) ? null : c.getGroupId().getValue(),
            "artifact-id", c -> (c.getArtifactId() == null) ? null : c.getArtifactId().getValue(),
            "classifier", c -> (c.getClassifier() == null) ? null : c.getClassifier().getValue(),
            "version", c -> (c.getVersion() == null) ? null : c.getVersion().getValue());

    private class RootBundleResolver extends Resolver {
        public RootBundleResolver(String expression) {
            if (expression.startsWith(ROOT_BUNDLE)
                    && BUNDLE.containsKey(fieldName(expression))
                    && rootBundle != null)
                set(Optional.ofNullable(resolve(fieldName(expression))));
            else
                setNoMatch();
        }

        private String fieldName(String expression) { return expression.substring(ROOT_BUNDLE.length()); }

        private String resolve(String fieldName) {
            String subExpression = BUNDLE.get(fieldName).apply(rootBundle);
            return (subExpression == null) ? null : Expressions.this.resolve(subExpression, null);
        }
    }

    private static final Pattern FUNCTION = Pattern.compile("(?<name>" + NAME_TOKEN + ")" + "(\\((?<body>.*)\\))");

    private class FunctionResolver extends Resolver {
        private String functionName;
        private List<Supplier<String>> params;

        public FunctionResolver(String expression) {
            Matcher matcher = FUNCTION.matcher(expression);
            this.match = matcher.matches(); // will be overwritten when function doesn't resolve. see #fail()
            this.functionName = match ? matcher.group("name") : null;
            this.params = match ? params(matcher.group("body")) : null;
            this.value = match ? resolve() : null;
        }

        private List<Supplier<String>> params(CharSequence body) {
            return split(body, ",")
                    .stream()
                    .map(String::trim)
                    .map(expression -> (Supplier<String>) () -> new OrResolver(expression).getValue())
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
            case "decrypt#1":
                return apply1(this::decrypt);
            case "decrypt#2":
                return apply2(this::decrypt);
            case "regex#2":
                return apply2(this::regex);
            default:
                throw badRequest("undefined function [" + functionName + "] with " + params.size() + " params");
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

        private String decrypt(String text) { return CipherFacade.decrypt(text, keyStore); }

        private String decrypt(String text, String alias) {
            return CipherFacade.decrypt(text, keyStore.withAlias(alias));
        }

        private String regex(String text, String pattern) {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            return matcher.matches() ? matcher.group(1) : fail();
        }

        private String fail() {
            setNoMatch();
            return null;
        }
    }

    private static List<String> split(CharSequence expression, String pattern) {
        List<String> list = new ArrayList<>();
        String current = "";
        if (expression.length() > 0)
            for (String split : expression.toString().split(pattern)) {
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

    public Expressions withRootBundle(RootBundleConfig rootBundle) {
        return new Expressions(this.variables, rootBundle, this.keyStore);
    }

    public Expressions withKeyStore(KeyStoreConfig keyStore) {
        return new Expressions(this.variables, this.rootBundle, keyStore);
    }

    public Expressions with(VariableName name, String value) {
        checkNotDefined(name);
        return new Expressions(
                ImmutableMap.<VariableName, String>builder().putAll(this.variables).put(name, value).build(),
                this.rootBundle,
                this.keyStore);
    }

    public Expressions withAllNew(Map<VariableName, String> variables) {
        return withAll(variables, true);
    }

    public Expressions withAllReplacing(Map<VariableName, String> variables) {
        return withAll(variables, false);
    }

    private Expressions withAll(Map<VariableName, String> variables, boolean checkNotDefined) {
        if (variables == null || variables.isEmpty())
            return this;
        Map<VariableName, String> builder = new LinkedHashMap<>(this.variables);
        for (Map.Entry<VariableName, String> entry : variables.entrySet()) {
            VariableName name = entry.getKey();
            if (checkNotDefined)
                checkNotDefined(name);
            builder.put(name, entry.getValue());
        }
        return new Expressions(ImmutableMap.copyOf(builder), this.rootBundle, this.keyStore);
    }

    private void checkNotDefined(VariableName name) {
        if (this.variables.containsKey(name))
            throw badRequest("Variable named [" + name + "] already set. It's not allowed to overwrite.");
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
