package com.github.t1.deployer.app.html.builder;

import lombok.*;

public abstract class TargetPage<T> extends Page {
    @Getter
    @Setter
    private T target;

    /** @deprecated just required by weld */
    @Deprecated
    public TargetPage() {
        this.target = null;
    }

    public TargetPage(T target) {
        this.target = target;
    }

    public TargetPage(HtmlBuilder container, T target) {
        super(container);
        this.target = target;
    }
}
