package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.model.LoggerConfig.*;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.Loggers;
import com.github.t1.deployer.model.LoggerConfig;

@Provider
public class LoggerHtmlWriter extends AbstractHtmlWriter<LoggerConfig> {
    public LoggerHtmlWriter() {
        super(LoggerConfig.class, LOGGERS);
    }

    private boolean isNew() {
        return NEW_LOGGER.equals(target.getCategory());
    }

    @Override
    protected String bodyTitle() {
        return isNew() ? "Add Logger" : target.getCategory();
    }

    @Override
    protected String title() {
        return isNew() ? "Add Logger" : "Logger: " + target.getCategory();
    }

    @Override
    protected void body() {
        href("&lt", Loggers.base(uriInfo));
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
    }

    private String delete() {
        return "<form method=\"POST\" action=\"" + Loggers.path(uriInfo, target) + "\">\n" //
                + "  <input type=\"hidden\" name=\"action\" value=\"delete\">\n" //
                + "  <input type=\"submit\" value=\"Delete\">\n" //
                + "</form>";
    }
}
