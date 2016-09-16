package com.github.t1.deployer.app;

import org.jetbrains.annotations.NotNull;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

import static com.github.t1.deployer.model.Variables.*;
import static javax.ws.rs.core.MediaType.*;

@Provider
@Produces(TEXT_HTML)
public class EffectivePlanHtmlWriter implements MessageBodyWriter<ConfigurationPlan> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ConfigurationPlan.class.equals(type);
    }

    @Override
    public long getSize(ConfigurationPlan plan, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(ConfigurationPlan plan, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        PrintWriter out = new PrintWriter(entityStream);
        String title = hostName() + "-config";
        out.print("<html>\n"
                + "<head>\n"
                + "    <style>\n"
                + "        body {\n"
                + "            font-family: \"Fira Code\", \"Courier New\", Courier, monospace;\n"
                + "            font-size: 14px;\n"
                + "        }\n"
                + "\n"
                + "        table {\n"
                + "            margin-top: 24pt;\n"
                + "            border: 1px solid rgb(221, 221, 221);\n"
                + "            border-collapse: collapse;\n"
                + "            box-sizing: border-box;\n"
                + "            color: rgb(51, 51, 51);\n"
                + "        }\n"
                + "\n"
                + "        tr {\n"
                + "            height: 37px;\n"
                + "        }\n"
                + "\n"
                + "        td {\n"
                + "            border: 1px solid rgb(221, 221, 221);\n"
                + "            border-collapse: collapse;\n"
                + "            padding: 8px 8px 0;\n"
                + "            vertical-align: top;\n"
                + "        }\n"
                + "    </style>\n"
                + "    <title>" + title + "</title>\n"
                + "</head>\n"
                + "<body>\n"
                + "<h1>" + title + "</h1>\n"
                + "\n");
        new PlanWriter(out, plan).write();
        out.print("</body>\n"
                + "</html>\n");
        out.flush();
    }

    private class PlanWriter {
        private final PrintWriter out;
        private final String[] lines;

        private boolean hasTable = false;
        private String rowHeader = null;
        private Map<String, String> row = new LinkedHashMap<>();

        private PlanWriter(PrintWriter out, ConfigurationPlan plan) {
            this.out = out;
            this.lines = plan.toYaml().split("\n");
        }

        public void write() {
            for (String line : lines) {
                if (line.startsWith("  ")) {
                    line = line.substring(2);
                    if (line.startsWith("  ")) {
                        line = line.substring(2);
                        String[] split = line.split(": ");
                        if (split.length == 2)
                            row.put(split[0], split[1]);
                    } else {
                        endRow();
                        rowHeader = stripTrailingColon(line);
                    }
                } else {
                    endTable();
                    line = stripTrailingColon(line);
                    printHeader(out, line);
                    rowHeader = null;
                    hasTable = true;
                }
            }
            endTable();
        }

        public void endTable() {
            if (hasTable) {
                endRow();
                out.println("</table>\n");
            }
        }

        public void endRow() {
            if (rowHeader != null)
                printRow();

            rowHeader = null;
            row.clear();
        }

        public void printRow() {
            for (Map.Entry<String, String> col : row.entrySet()) {
                out.print("    <tr>\n");
                if (rowHeader != null) {
                    out.print("        <td rowspan=\"" + row.size() + "\">" + rowHeader + "</td>\n");
                    rowHeader = null;
                }
                out.print(""
                        + "        <td>" + col.getKey() + "</td>\n"
                        + "        <td>" + col.getValue() + "</td>\n"
                        + "    </tr>\n");
            }
        }

        @NotNull public String stripTrailingColon(String line) {
            assert line.endsWith(":");
            line = line.substring(0, line.length() - 1);
            return line;
        }

        public void printHeader(PrintWriter out, String header) {
            out.print(""
                    + "<table>\n"
                    + "    <tr>\n"
                    + "        <th colspan=\"3\">" + header + "</th>\n"
                    + "    </tr>\n");
        }
    }
}
