package com.github.t1.deployer.app;

import static com.github.t1.deployer.app.Loggers.*;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.model.LoggerConfig;

@Provider
public class LoggerHtmlWriter extends AbstractHtmlWriter<LoggerConfig> {
    public LoggerHtmlWriter() {
        super(LoggerConfig.class);
    }

    private boolean isNew() {
        return NEW_LOGGER.equals(target.getCategory());
    }

    @Override
    protected String title() {
        return isNew() ? "Add Logger" : "Logger: " + target.getCategory();
    }

    @Override
    protected String body() {
        out.append("<a href=\"" + Loggers.base(uriInfo) + "\">&lt;</a>");
        if (isNew()) {
            out.append("<p>Enter the name of a new logger to configure</p>" //
                    + "<form method=\"POST\" action=\"" + Loggers.base(uriInfo) + "\">\n" //
                    + "  <input name=\"category\">\n" //
                    + "  <input name=\"level\">\n" //
                    + "  <input type=\"submit\" value=\"Add\">\n" //
                    + "</form>");
        } else {
            out.append("<br/>\n");
            out.append("    Name: ").append(target.getCategory()).append("<br/>\n");
            out.append("    Level: ").append(target.getLevel()).append("<br/>\n");
            out.append("<br/>\n");
            out.append(delete());
        }
        return out.toString();
    }

    private String delete() {
        return "<form method=\"POST\" action=\"" + Loggers.path(uriInfo, target) + "\">\n" //
                + "  <input type=\"hidden\" name=\"action\" value=\"delete\">\n" //
                + "  <input type=\"submit\" value=\"Delete\">\n" //
                + "</form>";
    }
}
