package com.github.t1.deployer.repository;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXB;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_XML;

@Provider
@Consumes({APPLICATION_XML, TEXT_XML})
public class XmlMessageBodyReader implements MessageBodyReader<Object> {
    @Override public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) { return true; }

    @Override public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) {
        return JAXB.unmarshal(entityStream, type);
    }
}
