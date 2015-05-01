package com.github.t1.deployer.app.html;


public class AbstractHtmlWriter<T> extends HtmlBuilder {
    protected T target;

    public AbstractHtmlWriter(T target) {
        this.target = target;
    }

    public AbstractHtmlWriter(HtmlBuilder container, T target) {
        super(container);
        this.target = target;
    }
}
