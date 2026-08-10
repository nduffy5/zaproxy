/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2024 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap.network;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link ZapRedirectHandler}.
 *
 * <p>Covers: 301/302/303/307/308 status codes, redirect count limit (default 100), redirect loop
 * detection via URL history, cross-scheme redirect (http→https allowed, https→http blocked), and
 * the followRedirects=false configuration.
 */
class ZapRedirectHandlerTest {

    /** Default maximum number of redirects ZAP should follow before giving up. */
    private static final int DEFAULT_MAX_REDIRECTS = 100;

    private MockHttpServer mockServer;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockHttpServer();
        mockServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    // -------------------------------------------------------------------------
    // Construction / configuration
    // -------------------------------------------------------------------------

    @Test
    void shouldCreateHandlerWithDefaultMaxRedirects() {
        // Given / When
        ZapRedirectHandler handler = new ZapRedirectHandler();
        // Then
        assertThat(handler.getMaxRedirects(), is(equalTo(DEFAULT_MAX_REDIRECTS)));
    }

    @Test
    void shouldCreateHandlerWithFollowRedirectsEnabledByDefault() {
        // Given / When
        ZapRedirectHandler handler = new ZapRedirectHandler();
        // Then
        assertTrue(handler.isFollowRedirects());
    }

    @Test
    void shouldAllowConfiguringMaxRedirects() {
        // Given
        ZapRedirectHandler handler = new ZapRedirectHandler();
        // When
        handler.setMaxRedirects(50);
        // Then
        assertThat(handler.getMaxRedirects(), is(equalTo(50)));
    }

    @Test
    void shouldThrowWhenMaxRedirectsIsNegative() {
        // Given
        ZapRedirectHandler handler = new ZapRedirectHandler();
        // When / Then
        assertThrows(IllegalArgumentException.class, () -> handler.setMaxRedirects(-1));
    }

    @Test
    void shouldAllowDisablingRedirectFollowing() {
        // Given
        ZapRedirectHandler handler = new ZapRedirectHandler();
        // When
        handler.setFollowRedirects(false);
        // Then
        assertFalse(handler.isFollowRedirects());
    }

    // -------------------------------------------------------------------------
    // followRedirects = false
    // -------------------------------------------------------------------------

    @Test
    void shouldNotFollowRedirectWhenFollowRedirectsIsFalse() throws IOException {
        // Given
        String targetUrl = "http://localhost:" + mockServer.getPort() + "/target";
        mockServer.enqueueRedirect(301, targetUrl);
        mockServer.enqueueResponse(200, "OK");

        ZapRedirectHandler handler = new ZapRedirectHandler();
        handler.setFollowRedirects(false);

        String initialUrl = "http://localhost:" + mockServer.getPort() + "/start";

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(initialUrl);

        // Then
        assertThat(result.getFinalStatusCode(), is(equalTo(301)));
        assertThat(result.getRedirectCount(), is(equalTo(0)));
        assertThat(result.getFinalUrl(), is(equalTo(initialUrl)));
    }

    // -------------------------------------------------------------------------
    // Redirect status codes: 301, 302, 303, 307, 308
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(ints = {301, 302, 303, 307, 308})
    void shouldFollowRedirectForStatusCode(int statusCode) throws IOException {
        // Given
        String finalUrl = "http://localhost:" + mockServer.getPort() + "/final";
        mockServer.enqueueRedirect(statusCode, finalUrl);
        mockServer.enqueueResponse(200, "Final destination");

        ZapRedirectHandler handler = new ZapRedirectHandler();
        String initialUrl = "http://localhost:" + mockServer.getPort() + "/start";

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(initialUrl);

        // Then
        assertThat(
                "Should follow " + statusCode + " redirect",
                result.getFinalStatusCode(),
                is(equalTo(200)));
        assertThat(result.getRedirectCount(), is(equalTo(1)));
        assertThat(result.getFinalUrl(), is(equalTo(finalUrl)));
    }

    @Test
    void shouldNotFollowNonRedirectStatusCode() throws IOException {
        // Given
        mockServer.enqueueResponse(200, "OK");

        ZapRedirectHandler handler = new ZapRedirectHandler();
        String initialUrl = "http://localhost:" + mockServer.getPort() + "/page";

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(initialUrl);

        // Then
        assertThat(result.getFinalStatusCode(), is(equalTo(200)));
        assertThat(result.getRedirectCount(), is(equalTo(0)));
    }

    @Test
    void shouldNotFollowStatusCode304() throws IOException {
        // Given
        mockServer.enqueueResponse(304, "Not Modified");

        ZapRedirectHandler handler = new ZapRedirectHandler();
        String initialUrl = "http://localhost:" + mockServer.getPort() + "/resource";

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(initialUrl);

        // Then
        assertThat(result.getFinalStatusCode(), is(equalTo(304)));
        assertThat(result.getRedirectCount(), is(equalTo(0)));
    }

    // -------------------------------------------------------------------------
    // Redirect chain (multiple hops)
    // -------------------------------------------------------------------------

    @Test
    void shouldFollowChainOfRedirects() throws IOException {
        // Given
        String hop2 = "http://localhost:" + mockServer.getPort() + "/hop2";
        String hop3 = "http://localhost:" + mockServer.getPort() + "/hop3";
        String finalUrl = "http://localhost:" + mockServer.getPort() + "/final";

        mockServer.enqueueRedirect(301, hop2);
        mockServer.enqueueRedirect(302, hop3);
        mockServer.enqueueRedirect(303, finalUrl);
        mockServer.enqueueResponse(200, "Done");

        ZapRedirectHandler handler = new ZapRedirectHandler();
        String initialUrl = "http://localhost:" + mockServer.getPort() + "/start";

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(initialUrl);

        // Then
        assertThat(result.getFinalStatusCode(), is(equalTo(200)));
        assertThat(result.getRedirectCount(), is(equalTo(3)));
        assertThat(result.getFinalUrl(), is(equalTo(finalUrl)));
    }

    @Test
    void shouldRecordAllHopsInRedirectChain() throws IOException {
        // Given
        String hop1 = "http://localhost:" + mockServer.getPort() + "/hop1";
        String hop2 = "http://localhost:" + mockServer.getPort() + "/hop2";

        mockServer.enqueueRedirect(301, hop1);
        mockServer.enqueueRedirect(302, hop2);
        mockServer.enqueueResponse(200, "Done");

        ZapRedirectHandler handler = new ZapRedirectHandler();
        String initialUrl = "http://localhost:" + mockServer.getPort() + "/start";

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(initialUrl);

        // Then
        List<String> visitedUrls = result.getVisitedUrls();
        assertThat(visitedUrls, is(notNullValue()));
        assertThat(visitedUrls.size(), is(greaterThanOrEqualTo(3)));
        assertTrue(visitedUrls.contains(initialUrl));
        assertTrue(visitedUrls.contains(hop1));
        assertTrue(visitedUrls.contains(hop2));
    }

    // -------------------------------------------------------------------------
    // Maximum redirect count enforcement (default 100)
    // -------------------------------------------------------------------------

    @Test
    void shouldStopFollowingRedirectsAtDefaultMaximum() throws IOException {
        // Given – enqueue more redirects than the default maximum
        int redirectsToEnqueue = DEFAULT_MAX_REDIRECTS + 5;
        for (int i = 1; i <= redirectsToEnqueue; i++) {
            String nextUrl = "http://localhost:" + mockServer.getPort() + "/hop" + i;
            mockServer.enqueueRedirect(302, nextUrl);
        }
        mockServer.enqueueResponse(200, "Final");

        ZapRedirectHandler handler = new ZapRedirectHandler();
        String initialUrl = "http://localhost:" + mockServer.getPort() + "/start";

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(initialUrl);

        // Then – handler must stop at or before the default maximum
        assertThat(result.getRedirectCount(), is(equalTo(DEFAULT_MAX_REDIRECTS)));
        assertTrue(result.isMaxRedirectsReached());
    }

    @Test
    void shouldStopFollowingRedirectsAtConfiguredMaximum() throws IOException {
        // Given
        int customMax = 3;
        for (int i = 1; i <= customMax + 2; i++) {
            String nextUrl = "http://localhost:" + mockServer.getPort() + "/hop" + i;
            mockServer.enqueueRedirect(302, nextUrl);
        }
        mockServer.enqueueResponse(200, "Final");

        ZapRedirectHandler handler = new ZapRedirectHandler();
        handler.setMaxRedirects(customMax);
        String initialUrl = "http://localhost:" + mockServer.getPort() + "/start";

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(initialUrl);

        // Then
        assertThat(result.getRedirectCount(), is(equalTo(customMax)));
        assertTrue(result.isMaxRedirectsReached());
    }

    @Test
    void shouldNotReportMaxRedirectsReachedWhenUnderLimit() throws IOException {
        // Given
        String finalUrl = "http://localhost:" + mockServer.getPort() + "/final";
        mockServer.enqueueRedirect(301, finalUrl);
        mockServer.enqueueResponse(200, "OK");

        ZapRedirectHandler handler = new ZapRedirectHandler();
        String initialUrl = "http://localhost:" + mockServer.getPort() + "/start";

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(initialUrl);

        // Then
        assertFalse(result.isMaxRedirectsReached());
    }

    // -------------------------------------------------------------------------
    // Redirect loop detection via URL history
    // -------------------------------------------------------------------------

    @Test
    void shouldDetectDirectRedirectLoop() throws IOException {
        // Given – A → A (self-redirect)
        String loopUrl = "http://localhost:" + mockServer.getPort() + "/loop";
        // Enqueue enough responses so the handler can detect the loop
        for (int i = 0; i < 5; i++) {
            mockServer.enqueueRedirect(302, loopUrl);
        }

        ZapRedirectHandler handler = new ZapRedirectHandler();

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(loopUrl);

        // Then
        assertTrue(result.isLoopDetected());
    }

    @Test
    void shouldDetectIndirectRedirectLoop() throws IOException {
        // Given – A → B → A (indirect loop)
        String urlA = "http://localhost:" + mockServer.getPort() + "/a";
        String urlB = "http://localhost:" + mockServer.getPort() + "/b";

        // Enqueue enough responses for the loop to be detected
        for (int i = 0; i < 6; i++) {
            mockServer.enqueueRedirect(302, urlB);
            mockServer.enqueueRedirect(302, urlA);
        }

        ZapRedirectHandler handler = new ZapRedirectHandler();

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(urlA);

        // Then
        assertTrue(result.isLoopDetected());
    }

    @Test
    void shouldNotFalselyReportLoopForLinearChain() throws IOException {
        // Given – A → B → C (no loop)
        String urlB = "http://localhost:" + mockServer.getPort() + "/b";
        String urlC = "http://localhost:" + mockServer.getPort() + "/c";

        mockServer.enqueueRedirect(301, urlB);
        mockServer.enqueueRedirect(302, urlC);
        mockServer.enqueueResponse(200, "Done");

        ZapRedirectHandler handler = new ZapRedirectHandler();
        String urlA = "http://localhost:" + mockServer.getPort() + "/a";

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(urlA);

        // Then
        assertFalse(result.isLoopDetected());
        assertThat(result.getFinalStatusCode(), is(equalTo(200)));
    }

    // -------------------------------------------------------------------------
    // Cross-scheme redirect: http → https (allowed) and https → http (blocked)
    // -------------------------------------------------------------------------

    @Test
    void shouldAllowCrossSchemeRedirectFromHttpToHttps() {
        // Given
        ZapRedirectHandler handler = new ZapRedirectHandler();
        String fromUrl = "http://example.com/page";
        String toUrl = "https://example.com/page";

        // When
        boolean allowed = handler.isCrossSchemeRedirectAllowed(fromUrl, toUrl);

        // Then
        assertTrue(allowed, "Redirect from http to https should be allowed");
    }

    @Test
    void shouldBlockCrossSchemeRedirectFromHttpsToHttp() {
        // Given
        ZapRedirectHandler handler = new ZapRedirectHandler();
        String fromUrl = "https://example.com/page";
        String toUrl = "http://example.com/page";

        // When
        boolean allowed = handler.isCrossSchemeRedirectAllowed(fromUrl, toUrl);

        // Then
        assertFalse(allowed, "Redirect from https to http should be blocked (downgrade)");
    }

    @Test
    void shouldAllowSameSchemeHttpRedirect() {
        // Given
        ZapRedirectHandler handler = new ZapRedirectHandler();
        String fromUrl = "http://example.com/a";
        String toUrl = "http://example.com/b";

        // When
        boolean allowed = handler.isCrossSchemeRedirectAllowed(fromUrl, toUrl);

        // Then
        assertTrue(allowed, "Same-scheme http redirect should be allowed");
    }

    @Test
    void shouldAllowSameSchemeHttpsRedirect() {
        // Given
        ZapRedirectHandler handler = new ZapRedirectHandler();
        String fromUrl = "https://example.com/a";
        String toUrl = "https://example.com/b";

        // When
        boolean allowed = handler.isCrossSchemeRedirectAllowed(fromUrl, toUrl);

        // Then
        assertTrue(allowed, "Same-scheme https redirect should be allowed");
    }

    @Test
    void shouldNotFollowHttpsToHttpRedirectDuringChain() throws IOException {
        // Given – server issues a redirect from http to https (allowed), then https to http
        // (blocked). We simulate this purely via the handler's cross-scheme check.
        ZapRedirectHandler handler = new ZapRedirectHandler();

        // Simulate the handler encountering an https→http redirect
        String fromHttps = "https://secure.example.com/resource";
        String toHttp = "http://insecure.example.com/resource";

        // When
        boolean allowed = handler.isCrossSchemeRedirectAllowed(fromHttps, toHttp);

        // Then
        assertFalse(allowed);
    }

    // -------------------------------------------------------------------------
    // Redirect validator integration
    // -------------------------------------------------------------------------

    @Test
    void shouldInvokeRedirectionValidatorForEachHop() throws IOException {
        // Given
        String hop1 = "http://localhost:" + mockServer.getPort() + "/hop1";
        String finalUrl = "http://localhost:" + mockServer.getPort() + "/final";

        mockServer.enqueueRedirect(301, hop1);
        mockServer.enqueueRedirect(302, finalUrl);
        mockServer.enqueueResponse(200, "Done");

        List<String> validatedUrls = new ArrayList<>();
        ZapRedirectHandler handler = new ZapRedirectHandler();
        handler.setRedirectionValidator(
                url -> {
                    validatedUrls.add(url);
                    return true; // allow all
                });

        String initialUrl = "http://localhost:" + mockServer.getPort() + "/start";

        // When
        handler.handleRequest(initialUrl);

        // Then – validator should have been called for each redirect target
        assertThat(validatedUrls.size(), is(equalTo(2)));
        assertTrue(validatedUrls.contains(hop1));
        assertTrue(validatedUrls.contains(finalUrl));
    }

    @Test
    void shouldStopFollowingRedirectsWhenValidatorRejectsUrl() throws IOException {
        // Given
        String blockedUrl = "http://localhost:" + mockServer.getPort() + "/blocked";
        mockServer.enqueueRedirect(302, blockedUrl);
        mockServer.enqueueResponse(200, "Should not reach here");

        ZapRedirectHandler handler = new ZapRedirectHandler();
        handler.setRedirectionValidator(url -> !url.contains("blocked"));

        String initialUrl = "http://localhost:" + mockServer.getPort() + "/start";

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(initialUrl);

        // Then – should stop at the 302 response, not follow to /blocked
        assertThat(result.getFinalStatusCode(), is(equalTo(302)));
        assertThat(result.getRedirectCount(), is(equalTo(0)));
    }

    // -------------------------------------------------------------------------
    // Redirect result captures hop messages
    // -------------------------------------------------------------------------

    @Test
    void shouldCaptureEachHopMessageForProxyHistory() throws IOException {
        // Given
        String hop1 = "http://localhost:" + mockServer.getPort() + "/hop1";
        String finalUrl = "http://localhost:" + mockServer.getPort() + "/final";

        mockServer.enqueueRedirect(307, hop1);
        mockServer.enqueueRedirect(308, finalUrl);
        mockServer.enqueueResponse(200, "Done");

        ZapRedirectHandler handler = new ZapRedirectHandler();
        String initialUrl = "http://localhost:" + mockServer.getPort() + "/start";

        // When
        ZapRedirectHandler.RedirectResult result = handler.handleRequest(initialUrl);

        // Then – each hop (including the initial request) should be captured
        assertThat(result.getHopCount(), is(equalTo(3))); // initial + 2 redirects
    }

    // -------------------------------------------------------------------------
    // Simple mock HTTP server for testing
    // -------------------------------------------------------------------------

    /**
     * A minimal single-threaded mock HTTP/1.1 server that serves pre-queued responses. Each call
     * to {@link #enqueueRedirect} or {@link #enqueueResponse} adds one response to the queue; the
     * server serves them in FIFO order.
     */
    private static class MockHttpServer {

        private ServerSocket serverSocket;
        private ExecutorService executor;
        private final List<byte[]> responseQueue = new ArrayList<>();
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile boolean running;

        void start() throws IOException {
            serverSocket = new ServerSocket(0); // bind to any free port
            running = true;
            executor = Executors.newSingleThreadExecutor();
            executor.submit(this::acceptLoop);
        }

        int getPort() {
            return serverSocket.getLocalPort();
        }

        void stop() throws IOException {
            running = false;
            serverSocket.close();
            executor.shutdownNow();
        }

        void enqueueRedirect(int statusCode, String location) {
            String response =
                    "HTTP/1.1 "
                            + statusCode
                            + " Redirect\r\n"
                            + "Location: "
                            + location
                            + "\r\n"
                            + "Content-Length: 0\r\n"
                            + "Connection: close\r\n"
                            + "\r\n";
            synchronized (responseQueue) {
                responseQueue.add(response.getBytes(StandardCharsets.UTF_8));
            }
        }

        void enqueueResponse(int statusCode, String body) {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            String response =
                    "HTTP/1.1 "
                            + statusCode
                            + " OK\r\n"
                            + "Content-Length: "
                            + bodyBytes.length
                            + "\r\n"
                            + "Connection: close\r\n"
                            + "\r\n"
                            + body;
            synchronized (responseQueue) {
                responseQueue.add(response.getBytes(StandardCharsets.UTF_8));
            }
        }

        private void acceptLoop() {
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    requestCount.incrementAndGet();
                    handleClient(client);
                } catch (IOException e) {
                    if (running) {
                        // Unexpected error during accept
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        private void handleClient(Socket client) {
            try (Socket s = client) {
                // Drain the request headers
                byte[] buf = new byte[4096];
                s.getInputStream().read(buf);

                byte[] responseBytes;
                synchronized (responseQueue) {
                    if (responseQueue.isEmpty()) {
                        // Default 500 if nothing queued
                        String err =
                                "HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\n\r\n";
                        responseBytes = err.getBytes(StandardCharsets.UTF_8);
                    } else {
                        responseBytes = responseQueue.remove(0);
                    }
                }

                OutputStream out = s.getOutputStream();
                out.write(responseBytes);
                out.flush();
            } catch (IOException e) {
                // Ignore client errors in tests
            }
        }
    }
}
