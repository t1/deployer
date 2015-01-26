package com.github.t1.deployer;

import static javax.ws.rs.core.Response.Status.*;

import java.io.IOException;
import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpHost;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

@Slf4j
public class RestClient implements AutoCloseable {
    @AllArgsConstructor
    public static class RestResponse implements AutoCloseable {
        private final CloseableHttpResponse response;

        public <T> T readEntity(Class<T> type) throws IOException {
            if (OK.getStatusCode() != response.getStatusLine().getStatusCode())
                throw new RuntimeException("rest error: " + response.getStatusLine());
            String string = EntityUtils.toString(response.getEntity());
            return new ObjectMapper().readValue(string, type);
        }

        @Override
        public void close() throws IOException {
            response.close();
        }
    }

    private final CloseableHttpClient http = HttpClients.createDefault();
    @Inject
    private URI baseUri;
    @Inject
    private Credentials credentials;

    public static RestClient of(URI uri) {
        RestClient result = new RestClient();
        result.baseUri = uri;
        return result;
    }

    @Override
    public void close() throws IOException {
        http.close();
    }

    @SuppressWarnings("resource")
    public RestResponse get(URI uri) throws IOException, ClientProtocolException {
        log.debug("GET {}", uri);
        HttpHost targetHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
        CloseableHttpResponse response = http.execute(targetHost, new HttpGet(uri), authContext(targetHost));
        log.debug("response {}", response);
        return new RestResponse(response);
    }

    private HttpClientContext authContext(HttpHost host) {
        HttpClientContext context = HttpClientContext.create();

        if (credentials != null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(host.getHostName(), host.getPort()), credentials);
            context.setCredentialsProvider(credsProvider);

            AuthCache authCache = new BasicAuthCache();
            authCache.put(host, new BasicScheme());
            context.setAuthCache(authCache);
        }

        return context;
    }

    public UriBuilder base() {
        return UriBuilder.fromUri(baseUri);
    }
}
