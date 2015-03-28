package com.github.t1.deployer.app;

import java.security.Principal;
import java.util.List;

import javax.inject.Inject;

import com.github.t1.deployer.container.Container;
import com.github.t1.deployer.model.Deployment;

public class DeploymentListHtmlWriter extends HtmlWriter {
    @Inject
    List<Deployment> deployments;
    @Inject
    Container container;
    @Inject
    Principal principal;

    @Override
    protected String title() {
        return "Deployments";
    }

    @Override
    protected String body() {
        StringBuilder out = new StringBuilder();
        deployments(out);
        footer(out);
        return out.toString();
    }

    private void deployments(StringBuilder out) {
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
        out.append("    <tr><td colspan='3'><a href=\"deployment-form\">+</a></td></tr>\n");
        out.append("    </table>\n");
        out.append("<br/><br/>\n");
        out.append("<a href=\"" + Loggers.base(uriInfo) + "\">Loggers</a>");
    }

    private void footer(StringBuilder out) {
        out.append("<footer>Principal: ").append((principal == null) ? "?" : principal.getName()).append("</footer>\n");
    }
}
