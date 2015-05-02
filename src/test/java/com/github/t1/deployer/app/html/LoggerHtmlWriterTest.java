package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.model.LoggerConfig.*;
import static com.github.t1.log.LogLevel.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.model.LoggerConfig;

@RunWith(MockitoJUnitRunner.class)
public class LoggerHtmlWriterTest extends AbstractHtmlWriterTest<LoggerConfig> {
    {
        super.writer = new LoggerHtmlWriter();
    }

    @Test
    public void shouldWriteNewLoggerForm() throws Exception {
        LoggerConfig logger = new LoggerConfig(NEW_LOGGER, INFO);

        String entity = write(logger);

        assertEquals(readFile(), entity);
    }

    @Test
    public void shouldWriteExistingLoggerForm() throws Exception {
        LoggerConfig logger = new LoggerConfig("foo", INFO);

        String entity = write(logger);

        assertEquals(readFile(), entity);
    }
}
