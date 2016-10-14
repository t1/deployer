package com.github.t1.deployer.tools;

import java.lang.reflect.*;
import java.util.Arrays;

import static java.util.stream.Collectors.*;

public class StringUtils {
    public static String typeString(Type type) {
        if (type == null)
            return null;
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return parameterizedType.getRawType().getTypeName()
                    + Arrays.stream(parameterizedType.getActualTypeArguments())
                            .map(StringUtils::typeString)
                            .collect(joining(", ", "<", ">"));
        } else {
            return type.getTypeName();
        }
    }
}
