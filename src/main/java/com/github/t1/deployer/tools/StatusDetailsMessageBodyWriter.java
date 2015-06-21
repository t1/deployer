package com.github.t1.deployer.tools;

import static javax.ws.rs.core.MediaType.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;

/** TODO this shouldn't be necessary, but it is in JBoss */
@Provider
public class StatusDetailsMessageBodyWriter implements MessageBodyWriter<StatusDetails> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return StatusDetails.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(StatusDetails statusDetails, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(StatusDetails statusDetails, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        httpHeaders.putSingle("Content-Type", APPLICATION_JSON);
        OutputStreamWriter out = new OutputStreamWriter(entityStream);
        out.write(statusDetails.toJson());
        out.flush();
    }
}
