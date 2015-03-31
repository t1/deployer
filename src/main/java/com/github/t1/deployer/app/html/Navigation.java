package com.github.t1.deployer.app.html;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import lombok.*;
import lombok.experimental.Accessors;

import com.github.t1.deployer.app.*;

@RequiredArgsConstructor
public enum Navigation {
    DEPLOYMENTS("Deployments") {
        @Override
        public URI href(UriInfo uriInfo) {
            return Deployments.pathAll(uriInfo);
        }
    },
    LOGGERS("Loggers") {
        @Override
        public URI href(UriInfo uriInfo) {
            return Loggers.base(uriInfo);
        }
    };

    @Getter
    @Accessors(fluent = true)
    private final String title;

    public abstract URI href(UriInfo uriInfo);
}
