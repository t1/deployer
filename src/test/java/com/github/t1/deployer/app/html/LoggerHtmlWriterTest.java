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

        assertEquals(header("Add Logger") //
                + "      <h1>Add Logger</h1>\n" //
                + "\n" //
                + "      <a href=\"http://localhost:8080/deployer/loggers\">&lt</a>\n" //
                + "      <p>Enter the name of a new logger to configure</p>\n" //
                + "      <form method=\"POST\" action=\"http://localhost:8080/deployer/loggers\">\n" //
                + "        <input name=\"category\"/>\n" //
                + "        <select name=\"level\">\n" //
                + "          <option>ALL</option>\n" //
                + "          <option>TRACE</option>\n" //
                + "          <option>DEBUG</option>\n" //
                + "          <option selected>INFO</option>\n" //
                + "          <option>WARN</option>\n" //
                + "          <option>ERROR</option>\n" //
                + "          <option>OFF</option>\n" //
                + "        </select>\n" //
                + "        <input type=\"submit\" value=\"Add\">\n" //
                + "      </form>\n" //
                + footer() //
        , entity);
    }

    @Test
    public void shouldWriteExistingLoggerForm() throws Exception {
        LoggerConfig logger = new LoggerConfig("foo", INFO);

        String entity = write(logger);

        assertEquals(header("Logger: foo") //
                + "      <h1>foo</h1>\n" //
                + "\n" //
                + "      <a href=\"http://localhost:8080/deployer/loggers\">&lt</a>\n" //
                + "      <form method=\"POST\" action=\"http://localhost:8080/deployer/loggers/foo\">\n" //
                + "        <select name=\"level\" onchange=\"this.form.submit()\">\n" //
                + "          <option>ALL</option>\n" //
                + "          <option>TRACE</option>\n" //
                + "          <option>DEBUG</option>\n" //
                + "          <option selected>INFO</option>\n" //
                + "          <option>WARN</option>\n" //
                + "          <option>ERROR</option>\n" //
                + "          <option>OFF</option>\n" //
                + "        </select>\n" //
                + "        <noscript>\n" //
                + "          <input type=\"submit\" value=\"Update\">\n" //
                + "        </noscript>\n" //
                + "      </form>\n" //
                + "      <form method=\"POST\" action=\"http://localhost:8080/deployer/loggers/foo\">\n" //
                + "        <input type=\"hidden\" name=\"action\" value=\"delete\"/>\n" //
                + "        <input type=\"submit\" value=\"Delete\">\n" //
                + "      </form>\n" //
                + footer() //
        , entity);
    }
}
