package com.github.t1.deployer.container;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import javax.xml.bind.annotation.XmlValue;

import static lombok.AccessLevel.*;

@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
@JsonSerialize(using = ToStringSerializer.class)
public class LogHandlerName {
    public static final LogHandlerName ALL = new LogHandlerName("*");

    @NonNull
    @XmlValue
    String value;

    @Override
    public String toString() {
        return value;
    }

    public boolean matches(@NonNull LogHandlerResource logHandler) { return this.equals(logHandler.name()); }
}
