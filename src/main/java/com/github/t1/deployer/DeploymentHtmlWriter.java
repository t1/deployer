package com.github.t1.deployer;

import javax.ws.rs.core.UriInfo;

public class DeploymentHtmlWriter extends HtmlWriter {
    private final DeploymentResource deployment;

    public DeploymentHtmlWriter(UriInfo uriInfo, DeploymentResource deployment) {
        super(uriInfo);
        this.deployment = deployment;
    }

    @Override
    protected String title() {
        return "Deployment: " + deployment.getContextRoot();
    }

    @Override
    protected String body() {
        StringBuilder out = new StringBuilder();
        out.append("    Name: ").append(deployment.getName()).append("<br/>\n");
        out.append("    Context-Root: ").append(deployment.getContextRoot()).append("<br/>\n");
        out.append("    Version: ").append(deployment.getVersion()).append("<br/>\n");
        out.append("    CheckSum: ").append(deployment.getCheckSum()).append("<br/>\n");
        out.append(actionForm("Undeploy", deployment.self())).append("<br/>\n");

        out.append("    <h2>Available Versions:</h2>");
        out.append("    <table>");
        for (Deployment deployment : deployment.getAvailableVersions()) {
            out.append("        <tr>");
            out.append("<td>").append(deployment.getVersion()).append("</td>");
            out.append("<td>").append(actionForm("Deploy", deployment)).append("</td>");
            out.append("</tr>\n");
        }
        out.append("    </table>\n");
        return out.toString();
    }

    private String actionForm(String action, Deployment deployment) {
        return "<form method=\"post\" action=\"" + Deployments.path(uriInfo, this.deployment.self()) + "\">\n" //
                + "<input type=\"hidden\" name=\"contextRoot\" value=\"" + deployment.getContextRoot() + "\">\n" //
                + "<input type=\"hidden\" name=\"checkSum\" value=\"" + deployment.getCheckSum() + "\">\n" //
                + "<input type=\"hidden\" name=\"action\" value=\"" + action.toLowerCase() + "\">\n" //
                + "<input type=\"submit\" value=\"" + action + "\">\n" //
                + "</form>";
    }
}
