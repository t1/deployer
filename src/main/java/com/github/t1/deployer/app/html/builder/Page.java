package com.github.t1.deployer.app.html.builder;

import java.net.URI;

public abstract class Page extends HtmlBuilder {
    private static final String MIN = ""; // ".min";

    public Page() {}

    public Page(HtmlBuilder container) {
        super(container);
    }

    public void html() {
        append("<!DOCTYPE html>\n");
        append("<html>\n"); // lang="en"
        in();
        head();
        append("<body class=\"container-fluid\" style=\"padding-top: 70px\">\n");
        in();
        navBar();
        append("<div class=\"jumbotron\">\n");
        in();
        append("<h1>").append(bodyTitle()).append("</h1>\n");
        nl();
        body();
        out();
        append("</div>\n");
        nl();
        append(script("jquery/jquery" + MIN + ".js")).append("\n");
        append(script("bootstrap/js/bootstrap" + MIN + ".js")).append("\n");
        out();
        append("</body>\n");
        out();
        append("</html>\n");
    }

    public void head() {
        append("<head>\n");
        in();
        append("<meta charset=\"utf-8\">\n");
        append("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n");
        append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        append("<title>").append(headerTitle()).append("</title>\n");
        nl();
        append(stylesheet("bootstrap/css/bootstrap" + MIN + ".css")).append("\n");
        append(stylesheet("webapp/css/style.css")).append("\n");
        out();
        append("</head>\n");
    }

    public void navBar() {
        append("<nav class=\"navbar navbar-default navbar-fixed-top\">\n");
        in();
        append("<div class=\"container-fluid\">\n");
        in();
        append("<div class=\"navbar-header\">\n");
        append("  <a class=\"navbar-brand\" href=\"#\">Deployer</a>\n");
        append("</div>\n");
        append("<div id=\"navbar\" class=\"navbar-collapse collapse\">\n");
        in();
        append("<ul class=\"nav navbar-nav navbar-right\">\n");
        in();
        navigation();
        out();
        append("</ul>\n");
        out();
        append("</div>\n");
        out();
        append("</div>\n");
        out();
        append("</nav>\n");
        nl();
    }

    public abstract void navigation();

    public String headerTitle() {
        return title();
    }

    public String bodyTitle() {
        return title();
    }

    public abstract String title();

    public abstract void body();

    public abstract URI base(String path);

    private String stylesheet(String path) {
        return "<link href=\"" + base(path) + "\" rel=\"stylesheet\"/>";
    }

    private String script(String path) {
        return "<script src=\"" + base(path) + "\"/>";
    }
}
