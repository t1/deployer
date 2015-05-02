package com.github.t1.deployer.app.html;

import static com.github.t1.log.LogLevel.*;
import static java.util.Arrays.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.model.LoggerConfig;

@RunWith(MockitoJUnitRunner.class)
public class LoggerListHtmlWriterTest extends AbstractHtmlWriterTest<List<LoggerConfig>> {
    {
        super.writer = new LoggerListHtmlWriter();
    }

    @Test
    public void shouldWriteLoggerList() throws Exception {
        List<LoggerConfig> loggers = asList(new LoggerConfig("foo", INFO), new LoggerConfig("bar", DEBUG));

        String entity = write(loggers);

        assertEquals(readFile(), entity);
    }
}
