package com.github.t1.deployer.app.html;

import javax.ws.rs.core.UriInfo;

import lombok.*;

import com.github.t1.deployer.app.html.builder2.*;

@Value
@EqualsAndHashCode(callSuper = true)
public class NavigationHref extends Component {
    public static final ThreadLocal<UriInfo> URI_INFO = new ThreadLocal<>();

    private final Navigation navigation;

    @Override
    public void writeTo(BuildContext out) {
        out.append(navigation.href(URI_INFO.get()));
    }
}
