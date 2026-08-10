/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2013 The ZAP Development Team
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
import java.nio.charset.StandardCharsets;

/**
 * An HTTP POST method implementation backed by {@link HttpRequest.Builder}.
 *
 * <p>Supports URL-encoded string bodies via {@link #setStringBody(String)} and binary bodies via
 * {@link #setByteArrayBody(byte[])}.
 *
 * @deprecated (2.12.0) Implementation details, do not use.
 */
@Deprecated
public class ZapPostMethod {

    private String uri;
    private HttpRequest.BodyPublisher bodyPublisher;

    /** Creates a new {@code ZapPostMethod} with no URI set. */
    public ZapPostMethod() {
        this.bodyPublisher = HttpRequest.BodyPublishers.noBody();
    }

    /**
     * Creates a new {@code ZapPostMethod} with the given URI.
     *
     * @param uri the URI for the POST request
     */
    public ZapPostMethod(String uri) {
        this.uri = uri;
        this.bodyPublisher = HttpRequest.BodyPublishers.noBody();
    }

    /**
     * Sets the URI for this POST request.
     *
     * @param uri the URI string
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Sets a URL-encoded string body for this POST request. Uses {@link
     * HttpRequest.BodyPublishers#ofString(String)} internally.
     *
     * @param body the URL-encoded body string
     */
    public void setStringBody(String body) {
        this.bodyPublisher = HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
    }

    /**
     * Sets a binary body for this POST request. Uses {@link
     * HttpRequest.BodyPublishers#ofByteArray(byte[])} internally.
     *
     * @param body the binary body bytes
     */
    public void setByteArrayBody(byte[] body) {
        this.bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(body);
    }

    /**
     * Builds and returns an {@link HttpRequest} with method POST and the configured body.
     *
     * @return the built {@link HttpRequest}
     */
    public HttpRequest buildRequest() {
        return HttpRequest.newBuilder().uri(URI.create(uri)).method("POST", bodyPublisher).build();
    }
}
