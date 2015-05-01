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
                + "        <input name=\"name\"/>\n" //
                + "        <input name=\"uri\"/>\n" //
                + "        <input type=\"submit\" value=\"Add\">\n" //
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
                + "        <input name=\"name\" value=\"foo\"/>\n" //
                + "        <input name=\"uri\" value=\"foo-uri\"/>\n" //
                + "        <input type=\"submit\" value=\"Update\">\n" //
                + "      </form>\n" //
                + "      <form method=\"POST\" action=\"http://localhost:8080/deployer/data-sources/foo\">\n" //
                + "        <input type=\"hidden\" name=\"action\" value=\"delete\"/>\n" //
                + "        <input type=\"submit\" value=\"Delete\">\n" //
                + "      </form>\n" //
                + footer() //
        , entity);
    }

    // TODO field labels
}
