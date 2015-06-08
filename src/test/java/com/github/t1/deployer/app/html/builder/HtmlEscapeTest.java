package com.github.t1.deployer.app.html.builder;

import static com.github.t1.deployer.app.html.builder.Static.*;
import static com.github.t1.deployer.app.html.builder.Tag.*;
import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

public class HtmlEscapeTest {
    private final Writer stringWriter = new StringWriter();
    private final BuildContext context = new BuildContext(text("foo")).writeTo(stringWriter);

    @Test
    public void shouldAppendEmptyString() {
        context.append("");

        assertEquals("foo", stringWriter.toString());
    }

    @Test
    public void shouldAppendSimpleString() {
        context.append("bar");

        assertEquals("foobar", stringWriter.toString());
    }

    @Test
    public void shouldAppendTag() {
        tag("bar").build().writeTo(context);

        assertEquals("foo<bar/>\n", stringWriter.toString());
    }

    @Test
    @Ignore
    public void shouldEscapeLessThan() {
        context.append("<");

        assertEquals("foo&lt;", stringWriter.toString());
    }
}
