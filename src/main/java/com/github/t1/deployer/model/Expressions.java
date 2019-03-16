package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.github.t1.deployer.tools.CipherFacade;
import com.github.t1.deployer.tools.KeyStoreConfig;
import com.github.t1.problem.ReturnStatus;
import com.github.t1.problem.WebApplicationApplicationException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Wither;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;
import static com.github.t1.deployer.model.Expressions.Match.Mode.matches;
import static com.github.t1.deployer.model.Expressions.Match.Mode.proceed;
import static com.github.t1.deployer.model.Expressions.Match.Mode.stop;
import static com.github.t1.problem.WebException.badRequest;
import static java.util.Collections.singletonMap;
import static java.util.Locale.US;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static lombok.AccessLevel.PRIVATE;

@Slf4j
@RequiredArgsConstructor
@Wither
public class Expressions {
    private static final Pattern NAME_TOKEN = Pattern.compile("[-._a-zA-Z0-9]{1,256}");

    @Value
    @NoArgsConstructor(access = PRIVATE, force = true)
    @JsonSerialize(using = ToStringSerializer.class)
    public static class VariableName implements Comparable<VariableName> {
        @NonNull String value;

        @JsonCreator(mode = DELEGATING) public VariableName(@NonNull String value) { this.value = check(value); }

        private static String check(String value) {
            if (!NAME_TOKEN.matcher(value).matches())
                throw new IllegalArgumentException("invalid variable name [" + value + "]");
            return value;
        }

        @Override public String toString() { return value; }

        @Override public int compareTo(@NotNull VariableName that) { return this.value.compareTo(that.value); }
    }

    @SneakyThrows(UnknownHostException.class)
    public static String hostName() { return InetAddress.getLocalHost().getHostName().split("\\.")[0]; }

    @SneakyThrows(UnknownHostException.class)
    public static String domainName() {
        String[] split = InetAddress.getLocalHost().getHostName().split("\\.", 2);
        return (split.length == 2) ? split[1] : null;
    }

    private static final Pattern VAR = Pattern.compile("\\$\\{([^}]*)}");
    private static final Pattern VARIABLE_VALUE = Pattern.compile("[- ._a-zA-Z0-9?*:|\\\\{}()\\[\\]]{1,256}");

    private final ImmutableMap<VariableName, String> variables;
    private final RootBundleConfig rootBundleConfig;
    private final KeyStoreConfig keyStore;
    private final Resolver finalResolver;

    public Expressions() { this(ImmutableMap.copyOf(systemProperties()), null, null, null); }

    private static Map<VariableName, String> systemProperties() {
        return System.getProperties().stringPropertyNames().stream()
            .filter(name -> NAME_TOKEN.matcher(name).matches())
            .collect(toMap(VariableName::new, System::getProperty));
    }

    public Expressions with(VariableName name, String value) { return withAllNew(singletonMap(name, value)); }

    public Expressions withAllNew(Map<VariableName, String> variables) { return withAll(variables, true); }

    public Expressions withAllReplacing(Map<VariableName, String> variables) { return withAll(variables, false); }

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
        return withVariables(ImmutableMap.copyOf(builder));
    }

    private void checkNotDefined(VariableName name) {
        if (this.variables.containsKey(name))
            throw badRequest("Variable named [" + name + "] already set. It's not allowed to overwrite.");
    }


    public boolean contains(VariableName name) { return variables.containsKey(name); }


    /**
     * Replaces all expressions starting with `${` and ending with `}` - may be escaped with a second `$`,
     * i.e. `$${a}` will be replaced by `${a}`.
     */
    public String resolve(String line) { return resolve(line, null); }

    public String resolve(String line, String alternative) {
        StringBuilder out = new StringBuilder();
        if (line.contains("#"))
            line = line.substring(0, line.indexOf('#'));
        Matcher matcher = VAR.matcher(line);
        int tail = 0;
        boolean hasNullValue = false;
        while (matcher.find()) {
            out.append(line, tail, matcher.start());
            if (matcher.start() > 0 && line.charAt(matcher.start() - 1) == '$') {
                // +1 to skip the var-$ as we already copied the escape-$
                out.append(line, matcher.start() + 1, matcher.end());
            } else {
                String expression = groupOneOr(matcher, alternative);
                Match match = resolver().match(expression);
                String value = match.orElseThrow(() -> new UnresolvedVariableException(expression));
                if (value == null)
                    hasNullValue = true;
                else
                    out.append(value);
            }
            tail = matcher.end();
        }
        out.append(line.substring(tail));
        return (hasNullValue && out.length() == 0) ? null : out.toString();
    }

    private String groupOneOr(Matcher matcher, String alternative) {
        String expression = matcher.group(1);
        if (alternative != null)
            expression += " or " + alternative;
        return expression;
    }

    public Resolver resolver() {
        ImmutableList.Builder<Resolver> resolvers = ImmutableList.builder();
        resolvers.add(new NullResolver());
        resolvers.add(new BooleanResolver());
        resolvers.add(new SwitchResolver());
        resolvers.add(new LiteralResolver());
        resolvers.add(new FunctionResolver());
        resolvers.add(new RootBundleResolver());
        resolvers.add(new VariableResolver());
        if (finalResolver != null)
            resolvers.add(finalResolver);
        return new OrResolver(resolvers.build());
    }

    public interface Resolver {
        Match match(String expression);
    }

    /** Like <code>Optional&lt;String&gt;</code>, but may contain <code>null</code> */
    @Value
    public static class Match {
        public enum Mode {matches, proceed, stop}

        public static final Match PROCEED = new Match(proceed, null);
        public static final Match STOP = new Match(stop, null);

        public static Match of(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> optional) {
            return optional.map(Match::of).orElse(Match.PROCEED);
        }

        public static Match of(String value) { return new Match(matches, value); }

        Mode mode;
        String value;

        String orElseThrow(Supplier<? extends RuntimeException> supplier) {
            if (mode == matches)
                return value;
            throw supplier.get();
        }

        String getValueOrNull() { return (mode == matches) ? value : null; }
    }

    @RequiredArgsConstructor
    private static class OrResolver implements Resolver {
        private final ImmutableList<Resolver> resolvers;

        @Override public Match match(String expression) {
            for (String subExpression : split(expression, " or ")) {
                log.trace("try to resolve variable expression [{}]", subExpression);
                resolvers:
                for (Resolver resolver : resolvers) {
                    Match match = resolver.match(subExpression);
                    switch (match.mode) {
                        case matches:
                            return match;
                        case stop:
                            break resolvers;
                        case proceed:
                            //noinspection UnnecessaryContinue
                            continue resolvers;
                    }
                }
            }
            return Match.PROCEED;
        }
    }


    private static class NullResolver implements Resolver {
        @Override public Match match(String expression) {
            return "null".equals(expression) ? Match.of((String) null) : Match.PROCEED;
        }
    }


    private static class BooleanResolver implements Resolver {
        @Override public Match match(String expression) {
            return ("true".equals(expression) || "false".equals(expression)) ? Match.of(expression) : Match.PROCEED;
        }
    }


    private class SwitchResolver implements Resolver {
        @Override public Match match(String expression) {
            if (!expression.startsWith("switch"))
                return Match.PROCEED;
            return Match.of(doSwitch(expression.substring(6)));
        }

        private String doSwitch(String expression) {
            String head = findBrackets("()", expression)
                .orElseThrow(() -> new IllegalArgumentException("unmatched brackets for switch statement"));
            String value = resolver().match(head).orElseThrow(() ->
                new IllegalArgumentException("no variable defined in switch header: '" + head + "'"));
            String body = expression.substring(head.length() + 2);
            int i = body.indexOf(" " + value + ":");
            if (i < 0)
                throw new IllegalArgumentException("no case label for '" + value + "' in switch statement");
            String rest = body.substring(i + value.length() + 2).trim();
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


    private static class LiteralResolver implements Resolver {
        @Override public Match match(String expression) {
            return Match.of(findBrackets("«»", expression));
        }
    }


    private class VariableResolver implements Resolver {
        @Override public Match match(String expression) {
            Matcher matcher = NAME_TOKEN.matcher(expression);
            if (!matcher.matches())
                return Match.PROCEED;
            VariableName variableName = new VariableName(matcher.group());
            if (variables.containsKey(variableName)) {
                String value = resolve(variables.get(variableName), "null");
                if (value != null && !VARIABLE_VALUE.matcher(value).matches())
                    throw badRequest("invalid character in variable value for [" + variableName + "]");
                return Match.of(value);
            } else {
                log.trace("undefined variable [{}]", expression);
                return Match.PROCEED;
            }
        }
    }


    private static final String ROOT_BUNDLE = "root-bundle:";
    private static final ImmutableMap<String, Function<RootBundleConfig, String>> BUNDLE = ImmutableMap.of(
        "group-id", c -> (c.getGroupId() == null) ? null : c.getGroupId().getValue(),
        "artifact-id", c -> (c.getArtifactId() == null) ? null : c.getArtifactId().getValue(),
        "classifier", c -> (c.getClassifier() == null) ? null : c.getClassifier().getValue(),
        "version", c -> (c.getVersion() == null) ? null : c.getVersion().getValue());

    private class RootBundleResolver implements Resolver {
        @Override public Match match(String expression) {
            if (!expression.startsWith(ROOT_BUNDLE))
                return Match.PROCEED;
            String fieldName = expression.substring(ROOT_BUNDLE.length());
            if (!BUNDLE.containsKey(fieldName))
                throw new IllegalArgumentException("undefined root-bundle expression: [" + expression + "]");
            if (rootBundleConfig == null)
                return Match.STOP;
            String subExpression = BUNDLE.get(fieldName).apply(rootBundleConfig);
            if (subExpression == null)
                return Match.STOP;
            return Match.of(Expressions.this.resolve(subExpression));
        }

    }

    private static final Pattern FUNCTION = Pattern.compile("(?<name>" + NAME_TOKEN + ")" + "(\\((?<body>.*)\\))");

    private class FunctionResolver implements Resolver {
        private final CipherFacade cipher = new CipherFacade();

        @Override public Match match(String expression) {
            Matcher matcher = FUNCTION.matcher(expression);
            if (!matcher.matches())
                return Match.PROCEED;
            return new FunctionMatch(matcher.group("name"), params(matcher.group("body"))).match();
        }

        @Value
        private class FunctionMatch {
            private final String functionName;
            private final List<Supplier<String>> params;

            private Match match() {
                log.trace("found function name [{}] with {} params", functionName, params.size());
                switch (functionName + "#" + params.size()) {
                    case "hostName#0":
                        return Match.of(hostName());
                    case "domainName#0":
                        return Match.of(domainName());
                    case "toUpperCase#1":
                        return apply1(s -> s.toUpperCase(US));
                    case "toLowerCase#1":
                        return apply1(s -> s.toLowerCase(US));
                    case "toInitCap#1":
                        return apply1(this::toInitCap);
                    case "decrypt#1":
                        return apply1(this::decrypt);
                    case "decrypt#2":
                        return apply2(this::decrypt);
                    case "regex#2":
                        return applyRegex();
                    default:
                        throw badRequest("undefined function [" + functionName + "] with " + params.size() + " params");
                }
            }

            private Match apply1(Function<String, String> function) {
                return Match.of(param(0).map(function));
            }

            private Match apply2(BiFunction<String, String, String> function) {
                Optional<String> param0 = param(0);
                Optional<String> param1 = param(1);
                return (param0.isPresent() && param1.isPresent())
                    ? Match.of(function.apply(param0.get(), param1.get()))
                    : Match.PROCEED;
            }

            private Optional<String> param(int index) { return Optional.ofNullable(params.get(index).get()); }

            private String toInitCap(String text) {
                return (text.length() == 0) ? "" : (Character.toUpperCase(text.charAt(0)) + text.substring(1));
            }

            private String decrypt(String text) { return cipher.decrypt(text, keyStore); }

            private String decrypt(String text, String alias) {
                return cipher.decrypt(text, keyStore.setAlias(alias));
            }

            private Match applyRegex() {
                Optional<String> text = param(0);
                Optional<Pattern> pattern = param(1).map(Pattern::compile);
                if (!text.isPresent() || !pattern.isPresent())
                    return Match.PROCEED;
                Matcher matcher = pattern.get().matcher(text.get());
                return matcher.matches() ? Match.of(matcher.group(1)) : Match.PROCEED;
            }
        }

        private List<Supplier<String>> params(CharSequence body) {
            return split(body, ",")
                .stream()
                .map(String::trim)
                .map(expression -> (Supplier<String>) () -> resolver().match(expression).getValue())
                .collect(toList());
        }
    }

    /** Like String#split, but considering round braces */
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
        do
        {
            ++n;
            offset = string.indexOf(c, offset + 1);
        } while (offset >= 0);
        return n;
    }

    @ReturnStatus(BAD_REQUEST)
    public static class UnresolvedVariableException extends WebApplicationApplicationException {
        private static final long serialVersionUID = -1L;

        @Getter private final String expression;

        private UnresolvedVariableException(String expression) {
            super("unresolved variable expression: " + expression);
            this.expression = expression;
        }
    }
}
