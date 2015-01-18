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
                + "    <link href=\"" + base("css/bootstrap.min.css") + "\" rel=\"stylesheet\">\n" //
                + "    <link href=\"" + base("css/style.css") + "\" rel=\"stylesheet\">\n" //
                + "    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->\n" //
                + "    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->\n" //
                + "    <!--[if lt IE 9]>\n" //
                + "      <script src=\"https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js\"></script>\n" //
                + "      <script src=\"https://oss.maxcdn.com/respond/1.4.2/respond.min.js\"></script>\n" //
                + "    <![endif]-->\n" //
                + "  </head>\n" //
                + "  <body>\n" //
                + "    <h1>" + title() + "</h1>\n" //
                + "\n" //
                + body() //
                + "\n" //
                + "    <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js\"></script>\n" //
                + "    <script src=\"" + base("js/bootstrap.min.js") + "\"></script>\n" //
                + "  </body>\n" //
                + "</html>";
    }

    protected URI base(String path) {
        return uriInfo.getBaseUriBuilder().path(path).build();
    }

    protected abstract String title();

    protected abstract String body();
}
