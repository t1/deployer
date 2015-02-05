package com.github.t1.deployer;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class HtmlWriter {
    protected final UriInfo uriInfo;

    @Override
    public final String toString() {
        return "<!DOCTYPE html>\n" //
                + "<html>\n" // lang="en"
                + "  <head>\n" //
                + "    <meta charset=\"utf-8\">\n" //
                + "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" //
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" //
                + "    <title>" + title() + "</title>\n" //
                + "\n" //
                + "    " + stylesheet("bootstrap/css/bootstrap.min.css") + "\n" //
                + "    " + stylesheet("webapp/css/style.css") + "\n" //
                + "  </head>\n" //
                + "  <body>\n" //
                + "    <h1>" + title() + "</h1>\n" //
                + "\n" //
                + body() //
                + "\n" //
                + "    " + script("jquery/jquery.min.js") + "\n" //
                + "    " + script("bootstrap/js/bootstrap.min.js") + "\n" //
                + "  </body>\n" //
                + "</html>";
    }

    private String stylesheet(String path) {
        return "<link href=\"" + base(path) + "\" rel=\"stylesheet\"/>";
    }

    private String script(String path) {
        return "<script src=\"" + base(path) + "\"/>";
    }

    protected URI base(String path) {
        return uriInfo.getBaseUriBuilder().path(path).build();
    }

    protected abstract String title();

    protected abstract String body();
}
