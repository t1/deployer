package com.github.t1.deployer.app.html.builder;

import lombok.*;

public abstract class TargetHtmlBuilder<T> extends HtmlBuilder {
    @Getter
    @Setter
    private T target;

    /** @deprecated just required by weld */
    @Deprecated
    public TargetHtmlBuilder() {
        this.target = null;
    }

    public TargetHtmlBuilder(T target) {
        this.target = target;
    }

    public TargetHtmlBuilder(BaseBuilder container, T target) {
        super(container);
        this.target = target;
    }
}
