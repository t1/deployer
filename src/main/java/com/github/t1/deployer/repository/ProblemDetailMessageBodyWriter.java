package com.github.t1.deployer.repository;

import com.github.t1.problem.ProblemDetail;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class ProblemDetailMessageBodyWriter implements MessageBodyWriter<ProblemDetail> {

    @Override public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ProblemDetail.class.equals(type);
    }

    @Override public void writeTo(ProblemDetail problemDetail, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) {
        JSONB.toJson(problemDetail, genericType, entityStream);
    }

    private static final Jsonb JSONB = JsonbBuilder.create();
}
