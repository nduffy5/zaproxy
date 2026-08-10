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

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * An HTTP POST method implementation based on {@link java.net.http.HttpRequest}.
 *
 * <p>Supports URL-encoded form parameters (via {@link BodyPublishers#ofString(String)}) and raw
 * binary bodies (via {@link BodyPublishers#ofByteArray(byte[])}).
 *
 * @deprecated (2.12.0) Implementation details, do not use.
 */
@Deprecated
public class ZapPostMethod {

    private final String uri;
    private final List<String[]> parameters = new ArrayList<>();
    private byte[] requestBody;

    /** Creates a new {@code ZapPostMethod} with no URI. */
    public ZapPostMethod() {
        this.uri = null;
    }

    /**
     * Creates a new {@code ZapPostMethod} with the given URI.
     *
     * @param uri the URI for the POST request
     */
    public ZapPostMethod(String uri) {
        this.uri = uri;
    }

    /**
     * Adds a form parameter to this POST request.
     *
     * @param name the parameter name
     * @param value the parameter value
     */
    public void addParameter(String name, String value) {
        parameters.add(new String[] {name, value});
    }

    /**
     * Returns the value of the first form parameter with the given name, or {@code null} if not
     * found.
     *
     * @param name the parameter name
     * @return the parameter value, or {@code null}
     */
    public String getParameter(String name) {
        for (String[] param : parameters) {
            if (param[0].equals(name)) {
                return param[1];
            }
        }
        return null;
    }

    /**
     * Sets the raw binary body for this POST request. When set, this takes precedence over any form
     * parameters.
     *
     * @param body the raw body bytes
     */
    public void setRequestBody(byte[] body) {
        this.requestBody = body;
    }

    /**
     * Returns the raw binary body, or {@code null} if not set.
     *
     * @return the raw body bytes, or {@code null}
     */
    public byte[] getRequestBody() {
        return requestBody;
    }

    /**
     * Returns the URI string provided at construction time, or {@code null} if none was provided.
     *
     * @return the URI string, or {@code null}
     */
    public String getUri() {
        return uri;
    }

    /**
     * Builds an {@link HttpRequest} using the URI provided at construction time.
     *
     * @return the built {@link HttpRequest}
     * @throws IllegalStateException if no URI was provided at construction time
     */
    public HttpRequest buildRequest() {
        if (uri == null) {
            throw new IllegalStateException("No URI provided.");
        }
        return buildRequest(URI.create(uri));
    }

    /**
     * Builds an {@link HttpRequest} using the given URI.
     *
     * <p>If a raw body has been set via {@link #setRequestBody(byte[])}, it is used as the body via
     * {@link BodyPublishers#ofByteArray(byte[])}. Otherwise, any form parameters are URL-encoded
     * and used as the body via {@link BodyPublishers#ofString(String)}. If neither is set, an empty
     * body is used.
     *
     * @param uri the URI for the request
     * @return the built {@link HttpRequest}
     */
    public HttpRequest buildRequest(URI uri) {
        BodyPublisher bodyPublisher = resolveBodyPublisher();
        return HttpRequest.newBuilder(uri).POST(bodyPublisher).build();
    }

    private BodyPublisher resolveBodyPublisher() {
        if (requestBody != null) {
            return BodyPublishers.ofByteArray(requestBody);
        }
        if (!parameters.isEmpty()) {
            return BodyPublishers.ofString(buildUrlEncodedBody(), StandardCharsets.UTF_8);
        }
        return BodyPublishers.ofByteArray(new byte[0]);
    }

    private String buildUrlEncodedBody() {
        StringBuilder sb = new StringBuilder();
        for (String[] param : parameters) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(encode(param[0])).append('=').append(encode(param[1]));
        }
        return sb.toString();
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
