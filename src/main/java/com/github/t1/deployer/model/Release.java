package com.github.t1.deployer.model;

import lombok.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.*;
import java.util.Comparator;

import static lombok.AccessLevel.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.NONE)
public class Release implements Comparable<Release> {
    public static final Comparator<Release> BY_VERSION = Comparator.comparing(r -> (r == null) ? null : r.getVersion());

    @NonNull
    @XmlValue
    Version version;

    @NonNull
    @XmlAttribute
    CheckSum checkSum;

    @Override
    public int compareTo(@NotNull Release that) { return BY_VERSION.compare(this, that); }
}
