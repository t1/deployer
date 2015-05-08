package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.builder2.TextHtmlMessageBodyWriter.*;
import lombok.*;

import com.github.t1.deployer.app.html.builder2.*;

@Value
@EqualsAndHashCode(callSuper = true)
public class NavigationHref extends Component {
    private final Navigation navigation;

    @Override
    public void writeTo(BuildContext out) {
        out.append(navigation.href(URI_INFO.get()));
    }
}
