package com.github.t1.deployer;

import java.security.Principal;
import java.util.List;

import javax.ws.rs.core.UriInfo;

public class DeploymentsListHtmlWriter extends HtmlWriter {
    private final List<Deployment> deployments;
    private final Principal principal;

    public DeploymentsListHtmlWriter(UriInfo uriInfo, Principal principal, List<Deployment> deployments) {
        super(uriInfo);
        this.principal = principal;
        this.deployments = deployments;
    }

    @Override
    protected String title() {
        return "Deployments";
    }

    @Override
    protected String body() {
        StringBuilder out = new StringBuilder();
        out.append("    <table>\n");
        for (Deployment deployment : deployments) {
            out.append("        ") //
                    .append("<tr><td><a href=\"").append(Deployments.path(uriInfo, deployment)).append("\">") //
                    .append(deployment.getContextRoot()).append("</a>") //
                    .append("</td><td>").append(deployment.getName()) //
                    .append("</td><td title=\"SHA-1: ").append(deployment.getCheckSum()).append("\">") //
                    .append(deployment.getVersion()) //
                    .append("</td></tr>\n");
        }
        out.append("    <tr><td colspan='3'><a href=\"deployment-form\">+</a></td></tr>");
        out.append("    </table>\n");
        out.append("<footer>Principal: ").append((principal == null) ? "?" : principal.getName()).append("</footer>\n");
        return out.toString();
    }
}
