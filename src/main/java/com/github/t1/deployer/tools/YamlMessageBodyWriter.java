package com.github.t1.deployer.tools;

import static javax.ws.rs.core.MediaType.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.github.t1.rest.fallback.ConverterTools;

@Provider
@Produces(WILDCARD)
public class YamlMessageBodyWriter implements MessageBodyWriter<Object> {
    private final Yaml yaml = new Yaml();

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return ConverterTools.isConvertible(type) && ConverterTools.isApplicationType(mediaType, "yaml");
    }

    @Override
    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) {
        yaml.dump(t, new OutputStreamWriter(entityStream));
    }
}
