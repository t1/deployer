package com.github.t1.deployer.app;

import static java.util.Arrays.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.HashSet;

import javax.servlet.FilterChain;
import javax.servlet.http.*;
import javax.ws.rs.WebApplicationException;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.github.t1.deployer.tools.User;

@RunWith(MockitoJUnitRunner.class)
public class AuthorizationFilterTest {
    private static final HashSet<String> ALL_PRIVILEGES = new HashSet<>(asList("deploy", "redeploy", "undeploy"));

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final String JBOSS_SERVER_CONFIG_DIR = "jboss.server.config.dir";

    private static final String FOO_BAR = "Zm9vOmJhcg==";

    AuthorizationFilter filter = new AuthorizationFilter();

    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;
    @Mock
    FilterChain filterChain;

    User user;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private String oldConfigDir;
    private Path configDir;

    @Before
    public void setup() throws Exception {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                user = User.getCurrent();
                return null;
            }
        }).when(filterChain).doFilter(request, response);
        oldConfigDir = System.getProperty(JBOSS_SERVER_CONFIG_DIR);
        configDir = Files.createTempDirectory(getClass().getName() + "-config");
        System.setProperty(JBOSS_SERVER_CONFIG_DIR, configDir.toString());
    }

    @After
    public void cleanup() throws Exception {
        if (oldConfigDir != null)
            System.setProperty(JBOSS_SERVER_CONFIG_DIR, oldConfigDir);
        if (configDir != null && Files.isDirectory(configDir))
            for (Path configFile : Files.newDirectoryStream(configDir))
                Files.deleteIfExists(configFile);
        Files.deleteIfExists(configDir);
    }

    private void expectWebException(String message) {
        expectedException.expect(WebApplicationException.class);
        expectedException.expectMessage(message);
    }

    private void givenUser(String userName, String secret) throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(configDir.resolve("mgmt-users.properties"), UTF_8)) {
            writer.write(userName + ":" + secret + "\n");
        }
    }

    private void givenAuthorizationHeader(String string) {
        when(request.getHeader("authorization")).thenReturn(string);
    }

    private void assertAnonymous() {
        assertEquals("anonymous", user.getName());
        assertTrue(user.getPrivileges().isEmpty());
    }

    @Test
    public void shouldAuthorizeAnonymous() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertAnonymous();
    }

    @Test
    public void shouldNotAuthorizeWrongType() throws Exception {
        givenAuthorizationHeader("Digest 123");

        filter.doFilter(request, response, filterChain);

        assertAnonymous();
    }

    @Test
    public void shouldNotAuthorizeWithMissingColon() throws Exception {
        expectWebException("Bad Request");

        givenAuthorizationHeader("Basic abcd");

        filter.doFilter(request, response, filterChain);
    }

    @Test
    public void shouldNotAuthorizeUnknownUser() throws Exception {
        expectWebException("Unauthorized");

        givenAuthorizationHeader("Basic " + FOO_BAR);

        filter.doFilter(request, response, filterChain);
    }

    @Test
    public void shouldNotAuthorizeWrongPassword() throws Exception {
        givenUser("foo", "blub");
        expectWebException("Unauthorized");

        givenAuthorizationHeader("Basic " + FOO_BAR);

        filter.doFilter(request, response, filterChain);
    }

    @Test
    public void shouldAuthorize() throws Exception {
        givenUser("foo", "b44c2a300af96cbefc6e42e55e010b29");
        givenAuthorizationHeader("Basic " + FOO_BAR);

        filter.doFilter(request, response, filterChain);

        assertEquals("foo", user.getName());
        assertEquals(ALL_PRIVILEGES, user.getPrivileges());
    }
}
