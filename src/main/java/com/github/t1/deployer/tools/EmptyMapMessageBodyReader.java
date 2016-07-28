package com.github.t1.deployer.tools;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import static com.github.t1.deployer.tools.StringUtils.*;
import static com.github.t1.problem.WebException.*;
import static java.util.Collections.*;
import static javax.ws.rs.core.MediaType.*;

@Slf4j
@Provider
@Consumes(WILDCARD)
public class EmptyMapMessageBodyReader implements MessageBodyReader<Map<String, String>> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean readable = WILDCARD_TYPE.equals(mediaType) || APPLICATION_OCTET_STREAM_TYPE.equals(mediaType);
        log.debug("isReadable: {}: {}: {} -> {}", typeString(genericType), annotations, mediaType, readable);
        return readable;
    }

    @Override
    public Map<String, String> readFrom(Class<Map<String, String>> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException {
        log.debug("readFrom: {}: {}: {}: {}", typeString(genericType), annotations, mediaType, httpHeaders);
        if (entityStream.read() >= 0)
            throw badRequest("Please specify a `Content-Type` header when sending a body. "
                    + "This MessageBodyReader can only \"read\" an empty map.");
        return emptyMap();
    }
}
