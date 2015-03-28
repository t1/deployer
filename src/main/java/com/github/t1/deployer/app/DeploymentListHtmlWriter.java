package com.github.t1.deployer.app;

import java.security.Principal;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.model.Deployment;

@Provider
public class DeploymentListHtmlWriter extends AbstractListHtmlWriter<Deployment> {
    public DeploymentListHtmlWriter() {
        super(Deployment.class);
    }

    @Inject
    Principal principal;

    @Override
    protected String title() {
        return "Deployments";
    }

    @Override
    protected String body() {
        deployments();
        footer();
        return out.toString();
    }

    private void deployments() {
        out.append("    <table>\n");
        for (Deployment deployment : target) {
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

    private void footer() {
        out.append("<footer>Principal: ").append((principal == null) ? "?" : principal.getName()).append("</footer>\n");
    }
}
