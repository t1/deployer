package com.github.t1.deployer;

import static javax.ws.rs.core.Response.Status.*;
import static lombok.AccessLevel.*;

import java.io.*;
import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.*;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.*;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

@Slf4j
@NoArgsConstructor
public class RestClient implements AutoCloseable {
    @AllArgsConstructor(access = PRIVATE)
    public class RestRequest {
        private final HttpUriRequest request;

        public RestRequest header(String key, String value) {
            request.addHeader(key, value);
            return this;
        }

        @SuppressWarnings("resource")
        public RestResponse execute() throws IOException {
            URI uri = request.getURI();
            log.debug("{} {}", request.getMethod(), uri);
            HttpHost targetHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
            CloseableHttpResponse response = http.execute(targetHost, request, authContext(targetHost));
            log.debug("response {}", response);
            return new RestResponse(response);
        }
    }

    @AllArgsConstructor
    public static class RestResponse implements AutoCloseable {
        private final CloseableHttpResponse response;

        public void expecting(Status expected) {
            StatusLine actual = response.getStatusLine();
            if (expected.getStatusCode() != actual.getStatusCode())
                throw new RuntimeException("rest error: " //
                        + "expected " + expected.getStatusCode() + " " + expected.getReasonPhrase() //
                        + " but got " + actual.getStatusCode() + " " + actual.getReasonPhrase());
        }

        public <T> T readEntity(Class<T> type) throws IOException {
            expecting(OK);
            String string = EntityUtils.toString(response.getEntity());
            return new ObjectMapper().readValue(string, type);
        }

        public InputStream stream() throws IOException {
            expecting(OK);
            return response.getEntity().getContent();
        }

        @Override
        public void close() throws IOException {
            response.close();
        }
    }

    private final CloseableHttpClient http = HttpClients.createDefault();

    @Inject
    @Artifactory
    private URI baseUri;

    @Inject
    @Artifactory
    private Credentials credentials;

    public RestClient(URI baseUri) {
        this(baseUri, null);
    }

    public RestClient(URI baseUri, Credentials credentials) {
        this.baseUri = baseUri;
        this.credentials = credentials;
    }

    @Override
    public void close() throws IOException {
        http.close();
    }

    public RestRequest get(URI uri) {
        return new RestRequest(new HttpGet(uri));
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
