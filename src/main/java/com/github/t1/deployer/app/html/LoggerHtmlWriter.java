package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerComponents.*;
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

import java.net.URI;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.Loggers;
import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.Select.SelectBuilder;
import com.github.t1.deployer.app.html.builder.Tags.AppendingComponent;
import com.github.t1.deployer.model.LoggerConfig;
import com.github.t1.log.LogLevel;

@Provider
public class LoggerHtmlWriter extends TextHtmlMessageBodyWriter<LoggerConfig> {
    private static final String MAIN_FORM_ID = "main";
    private static final Component LOGGERS = new AppendingComponent<URI>() {
        @Override
        protected URI contentFrom(BuildContext out) {
            return Loggers.base(out.get(UriInfo.class));
        }
    };

    private static final Component existingLogger(UriInfo uriInfo, LoggerConfig logger) {
        return panelPage() //
                .title(new AppendingComponent<String>() {
                    @Override
                    protected String contentFrom(BuildContext out) {
                        LoggerConfig logger = out.get(LoggerConfig.class);
                        return "Logger: " + logger.getCategory();
                    }
                }) //
                .backLink(new AppendingComponent<URI>() {
                    @Override
                    protected URI contentFrom(BuildContext out) {
                        return Loggers.base(out.get(UriInfo.class));
                    }
                }) //
                .panelBody(div().style("float: right") //
                        .body(form("delete").action(Loggers.path(uriInfo, logger)) //
                                .body(hiddenAction("delete")) //
                                .build()) //
                        .body(buttonGroup() //
                                .button(remove("delete", S)) //
                                .build()) //
                        .build()) //
                .body(nl()) //
                .panelBody(form(MAIN_FORM_ID).action(Loggers.path(uriInfo, logger)) //
                        .body(levelSelect(logger.getLevel())) //
                        .build() //
                ) //
                .build();
    }

    public static Component levelSelect(LogLevel selectedLevel) {
        return compound( //
                levelSelectBuilder(selectedLevel).autosubmit().build(), //
                noscript(input().type("submit").value("Update").build()) //
        ).build();
    }

    private static SelectBuilder levelSelectBuilder(LogLevel selectedLevel) {
        SelectBuilder select = select("level");
        for (LogLevel logLevel : LogLevel.values()) {
            if (logLevel == _DERIVED_)
                continue;
            select.option(option().selected(logLevel == selectedLevel).body(logLevel.name()).build());
        }
        return select;
    }

    private static final Component NEW_LOGGER = panelPage() //
            .title(text("Add Logger")) //
            .panelBody(compound( //
                    p("Enter the name of a new logger to configure"), //
                    form(MAIN_FORM_ID).action(LOGGERS) //
                            .body(input("category").label("Category").build()) //
                            .body(levelSelectBuilder(DEBUG).build()) //
                            .build(), //
                    buttonGroup().justified() //
                            .button(button().style(primary).forForm(MAIN_FORM_ID).body(text("Add")).build()) //
                            .build() //
                    ).build()).build();

    @Override
    protected void prepare(BuildContext buildContext) {
        buildContext.put(Navigation.LOGGERS);
    }

    @Override
    protected Component component() {
        return new Component() {
            @Override
            public void writeTo(BuildContext out) {
                LoggerConfig target = out.get(LoggerConfig.class);
                UriInfo uriInfo = out.get(UriInfo.class);
                Component page;
                if (target.isNew())
                    page = NEW_LOGGER;
                else
                    page = existingLogger(uriInfo, target);
                page.writeTo(out);
            }
        };
    }
}
