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
        out.append("    <table>\n");
        for (Deployment deployment : target) {
            out.append("        <tr>");

            out.append("<td>");
            href(deployment.getContextRoot().getValue(), Deployments.path(uriInfo, deployment.getContextRoot()));
            out.append("</td>");

            out.append("<td>").append(deployment.getName()).append("</td>");

            out.append("<td title=\"SHA-1: ").append(deployment.getCheckSum()).append("\">") //
                    .append(deployment.getVersion()) //
                    .append("</td>");

            out.append("</tr>\n");
        }
        out.append("    <tr><td colspan='3'>");
        href("+", Deployments.newDeployment(uriInfo));
        out.append("</td></tr>\n");
        out.append("    </table>\n");
        out.append("<br/><br/>\n");
    }
}
