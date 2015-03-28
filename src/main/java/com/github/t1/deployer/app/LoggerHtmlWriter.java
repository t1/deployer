package com.github.t1.deployer.app;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.model.LoggerConfig;

@Provider
public class LoggerHtmlWriter extends AbstractHtmlWriter<LoggerConfig> {
    public LoggerHtmlWriter() {
        super(LoggerConfig.class);
    }

    @Override
    protected String title() {
        return "Logger: " + target.getCategory();
    }

    @Override
    protected String body() {
        out.append("<a href=\"" + Loggers.base(uriInfo) + "\">&lt;</a>");
        out.append("<br/>\n");
        out.append("    Name: ").append(target.getCategory()).append("<br/>\n");
        out.append("    Level: ").append(target.getLevel()).append("<br/>\n");
        out.append("<br/>\n");
        out.append(delete());
        return out.toString();
    }

    private String delete() {
        return "<form method=\"POST\" action=\"" + Loggers.path(uriInfo, target) + "\">\n" //
                + "  <input type=\"hidden\" name=\"action\" value=\"delete\">\n" //
                + "  <input type=\"submit\" value=\"Delete\">\n" //
                + "</form>";
    }
}
