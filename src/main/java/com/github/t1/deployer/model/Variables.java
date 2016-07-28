package com.github.t1.deployer.model;

import lombok.SneakyThrows;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import static com.github.t1.problem.WebException.*;

public class Variables {
    private static final Pattern VAR = Pattern.compile("\\$\\{([^}]*)\\}");

    private Map<String, String> variables;

    private Map<String, String> variables() {
        if (variables == null)
            //noinspection unchecked
            variables = new HashMap<>((Map<String, String>) (Map) System.getProperties());
        return variables;
    }


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
                    String key = matcher.group(1);
                    out.append(variables().getOrDefault(key, "${" + key + "}"));
                }
                tail = matcher.end();
            }
            out.append(line.substring(tail));
            out.append('\n');
        }
        return new StringReader(out.toString());
    }

    private BufferedReader buffered(Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    }

    public void addAll(Map<String, String> variables) {
        if (variables != null)
            for (Map.Entry<String, String> entry : variables.entrySet())
                add(entry.getKey(), entry.getValue());
    }

    private void add(String key, String value) {
        if (variables().containsKey(key))
            throw badRequest("Variable named [" + key + "] already set. It's not allowed to overwrite.");
        variables().put(key, value);
    }
}
