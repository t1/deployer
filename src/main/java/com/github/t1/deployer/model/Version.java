package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

import java.util.Comparator;

import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.github.t1.ramlap.ApiExample;

import lombok.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
@XmlAccessorType(XmlAccessType.NONE)
@JsonSerialize(using = ToStringSerializer.class)
public class Version implements Comparable<Version> {
    @NonNull
    @XmlValue
    @ApiExample("1.12.3")
    private String version;

    @Override
    public String toString() {
        return version;
    }

    @Override
    public int compareTo(Version that) {
        return COMPARATOR.compare(this, that);
    }

    public static final Comparator<Version> COMPARATOR = new Comparator<Version>() {
        @Override
        public int compare(Version thisVersion, Version thatVersion) {
            String[] thisParts = split(thisVersion.version);
            String[] thatParts = split(thatVersion.version);
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
