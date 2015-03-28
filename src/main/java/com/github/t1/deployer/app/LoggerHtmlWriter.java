package com.github.t1.deployer.app;

import static javax.ws.rs.core.MediaType.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;

import lombok.extern.slf4j.Slf4j;

import com.github.t1.deployer.model.LoggerConfig;

@Slf4j
@Provider
@Produces(TEXT_HTML)
public class LoggerHtmlWriter extends HtmlWriter implements MessageBodyWriter<LoggerConfig> {

    @Context
    UriInfo uriInfo;

    private LoggerConfig logger;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return LoggerConfig.class.equals(type);
    }

    @Override
    public long getSize(LoggerConfig t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(LoggerConfig logger, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        log.debug("write as html: {}", logger);
        uriInfo(uriInfo);
        this.logger = logger;
        Writer out = new OutputStreamWriter(entityStream);
        out.write(toString());
        out.flush();
    }

    @Override
    protected String title() {
        return "Logger: " + logger.getCategory();
    }

    @Override
    protected String body() {
        StringBuilder out = new StringBuilder();
        out.append("<a href=\"" + Loggers.base(uriInfo) + "\">&lt;</a>");
        out.append("<br/>\n");
        out.append("    Name: ").append(logger.getCategory()).append("<br/>\n");
        out.append("    Level: ").append(logger.getLevel()).append("<br/>\n");
        out.append("<br/>\n");
        out.append(delete());
        return out.toString();
    }

    private String delete() {
        return "<form method=\"POST\" action=\"" + Loggers.path(uriInfo, logger) + "\">\n" //
                + "  <input type=\"hidden\" name=\"action\" value=\"delete\">\n" //
                + "  <input type=\"submit\" value=\"Delete\">\n" //
                + "</form>";
    }
}
