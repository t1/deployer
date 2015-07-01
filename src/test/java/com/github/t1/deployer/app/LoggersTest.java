package com.github.t1.deployer.app;

import static com.github.t1.deployer.app.Loggers.PostLoggerAction.*;
import static com.github.t1.deployer.model.LoggerConfig.*;
import static com.github.t1.log.LogLevel.*;
import static java.util.Arrays.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;

import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.t1.deployer.container.LoggerContainer;
import com.github.t1.deployer.model.*;
import com.github.t1.deployer.tools.StatusDetails;
import com.github.t1.log.LogLevel;

@RunWith(MockitoJUnitRunner.class)
public class LoggersTest {
    private static final LoggerPatch LEVEL_PATCH = LoggerPatch.loggerPatch().logLevel(TRACE).build();

    private static final LoggerConfig L1 = new LoggerConfig("l1", DEBUG);
    private static final LoggerConfig L2 = new LoggerConfig("l2", INFO);
    private static final LoggerConfig L3 = new LoggerConfig("l3", WARN);

    @InjectMocks
    Loggers loggers;

    @Mock
    LoggerContainer container;

    UriInfo uriInfo = mock(UriInfo.class);

    @Before
    public void setup() {
        when(uriInfo.getBaseUriBuilder()).thenAnswer(new Answer<UriBuilder>() {
            @Override
            public UriBuilder answer(InvocationOnMock invocation) {
                return new JerseyUriBuilder();
            }
        });
    }

    @After
    public void verifyAfter() {
        verify(container, atLeast(0)).hasLogger(anyString());
        verify(container, atLeast(0)).hasLogger(any(LoggerConfig.class));
        verify(container, atLeast(0)).getLogger(anyString());
        verify(container, atLeast(0)).getLoggers();
        verifyNoMoreInteractions(container);
    }

    private void givenLoggers(LoggerConfig... loggers) {
        for (LoggerConfig logger : loggers) {
            when(container.getLogger(logger.getCategory())).thenReturn(logger);
            when(container.hasLogger(logger)).thenReturn(true);
            when(container.hasLogger(logger.getCategory())).thenReturn(true);
        }
        when(container.getLoggers()).thenReturn(asList(loggers));
    }

    @Test
    public void shouldGetOneLogger() {
        givenLoggers(L1, L2, L3);

        LoggerConfig result = loggers.getLogger(L2.getCategory());

        assertEquals(L2, result);
    }

    @Test
    public void shouldPostNewLogger() {
        Response response = loggers.postNew(uriInfo, L2.getCategory(), L2.getLevel());

        verify(container).add(L2);
        assertEquals(SEE_OTHER, response.getStatusInfo());
        assertEquals(URI.create("/loggers"), response.getLocation());
    }

    @Test
    public void shouldPutNewLoggerWithCategory() {
        givenLoggers(L1, L3);

        Response response = loggers.put(uriInfo, L2.getCategory(), L2);

        verify(container).add(L2);
        assertEquals(CREATED, response.getStatusInfo());
        assertEquals(URI.create("/loggers/l2"), response.getLocation());
    }

    @Test
    public void shouldPutNewLoggerWithoutCategory() throws IOException {
        givenLoggers(L1, L3);

        LoggerConfig levelOnlyLogger = new ObjectMapper().readValue("{\"level\": \"INFO\"}", LoggerConfig.class);
        Response response = loggers.put(uriInfo, L2.getCategory(), levelOnlyLogger);

        verify(container).add(L2);
        assertEquals(CREATED, response.getStatusInfo());
        assertEquals(URI.create("/loggers/l2"), response.getLocation());
    }

    @Test
    public void shouldFailToPutLoggerWithDifferentCategory() {
        givenLoggers(L1, L3);

        try {
            loggers.put(uriInfo, L1.getCategory(), L2);
        } catch (WebApplicationException e) {
            StatusDetails entity = (StatusDetails) e.getResponse().getEntity();
            assertEquals(BAD_REQUEST, entity.getStatus());
            assertEquals("path category 'l1' and body category 'l2' don't match (and body category is not null).",
                    entity.getType());
        }
    }

    @Test
    public void shouldPutExistingLogger() {
        givenLoggers(L1, L2, L3);

        Response response = loggers.put(uriInfo, L2.getCategory(), L2);

        verify(container).update(L2);
        assertEquals(NO_CONTENT, response.getStatusInfo());
    }

    @Test
    public void shouldGetNewLogger() {
        givenLoggers(L1, L2, L3);

        LoggerConfig result = loggers.getLogger(NEW_LOGGER);

        assertEquals(NEW_LOGGER, result.getCategory());
        assertEquals(OFF, result.getLevel());
    }

    @Test
    public void shouldFailToGetUnknownLogger() {
        givenLoggers(L1, L3);

        LoggerConfig result = loggers.getLogger(L2.getCategory());

        assertNull(result);
    }

    @Test
    public void shouldGetAllLoggers() {
        givenLoggers(L1, L2, L3);

        List<LoggerConfig> result = loggers.getAllLoggers();

        assertEquals(asList(L1, L2, L3), result);
    }

    @Test
    public void shouldGetLogLevel() {
        givenLoggers(L2);

        LogLevel level = loggers.getLevel(L2.getCategory());

        assertEquals(INFO, level);
    }

    @Test
    public void shouldPatchLogLevel() {
        givenLoggers(L1, L2);

        Response response = loggers.patch(L2.getCategory(), LEVEL_PATCH);

        verify(container).update(L2.copy().level(TRACE).build());
        assertEquals(NO_CONTENT, response.getStatusInfo());
    }

    @Test
    public void shouldPutLogLevel() {
        givenLoggers(L1, L2);

        loggers.putLevel(L2.getCategory(), TRACE);

        verify(container).update(L2.copy().level(TRACE).build());
    }

    @Test
    public void shouldDelete() {
        givenLoggers(L1, L2);

        loggers.delete(L1.getCategory());

        verify(container).remove(L1);
    }

    @Test
    public void shouldPostLevelPatch() {
        givenLoggers(L1, L2);

        Response response = loggers.post(null, L2.getCategory(), patch, TRACE);

        verify(container).update(L2.copy().level(TRACE).build());
        assertEquals(NO_CONTENT, response.getStatusInfo());
    }

    @Test
    public void shouldPostDelete() {
        givenLoggers(L1, L2);

        Response response = loggers.post(uriInfo, L1.getCategory(), delete, null);

        verify(container).remove(L1);
        assertEquals(SEE_OTHER, response.getStatusInfo());
    }

    @Test
    public void shouldFailToPostMissingAction() {
        try {
            loggers.post(uriInfo, L1.getCategory(), null, null);
            fail("expected WebApplicationException");
        } catch (WebApplicationException e) {
            StatusDetails entity = (StatusDetails) e.getResponse().getEntity();
            assertEquals(BAD_REQUEST, entity.getStatus());
            assertEquals("missing action form param", entity.getType());
        }
    }
}
