package com.github.t1.deployer.container;

import lombok.*;

import javax.xml.bind.annotation.XmlValue;

import static lombok.AccessLevel.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
public class ContextRoot {
    @NonNull
    @XmlValue
    String value;

    @Override
    public String toString() {
        return value;
    }

    public boolean matches(@NonNull Deployment deployment) { return this.equals(deployment.getContextRoot()); }
}
