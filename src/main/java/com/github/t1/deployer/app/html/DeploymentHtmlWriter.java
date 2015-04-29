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
        return NULL_DEPLOYMENT.equals(target);
    }

    @Override
    protected String bodyTitle() {
        return isNew() ? "Add Deployment" : target.getContextRoot().toString();
    }

    @Override
    protected String title() {
        return isNew() ? "Add Deployment" : "Deployment: " + target.getContextRoot();
    }

    @Override
    protected void body() {
        if (isNew()) {
            newForm();
        } else {
            info();
            availableVersions();
        }
    }

    private void newForm() {
        append("<p>Enter the checksum of a new artifact to deploy</p>");
        append("<form method=\"POST\" action=\"").append(Deployments.base(uriInfo)).append("\">\n");
        append("  <input type=\"hidden\" name=\"action\" value=\"deploy\">\n");
        append("  <input name=\"checkSum\">\n");
        append("  <input type=\"submit\" value=\"Deploy\">\n");
        append("</form>");
    }

    private void info() {
        href("&lt;", Deployments.pathAll(uriInfo));
        append("<br/><br/>\n");
        append("    Name: ").append(target.getName()).append("<br/>\n");
        append("    Context-Root: ").append(target.getContextRoot()).append("<br/>\n");
        append("    Version: ").append(target.getVersion()).append("<br/>\n");
        append("    CheckSum: ").append(target.getCheckSum()).append("<br/>\n");
        append(actionForm("Undeploy", "undeploy", target)).append("<br/>\n");
        append("<br/><br/>\n");
    }

    private void availableVersions() {
        append("    <h2>Available Versions:</h2>");
        append("    <table>");
        for (Deployment deployment : repository.availableVersionsFor(target.getCheckSum())) {
            append("        <tr>");
            append("<td>").append(deployment.getVersion()).append("</td>");
            append("<td>").append(actionForm("Deploy", "redeploy", deployment)).append("</td>");
            append("</tr>\n");
        }
        append("    </table>\n");
    }

    private String actionForm(String title, String action, Deployment deployment) {
        return "<form method=\"post\" action=\"" + Deployments.path(uriInfo, deployment.getContextRoot()) + "\">\n" //
                + "  <input type=\"hidden\" name=\"contextRoot\" value=\"" + deployment.getContextRoot() + "\">\n" //
                + "  <input type=\"hidden\" name=\"checkSum\" value=\"" + deployment.getCheckSum() + "\">\n" //
                + "  <input type=\"hidden\" name=\"action\" value=\"" + action + "\">\n" //
                + "  <input type=\"submit\" value=\"" + title + "\">\n" //
                + "</form>";
    }
}
