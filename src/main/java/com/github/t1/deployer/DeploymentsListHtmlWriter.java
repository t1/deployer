package com.github.t1.deployer;

import java.util.List;

import javax.ws.rs.core.UriInfo;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DeploymentsListHtmlWriter extends HtmlWriter {
    private final UriInfo uriInfo;
    private final List<Deployment> deployments;

    @Override
    protected String title() {
        return "Deployments";
    }

    @Override
    protected String body() {
        StringBuilder out = new StringBuilder();
        out.append("    <table>");
        for (Deployment deployment : deployments) {
            out.append("        ") //
                    .append("<tr><td><a href=\"").append(Deployments.path(uriInfo, deployment)).append("\">") //
                    .append(deployment.getContextRoot()).append("</a>") //
                    .append("</td><td>").append(deployment.getName()) //
                    .append("</td><td title=\"SHA-1: ").append(deployment.getCheckSum()).append("\">") //
                    .append(deployment.getVersion()) //
                    .append("</td></tr>\n");
        }
        out.append("    </table>\n");
        return out.toString();
    }
}
