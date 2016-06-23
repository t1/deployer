package com.github.t1.deployer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.*;
import java.util.Comparator;

import static lombok.AccessLevel.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@XmlAccessorType(XmlAccessType.NONE)
public class Version implements Comparable<Version> {
    @NonNull
    @XmlValue
    private final String value;

    @JsonCreator
    public Version(String value) { this.value = value; }

    /** this is called when YAML deserializes a version '1' */
    public Version(int value) { this.value = Integer.toString(value); }

    /** this is called when YAML deserializes a version '1.0' */
    public Version(double value) { this.value = Double.toString(value); }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(@NotNull Version that) { return COMPARATOR.compare(this, that); }

    public static final Comparator<Version> COMPARATOR = new Comparator<Version>() {
        @Override
        public int compare(Version thisVersion, Version thatVersion) {
            String[] thisParts = split(thisVersion.value);
            String[] thatParts = split(thatVersion.value);
            int i;
            for (i = 0; i < thisParts.length; i++) {
                String thisPart = thisParts[i];
                if (thatParts.length < i + 1)
                    return isNumeric(thisPart) ? 1 : -1; // .1 is bigger; -SNAPSHOT is smaller
                String thatPart = thatParts[i];

                if (thisPart.equals(thatPart))
                    continue;

                if (isNumeric(thisPart)) {
                    if (isNumeric(thatPart)) {
                        Integer thisInt = Integer.valueOf(thisPart);
                        Integer thatInt = Integer.valueOf(thatPart);
                        int c = thisInt.compareTo(thatInt);
                        if (c == 0)
                            continue;
                        return c;
                    }
                    return 1; // numbers are always bigger than strings
                }
                if (isNumeric(thatPart))
                    return -1; // strings are always smaller than numbers
                return thisPart.compareToIgnoreCase(thatPart);
            }
            if (thatParts.length > i)
                return -1;
            return 0;
        }

        private String[] split(String thisVersion) {
            return thisVersion.split("[.-]");
        }

        private boolean isNumeric(String string) {
            return string.matches("\\d+");
        }
    };
}
