package com.github.t1.deployer.app;


public class NewLoggerFormHtmlWriter extends HtmlWriter {
    @Override
    protected String title() {
        return "Add Logger";
    }

    @Override
    protected String body() {
        return "<p>Enter the name of a new logger to configure</p>" //
                + "<form method=\"POST\" action=\"" + Loggers.base(uriInfo) + "\">\n" //
                + "  <input name=\"category\">\n" //
                + "  <input name=\"level\">\n" //
                + "  <input type=\"submit\" value=\"Add\">\n" //
                + "</form>";
    }
}
