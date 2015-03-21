package com.github.t1.deployer;

import java.security.Principal;
import java.util.List;

import javax.inject.Inject;

public class DeploymentsListHtmlWriter extends HtmlWriter {
    @Inject
    List<Deployment> deployments;
    @Inject
    Principal principal;

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
