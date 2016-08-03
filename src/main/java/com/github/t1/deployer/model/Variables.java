package com.github.t1.deployer.model;

import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;

import java.io.*;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.*;

import static com.github.t1.problem.WebException.*;
import static java.util.Locale.*;

public class Variables {
    private static final Pattern VAR = Pattern.compile("\\$\\{([^}]*)\\}");
    private static final Pattern FUNCTION = Pattern.compile("(?<function>[-._a-zA-Z0-9]*)(\\((?<variable>[^)]*)\\))?");

    private final ImmutableMap<String, String> variables;

    @SuppressWarnings("unchecked")
    public Variables() { this(ImmutableMap.copyOf((Map<String, String>) (Map) System.getProperties())); }

    public Variables(ImmutableMap<String, String> variables) { this.variables = variables; }


    /**
     * Reads from the reader, removes all comments (starting with a `#`), and replaces all variables
     * (starting with `${` and ending with `}` - may be escaped with a second `$`, i.e. `$${a}` will be replaced by `${a}`).
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
                    out.append(resolveVariable(matcher.group(1)));
                }
                tail = matcher.end();
            }
            out.append(line.substring(tail));
            out.append('\n');
        }
        return new StringReader(out.toString());
    }

    private String resolveVariable(String key) {
        Matcher matcher = FUNCTION.matcher(key);
        if (!matcher.matches())
            throw badRequest("unparseable variable key: " + key);
        String variableName = matcher.group("variable");
        Function<String, String> function;
        if (variableName == null) {
            function = Function.identity();
            variableName = matcher.group("function");
        } else {
            function = function(matcher.group("function"));
        }
        if (!variables.containsKey(variableName))
            throw badRequest("unresolved variable key: " + variableName);
        String value = variables.get(variableName);
        return function.apply(value);
    }

    private Function<String, String> function(String name) {
        switch (name) {
        case "toUpperCase":
            return value -> value.toUpperCase(US);
        case "toLowerCase":
            return value -> value.toLowerCase(US);
        default:
            throw badRequest("undefined variable function: [" + name + "]");
        }
    }

    private BufferedReader buffered(Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
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
}
