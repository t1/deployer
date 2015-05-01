package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.app.html.Navigation.*;
import static java.util.Arrays.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.model.DataSourceConfig;

@RunWith(MockitoJUnitRunner.class)
public class DataSourcesListHtmlWriterTest extends AbstractHtmlWriterTest<List<DataSourceConfig>> {
    {
        super.writer = new DataSourcesListHtmlWriter();
    }

    @Test
    public void shouldWriteExistinDataSourceForm() throws Exception {
        List<DataSourceConfig> dataSources = asList( //
                DataSourceConfig.builder().name("foo").build(), //
                DataSourceConfig.builder().name("bar").build());

        String entity = write(dataSources);

        assertEquals(
                header("Data-Sources", DATA_SOURCES) //
                        + "      <h1>Data-Sources</h1>\n" //
                        + "\n" //
                        + "      <table>\n" //
                        + "        <tr><td><a href=\"http://localhost:8080/deployer/data-sources/bar\">bar</a></td><td>\n" //
                        + "          <form method=\"POST\" action=\"http://localhost:8080/deployer/data-sources/bar\">\n" //
                        + "            <input type=\"hidden\" name=\"action\" value=\"delete\">\n" //
                        + "            <input type=\"submit\" value=\"Delete\">\n" //
                        + "          </form>\n" //
                        + "        </td></tr>\n" //
                        + "        <tr><td><a href=\"http://localhost:8080/deployer/data-sources/foo\">foo</a></td><td>\n" //
                        + "          <form method=\"POST\" action=\"http://localhost:8080/deployer/data-sources/foo\">\n" //
                        + "            <input type=\"hidden\" name=\"action\" value=\"delete\">\n" //
                        + "            <input type=\"submit\" value=\"Delete\">\n" //
                        + "          </form>\n" //
                        + "        </td></tr>\n" //
                        + "        <tr><td colspan='2'><a href=\"http://localhost:8080/deployer/data-sources/!\">+</a></td></tr>\n" //
                        + "      </table>\n" //
                        + footer() //
                , entity);
    }
}
