package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.app.html.builder.StyleVariation.*;
import static com.github.t1.deployer.model.LoggerConfig.*;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.Loggers;
import com.github.t1.deployer.model.LoggerConfig;

@Provider
public class LoggerHtmlWriter extends AbstractHtmlBodyWriter<LoggerConfig> {
    public LoggerHtmlWriter() {
        super(LoggerConfig.class, LOGGERS);
    }

    private boolean isNew() {
        return NEW_LOGGER.equals(target.getCategory());
    }

    @Override
    public String bodyTitle() {
        return isNew() ? "Add Logger" : target.getCategory();
    }

    @Override
    public String title() {
        return isNew() ? "Add Logger" : "Logger: " + target.getCategory();
    }

    @Override
    public void body() {
        indent().href("&lt", Loggers.base(uriInfo)).nl();
        if (isNew()) {
            append("<p>Enter the name of a new logger to configure</p>\n");
            startForm(Loggers.base(uriInfo));
            append("<input name=\"category\"/>\n");
            new LogLevelSelectForm(target.getLevel(), this).write();
            endForm("Add", false);
        } else {
            startForm(Loggers.path(uriInfo, target));
            new LogLevelSelectForm(target.getLevel(), this).autoSubmit().write();
            endForm("Update", true);
            delete();
        }
    }

    private void delete() {
        form().action(Loggers.path(uriInfo, target)) //
                .hiddenInput("action", "delete") //
                .submit("Delete", danger) //
                .close();
    }
}
