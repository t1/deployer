package com.github.t1.deployer.app.html;

import static org.mockito.Mockito.*;

import java.io.*;

import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.junit.Before;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * this should currently be seen as an experiment... I'm not sure if the html is going to stay 'semantic' enough to be
 * practical to test like this.
 */
public class AbstractHtmlWriterTest<T> {
    @InjectMocks
    MessageBodyWriter<T> writer;
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

    protected String write(T target) throws IOException {
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        writer.writeTo(target, getClass(), getClass(), null, null, null, entityStream);
        return new String(entityStream.toByteArray());
    }

    protected String header(String title) {
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
                + "    <nav class=\"navbar navbar-default\">\n" //
                + "      <div class=\"container-fluid\">\n" //
                + "        <div class=\"navbar-header\">\n" //
                + "          <a class=\"navbar-brand\" href=\"#\">Deployer</a>\n" //
                + "        </div>\n" //
                + "        <div id=\"navbar\" class=\"navbar-collapse collapse\">\n" //
                + "          <ul class=\"nav navbar-nav navbar-right\">\n" //
                + "            <li ><a href=\"http://localhost:8080/deployer/deployments/*\">Deployments</a></li>\n" //
                + "            <li class=\"active\"><a href=\"http://localhost:8080/deployer/loggers\">Loggers</a></li>\n" //
                + "          </ul>\n" //
                + "        </div>\n" //
                + "      </div>\n" //
                + "    </nav>\n" //
                + "    \n" //
                + "    <div class=\"jumbotron\">\n";
    }

    protected String footer() {
        return "\n" //
                + "      <script src=\"http://localhost:8080/deployer/jquery/jquery.js\"/>\n" //
                + "      <script src=\"http://localhost:8080/deployer/bootstrap/js/bootstrap.js\"/>\n" //
                + "    </div>\n" //
                + "  </body>\n" //
                + "</html>\n";
    }

}
