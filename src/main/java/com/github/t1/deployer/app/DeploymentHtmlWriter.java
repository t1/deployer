package com.github.t1.deployer.app;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.model.Deployment;

@Provider
public class DeploymentHtmlWriter extends AbstractHtmlWriter<DeploymentResource> {
    public DeploymentHtmlWriter() {
        super(DeploymentResource.class);
    }

    @Override
    protected String title() {
        return "Deployment: " + target.getContextRoot();
    }

    @Override
    protected String body() {
        info();
        availableVersions();
        return out.toString();
    }

    private void info() {
        out.append("<a href=\"" + Deployments.pathAll(uriInfo) + "\">&lt;</a>");
        out.append("<br/><br/>\n");
        out.append("    Name: ").append(target.getName()).append("<br/>\n");
        out.append("    Context-Root: ").append(target.getContextRoot()).append("<br/>\n");
        out.append("    Version: ").append(target.getVersion()).append("<br/>\n");
        out.append("    CheckSum: ").append(target.getCheckSum()).append("<br/>\n");
        out.append(actionForm("Undeploy", "undeploy")).append("<br/>\n");
        out.append("<br/><br/>\n");
    }

    private void availableVersions() {
        out.append("    <h2>Available Versions:</h2>");
        out.append("    <table>");
        for (Deployment deployment : target.getAvailableVersions()) {
            out.append("        <tr>");
            out.append("<td>").append(deployment.getVersion()).append("</td>");
            out.append("<td>").append(actionForm("Deploy", "redeploy")).append("</td>");
            out.append("</tr>\n");
        }
        out.append("    </table>\n");
    }

    private String actionForm(String title, String action) {
        return "<form method=\"post\" action=\"" + Deployments.path(uriInfo, target.deployment()) + "\">\n" //
                + "  <input type=\"hidden\" name=\"contextRoot\" value=\"" + target.getContextRoot() + "\">\n" //
                + "  <input type=\"hidden\" name=\"checkSum\" value=\"" + target.getCheckSum() + "\">\n" //
                + "  <input type=\"hidden\" name=\"action\" value=\"" + action + "\">\n" //
                + "  <input type=\"submit\" value=\"" + title + "\">\n" //
                + "</form>";
    }
}
