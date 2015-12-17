package com.github.t1.deployer.app;

import static com.github.t1.config.ConfigInfo.*;
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
    public List<ConfigInfo> getConfig() {
        return configs.stream().sorted(BY_ORDER).collect(toList());
    }

    @POST
    @ApiResponse(type = InvalidDuplicationOfFormParameters.class)
    @ApiResponse(type = InvalidFormParameter.class)
    public Response postConfig(Form form) {
        form.asMap().forEach((key, values) -> {
            String value = singleString(values);
            log.debug("update {} to {}", key, value);
            configs.stream()
                    .filter(byName(key))
                    .findAny()
                    .orElseThrow(() -> new InvalidFormParameter().toWebException())
                    .updateTo(value);
        });
        return Response.noContent().build();
    }

    private String singleString(List<String> values) {
        if (values.size() != 1)
            throw new InvalidDuplicationOfFormParameters().toWebException();
        return values.get(0);
    }
}
