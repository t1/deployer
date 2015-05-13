package com.github.t1.deployer.app.html.builder;

import lombok.*;
import lombok.experimental.NonFinal;

@Value
@NonFinal
@EqualsAndHashCode(callSuper = true)
public abstract class DelegateComponent extends Component {

    Component component;

    @Override
    public void writeTo(BuildContext out) {
        component.writeTo(out);
    }

    @Override
    public String toString() {
        return component.toString();
    }

    @Override
    public boolean isMultiLine() {
        return component.isMultiLine();
    }
}
