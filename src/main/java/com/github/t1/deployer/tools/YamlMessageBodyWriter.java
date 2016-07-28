package com.github.t1.deployer.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.t1.rest.fallback.ConverterTools;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.*;
import static com.github.t1.deployer.tools.StringUtils.*;
import static javax.ws.rs.core.MediaType.*;

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
        boolean writable = ConverterTools.isConvertible(type) && ConverterTools.isApplicationType(mediaType, "yaml");
        log.debug("isWritable: {}: {}: {} -> {}", typeString(genericType), annotations, mediaType, writable);
        return writable;
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
