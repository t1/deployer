package com.github.t1.deployer.app;

import static com.github.t1.deployer.app.Deployments.*;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.model.Deployment;
import com.github.t1.deployer.repository.Repository;

@Provider
public class DeploymentHtmlWriter extends AbstractHtmlWriter<Deployment> {
    @Inject
    Repository repository;

    public DeploymentHtmlWriter() {
        super(Deployment.class);
    }

    private boolean isNew() {
        return NULL_DEPLOYMENT.equals(target);
    }

    @Override
    protected String title() {
        return isNew() ? "Add Deployment" : "Deployment: " + target.getContextRoot();
    }

    @Override
    protected String body() {
        if (isNew()) {
            newForm();
        } else {
            info();
            availableVersions();
        }
        return out.toString();
    }

    private StringBuilder newForm() {
        return out.append("<p>Enter the checksum of a new artifact to deploy</p>" //
                + "<form method=\"POST\" action=\"" + Deployments.base(uriInfo) + "\">\n" //
                + "  <input type=\"hidden\" name=\"action\" value=\"deploy\">\n" //
                + "  <input name=\"checkSum\">\n" //
                + "  <input type=\"submit\" value=\"Deploy\">\n" //
                + "</form>");
    }

    private void info() {
        out.append("<a href=\"" + Deployments.pathAll(uriInfo) + "\">&lt;</a>");
        out.append("<br/><br/>\n");
        out.append("    Name: ").append(target.getName()).append("<br/>\n");
        out.append("    Context-Root: ").append(target.getContextRoot()).append("<br/>\n");
        out.append("    Version: ").append(target.getVersion()).append("<br/>\n");
        out.append("    CheckSum: ").append(target.getCheckSum()).append("<br/>\n");
        out.append(actionForm("Undeploy", "undeploy", target)).append("<br/>\n");
        out.append("<br/><br/>\n");
    }

    private void availableVersions() {
        out.append("    <h2>Available Versions:</h2>");
        out.append("    <table>");
        for (Deployment deployment : repository.availableVersionsFor(target.getCheckSum())) {
            out.append("        <tr>");
            out.append("<td>").append(deployment.getVersion()).append("</td>");
            out.append("<td>").append(actionForm("Deploy", "redeploy", deployment)).append("</td>");
            out.append("</tr>\n");
        }
        out.append("    </table>\n");
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
