package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static java.util.Collections.*;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.Loggers;
import com.github.t1.deployer.model.LoggerConfig;

@Provider
public class LoggerListHtmlWriter extends AbstractListHtmlBodyWriter<LoggerConfig> {
    public LoggerListHtmlWriter() {
        super(LoggerConfig.class, LOGGERS);
    }

    @Override
    public void body() {
        append("<table>\n");
        in();
        sort(target);
        for (LoggerConfig logger : target) {
            append("<tr><td>");
            href(logger.getCategory(), Loggers.path(uriInfo, logger));
            out.append("</td><td>\n");
            in();
            buttons(logger);
            out();
            append("</td></tr>\n");
        }
        append("<tr><td colspan='3'>");
        href("+", Loggers.newLogger(uriInfo));
        out.append("</td></tr>\n");
        out();
        append("</table>\n");
    }

    private void buttons(LoggerConfig logger) {
        startForm(Loggers.path(uriInfo, logger));
        new LogLevelSelectForm(logger.getLevel(), this).autoSubmit().write();
        endForm("Update", true);
        append("</td><td>");
        append("<form method=\"POST\" action=\"" + Loggers.path(uriInfo, logger) + "\">\n");
        append("  <input type=\"hidden\" name=\"action\" value=\"delete\">\n");
        append("  <input type=\"submit\" value=\"Delete\">\n");
        append("</form>\n");
    }
}
