package com.github.t1.deployer.app.html;

import static javax.ws.rs.core.MediaType.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;

import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.junit.Before;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public abstract class AbstractHtmlWriterTest<T> {
    @InjectMocks
    MessageBodyWriter<T> writer;
    @Mock
    UriInfo uriInfo;

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
    }

    protected String write(T target) throws IOException {
        Type generic = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        assertTrue(writer.isWriteable(target.getClass(), generic, null, TEXT_HTML_TYPE));

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();

        writer.writeTo(target, target.getClass(), generic, null, null, headers, entityStream);

        assertEquals("[DENY]", headers.get("X-Frame-Options").toString());
        assertEquals("[1; mode=block]", headers.get("X-XSS-Protection").toString());
        assertEquals("[nosniff]", headers.get("X-Content-Type-Options").toString());

        return new String(entityStream.toByteArray());
    }

    protected String readFile() throws IOException, URISyntaxException {
        StackTraceElement caller = new RuntimeException().getStackTrace()[1];
        assertEquals(getClass().getName(), caller.getClassName());
        String fileName = getClass().getSimpleName() + "#" + caller.getMethodName() + ".html";
        URL resource = getClass().getResource(fileName);
        if (resource == null)
            throw new AssertionError("test file not found: " + fileName);
        Path path = Paths.get(resource.toURI());
        return new String(Files.readAllBytes(path));
    }
}
