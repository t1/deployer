package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.builder.Tags.*;

import javax.ws.rs.core.UriInfo;

import com.github.t1.deployer.app.Navigation;
import com.github.t1.deployer.app.html.builder.Component;

public class NavigationLink {
    public static Component link(Navigation navigation) {
        return append(out -> navigation.uri(out.get(UriInfo.class)));
    }
}
