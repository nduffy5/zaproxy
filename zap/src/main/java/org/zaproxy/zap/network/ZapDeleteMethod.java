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

/**
 * An HTTP DELETE method implementation backed by {@link java.net.http.HttpRequest.Builder}.
 *
 * @deprecated (2.12.0) Implementation details, do not use.
 */
@Deprecated
public class ZapDeleteMethod {

    private final HttpRequest.Builder builder;

    /** Creates a new {@code ZapDeleteMethod} with no URI set. */
    public ZapDeleteMethod() {
        this.builder =
                HttpRequest.newBuilder().method("DELETE", HttpRequest.BodyPublishers.noBody());
    }

    /**
     * Creates a new {@code ZapDeleteMethod} with the given URI.
     *
     * @param uri the request URI
     */
    public ZapDeleteMethod(String uri) {
        this.builder =
                HttpRequest.newBuilder(URI.create(uri))
                        .method("DELETE", HttpRequest.BodyPublishers.noBody());
    }

    /**
     * Returns {@code DELETE}.
     *
     * @return {@code DELETE}
     */
    public String getName() {
        return "DELETE";
    }

    /**
     * Returns the {@link HttpRequest.Builder} configured for this DELETE request.
     *
     * <p>The builder can be further customised (e.g. to add headers or a request body) before
     * calling {@link HttpRequest.Builder#build()}.
     *
     * @return the underlying {@code HttpRequest.Builder}
     */
    public HttpRequest.Builder getBuilder() {
        return builder;
    }
}
