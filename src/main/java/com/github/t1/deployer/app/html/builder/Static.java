package com.github.t1.deployer.app.html.builder;

import static lombok.AccessLevel.*;

import lombok.*;

@Value
@RequiredArgsConstructor
public class Static implements Component {
    public static Static nl() {
        return text("\n");
    }

    public static Static textOr(Object text, String nullValue) {
        return new Static((text == null) ? nullValue : text.toString());
    }

    public static Static text(Object text) {
        return new Static(text.toString());
    }

    @NonNull
    private final String text;

    @Getter(NONE)
    private final boolean multilineFlag;

    public Static(String text) {
        this(text, text.contains("\n"));
    }

    @Override
    public void writeTo(BuildContext out) {
        out.append(text);
    }

    @Override
    public boolean isMultiLine() {
        return multilineFlag;
    }

    @Override
    public String toString() {
        return "'" + text + "'";
    }

    public Static multiline() {
        return new Static(text, true);
    }
}
