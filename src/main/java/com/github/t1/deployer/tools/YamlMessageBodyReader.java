package com.github.t1.deployer.tools;

import com.github.t1.rest.fallback.ConverterTools;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.github.t1.deployer.tools.StringUtils.*;
import static javax.ws.rs.core.MediaType.*;

@Slf4j
@Provider
@Consumes(WILDCARD)
public class YamlMessageBodyReader implements MessageBodyReader<Object> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean readable = ConverterTools.isConvertible(type) && ConverterTools.isApplicationType(mediaType, "yaml");
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
