package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerPage.*;
import static com.github.t1.deployer.app.html.builder.Button.*;
import static com.github.t1.deployer.app.html.builder.ButtonGroup.*;
import static com.github.t1.deployer.app.html.builder.Compound.*;
import static com.github.t1.deployer.app.html.builder.Form.*;
import static com.github.t1.deployer.app.html.builder.Input.*;
import static com.github.t1.deployer.app.html.builder.Select.*;
import static com.github.t1.deployer.app.html.builder.SizeVariation.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.StyleVariation.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;
import static com.github.t1.log.LogLevel.*;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.*;
import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.Compound.CompoundBuilder;
import com.github.t1.deployer.app.html.builder.Select.SelectBuilder;
import com.github.t1.deployer.model.LoggerConfig;
import com.github.t1.log.LogLevel;

@Provider
public class LoggerHtmlWriter extends TextHtmlMessageBodyWriter<LoggerConfig> {
    private static final String MAIN_FORM_ID = "main";
    private static final Component LOGGERS = append(context -> Loggers.base(context.get(UriInfo.class)));

    private static final DeployerPage existingLogger(UriInfo uriInfo, LoggerConfig logger) {
        return deployerPage() //
                .title(append(context -> "Logger: " + context.get(LoggerConfig.class).getCategory())) //
                .backLink(append(context -> Loggers.base(context.get(UriInfo.class)))) //
                .panelBody(div().style("float: right") //
                        .body(form("delete") //
                                .action(Loggers.path(uriInfo, logger)) //
                                .input(hiddenAction("delete")) //
        ) //
                        .body(buttonGroup() //
                                .button(button().size(S).style(danger).forForm("delete").body(text("Delete")) //
        ) //
        ) //
        ) //
                .body(nl()) //
                .panelBody(form(MAIN_FORM_ID) //
                        .action(Loggers.path(uriInfo, logger)) //
                        .input(hiddenAction("patch")) //
                        .body(levelSelect(logger.getLevel())) //
        ) //
                .build();
    }

    public static CompoundBuilder levelSelect(LogLevel selectedLevel) {
        return compound( //
                levelSelectBuilder(selectedLevel).autosubmit(), //
                noscript(input().type("submit").value("Update")) //
        );
    }

    private static SelectBuilder levelSelectBuilder(LogLevel selectedLevel) {
        SelectBuilder select = select("level");
        for (LogLevel logLevel : LogLevel.values()) {
            if (logLevel == _DERIVED_)
                continue;
            select.option(option().selected(logLevel == selectedLevel).body(logLevel.name()));
        }
        return select;
    }

    private static final Component NEW_LOGGER = deployerPage() //
            .title(text("Add Logger")) //
            .panelBody( //
                    compound( //
                            p("Enter the category of a new logger to configure"), //
                            form(MAIN_FORM_ID).action(LOGGERS) //
                                    .input(input("category").placeholder("Category").required().autofocus()) //
                                    .body(div().classes("form-group").body(levelSelectBuilder(DEBUG))), //
                            buttonGroup() //
                                    .button(button().style(primary).forForm(MAIN_FORM_ID).body(text("Add"))) //
    )) //
            .build();

    @Override
    protected void prepare(BuildContext buildContext) {
        buildContext.put(Navigation.loggers);
    }

    @Override
    protected Component component() {
        return out -> {
            LoggerConfig target = out.get(LoggerConfig.class);
            UriInfo uriInfo = out.get(UriInfo.class);
            Component page;
            if (target.isNew())
                page = NEW_LOGGER;
            else
                page = existingLogger(uriInfo, target);
            page.writeTo(out);
        };
    }
}
