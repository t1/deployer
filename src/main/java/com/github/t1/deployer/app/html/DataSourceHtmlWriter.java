package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerPage.*;
import static com.github.t1.deployer.app.html.builder2.Components.*;
import static com.github.t1.deployer.app.html.builder2.Compound.*;
import static com.github.t1.deployer.app.html.builder2.Static.*;
import static com.github.t1.deployer.app.html.builder2.Tag.*;
import static com.github.t1.deployer.app.html.builder2.Tags.*;
import static com.github.t1.deployer.model.DataSourceConfig.*;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.DataSources;
import com.github.t1.deployer.app.html.builder2.*;
import com.github.t1.deployer.app.html.builder2.Tag.TagBuilder;
import com.github.t1.deployer.model.DataSourceConfig;

@Provider
public class DataSourceHtmlWriter extends TextHtmlMessageBodyWriter<DataSourceConfig> {
    private static final DeployerPage PAGE = deployerPage() //
            .title(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    DataSourceConfig target = out.getTarget();
                    text(title(target)).writeInlineTo(out);
                }
            }) //
            .body(link(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    text(DataSources.base(URI_INFO.get())).writeInlineTo(out);
                }
            }).body(text("&lt;")).build()) //
            .body(new Component() {
                @Override
                public void writeTo(BuildContext out) {
                    DataSourceConfig target = out.getTarget();
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
                        .a("action", "http://localhost:8080/deployer/data-sources") // FIXME
                        .body(tag("label").a("for", "name").body(text("Name")).build()) //
                        .body(input("name").build()) //
                        .body(tag("label").a("for", "uri").body(text("URI")).build()) //
                        .body(input("uri").build()) //
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
                        .a("action", "http://localhost:8080/deployer/data-sources/foo") // FIXME
                        .body(tag("input").multiline() //
                                .a("type", "hidden").a("name", "action").a("value", "delete") //
                                .build()) //
                        .build() //
                , //
                tag("form").id("main").a("method", "POST") //
                        .a("action", "http://localhost:8080/deployer/data-sources/foo") // FIXME
                        .body(tag("label").a("for", "name").body(text("Name")).build()) //
                        .body(input("name").a("value", "foo").build()) // FIXME
                        .body(tag("label").a("for", "uri").body(text("URI")).build()) //
                        .body(input("uri").a("value", "foo-uri").build()) // FIXME
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

    private static TagBuilder input(String idAndName) {
        return tag("input").classes("form-control").a("name", idAndName).id(idAndName).a("required");
    }

    private static boolean isNew(DataSourceConfig target) {
        return NEW_DATA_SOURCE.equals(target.getName());
    }

    public static String bodyTitle(DataSourceConfig target) {
        return isNew(target) ? "Add Data-Source" : target.getName();
    }

    public static String title(DataSourceConfig target) {
        return isNew(target) ? "Add Data-Source" : target.getName();
    }

    @Override
    protected Component component() {
        return PAGE;
    }
}
