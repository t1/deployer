package com.github.t1.deployer.app.html.builder;

public abstract class TargetHtmlBuilder<T> extends HtmlBuilder {
    public T target;

    public TargetHtmlBuilder(T target) {
        this.target = target;
    }

    public TargetHtmlBuilder(BaseBuilder container, T target) {
        super(container);
        this.target = target;
    }
}
