package com.github.t1.deployer;

import java.util.List;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DeploymentsListHtml {
    private final List<Deployment> deployments;

    @Override
    public String toString() {
        return "<html><body>" + listDeployments() + "</body></html>";
    }

    private String listDeployments() {
        StringBuilder out = new StringBuilder();
        out.append("<table>");
        for (Deployment deployment : deployments) {
            out.append("") //
                    .append("<tr><td>") //
                    .append(deployment.getContextRoot()) //
                    .append("</td><td>") //
                    .append(deployment.getVersion()) //
                    .append("</td><td>") //
                    .append(deployment.getCheckSum()) //
                    .append("</td></tr>");
        }
        out.append("</table>");
        return out.toString();
    }


}
