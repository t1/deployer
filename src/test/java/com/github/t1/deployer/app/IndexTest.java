package com.github.t1.deployer.app;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.List;

import javax.ws.rs.core.*;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(MockitoJUnitRunner.class)
public class IndexTest {
    @Mock
    UriInfo uriInfo;

    @Before
    public void before() {
        when(uriInfo.getBaseUriBuilder()).then(new Answer<UriBuilder>() {
            @Override
            public UriBuilder answer(InvocationOnMock invocation) {
                return new JerseyUriBuilder();
            }
        });
    }

    private String json(Object object) {
        StringWriter out = new StringWriter();
        try {
            new ObjectMapper().writeValue(out, object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toString();
    }

    @Test
    public void shouldProduceJsonIndex() {
        Index index = new Index();

        List<Index.Link> links = index.getIndexList(uriInfo);

        assertEquals("[{" //
                + "\"uri\":\"/deployments/*\"," //
                + "\"rel\":\"deployments\"," //
                + "\"title\":\"Deployments\"" //
                + "},{" //
                + "\"uri\":\"/loggers\"," //
                + "\"rel\":\"loggers\"," //
                + "\"title\":\"Loggers\"" //
                + "},{" //
                + "\"uri\":\"/data-sources\"," //
                + "\"rel\":\"data-sources\"," //
                + "\"title\":\"Data-Sources\"" //
                + "}]", //
                json(links));
    }
}
