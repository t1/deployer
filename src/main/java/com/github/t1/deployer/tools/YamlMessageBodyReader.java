package com.github.t1.deployer.tools;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.github.t1.deployer.tools.StringUtils.typeString;
import static com.github.t1.deployer.tools.YamlMessageBodyWriter.isConvertible;
import static com.github.t1.deployer.tools.YamlMessageBodyWriter.isYaml;
import static javax.ws.rs.core.MediaType.WILDCARD;

@Slf4j
@Provider
@Consumes(WILDCARD)
public class YamlMessageBodyReader implements MessageBodyReader<Object> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean readable = isConvertible(type) && isYaml(mediaType);
        log.debug("isReadable: {}: {}: {} -> {}", typeString(genericType), annotations, mediaType, readable);
        return readable;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations,
                           MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException {
        return YamlMessageBodyWriter.YAML.readValue(entityStream, type);
    }
}
