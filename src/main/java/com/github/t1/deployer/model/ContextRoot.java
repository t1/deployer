package com.github.t1.deployer.model;

import static lombok.AccessLevel.*;

import javax.xml.bind.annotation.XmlValue;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import io.swagger.annotations.*;
import lombok.*;

@ApiModel("The first path item of the uri of the app, a.k.a. web context")
@Value
@NoArgsConstructor(access = PRIVATE, force = true)
@RequiredArgsConstructor
@JsonSerialize(using = ToStringSerializer.class)
public class ContextRoot {
    @NonNull
    @XmlValue
    @ApiModelProperty(example = "myapp")
    String value;

    @Override
    public String toString() {
        return value;
    }
}
