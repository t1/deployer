package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.Loggers;
import com.github.t1.deployer.model.LoggerConfig;

@Provider
public class LoggerListHtmlWriter extends AbstractListHtmlWriter<LoggerConfig> {
    public LoggerListHtmlWriter() {
        super(LoggerConfig.class, LOGGERS);
    }

    @Override
    protected String title() {
        return "Loggers";
    }

    @Override
    protected void body() {
        append("    <table>\n");
        for (LoggerConfig logger : target) {
            append("        <tr><td>");
            href(logger.getCategory(), Loggers.path(uriInfo, logger));
            append("</td><td>");
            append(logger.getLevel());
            append("</td><td>");
            append(delete(logger));
            append("</td></tr>\n");
        }
        append("    <tr><td colspan='3'>");
        href("+", Loggers.newLogger(uriInfo));
        append("</td></tr>");
        append("    </table>\n");
    }

    private String delete(LoggerConfig logger) {
        return "<form method=\"POST\" action=\"" + Loggers.path(uriInfo, logger) + "\">\n" //
                + "  <input type=\"hidden\" name=\"action\" value=\"delete\">\n" //
                + "  <input type=\"submit\" value=\"Delete\">\n" //
                + "</form>";
    }
}
