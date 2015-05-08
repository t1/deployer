package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerPage.*;
import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.app.html.builder2.Components.*;
import static com.github.t1.deployer.app.html.builder2.Compound.*;
import static com.github.t1.deployer.app.html.builder2.Input.*;
import static com.github.t1.deployer.app.html.builder2.Static.*;
import static com.github.t1.deployer.app.html.builder2.Tag.*;
import static com.github.t1.deployer.app.html.builder2.Tags.*;
import static com.github.t1.deployer.model.DataSourceConfig.*;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.DataSources;
import com.github.t1.deployer.app.html.builder2.*;
import com.github.t1.deployer.model.DataSourceConfig;

@Provider
public class DataSourceHtmlWriter extends TextHtmlMessageBodyWriter<DataSourceConfig> {
    private static final DeployerPage PAGE = deployerPage() //
            .title(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    DataSourceConfig target = out.get(DataSourceConfig.class);
                    text(title(target)).writeInlineTo(out);
                }
            }) //
            .body(link(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    text(DataSources.base(out.get(UriInfo.class))).writeInlineTo(out);
                }
            }).body(text("&lt;")).build()) //
            .body(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    DataSourceConfig target = out.get(DataSourceConfig.class);
                    if (isNew(target))
                        newDataSourceForm().writeTo(out);
                    else
                        existingDataSourceForm().writeTo(out);
                }
            }) //
            .build();

    private static Component newDataSourceForm() {
        return compound( //
                tag("p").body(text("Enter the name of a new data source to configure")).build() //
                , //
                tag("form").id("main").a("method", "POST") //
                        .a("action", DATA_SOURCES.link()) //
                        .body(input("name").label("Name").build()) //
                        .body(input("uri").label("URI").build()) //
                        .build() //
                , //
                buttonGroup().classes("btn-group-justified") //
                        .body(buttonGroup() //
                                .body(tag("button").multiline().classes("btn", "btn-block", "btn-primary") //
                                        .a("form", "main").a("type", "submit") //
                                        .body(text("Add")) //
                                        .build()) //
                                .build() //
                        ).build() //
        ).build();
    }

    private static Component existingDataSourceForm() {
        return compound( //
                tag("form").id("delete").a("method", "POST") //
                        .a("action", dataSourceLink()) //
                        .body(tag("input").multiline() //
                                .a("type", "hidden").a("name", "action").a("value", "delete") //
                                .build()) //
                        .build() //
                , //
                tag("form").id("main").a("method", "POST") //
                        .a("action", dataSourceLink()) //
                        .body(input("name").label("Name").value(new Component() {
                            @Override
                            public void writeTo(BuildContext out) {
                                out.append(out.get(DataSourceConfig.class).getName());
                            }
                        }).build()) //
                        .body(input("uri").label("URI").value(new Component() {
                            @Override
                            public void writeTo(BuildContext out) {
                                out.append(out.get(DataSourceConfig.class).getUri());
                            }
                        }).build()) //
                        .build() //
                , //
                buttonGroup().classes("btn-group-justified") //
                        .body(buttonGroup() //
                                .body(tag("button").multiline() //
                                        .classes("btn", "btn-block", "btn-primary") //
                                        .a("form", "main").a("type", "submit") //
                                        .body(text("Update")) //
                                        .build()).build()) //
                        .body(buttonGroup() //
                                .body(iconButton("delete", "remove", "btn-danger").build()) //
                                .build()) //
                        .build()

        ).build();
    }

    private static Component dataSourceLink() {
        return new Component() {
            @Override
            public void writeTo(BuildContext out) {
                out.append(DataSources.path(out.get(UriInfo.class), out.get(DataSourceConfig.class)));
            }
        };
    }

    private static boolean isNew(DataSourceConfig target) {
        return NEW_DATA_SOURCE.equals(target.getName());
    }

    public static String title(DataSourceConfig target) {
        return isNew(target) ? "Add Data-Source" : target.getName();
    }

    @Override
    protected void prepare(BuildContext buildContext) {
        buildContext.put(DATA_SOURCES);
    }

    @Override
    protected Component component() {
        return PAGE;
    }
}
