package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
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
        append("<p>Enter the checksum of a new artifact to deploy</p>");
        form().action(Deployments.base(getUriInfo())) //
                .hiddenInput("action", "deploy") //
                .input("Checksum", "checkSum") //
                .submit("Deploy") //
                .close();
    }

    private void info() {
        href("&lt;", Deployments.pathAll(getUriInfo()));
        append("<br/><br/>\n");
        append("    Name: ").append(getTarget().getName()).append("<br/>\n");
        append("    Context-Root: ").append(getTarget().getContextRoot()).append("<br/>\n");
        append("    Version: ").append(getTarget().getVersion()).append("<br/>\n");
        append("    CheckSum: ").append(getTarget().getCheckSum()).append("<br/>\n");
        actionForm("Undeploy", "undeploy", getTarget());
        append("<br/><br/>\n");
    }

    private void availableVersions() {
        append("    <h2>Available Versions:</h2>");
        append("    <table>");
        for (Deployment deployment : repository.availableVersionsFor(getTarget().getCheckSum())) {
            append("        <tr>");
            append("<td>").append(deployment.getVersion()).append("</td>");
            append("<td>");
            actionForm("Deploy", "redeploy", deployment);
            rawAppend("</td>");
            append("</tr>\n");
        }
        append("    </table>\n");
    }

    private void actionForm(String title, String action, Deployment deployment) {
        form().action(Deployments.path(getUriInfo(), deployment.getContextRoot())) //
                .hiddenInput("contextRoot", deployment.getContextRoot()) //
                .hiddenInput("checkSum", deployment.getCheckSum()) //
                .hiddenInput("action", action) //
                .submit(title) //
                .close();
    }
}
