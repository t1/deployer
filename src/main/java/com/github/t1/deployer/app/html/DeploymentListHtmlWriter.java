package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;

import javax.ws.rs.ext.Provider;

import com.github.t1.deployer.app.Deployments;
import com.github.t1.deployer.model.Deployment;
import com.github.t1.deployer.tools.User;

@Provider
public class DeploymentListHtmlWriter extends AbstractListHtmlWriter<Deployment> {
    public DeploymentListHtmlWriter() {
        super(Deployment.class, DEPLOYMENTS);
    }

    User user = User.getCurrent();

    @Override
    protected String title() {
        return "Deployments";
    }

    @Override
    protected void body() {
        append("    <table>\n");
        for (Deployment deployment : target) {
            append("        <tr>");

            append("<td>");
            href(deployment.getContextRoot().getValue(), Deployments.path(uriInfo, deployment.getContextRoot()));
            append("</td>");

            append("<td>").append(deployment.getName()).append("</td>");

            append("<td title=\"SHA-1: ").append(deployment.getCheckSum()).append("\">") //
                    .append(deployment.getVersion()) //
                    .append("</td>");

            append("</tr>\n");
        }
        append("    <tr><td colspan='3'>");
        href("+", Deployments.newDeployment(uriInfo));
        append("</td></tr>\n");
        append("    </table>\n");
        append("<br/><br/>\n");
    }
}
