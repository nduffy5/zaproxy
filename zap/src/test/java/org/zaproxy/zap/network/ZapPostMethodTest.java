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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Unit test for {@link ZapPostMethod}. */
@SuppressWarnings("deprecation")
class ZapPostMethodTest {

    private static final String TEST_URI = "http://example.com/post";

    @Test
    void shouldCreateWithDefaultConstructor() {
        ZapPostMethod method = new ZapPostMethod();
        assertNotNull(method);
    }

    @Test
    void shouldCreateWithUriConstructor() {
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        assertNotNull(method);
    }

    @Test
    void shouldBuildRequestWithPostMethod() throws Exception {
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        HttpRequest request = method.buildRequest();
        assertEquals("POST", request.method());
    }

    @Test
    void shouldBuildRequestWithCorrectUri() throws Exception {
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        HttpRequest request = method.buildRequest();
        assertEquals(URI.create(TEST_URI), request.uri());
    }

    @Test
    void shouldBuildRequestWithGivenUri() throws Exception {
        ZapPostMethod method = new ZapPostMethod();
        URI uri = URI.create(TEST_URI);
        HttpRequest request = method.buildRequest(uri);
        assertEquals(uri, request.uri());
        assertEquals("POST", request.method());
    }

    @Test
    void shouldBuildRequestWithUrlEncodedBodyFromFormParameters() throws Exception {
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        method.addParameter("key1", "value1");
        method.addParameter("key2", "value2");

        HttpRequest request = method.buildRequest();

        assertEquals("POST", request.method());
        // Body publisher should be present
        assertNotNull(request.bodyPublisher().orElse(null));
        // Content length should be positive (URL-encoded body)
        long contentLength =
                request.bodyPublisher().map(HttpRequest.BodyPublisher::contentLength).orElse(-1L);
        // ofString produces a body with known content length >= 0
        assertEquals("key1=value1&key2=value2".length(), contentLength);
    }

    @Test
    void shouldBuildRequestWithByteArrayBody() throws Exception {
        byte[] body = "raw body data".getBytes(StandardCharsets.UTF_8);
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        method.setRequestBody(body);

        HttpRequest request = method.buildRequest();

        assertEquals("POST", request.method());
        assertNotNull(request.bodyPublisher().orElse(null));
        long contentLength =
                request.bodyPublisher().map(HttpRequest.BodyPublisher::contentLength).orElse(-1L);
        assertEquals(body.length, contentLength);
    }

    @Test
    void shouldBuildRequestWithEmptyBodyWhenNoParametersOrBody() throws Exception {
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        HttpRequest request = method.buildRequest();

        assertEquals("POST", request.method());
        // Empty body publisher should have content length 0
        long contentLength =
                request.bodyPublisher().map(HttpRequest.BodyPublisher::contentLength).orElse(-1L);
        assertEquals(0L, contentLength);
    }

    @Test
    void shouldGetFormParameterValue() {
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        method.addParameter("myKey", "myValue");
        assertEquals("myValue", method.getParameter("myKey"));
    }

    @Test
    void shouldReturnNullForMissingParameter() {
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        assertEquals(null, method.getParameter("nonExistent"));
    }

    @Test
    void shouldGetRequestBodyBytes() {
        byte[] body = {1, 2, 3, 4, 5};
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        method.setRequestBody(body);
        assertArrayEquals(body, method.getRequestBody());
    }

    @Test
    void shouldGetUriString() {
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        assertEquals(TEST_URI, method.getUri());
    }

    @Test
    void shouldReturnNullUriWhenDefaultConstructorUsed() {
        ZapPostMethod method = new ZapPostMethod();
        assertEquals(null, method.getUri());
    }

    @Test
    void shouldBuildRequestWithSingleFormParameter() throws Exception {
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        method.addParameter("name", "Alice");

        HttpRequest request = method.buildRequest();

        assertEquals("POST", request.method());
        long contentLength =
                request.bodyPublisher().map(HttpRequest.BodyPublisher::contentLength).orElse(-1L);
        assertEquals("name=Alice".length(), contentLength);
    }

    @Test
    void shouldPreferByteArrayBodyOverFormParameters() throws Exception {
        byte[] body = "override".getBytes(StandardCharsets.UTF_8);
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        method.addParameter("key", "value");
        method.setRequestBody(body);

        HttpRequest request = method.buildRequest();

        assertEquals("POST", request.method());
        long contentLength =
                request.bodyPublisher().map(HttpRequest.BodyPublisher::contentLength).orElse(-1L);
        assertEquals(body.length, contentLength);
    }
}
