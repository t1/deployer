package com.github.t1.deployer.app;

import java.net.URI;

public class NewDeploymentFormHtmlWriter extends HtmlWriter {
    @Override
    protected String title() {
        return "Add Deployment";
    }

    @Override
    protected String body() {
        URI actionUri = uriInfo.getBaseUriBuilder().path(Deployments.class).build();
        return "<p>Enter the checksum of a new artifact to deploy</p>" //
                + "<form method=\"POST\" action=\"" + actionUri + "\">\n" //
                + "  <input type=\"hidden\" name=\"action\" value=\"deploy\">\n" //
                + "  <input name=\"checkSum\">\n" //
                + "  <input type=\"submit\" value=\"Deploy\">\n" //
                + "</form>";
    }
}
