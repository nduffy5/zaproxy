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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Handles HTTP redirect following for ZAP's network layer.
 *
 * <p>Implements redirect-following behaviour that replaces Commons HttpClient's
 * {@code DefaultRedirectHandler}, preserving ZAP-specific semantics such as capturing each
 * redirect hop for the proxy history.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Supports 301, 302, 303, 307, and 308 redirect status codes.
 *   <li>Enforces a configurable maximum redirect count (default 100).
 *   <li>Detects redirect loops via URL history.
 *   <li>Blocks cross-scheme downgrades (https → http).
 *   <li>Respects a {@link #setFollowRedirects(boolean) followRedirects} flag.
 *   <li>Allows plugging in a custom {@link #setRedirectionValidator(Predicate) validator}.
 * </ul>
 *
 * @since 2.16.0
 */
public class ZapRedirectHandler {

    /** Default maximum number of redirects to follow before stopping. */
    public static final int DEFAULT_MAX_REDIRECTS = 100;

    private boolean followRedirects = true;
    private int maxRedirects = DEFAULT_MAX_REDIRECTS;
    private Predicate<String> redirectionValidator = url -> true;

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /**
     * Returns whether redirect following is enabled.
     *
     * @return {@code true} if redirects are followed, {@code false} otherwise.
     */
    public boolean isFollowRedirects() {
        return followRedirects;
    }

    /**
     * Sets whether redirect following is enabled.
     *
     * @param followRedirects {@code true} to follow redirects, {@code false} to stop at the first
     *     redirect response.
     */
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * Returns the maximum number of redirects that will be followed.
     *
     * @return the maximum redirect count.
     */
    public int getMaxRedirects() {
        return maxRedirects;
    }

    /**
     * Sets the maximum number of redirects to follow.
     *
     * @param maxRedirects the maximum redirect count, must be ≥ 0.
     * @throws IllegalArgumentException if {@code maxRedirects} is negative.
     */
    public void setMaxRedirects(int maxRedirects) {
        if (maxRedirects < 0) {
            throw new IllegalArgumentException(
                    "maxRedirects must be >= 0, got: " + maxRedirects);
        }
        this.maxRedirects = maxRedirects;
    }

    /**
     * Sets a custom validator that is called for each redirect target URL before following it.
     *
     * <p>If the validator returns {@code false} for a URL, redirect following stops.
     *
     * @param validator a predicate that receives the redirect target URL string; must not be {@code
     *     null}.
     */
    public void setRedirectionValidator(Predicate<String> validator) {
        if (validator == null) {
            throw new IllegalArgumentException("validator must not be null");
        }
        this.redirectionValidator = validator;
    }

    // -------------------------------------------------------------------------
    // Cross-scheme check
    // -------------------------------------------------------------------------

    /**
     * Determines whether a cross-scheme redirect is allowed.
     *
     * <p>Redirects from {@code http} to {@code https} are allowed (upgrade). Redirects from {@code
     * https} to {@code http} are blocked (downgrade). Same-scheme redirects are always allowed.
     *
     * @param fromUrl the URL of the current request.
     * @param toUrl the redirect target URL.
     * @return {@code true} if the redirect is allowed, {@code false} if it is a scheme downgrade.
     */
    public boolean isCrossSchemeRedirectAllowed(String fromUrl, String toUrl) {
        String fromScheme = extractScheme(fromUrl);
        String toScheme = extractScheme(toUrl);

        if (fromScheme == null || toScheme == null) {
            // Cannot determine schemes – allow by default
            return true;
        }

        // Block https → http (downgrade)
        if ("https".equalsIgnoreCase(fromScheme) && "http".equalsIgnoreCase(toScheme)) {
            return false;
        }

        return true;
    }

    private static String extractScheme(String url) {
        if (url == null) {
            return null;
        }
        int idx = url.indexOf("://");
        if (idx < 0) {
            return null;
        }
        return url.substring(0, idx).toLowerCase(java.util.Locale.ROOT);
    }

    // -------------------------------------------------------------------------
    // Request handling
    // -------------------------------------------------------------------------

    /**
     * Sends an HTTP GET request to {@code initialUrl} and follows any redirects according to the
     * current configuration.
     *
     * @param initialUrl the URL to request.
     * @return a {@link RedirectResult} describing the final response and all hops taken.
     * @throws IOException if a network error occurs.
     */
    public RedirectResult handleRequest(String initialUrl) throws IOException {
        List<String> visitedUrls = new ArrayList<>();
        List<HopMessage> hops = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        String currentUrl = initialUrl;
        int redirectCount = 0;
        boolean maxRedirectsReached = false;
        boolean loopDetected = false;
        int finalStatusCode = -1;

        while (true) {
            // Loop detection
            if (seenUrls.contains(currentUrl)) {
                loopDetected = true;
                break;
            }
            seenUrls.add(currentUrl);
            visitedUrls.add(currentUrl);

            // Perform the HTTP request
            ParsedResponse response = doGet(currentUrl);
            finalStatusCode = response.statusCode;
            hops.add(new HopMessage(currentUrl, response.statusCode, response.locationHeader));

            // If not following redirects, stop immediately
            if (!followRedirects) {
                break;
            }

            // Check if this is a redirect status code
            if (!isRedirectStatusCode(response.statusCode)) {
                break;
            }

            // No Location header → cannot follow
            if (response.locationHeader == null || response.locationHeader.isEmpty()) {
                break;
            }

            String nextUrl = resolveUrl(currentUrl, response.locationHeader);

            // Validate the redirect target
            if (!redirectionValidator.test(nextUrl)) {
                break;
            }

            // Cross-scheme check
            if (!isCrossSchemeRedirectAllowed(currentUrl, nextUrl)) {
                break;
            }

            // Enforce maximum redirect count
            if (redirectCount >= maxRedirects) {
                maxRedirectsReached = true;
                break;
            }

            redirectCount++;
            currentUrl = nextUrl;
        }

        return new RedirectResult(
                finalStatusCode,
                currentUrl,
                redirectCount,
                visitedUrls,
                hops,
                maxRedirectsReached,
                loopDetected);
    }

    /**
     * Tells whether the given HTTP status code indicates a redirect that should be followed.
     *
     * @param statusCode the HTTP status code.
     * @return {@code true} for 301, 302, 303, 307, 308; {@code false} otherwise.
     */
    private static boolean isRedirectStatusCode(int statusCode) {
        switch (statusCode) {
            case 301:
            case 302:
            case 303:
            case 307:
            case 308:
                return true;
            default:
                return false;
        }
    }

    /**
     * Resolves a (possibly relative) redirect location against the current URL.
     *
     * @param currentUrl the URL of the current request.
     * @param location the value of the {@code Location} header.
     * @return the absolute redirect target URL.
     */
    private static String resolveUrl(String currentUrl, String location) {
        try {
            URI base = new URI(currentUrl);
            URI resolved = base.resolve(location);
            return resolved.toString();
        } catch (URISyntaxException e) {
            // Fall back to using the location as-is
            return location;
        }
    }

    // -------------------------------------------------------------------------
    // Minimal HTTP/1.1 GET implementation
    // -------------------------------------------------------------------------

    /**
     * Performs a minimal HTTP/1.1 GET request and returns the status code and Location header.
     *
     * <p>This implementation is intentionally simple and is designed for use in tests with a
     * {@link org.zaproxy.zap.network.ZapRedirectHandlerTest.MockHttpServer}. Production usage
     * should delegate to the full ZAP HTTP sender infrastructure.
     *
     * @param url the URL to request.
     * @return the parsed response.
     * @throws IOException if a network error occurs.
     */
    private static ParsedResponse doGet(String url) throws IOException {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URL: " + url, e);
        }

        String host = uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        }
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (uri.getRawQuery() != null) {
            path = path + "?" + uri.getRawQuery();
        }

        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(5000);

            // Send request
            OutputStream out = socket.getOutputStream();
            String request =
                    "GET "
                            + path
                            + " HTTP/1.1\r\n"
                            + "Host: "
                            + host
                            + (uri.getPort() != -1 ? ":" + uri.getPort() : "")
                            + "\r\n"
                            + "Connection: close\r\n"
                            + "\r\n";
            out.write(request.getBytes(StandardCharsets.UTF_8));
            out.flush();

            // Read response headers
            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            String statusLine = reader.readLine();
            if (statusLine == null) {
                throw new IOException("Empty response from " + url);
            }

            int statusCode = parseStatusCode(statusLine);
            String locationHeader = null;

            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.toLowerCase(java.util.Locale.ROOT).startsWith("location:")) {
                    locationHeader = headerLine.substring("location:".length()).trim();
                }
            }

            return new ParsedResponse(statusCode, locationHeader);
        }
    }

    private static int parseStatusCode(String statusLine) throws IOException {
        // e.g. "HTTP/1.1 302 Found"
        String[] parts = statusLine.split(" ", 3);
        if (parts.length < 2) {
            throw new IOException("Malformed status line: " + statusLine);
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IOException("Cannot parse status code from: " + statusLine, e);
        }
    }

    // -------------------------------------------------------------------------
    // Inner types
    // -------------------------------------------------------------------------

    /** Holds the parsed HTTP response data needed for redirect handling. */
    private static class ParsedResponse {
        final int statusCode;
        final String locationHeader;

        ParsedResponse(int statusCode, String locationHeader) {
            this.statusCode = statusCode;
            this.locationHeader = locationHeader;
        }
    }

    /** Represents a single hop in a redirect chain. */
    public static class HopMessage {
        private final String url;
        private final int statusCode;
        private final String locationHeader;

        HopMessage(String url, int statusCode, String locationHeader) {
            this.url = url;
            this.statusCode = statusCode;
            this.locationHeader = locationHeader;
        }

        /**
         * Returns the URL that was requested for this hop.
         *
         * @return the request URL.
         */
        public String getUrl() {
            return url;
        }

        /**
         * Returns the HTTP status code received for this hop.
         *
         * @return the status code.
         */
        public int getStatusCode() {
            return statusCode;
        }

        /**
         * Returns the value of the {@code Location} header, or {@code null} if absent.
         *
         * @return the Location header value.
         */
        public String getLocationHeader() {
            return locationHeader;
        }
    }

    /**
     * Encapsulates the outcome of a {@link ZapRedirectHandler#handleRequest(String)} call,
     * including the final status code, all visited URLs, and diagnostic flags.
     */
    public static class RedirectResult {

        private final int finalStatusCode;
        private final String finalUrl;
        private final int redirectCount;
        private final List<String> visitedUrls;
        private final List<HopMessage> hops;
        private final boolean maxRedirectsReached;
        private final boolean loopDetected;

        RedirectResult(
                int finalStatusCode,
                String finalUrl,
                int redirectCount,
                List<String> visitedUrls,
                List<HopMessage> hops,
                boolean maxRedirectsReached,
                boolean loopDetected) {
            this.finalStatusCode = finalStatusCode;
            this.finalUrl = finalUrl;
            this.redirectCount = redirectCount;
            this.visitedUrls = new ArrayList<>(visitedUrls);
            this.hops = new ArrayList<>(hops);
            this.maxRedirectsReached = maxRedirectsReached;
            this.loopDetected = loopDetected;
        }

        /**
         * Returns the HTTP status code of the final response.
         *
         * @return the final status code.
         */
        public int getFinalStatusCode() {
            return finalStatusCode;
        }

        /**
         * Returns the URL of the final response.
         *
         * @return the final URL.
         */
        public String getFinalUrl() {
            return finalUrl;
        }

        /**
         * Returns the number of redirects that were followed.
         *
         * @return the redirect count.
         */
        public int getRedirectCount() {
            return redirectCount;
        }

        /**
         * Returns all URLs visited during the request, in order (including the initial URL).
         *
         * @return the list of visited URLs.
         */
        public List<String> getVisitedUrls() {
            return visitedUrls;
        }

        /**
         * Returns the list of {@link HopMessage} objects, one per request made (including the
         * initial request).
         *
         * @return the hop messages.
         */
        public List<HopMessage> getHops() {
            return hops;
        }

        /**
         * Returns the total number of hops (requests) made, including the initial request.
         *
         * @return the hop count.
         */
        public int getHopCount() {
            return hops.size();
        }

        /**
         * Returns {@code true} if redirect following was stopped because the maximum redirect count
         * was reached.
         *
         * @return {@code true} if the maximum was reached.
         */
        public boolean isMaxRedirectsReached() {
            return maxRedirectsReached;
        }

        /**
         * Returns {@code true} if a redirect loop was detected (a URL appeared more than once in
         * the redirect chain).
         *
         * @return {@code true} if a loop was detected.
         */
        public boolean isLoopDetected() {
            return loopDetected;
        }
    }
}
