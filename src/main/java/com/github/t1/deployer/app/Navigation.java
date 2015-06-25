package com.github.t1.deployer.app;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import lombok.*;
import lombok.experimental.Accessors;

@RequiredArgsConstructor
public enum Navigation {
    DEPLOYMENTS("Deployments") {
        @Override
        public URI uri(UriInfo uriInfo) {
            return Deployments.pathAll(uriInfo);
        }
    },
    LOGGERS("Loggers") {
        @Override
        public URI uri(UriInfo uriInfo) {
            return Loggers.base(uriInfo);
        }
    },
    DATA_SOURCES("Data-Sources") {
        @Override
        public URI uri(UriInfo uriInfo) {
            return DataSources.base(uriInfo);
        }
    };

    @Getter
    @Accessors(fluent = true)
    private final String title;

    public abstract URI uri(UriInfo uriInfo);

    public String linkName() {
        return name().toLowerCase().replace("_", "");
    }
}
