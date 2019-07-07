package com.github.t1.deployer.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.MINIMIZE_QUOTES;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static com.github.t1.deployer.tools.StringUtils.typeString;
import static javax.ws.rs.core.MediaType.WILDCARD;

@Provider
@Produces(WILDCARD)
@Slf4j
public class YamlMessageBodyWriter implements MessageBodyWriter<Object> {
    static final ObjectMapper YAML = new ObjectMapper(
        new YAMLFactory()
            .enable(MINIMIZE_QUOTES)
            .disable(WRITE_DOC_START_MARKER))
        .setSerializationInclusion(NON_EMPTY)
        // .setPropertyNamingStrategy(KEBAB_CASE)
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .findAndRegisterModules();

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean writable = isConvertible(type) && isYaml(mediaType);
        log.debug("isWritable: {}: {}: {} -> {}", typeString(genericType), annotations, mediaType, writable);
        return writable;
    }

    static boolean isConvertible(Class<?> type) {
        return type != String.class && !Closeable.class.isAssignableFrom(type);
    }

    static boolean isYaml(MediaType mediaType) {
        return "application".equals(mediaType.getType())
            && ("yaml".equals(mediaType.getSubtype()) || mediaType.getSubtype().endsWith("+yaml"));
    }

    @Override
    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) {
        log.debug("writeTo: {}: {}: {}: {}", typeString(genericType), annotations, mediaType, httpHeaders);
        try {
            YAML.writeValue(entityStream, t);
        } catch (IOException e) {
            throw new RuntimeException("can't serialize to YAML", e);
        }
    }
}
