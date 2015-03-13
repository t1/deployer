package com.github.t1.deployer;

import javax.ws.rs.core.UriInfo;

public class DeploymentsFormHtmlWriter extends HtmlWriter {
    public DeploymentsFormHtmlWriter(UriInfo uriInfo) {
        super(uriInfo);
    }

    @Override
    protected String title() {
        return "Add Deployment";
    }

    @Override
    protected String body() {
        StringBuilder out = new StringBuilder();
        out.append("    <form>\n");
        out.append("    </form>\n");
        return out.toString();
    }
}
