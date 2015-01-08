package com.github.t1.deployer;

public abstract class HtmlWriter {
    @Override
    public final String toString() {
        return "<!DOCTYPE html>" //
                + "<html>" //
                + "<head>\n" //
                + "    <title>"
                + title()
                + "</title>\n" //
                + "    <meta charset=\"utf-8\" />\n" //
                + "    <link rel='stylesheet' href=\"http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css\" />\n" //
                + "    <link rel='stylesheet' href=\"../style.css\" />\n" //
                + "</head>\n" //
                + "\n" //
                + "<body>\n" //
                + "    <h1>" + title() + "</h1>\n" //
                + body() //
                + "</body>\n" + "</html>\n";
    }

    protected abstract String title();

    protected abstract String body();
}
