package com.github.t1.deployer.app.html;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import com.github.t1.deployer.app.Navigation;
import com.github.t1.deployer.app.html.builder.*;
import com.github.t1.deployer.app.html.builder.Tags.AppendingComponent;

public class NavigationLink {
    public static Component link(final Navigation navigation) {
        return new AppendingComponent<URI>() {
            @Override
            protected URI contentFrom(BuildContext out) {
                return navigation.uri(out.get(UriInfo.class));
            }
        };
    }
}
