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
    public void shouldWriteExistinLoggerForm() throws Exception {
        List<LoggerConfig> loggers = asList(new LoggerConfig("foo", INFO), new LoggerConfig("bar", DEBUG));

        String entity = write(loggers);

        assertEquals(header("Loggers") //
                + "      <h1>Loggers</h1>\n" //
                + "\n" //
                + "      <table>\n" //
                + "        <tr><td><a href=\"http://localhost:8080/deployer/loggers/foo\">foo</a></td><td>\n" //
                + "          <form method=\"POST\" action=\"http://localhost:8080/deployer/loggers/foo\">\n"
                + "            <select name=\"level\" onchange=\"this.form.submit()\">\n" //
                + "              <option>ALL</option>\n" //
                + "              <option>TRACE</option>\n" //
                + "              <option>DEBUG</option>\n" //
                + "              <option selected>INFO</option>\n" //
                + "              <option>WARN</option>\n" //
                + "              <option>ERROR</option>\n" //
                + "              <option>OFF</option>\n" //
                + "            </select>\n" //
                + "            <noscript>\n" //
                + "              <input type=\"submit\" value=\"Update\">\n" //
                + "            </noscript>\n" //
                + "          </form>\n" //
                + "          </td><td>" //
                + "          <form method=\"POST\" action=\"http://localhost:8080/deployer/loggers/foo\">\n" //
                + "            <input type=\"hidden\" name=\"action\" value=\"delete\">\n" //
                + "            <input type=\"submit\" value=\"Delete\">\n" //
                + "          </form>\n"
                + "        </td></tr>\n" //
                + "        <tr><td><a href=\"http://localhost:8080/deployer/loggers/bar\">bar</a></td><td>\n" //
                + "          <form method=\"POST\" action=\"http://localhost:8080/deployer/loggers/bar\">\n"
                + "            <select name=\"level\" onchange=\"this.form.submit()\">\n" //
                + "              <option>ALL</option>\n" //
                + "              <option>TRACE</option>\n" //
                + "              <option selected>DEBUG</option>\n" //
                + "              <option>INFO</option>\n" //
                + "              <option>WARN</option>\n" //
                + "              <option>ERROR</option>\n" //
                + "              <option>OFF</option>\n" //
                + "            </select>\n" //
                + "            <noscript>\n" //
                + "              <input type=\"submit\" value=\"Update\">\n" //
                + "            </noscript>\n" //
                + "          </form>\n" //
                + "          </td><td>" //
                + "          <form method=\"POST\" action=\"http://localhost:8080/deployer/loggers/bar\">\n" //
                + "            <input type=\"hidden\" name=\"action\" value=\"delete\">\n" //
                + "            <input type=\"submit\" value=\"Delete\">\n" //
                + "          </form>\n" //
                + "        </td></tr>\n" //
                + "        <tr><td colspan='3'><a href=\"http://localhost:8080/deployer/loggers/!\">+</a></td></tr>\n" //
                + "      </table>\n" //
                + footer() //
        , entity);
    }
}
