package com.github.t1.deployer.app.html;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import lombok.*;
import lombok.experimental.Accessors;

import com.github.t1.deployer.app.*;
import com.github.t1.deployer.app.html.builder2.*;

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
    },
    DATA_SOURCES("Data-Sources") {
        @Override
        public URI href(UriInfo uriInfo) {
            return DataSources.base(uriInfo);
        }
    };

    @Getter
    @Accessors(fluent = true)
    private final String title;

    public abstract URI href(UriInfo uriInfo);

    public Component link() {
        return new Component() {
            @Override
            public void writeTo(BuildContext out) {
                out.append(href(out.get(UriInfo.class)));
            }
        };
    }
}
