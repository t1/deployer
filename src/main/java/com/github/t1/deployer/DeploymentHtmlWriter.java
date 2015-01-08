package com.github.t1.deployer;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DeploymentHtmlWriter extends HtmlWriter {
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
            out.append("<td><a href=\"").append("install").append("\">").append("Install").append("</td>");
            out.append("</tr>\n");
        }
        out.append("    </table>\n");
        return out.toString();
    }
}