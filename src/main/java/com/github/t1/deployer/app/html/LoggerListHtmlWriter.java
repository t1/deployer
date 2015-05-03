package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.app.html.builder.SizeVariation.*;
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
        int i = 0;
        for (LoggerConfig logger : getTarget()) {
            append("<tr><td>") //
                    .append(href(logger.getCategory(), Loggers.path(getUriInfo(), logger))) //
                    .append("</td><td>\n");
            in();
            buttons(logger, i++);
            out();
            append("</td></tr>\n");
        }
        append("<tr><td colspan='3'>") //
                .append(href("+", Loggers.newLogger(getUriInfo()))) //
                .append("</td></tr>\n");
        out();
        append("</table>\n");
    }

    private void buttons(LoggerConfig logger, int i) {
        form().action(Loggers.path(getUriInfo(), logger)) //
                .enclosing(new LogLevelSelect(logger.getLevel()).autoSubmit()) //
                .noscriptSubmit("Update") //
                .close();
        append("</td><td>\n");
        form().id("delete-" + i) //
                .action(Loggers.path(getUriInfo(), logger)) //
                .hiddenInput("action", "delete") //
                .close();
        buttonGroup() //
                .button().size(XS).style(danger).form("delete-" + i).type("submit").icon("remove").close() //
                .close();
    }
}
