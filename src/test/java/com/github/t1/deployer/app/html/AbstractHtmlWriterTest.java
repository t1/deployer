package com.github.t1.deployer.app.html;

import static java.util.Arrays.*;
import static javax.ws.rs.core.MediaType.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.security.Principal;
import java.util.*;
import java.util.regex.*;

import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.junit.Before;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public abstract class AbstractHtmlWriterTest<T> {
    private static final Pattern VAR_DEF = Pattern.compile("\\s*(?<name>.*?)\\s*=\\s*\"(?<value>.*?)\"\\s*");
    @InjectMocks
    MessageBodyWriter<T> writer;
    @Mock
    UriInfo uriInfo;
    @Mock
    Principal principal;

    public AbstractHtmlWriterTest(MessageBodyWriter<T> writer) {
        this.writer = writer;
    }

    @Before
    public void setup() {
        when(uriInfo.getBaseUriBuilder()).then(new Answer<UriBuilder>() {
            @Override
            public UriBuilder answer(InvocationOnMock invocation) {
                return new JerseyUriBuilder().uri("http://localhost:8080/deployer");
            }
        });
        when(principal.getName()).thenReturn("Joe Doe");
    }

    protected String write(T target) throws IOException {
        Type generic = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        assertTrue("is writeable: " + generic,
                writer.isWriteable(target.getClass(), generic, null, TEXT_HTML_TYPE));

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();

        writer.writeTo(target, target.getClass(), generic, null, null, headers, entityStream);

        assertEquals("[DENY]", headers.get("X-Frame-Options").toString());
        assertEquals("[1; mode=block]", headers.get("X-XSS-Protection").toString());
        assertEquals("[nosniff]", headers.get("X-Content-Type-Options").toString());

        return new String(entityStream.toByteArray());
    }

    protected String readFile() throws IOException, URISyntaxException {
        String fileName = callerToHtmlFileName();
        List<String> lines = readFile(fileName);
        Map<String, String> variables = new HashMap<>();
        replaceImports(lines, variables);
        replaceVariables(lines, variables);
        return String.join("\n", lines) + "\n";
    }

    private String callerToHtmlFileName() {
        StackTraceElement caller = new RuntimeException().getStackTrace()[2];
        assertEquals(getClass().getName(), caller.getClassName());
        return getClass().getSimpleName() + "#" + caller.getMethodName() + ".html";
    }

    private List<String> readFile(String fileName) throws AssertionError, URISyntaxException, IOException {
        URL resource = getClass().getResource(fileName);
        if (resource == null)
            throw new AssertionError("test file not found: " + fileName);
        Path path = Paths.get(resource.toURI());
        return Files.readAllLines(path);
    }

    private void replaceImports(List<String> lines, Map<String, String> variables)
            throws URISyntaxException, IOException {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("@include ")) {
                String[] words = line.split("\\s");
                String includeFileName = words[1];

                readVariables(String.join(" ", asList(words).subList(2, words.length)), variables);
                List<String> includedLines = readFile(includeFileName);

                lines.remove(i);
                for (String includedLine : includedLines)
                    lines.add(i++, includedLine);
            }
        }
    }

    private void readVariables(String string, Map<String, String> variables) {
        Matcher matcher = VAR_DEF.matcher(string);
        while (matcher.find())
            variables.put(matcher.group("name"), matcher.group("value"));
    }

    private void replaceVariables(List<String> lines, Map<String, String> variables) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = Pattern.compile("\\$\\{(?<name>.*?)\\}").matcher(line);
            StringBuffer replaced = new StringBuffer();
            while (matcher.find()) {
                String replacement = variables.get(matcher.group("name"));
                matcher.appendReplacement(replaced, (replacement == null) ? "" : replacement);
            }
            matcher.appendTail(replaced);
            lines.set(i, replaced.toString());
        }
    }
}
