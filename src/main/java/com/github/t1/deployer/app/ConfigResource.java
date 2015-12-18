package com.github.t1.deployer.app;

import static java.util.stream.Collectors.*;

import java.net.URI;
import java.util.*;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.github.t1.config.ConfigInfo;
import com.github.t1.ramlap.annotations.ApiResponse;
import com.github.t1.ramlap.tools.ProblemDetail.BadRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Boundary
@Path("/config")
public class ConfigResource {
    public static class InvalidDuplicationOfFormParameters extends BadRequest {}

    public static class InvalidFormParameter extends BadRequest {}

    public static final Comparator<? super ConfigInfo> BY_ORDER = (left, right) -> order(left).compareTo(order(right));

    private static Integer order(ConfigInfo left) {
        return left.getMeta().getInt("order", 0);
    }

    private static UriBuilder baseBuilder(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(ConfigResource.class);
    }

    public static URI base(UriInfo uriInfo) {
        return baseBuilder(uriInfo).build();
    }

    @Inject
    List<ConfigInfo> configs;

    @GET
    public List<ConfigInfo> getConfigs() {
        return configs.stream().sorted(BY_ORDER).collect(toList());
    }

    @POST
    @ApiResponse(type = InvalidDuplicationOfFormParameters.class)
    @ApiResponse(type = InvalidFormParameter.class)
    public Response postConfig(Form form) {
        configs.forEach(config -> update(config, formValue(form, config.getName())));
        if (!form.asMap().isEmpty())
            return new InvalidFormParameter().toResponse();
        return Response.noContent().build();
    }

    private String formValue(Form form, String name) {
        return singleString(form.asMap().remove(name));
    }

    private String singleString(List<String> values) {
        if (values == null)
            return null;
        if (values.size() != 1)
            throw new InvalidDuplicationOfFormParameters().toWebException();
        return values.get(0);
    }

    private void update(ConfigInfo configInfo, String value) {
        if (Objects.equals(value, stringValue(configInfo))) {
            log.debug("don't update {} to old value", configInfo.getName());
        } else {
            log.debug("update {} to {}: [{}]", configInfo.getName(), configInfo.getType().getName(),
                    isConfidential(configInfo) ? "?" : value);
            configInfo.updateTo(value);
        }
    }

    private String stringValue(ConfigInfo configInfo) {
        return (configInfo.getValue() == null) ? null : configInfo.getValue().toString();
    }

    private boolean isConfidential(ConfigInfo configInfo) {
        return configInfo.getMeta().getBoolean("confidential", false);
    }
}
