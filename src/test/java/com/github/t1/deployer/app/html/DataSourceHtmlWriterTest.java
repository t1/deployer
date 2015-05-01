package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static com.github.t1.deployer.model.DataSourceConfig.*;
import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.model.DataSourceConfig;

@RunWith(MockitoJUnitRunner.class)
public class DataSourceHtmlWriterTest extends AbstractHtmlWriterTest<DataSourceConfig> {
    {
        super.writer = new DataSourceHtmlWriter();
    }

    @Test
    public void shouldWriteNewDataSourceForm() throws Exception {
        DataSourceConfig dataSource = DataSourceConfig.builder().name(NEW_DATA_SOURCE).build();

        String entity = write(dataSource);

        assertEquals(header("Add Data-Source", DATA_SOURCES) //
                + "      <h1>Add Data-Source</h1>\n" //
                + "\n" //
                + "      <a href=\"http://localhost:8080/deployer/data-sources\">&lt</a>\n" //
                + "      <p>Enter the name of a new data source to configure</p>\n" //
                + "      <form method=\"POST\" action=\"http://localhost:8080/deployer/data-sources\">\n" //
                + "        <label for=\"name\">Name</label>\n" //
                + "        <input class=\"form-control\" name=\"name id=\"name\" required/>\n" //
                + "        <label for=\"uri\">URI</label>\n" //
                + "        <input class=\"form-control\" name=\"uri id=\"uri\" required/>\n" //
                + "        <div class=\"btn-group btn-group-justified\" role=\"group\">\n" //
                + "          <div class=\"btn-group\" role=\"group\">\n" //
                + "            <button class=\"btn btn-lg btn-primary btn-block\" type=\"submit\">Add</button>\n" //
                + "          </div>\n" //
                + "        </div>\n" //
                + "      </form>\n" //
                + footer() //
        , entity);
    }

    @Test
    public void shouldWriteExistingDataSourceForm() throws Exception {
        DataSourceConfig dataSource = DataSourceConfig.builder().name("foo").uri(URI.create("foo-uri")).build();

        String entity = write(dataSource);

        assertEquals(header("Data-Source: foo", DATA_SOURCES) //
                + "      <h1>foo</h1>\n" //
                + "\n" //
                + "      <a href=\"http://localhost:8080/deployer/data-sources\">&lt</a>\n" //
                + "      <form method=\"POST\" action=\"http://localhost:8080/deployer/data-sources/foo\">\n" //
                + "        <input type=\"hidden\" name=\"action\" value=\"delete\"/>\n" //
                + "        <div class=\"btn-group btn-group-justified\" role=\"group\">\n" //
                + "          <div class=\"btn-group\" role=\"group\">\n" //
                + "            <button class=\"btn btn-lg btn-danger btn-block\" type=\"submit\">\n" //
                + "              <span class=\"glyphicon glyphicon-remove\"></span>\n" //
                + "            </button>\n" //
                + "          </div>\n" //
                + "        </div>\n" //
                + "      </form>\n" //
                + "      <form method=\"POST\" action=\"http://localhost:8080/deployer/data-sources/foo\">\n" //
                + "        <label for=\"name\">Name</label>\n" //
                + "        <input class=\"form-control\" name=\"name id=\"name\" value=\"foo\" required/>\n" //
                + "        <label for=\"uri\">URI</label>\n" //
                + "        <input class=\"form-control\" name=\"uri id=\"uri\" value=\"foo-uri\" required/>\n" //
                + "        <div class=\"btn-group btn-group-justified\" role=\"group\">\n" //
                + "          <div class=\"btn-group\" role=\"group\">\n" //
                + "            <button class=\"btn btn-lg btn-primary btn-block\" type=\"submit\">Update</button>\n" //
                + "          </div>\n" //
                + "        </div>\n" //
                + "      </form>\n" //
                + footer() //
        , entity);
    }

    // TODO group buttons
}
