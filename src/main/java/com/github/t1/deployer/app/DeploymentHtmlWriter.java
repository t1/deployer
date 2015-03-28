package com.github.t1.deployer.app;

import com.github.t1.deployer.model.Deployment;

public class DeploymentHtmlWriter extends HtmlWriter {
    private DeploymentResource deploymentResource;

    public DeploymentHtmlWriter resource(DeploymentResource deploymentResource) {
        this.deploymentResource = deploymentResource;
        return this;
    }

    @Override
    protected String title() {
        return "Deployment: " + deploymentResource.getContextRoot();
    }

    @Override
    protected String body() {
        StringBuilder out = new StringBuilder();
        info(out);
        availableVersions(out);
        return out.toString();
    }

    private void info(StringBuilder out) {
        out.append("<a href=\"" + Deployments.pathAll(uriInfo) + "\">&lt;</a>");
        out.append("<br/><br/>\n");
        out.append("    Name: ").append(deploymentResource.getName()).append("<br/>\n");
        out.append("    Context-Root: ").append(deploymentResource.getContextRoot()).append("<br/>\n");
        out.append("    Version: ").append(deploymentResource.getVersion()).append("<br/>\n");
        out.append("    CheckSum: ").append(deploymentResource.getCheckSum()).append("<br/>\n");
        out.append(actionForm("Undeploy", "undeploy", deploymentResource.self())).append("<br/>\n");
        out.append("<br/><br/>\n");
    }

    private void availableVersions(StringBuilder out) {
        out.append("    <h2>Available Versions:</h2>");
        out.append("    <table>");
        for (Deployment deployment : deploymentResource.getAvailableVersions()) {
            out.append("        <tr>");
            out.append("<td>").append(deployment.getVersion()).append("</td>");
            out.append("<td>").append(actionForm("Deploy", "redeploy", deployment)).append("</td>");
            out.append("</tr>\n");
        }
        out.append("    </table>\n");
    }

    private String actionForm(String title, String action, Deployment deployment) {
        return "<form method=\"post\" action=\"" + Deployments.path(uriInfo, deploymentResource.self()) + "\">\n" //
                + "  <input type=\"hidden\" name=\"contextRoot\" value=\"" + deployment.getContextRoot() + "\">\n" //
                + "  <input type=\"hidden\" name=\"checkSum\" value=\"" + deployment.getCheckSum() + "\">\n" //
                + "  <input type=\"hidden\" name=\"action\" value=\"" + action + "\">\n" //
                + "  <input type=\"submit\" value=\"" + title + "\">\n" //
                + "</form>";
    }
}
