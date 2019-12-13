package com.github.t1.deployer.tools;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import static com.github.t1.deployer.tools.StringUtils.typeString;
import static java.util.Collections.emptyMap;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;

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
            throw new BadRequestException("Please specify a `Content-Type` header when sending a body. "
                    + "This MessageBodyReader can only \"read\" an empty map.");
        return emptyMap();
    }
}
