package com.github.t1.deployer.app;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Navigation {
    DEPLOYMENTS("Deployments", null) {
        @Override
        public URI uri(UriInfo uriInfo) {
            return Deployments.pathAll(uriInfo);
        }
    },
    LOGGERS("Loggers", null) {
        @Override
        public URI uri(UriInfo uriInfo) {
            return Loggers.base(uriInfo);
        }
    },
    DATA_SOURCES("Data-Sources", null) {
        @Override
        public URI uri(UriInfo uriInfo) {
            return DataSources.base(uriInfo);
        }
    },
    CONFIG(null, "cog") {
        @Override
        public URI uri(UriInfo uriInfo) {
            return DataSources.base(uriInfo);
        }

        @Override
        public String linkName() {
            return "config";
        }
    };

    private final String title;
    private final String icon;

    public abstract URI uri(UriInfo uriInfo);

    public String linkName() {
        return name().toLowerCase().replace("_", "");
    }
}
