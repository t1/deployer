package com.github.t1.deployer.app;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Navigation {
    deployments("Deployments", null) {
        @Override
        public URI uri(UriInfo uriInfo) {
            return Deployments.pathAll(uriInfo);
        }
    },
    loggers("Loggers", null) {
        @Override
        public URI uri(UriInfo uriInfo) {
            return Loggers.base(uriInfo);
        }
    },
    datasources("Data-Sources", null) {
        @Override
        public URI uri(UriInfo uriInfo) {
            return DataSources.base(uriInfo);
        }
    },
    config(null, "cog") {
        @Override
        public URI uri(UriInfo uriInfo) {
            return ConfigResource.base(uriInfo);
        }
    };

    private final String title;
    private final String icon;

    public abstract URI uri(UriInfo uriInfo);
}
