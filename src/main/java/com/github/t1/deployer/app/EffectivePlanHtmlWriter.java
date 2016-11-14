package com.github.t1.deployer.app;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.t1.deployer.model.Plan;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.*;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.github.t1.deployer.model.Expressions.*;
import static java.util.Collections.*;
import static javax.ws.rs.core.MediaType.*;

@Provider
@Produces(TEXT_HTML)
public class EffectivePlanHtmlWriter implements MessageBodyWriter<Plan> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return Plan.class.equals(type);
    }

    @Override
    public long getSize(Plan p, Class<?> t, Type g, Annotation[] a, MediaType m) { return -1; }

    @SuppressWarnings("resource") @Override
    public void writeTo(Plan plan, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        PrintWriter out = new PrintWriter(entityStream);
        String title = hostName() + "-plan";
        new HtmlWriter(out, title).writeObject(plan);
    }

    private static class HtmlWriter extends GeneratorBase {
        private static final ObjectMapper MAPPER = new ObjectMapper()
                .setSerializationInclusion(NON_EMPTY)
                .findAndRegisterModules();

        private final PrintWriter out;
        private String title;

        private int depth = 0;
        private String rowHeader = null;
        private String key = null;
        private String parentKey = null;
        private Map<String, Object> row = new LinkedHashMap<>();

        private HtmlWriter(PrintWriter out, String title) {
            super(0, MAPPER);
            this.out = out;
            this.title = title;
        }

        @Override public void writeObject(Object value) throws IOException {
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
            super.writeObject(value);
            out.print("</body>\n"
                    + "</html>\n");
            flush();
        }

        public void endRow() {
            if (rowHeader != null)
                printRow();

            rowHeader = null;
            row.clear();
        }

        public void printRow() {
            for (Map.Entry<String, Object> col : row.entrySet()) {
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

        public void printHeader(PrintWriter out, String header) {
            out.print(""
                    + "<table>\n"
                    + "    <tr>\n"
                    + "        <th colspan=\"3\">" + header + "</th>\n"
                    + "    </tr>\n");
        }

        @Override public void writeStartArray() throws IOException {}

        @Override public void writeEndArray() throws IOException {}

        @Override public void writeStartObject() throws IOException { ++depth; }

        @Override public void writeEndObject() throws IOException {
            --depth;
            endRow();
            if (depth == 1) {
                out.println("</table>\n");
            }
        }

        @Override public void writeFieldName(String name) throws IOException {
            switch (depth) {
            case 1:
                printHeader(out, name);
                rowHeader = null;
                break;
            case 2:
                endRow();
                rowHeader = name;
                break;
            case 3:
                parentKey = key = name;
                break;
            default:
                key = parentKey + ":" + name;
                break;
            }
        }


        private void add(Object text) {
            Object value;
            if (row.containsKey(key)) {
                Object tmp = row.get(key);
                List<Object> list = (tmp instanceof List) ? (List) tmp : new ArrayList<>(singleton(tmp));
                list.add(text);
                value = list;
            } else
                value = text;
            row.put(key, value);
        }

        @Override public void writeString(String text) { add(text); }

        @Override public void writeString(char[] text, int offset, int len) { add(new String(text, offset, len)); }

        @Override public void writeRawUTF8String(byte[] text, int offset, int length) {
            add(new String(text, offset, length));
        }

        @Override public void writeUTF8String(byte[] text, int offset, int length) {
            add(new String(text, offset, length));
        }

        @Override public void writeRaw(String value) { add(value); }

        @Override public void writeRaw(String text, int offset, int len) { add(text.substring(offset, offset + len)); }

        @Override public void writeRaw(char[] text, int offset, int len) { add(new String(text, offset, len)); }

        @Override public void writeRaw(char c) { add(c); }

        @Override public void writeBinary(Base64Variant bv, byte[] data, int offset, int len) {}

        @Override public void writeNumber(int value) { add(value); }

        @Override public void writeNumber(long value) { add(value); }

        @Override public void writeNumber(BigInteger value) { add(value); }

        @Override public void writeNumber(double value) { add(value); }

        @Override public void writeNumber(float value) { add(value); }

        @Override public void writeNumber(BigDecimal value) { add(value); }

        @Override public void writeNumber(String value) { add(value); }

        @Override public void writeBoolean(boolean value) { add(value); }

        @Override public void writeNull() {}

        @Override public void flush() throws IOException { out.flush(); }

        @Override protected void _releaseBuffers() {}

        @Override protected void _verifyValueWrite(String typeMsg) {}
    }
}
