package com.github.t1.deployer.tools;

import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

import static com.github.t1.deployer.tools.StringUtils.*;
import static javax.ws.rs.core.MediaType.*;

@Slf4j
@Provider
@Consumes(APPLICATION_FORM_URLENCODED)
public class FormMessageBodyReader implements MessageBodyReader<Map<String, String>> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean readable = APPLICATION_FORM_URLENCODED_TYPE.equals(mediaType);
        log.debug("isReadable: {}: {}: {} -> {}", typeString(genericType), annotations, mediaType, readable);
        return readable;
    }

    @Override
    public Map<String, String> readFrom(Class<Map<String, String>> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException {
        log.debug("readFrom: {}: {}: {}: {}", typeString(genericType), annotations, mediaType, httpHeaders);
        Map<String, String> map = new LinkedHashMap<>();
        String string = read(entityStream);
        for (String pair : string.split("&")) {
            if (pair.isEmpty())
                continue;
            String[] split = pair.split("=", 2);
            if (split.length < 2)
                continue;
            map.put(split[0], split[1]);
        }
        return map;
    }

    protected String read(InputStream entityStream) {
        try {
            return new Scanner(entityStream).useDelimiter("\\Z").next();
        } catch (NoSuchElementException e) {
            return "";
        }
    }
}
