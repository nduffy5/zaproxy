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

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Handles HTTP redirects manually, following each hop, enforcing a redirect limit, and detecting
 * redirect loops.
 *
 * <p>Uses a {@link RequestExecutor} callback for each hop so that intermediate responses can be
 * recorded (e.g. for proxy history).
 *
 * <p><strong>Note:</strong> Not part of the public API.
 *
 * @since 2.15.0
 */
public class ZapRedirectHandler {

    /** The maximum number of redirects that will be followed before an exception is thrown. */
    public static final int MAX_REDIRECTS = 100;

    private final RequestExecutor executor;

    /**
     * Constructs a {@code ZapRedirectHandler} with the given {@link RequestExecutor}.
     *
     * @param executor the callback invoked for each request hop; must not be {@code null}.
     * @throws NullPointerException if {@code executor} is {@code null}.
     */
    public ZapRedirectHandler(RequestExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "Parameter executor must not be null.");
    }

    /**
     * Executes the request for the given URI, following any redirects manually.
     *
     * <p>The {@link RequestExecutor} is called for each hop (initial request and every redirect).
     * Redirects are followed only when:
     *
     * <ul>
     *   <li>the response status code is one of 301, 302, 303, 307, or 308;
     *   <li>a non-null {@code Location} header is present;
     *   <li>the redirect target uses the same scheme or upgrades from {@code http} to {@code
     *       https};
     *   <li>the redirect target has not been visited before (loop detection);
     *   <li>the total number of redirects has not exceeded {@link #MAX_REDIRECTS}.
     * </ul>
     *
     * @param startUri the URI to request; must not be {@code null}.
     * @return the final {@link Response} after all redirects have been followed.
     * @throws IOException if an I/O error occurs, if the redirect limit is exceeded, if a redirect
     *     loop is detected, or if an invalid scheme change is attempted.
     * @throws NullPointerException if {@code startUri} is {@code null}.
     */
    public Response execute(URI startUri) throws IOException {
        Objects.requireNonNull(startUri, "Parameter startUri must not be null.");

        Set<URI> visited = new HashSet<>();
        visited.add(startUri);

        URI currentUri = startUri;
        int redirectCount = 0;

        while (true) {
            Response response = executor.send(currentUri, -1, null);

            if (!isRedirectStatusCode(response.statusCode())) {
                return response;
            }

            String location = response.locationHeader();
            if (location == null || location.isEmpty()) {
                return response;
            }

            URI nextUri = resolveLocation(currentUri, location);

            validateScheme(currentUri, nextUri);

            if (!visited.add(nextUri)) {
                throw new IOException(
                        "Redirect loop detected: " + nextUri + " has already been visited.");
            }

            redirectCount++;
            if (redirectCount > MAX_REDIRECTS) {
                throw new IOException(
                        "Too many redirects: exceeded the maximum of " + MAX_REDIRECTS + ".");
            }

            currentUri = nextUri;
        }
    }

    /**
     * Tells whether the given status code indicates a redirect that should be followed.
     *
     * @param statusCode the HTTP status code.
     * @return {@code true} if the status code is 301, 302, 303, 307, or 308.
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
     * Resolves the {@code Location} header value against the current URI.
     *
     * @param base the current request URI.
     * @param location the value of the {@code Location} header.
     * @return the resolved target URI.
     * @throws IOException if the location cannot be parsed as a URI.
     */
    private static URI resolveLocation(URI base, String location) throws IOException {
        try {
            URI locationUri = URI.create(location);
            if (locationUri.isAbsolute()) {
                return locationUri;
            }
            return base.resolve(locationUri);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid redirect location: " + location, e);
        }
    }

    /**
     * Validates that the scheme change from {@code from} to {@code to} is permitted.
     *
     * <p>Permitted transitions are:
     *
     * <ul>
     *   <li>same scheme (e.g. {@code http} → {@code http}, {@code https} → {@code https});
     *   <li>{@code http} → {@code https} (upgrade).
     * </ul>
     *
     * <p>Downgrading from {@code https} to {@code http} is rejected.
     *
     * @param from the URI of the current request.
     * @param to the URI of the redirect target.
     * @throws IOException if the scheme change is not permitted.
     */
    private static void validateScheme(URI from, URI to) throws IOException {
        String fromScheme = from.getScheme();
        String toScheme = to.getScheme();

        if (fromScheme == null || toScheme == null) {
            return;
        }

        if (fromScheme.equalsIgnoreCase(toScheme)) {
            return;
        }

        if ("http".equalsIgnoreCase(fromScheme) && "https".equalsIgnoreCase(toScheme)) {
            return;
        }

        throw new IOException(
                "Redirect scheme change not allowed: "
                        + fromScheme
                        + " -> "
                        + toScheme
                        + ". Only same-scheme or http-to-https upgrades are permitted.");
    }

    /**
     * A callback invoked for each request hop during redirect following.
     *
     * <p>Implementations are responsible for sending the actual HTTP request and returning the
     * response details. Each intermediate response should be recorded (e.g. in proxy history) by
     * the implementation.
     */
    @FunctionalInterface
    public interface RequestExecutor {

        /**
         * Sends an HTTP request to the given URI and returns the response.
         *
         * @param uri the URI to request; never {@code null}.
         * @param previousStatusCode the status code of the previous response, or {@code -1} for the
         *     initial request.
         * @param previousLocationHeader the value of the {@code Location} header from the previous
         *     response, or {@code null} for the initial request.
         * @return the response; must not be {@code null}.
         * @throws IOException if an I/O error occurs while sending the request.
         */
        Response send(URI uri, int previousStatusCode, String previousLocationHeader)
                throws IOException;
    }

    /**
     * Represents the relevant parts of an HTTP response for redirect handling.
     *
     * @param statusCode the HTTP status code.
     * @param locationHeader the value of the {@code Location} response header, or {@code null} if
     *     absent.
     */
    public record Response(int statusCode, String locationHeader) {}
}
