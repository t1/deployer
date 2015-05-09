package com.github.t1.deployer.app.html.builder2;

import lombok.*;
import lombok.experimental.NonFinal;

@Value
@NonFinal
@EqualsAndHashCode(callSuper = true)
public abstract class DelegateComponent<T extends Component> extends Component {

    T component;

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
