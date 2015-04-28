package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.model.LoggerConfig.*;
import static com.github.t1.log.LogLevel.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.*;

import javax.ws.rs.core.*;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.github.t1.deployer.model.LoggerConfig;

/**
 * this should currently be seen as an experiment... I'm not sure if the html is going to stay 'semantic' enough to be
 * practical to test like this.
 */
@RunWith(MockitoJUnitRunner.class)
public class LoggerHtmlWriterTest {
    @InjectMocks
    LoggerHtmlWriter writer;
    @Mock
    UriInfo uriInfo;

    @Before
    public void setup() {
        when(uriInfo.getBaseUriBuilder()).then(new Answer<UriBuilder>() {
            @Override
            public UriBuilder answer(InvocationOnMock invocation) {
                return new JerseyUriBuilder().uri("http://localhost:8080/deployer");
            }
        });
    }

    private String write(LoggerConfig logger) throws IOException {
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        writer.writeTo(logger, getClass(), getClass(), null, null, null, entityStream);
        return new String(entityStream.toByteArray());
    }

    private String header(String title) {
        return "<!DOCTYPE html>\n" //
                + "<html>\n" //
                + "  <head>\n" //
                + "    <meta charset=\"utf-8\">\n" //
                + "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" //
                + "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" //
                + "    <title>"
                + title
                + "</title>\n" //
                + "\n" //
                + "    <link href=\"http://localhost:8080/deployer/bootstrap/css/bootstrap.css\" rel=\"stylesheet\"/>\n" //
                + "    <link href=\"http://localhost:8080/deployer/webapp/css/style.css\" rel=\"stylesheet\"/>\n" //
                + "  </head>\n" //
                + "  <body class=\"container\">\n" //
                + "      <nav class=\"navbar navbar-default\">\n" //
                + "        <div class=\"container-fluid\">\n" //
                + "          <div class=\"navbar-header\">\n" //
                + "            <a class=\"navbar-brand\" href=\"#\">Deployer</a>\n" //
                + "          </div>\n" //
                + "          <div id=\"navbar\" class=\"navbar-collapse collapse\">\n" //
                + "            <ul class=\"nav navbar-nav navbar-right\">\n" //
                + "              <li ><a href=\"http://localhost:8080/deployer/deployments/*\">Deployments</a></li>\n" //
                + "              <li class=\"active\"><a href=\"http://localhost:8080/deployer/loggers\">Loggers</a></li>\n" //
                + "            </ul>\n" //
                + "          </div>\n" //
                + "        </div>\n" //
                + "      </nav>\n" //
                + "\n" //
                + "  <div class=\"jumbotron\">\n";
    }

    private String footer() {
        return "\n" //
                + "    <script src=\"http://localhost:8080/deployer/jquery/jquery.js\"/>\n" //
                + "    <script src=\"http://localhost:8080/deployer/bootstrap/js/bootstrap.js\"/>\n" //
                + "  </div>\n" //
                + "  </body>\n" //
                + "</html>\n";
    }

    @Test
    public void shouldWriteNewLoggerForm() throws Exception {
        LoggerConfig logger = new LoggerConfig(NEW_LOGGER, INFO);

        String entity = write(logger);

        assertEquals(header("Add Logger") //
                + "    <h1>Add Logger</h1>\n" //
                + "\n" //
                + "    <a href=\"http://localhost:8080/deployer/loggers\">&lt</a>\n" //
                + "    <p>Enter the name of a new logger to configure</p>\n" //
                + "    <form method=\"POST\" action=\"http://localhost:8080/deployer/loggers\">\n" //
                + "      <input name=\"category\"/>\n" //
                + "      <select name=\"level\"\n" //
                + "        <option>ALL</option>\n" //
                + "        <option>TRACE</option>\n" //
                + "        <option>DEBUG</option>\n" //
                + "        <option selected>INFO</option>\n" //
                + "        <option>WARN</option>\n" //
                + "        <option>ERROR</option>\n" //
                + "        <option>OFF</option>\n" //
                + "      </select>\n" //
                + "      <input type=\"submit\" value=\"Add\">\n" //
                + "    </form>\n" //
                + footer() //
        , entity);
    }

    @Test
    public void shouldWriteExistinLoggerForm() throws Exception {
        LoggerConfig logger = new LoggerConfig("foo", INFO);

        String entity = write(logger);

        assertEquals(header("Logger: foo") //
                + "    <h1>foo</h1>\n" //
                + "\n" //
                + "    <a href=\"http://localhost:8080/deployer/loggers\">&lt</a>\n" //
                + "    <form method=\"POST\" action=\"http://localhost:8080/deployer/loggers\">\n" //
                + "      <select name=\"level\"\n" //
                + "        <option>ALL</option>\n" //
                + "        <option>TRACE</option>\n" //
                + "        <option>DEBUG</option>\n" //
                + "        <option selected>INFO</option>\n" //
                + "        <option>WARN</option>\n" //
                + "        <option>ERROR</option>\n" //
                + "        <option>OFF</option>\n" //
                + "      </select>\n" //
                + "      <input type=\"submit\" value=\"Update\">\n" //
                + "    </form>\n" //
                + "    <form method=\"POST\" action=\"http://localhost:8080/deployer/loggers/foo\">\n" //
                + "      <input type=\"hidden\" name=\"action\" value=\"delete\">\n" //
                + "      <input type=\"submit\" value=\"Delete\">\n" //
                + "    </form>\n" //
                + footer() //
        , entity);
    }
}
