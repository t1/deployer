package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.DeployerPage.*;
import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Table.*;
import static com.github.t1.deployer.app.html.builder.Tags.*;

import java.net.URI;
import java.util.*;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.Deployments;
import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.Table.Cell;
import com.github.t1.deployer.app.html.builder.Table.TableBuilder;
import com.github.t1.deployer.app.html.builder.Tags.AppendingComponent;
import com.github.t1.deployer.model.*;

@Provider
public class DeploymentListHtmlWriter extends TextHtmlListMessageBodyWriter<Deployment> {
    private static final Cell ADD_DEPLOYMENT_ROW = cell().colspan(3).body(link(new AppendingComponent<URI>() {
        @Override
        protected URI contentFrom(BuildContext out) {
            return Deployments.newDeployment(out.get(UriInfo.class));
        }
    }).body(text("+")).build()).build();

    private static final Component TABLE = new Component() {
        @Override
        public void writeTo(BuildContext out) {
            TableBuilder table = table();
            @SuppressWarnings("unchecked")
            List<Deployment> deployments = out.get(List.class);
            Collections.sort(deployments);
            UriInfo uriInfo = out.get(UriInfo.class);
            for (Deployment deployment : deployments) {
                ContextRoot contextRoot = deployment.getContextRoot();
                URI uri = Deployments.path(uriInfo, contextRoot);
                String checksum = "SHA-1: " + deployment.getCheckSum();
                table.row( //
                        cell().body(link(uri).body(text(contextRoot)).build()).build(), //
                        cell().body(text(deployment.getName())).build(), //
                        cell().title(checksum).body(textOr(deployment.getVersion(), "unknown")).build() //
                );
            }
            table.row(ADD_DEPLOYMENT_ROW);
            table.build().writeTo(out);
        }
    };

    private static final DeployerPage PAGE = panelPage() //
            .title(text("Deployments")) //
            .body(TABLE) //
            .build();

    @Override
    protected void prepare(BuildContext buildContext) {
        buildContext.put(Navigation.DEPLOYMENTS);
    }

    @Override
    protected Component component() {
        return PAGE;
    }
}
