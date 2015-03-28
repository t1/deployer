package com.github.t1.deployer.app;

import static javax.ws.rs.core.MediaType.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;

import lombok.extern.slf4j.Slf4j;

import com.github.t1.deployer.model.LoggerConfig;

@Slf4j
@Provider
@Produces(TEXT_HTML)
public class LoggerListHtmlWriter extends HtmlWriter implements MessageBodyWriter<List<LoggerConfig>> {

    @Context
    UriInfo uriInfo;

    private List<LoggerConfig> loggers;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return List.class.isAssignableFrom(type) //
                && genericType instanceof ParameterizedType //
                && ((ParameterizedType) genericType).getActualTypeArguments().length == 1 //
                && ((ParameterizedType) genericType).getActualTypeArguments()[0] instanceof Class //
                && LoggerConfig.class.isAssignableFrom((Class<?>) //
                        ((ParameterizedType) genericType).getActualTypeArguments()[0]);
    }

    @Override
    public long getSize(List<LoggerConfig> t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(List<LoggerConfig> logger, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        log.debug("write as html: {}", logger);
        uriInfo(uriInfo);
        this.loggers = logger;
        Writer out = new OutputStreamWriter(entityStream);
        out.write(toString());
        out.flush();
    }

    @Override
    protected String title() {
        return "Loggers";
    }

    @Override
    protected String body() {
        StringBuilder out = new StringBuilder();
        out.append("<a href=\"" + Deployments.pathAll(uriInfo) + "\">&lt;</a>");
        out.append("<br/><br/>\n");
        out.append("    <table>\n");
        for (LoggerConfig logger : loggers)
            out.append("        <tr>") //
                    .append("<td><a href=\"").append(Loggers.path(uriInfo, logger)).append("\">") //
                    .append(logger.getCategory()).append("</a>") //
                    .append("</td>") //
                    .append("<td>").append(logger.getLevel()).append("</td>") //
                    .append("<td>").append(delete(logger)).append("</td>") //
                    .append("</tr>\n");
        out.append("    <tr><td colspan='2'><a href=\"" + Loggers.newForm(uriInfo) + "\">+</a></td></tr>");
        out.append("    </table>\n");
        return out.toString();
    }

    private String delete(LoggerConfig logger) {
        return "<form method=\"POST\" action=\"" + Loggers.path(uriInfo, logger) + "\">\n" //
                + "  <input type=\"hidden\" name=\"action\" value=\"delete\">\n" //
                + "  <input type=\"submit\" value=\"Delete\">\n" //
                + "</form>";
    }
}
