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
                + "    <link href=\"" + base("bootstrap/css/bootstrap.min.css") + "\" rel=\"stylesheet\">\n" //
                + "    <link href=\"" + base("webapp/css/style.css") + "\" rel=\"stylesheet\">\n" //
                + "  </head>\n" //
                + "  <body>\n" //
                + "    <h1>" + title() + "</h1>\n" //
                + "\n" //
                + body() //
                + "\n" //
                + "    <script src=\"" + base("jquery/jquery.min.js") + "\"></script>\n" //
                + "    <script src=\"" + base("bootstrap/js/bootstrap.min.js") + "\"></script>\n" //
                + "  </body>\n" //
                + "</html>";
    }

    protected URI base(String path) {
        return uriInfo.getBaseUriBuilder().path(path).build();
    }

    protected abstract String title();

    protected abstract String body();
}
