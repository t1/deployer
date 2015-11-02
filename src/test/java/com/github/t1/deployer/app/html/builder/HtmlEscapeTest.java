package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

public class HtmlEscapeTest {
    private final Writer stringWriter = new StringWriter();
    private final BuildContext context = new BuildContext();

    private void write(Component component) {
        context.write(component, stringWriter);
    }

    @Test
    public void shouldAppendEmptyString() {
        write(text(""));

        assertEquals("", stringWriter.toString());
    }

    @Test
    public void shouldAppendSimpleString() {
        write(text("bar"));

        assertEquals("bar", stringWriter.toString());
    }

    @Test
    public void shouldAppendTag() {
        write(tag("bar").build());

        assertEquals("<bar/>\n", stringWriter.toString());
    }

    // FIXME escape html!
    @Test
    @Ignore
    public void shouldEscapeLessThan() {
        write(text("<"));

        assertEquals("&lt;", stringWriter.toString());
    }
}
