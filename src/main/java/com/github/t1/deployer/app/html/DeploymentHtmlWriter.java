package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.app.html.builder.SizeVariation.*;
import static com.github.t1.deployer.app.html.builder.StyleVariation.*;
import static com.github.t1.deployer.model.Deployment.*;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.Deployments;
import com.github.t1.deployer.model.Deployment;
import com.github.t1.deployer.repository.Repository;

@Provider
public class DeploymentHtmlWriter extends AbstractHtmlBodyWriter<Deployment> {
    @Inject
    Repository repository;

    public DeploymentHtmlWriter() {
        super(Deployment.class, DEPLOYMENTS);
    }

    private boolean isNew() {
        return NULL_DEPLOYMENT.equals(getTarget());
    }

    @Override
    public String bodyTitle() {
        return isNew() ? "Add Deployment" : getTarget().getContextRoot().toString();
    }

    @Override
    public String title() {
        return isNew() ? "Add Deployment" : "Deployment: " + getTarget().getContextRoot();
    }

    @Override
    public void body() {
        if (isNew()) {
            newForm();
        } else {
            info();
            availableVersions();
        }
    }

    private void newForm() {
        append("<p>Enter the checksum of a new artifact to deploy</p>\n");
        form().id("main") //
                .action(Deployments.base(getUriInfo())) //
                .hiddenInput("action", "deploy") //
                .input("Checksum", "checkSum") //
                .close();
        buttonGroup() //
                .button().size(L).style(primary).form("main").type("submit").label("Deploy").close() //
                .close();
    }

    private void info() {
        append(href("&lt;", Deployments.pathAll(getUriInfo()))).append("\n");
        append("<br/><br/>\n");
        append("    Name: ").append(getTarget().getName()).append("<br/>\n");
        append("    Context-Root: ").append(getTarget().getContextRoot()).append("<br/>\n");
        append("    Version: ").append(getTarget().getVersion()).append("<br/>\n");
        append("    CheckSum: ").append(getTarget().getCheckSum()).append("<br/>\n");
        Deployment deployment = getTarget();
        form().id("undeploy") //
                .action(Deployments.path(getUriInfo(), deployment.getContextRoot())) //
                .hiddenInput("contextRoot", deployment.getContextRoot()) //
                .hiddenInput("checkSum", deployment.getCheckSum()) //
                .hiddenInput("action", "undeploy") //
                .close();
        buttonGroup() //
                .button().size(S).style(danger).form("undeploy").type("submit").icon("remove").close() //
                .close();
        append("<br/><br/>\n");
    }

    private void availableVersions() {
        append("    <h2>Available Versions:</h2>\n");
        append("    <table>");
        int i = 0;
        for (Deployment deployment : repository.availableVersionsFor(getTarget().getCheckSum())) {
            append("        <tr>");
            append("<td>").append(deployment.getVersion()).append("</td>");
            append("<td>\n");
            redeploy(deployment, i++);
            rawAppend("</td>");
            append("</tr>\n");
        }
        append("    </table>\n");
    }

    private void redeploy(Deployment deployment, int i) {
        form().id("redeploy-" + i) //
                .action(Deployments.path(getUriInfo(), deployment.getContextRoot())) //
                .hiddenInput("contextRoot", deployment.getContextRoot()) //
                .hiddenInput("checkSum", deployment.getCheckSum()) //
                .hiddenInput("action", "redeploy") //
                .close();
        buttonGroup() //
                .button().size(S).style(primary).form("redeploy-" + i).type("submit").label("Deploy").close() //
                .close();
    }
}
