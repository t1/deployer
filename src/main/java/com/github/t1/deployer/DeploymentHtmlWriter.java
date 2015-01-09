package com.github.t1.deployer;

import javax.ws.rs.core.UriInfo;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DeploymentHtmlWriter extends HtmlWriter {
    private final UriInfo uriInfo;
    private final DeploymentResource deployment;

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

        out.append("    <h2>Available Versions:</h2>");
        out.append("    <table>");
        for (Version version : deployment.getAvailableVersions()) {
            out.append("        <tr>");
            out.append("<td>").append(version).append("</td>");
            out.append("<td>").append(installForm()).append("</td>");
            out.append("</tr>\n");
        }
        out.append("    </table>\n");
        return out.toString();
    }

    private String installForm() {
        return "<form method=\"post\" action=\"" + Deployments.path(uriInfo, deployment.self()) + "\">\n" //
                + "<input type=\"hidden\" name=\"contextRoot\" value=\"" + deployment.getContextRoot() + "\">\n" //
                + "<input type=\"hidden\" name=\"checkSum\" value=\"" + deployment.getCheckSum() + "\">\n" //
                + "<input type=\"submit\" value=\"Deploy\">\n" //
                + "</form>";
    }
}
