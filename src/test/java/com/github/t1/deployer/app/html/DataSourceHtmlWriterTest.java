package com.github.t1.deployer.app.html;

import static com.github.t1.deployer.model.DataSourceConfig.*;
import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.github.t1.deployer.model.DataSourceConfig;

@RunWith(MockitoJUnitRunner.class)
public class DataSourceHtmlWriterTest extends AbstractHtmlWriterTest<DataSourceConfig> {
    public DataSourceHtmlWriterTest() {
        super(new DataSourceHtmlWriter());
    }

    @Test
    public void shouldWriteNewDataSourceForm() throws Exception {
        DataSourceConfig dataSource = DataSourceConfig.builder().name(NEW_DATA_SOURCE).build();

        String entity = write(dataSource);

        assertEquals(readFile(), entity);
    }

    @Test
    public void shouldWriteExistingDataSourceForm() throws Exception {
        DataSourceConfig dataSource = DataSourceConfig.builder().name("foo").uri(URI.create("foo-uri")).build();

        String entity = write(dataSource);

        assertEquals(readFile(), entity);
    }
}
