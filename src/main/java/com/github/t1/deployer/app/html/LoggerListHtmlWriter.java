package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.app.html.builder.StyleVariation.*;
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
        sort(getTarget());
        for (LoggerConfig logger : getTarget()) {
            append("<tr><td>");
            href(logger.getCategory(), Loggers.path(getUriInfo(), logger));
            rawAppend("</td><td>\n");
            in();
            buttons(logger);
            out();
            append("</td></tr>\n");
        }
        append("<tr><td colspan='3'>");
        href("+", Loggers.newLogger(getUriInfo()));
        rawAppend("</td></tr>\n");
        out();
        append("</table>\n");
    }

    private void buttons(LoggerConfig logger) {
        form().action(Loggers.path(getUriInfo(), logger)) //
                .closing(new LogLevelSelectForm(logger.getLevel(), this).autoSubmit()) //
                .noscriptSubmit("Update") //
                .close();
        append("</td><td>");
        form().action(Loggers.path(getUriInfo(), logger)) //
                .hiddenInput("action", "delete") //
                .submitIcon("remove", danger) //
                .close();
    }
}
