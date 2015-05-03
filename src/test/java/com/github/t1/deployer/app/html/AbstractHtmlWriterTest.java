package com.github.t1.deployer.app.html;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.*;
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
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        writer.writeTo(target, getClass(), getClass(), null, null, null, entityStream);
        return new String(entityStream.toByteArray());
    }

    protected String readFile() throws IOException, URISyntaxException {
        StackTraceElement caller = new RuntimeException().getStackTrace()[1];
        assertEquals(getClass().getName(), caller.getClassName());
        String fileName = getClass().getSimpleName() + "#" + caller.getMethodName();
        URL resource = getClass().getResource(fileName);
        if (resource == null)
            throw new AssertionError("test file not found: " + fileName);
        Path path = Paths.get(resource.toURI());
        return new String(Files.readAllBytes(path));
    }
}
