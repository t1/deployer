package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.app.html.builder.SizeVariation.*;
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
        return NEW_LOGGER.equals(getTarget().getCategory());
    }

    @Override
    public String bodyTitle() {
        return isNew() ? "Add Logger" : getTarget().getCategory();
    }

    @Override
    public String title() {
        return isNew() ? "Add Logger" : "Logger: " + getTarget().getCategory();
    }

    @Override
    public void body() {
        append(href("&lt", Loggers.base(getUriInfo()))).append("\n");
        if (isNew()) {
            append("<p>Enter the name of a new logger to configure</p>\n");
            form().id("main") //
                    .action(Loggers.base(getUriInfo())) //
                    .input("Category", "category") //
                    .enclosing(new LogLevelSelect(getTarget().getLevel())) //
                    .close();
            buttonGroup().justified() //
                    .button().size(L).style(primary).form("main").type("submit").label("Add").close() //
                    .close();
        } else {
            form().id("main") //
                    .action(Loggers.path(getUriInfo(), getTarget())) //
                    .enclosing(new LogLevelSelect(getTarget().getLevel()).autoSubmit()) //
                    .noscriptSubmit("Update") //
                    .close();
            form().id("delete") //
                    .action(Loggers.path(getUriInfo(), getTarget())) //
                    .hiddenInput("action", "delete") //
                    .close();
            buttonGroup() //
                    .button().size(S).style(danger).form("delete").type("submit").icon("remove").close() //
                    .close();
        }
    }
}
