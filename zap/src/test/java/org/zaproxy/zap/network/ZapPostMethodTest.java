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

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ZapPostMethod} request-building behaviour.
 *
 * <p>These tests verify that {@code ZapPostMethod} correctly serialises the request body, sets the
 * {@code Content-Type} header, and produces an {@link HttpRequest} with method {@code POST}.
 */
class ZapPostMethodTest {

    private static final String TEST_URL = "http://example.com/test";

    // -------------------------------------------------------------------------
    // URL-encoded form body
    // -------------------------------------------------------------------------

    @Test
    void shouldSetMethodToPost() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.method()).isEqualTo("POST");
    }

    @Test
    void shouldSetUriFromConstructorArgument() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.uri()).isEqualTo(URI.create(TEST_URL));
    }

    @Test
    void shouldEncodeUrlEncodedFormBody() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        method.setRequestBody("field1=value1&field2=value2");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<HttpRequest.BodyPublisher> bodyPublisher = request.bodyPublisher();
        assertThat(bodyPublisher).isPresent();
        assertThat(bodyPublisher.get().contentLength())
                .isEqualTo("field1=value1&field2=value2".getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void shouldSetContentTypeToUrlEncodedByDefault() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        method.setRequestBody("a=b");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType).isPresent();
        assertThat(contentType.get())
                .contains("application/x-www-form-urlencoded");
    }

    // -------------------------------------------------------------------------
    // Multipart body
    // -------------------------------------------------------------------------

    @Test
    void shouldSetContentTypeToMultipartWhenSpecified() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        String multipartBody =
                "--"
                        + boundary
                        + "\r\nContent-Disposition: form-data; name=\"file\"\r\n\r\ndata\r\n--"
                        + boundary
                        + "--";
        method.setRequestBody(multipartBody);
        method.setContentType("multipart/form-data; boundary=" + boundary);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType).isPresent();
        assertThat(contentType.get()).startsWith("multipart/form-data");
        assertThat(contentType.get()).contains(boundary);
    }

    @Test
    void shouldIncludeMultipartBodyInRequest() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        String boundary = "----boundary123";
        String multipartBody =
                "--" + boundary + "\r\nContent-Disposition: form-data; name=\"f\"\r\n\r\nv\r\n--"
                        + boundary + "--";
        method.setRequestBody(multipartBody);
        method.setContentType("multipart/form-data; boundary=" + boundary);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<HttpRequest.BodyPublisher> bodyPublisher = request.bodyPublisher();
        assertThat(bodyPublisher).isPresent();
        assertThat(bodyPublisher.get().contentLength()).isGreaterThan(0);
    }

    // -------------------------------------------------------------------------
    // Explicit Content-Type override
    // -------------------------------------------------------------------------

    @Test
    void shouldRespectExplicitContentTypeOverride() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        method.setRequestBody("{\"key\":\"value\"}");
        method.setContentType("application/json");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType).isPresent();
        assertThat(contentType.get()).isEqualTo("application/json");
    }

    @Test
    void shouldRespectExplicitContentTypeWithCharset() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        method.setRequestBody("data");
        method.setContentType("text/plain; charset=UTF-8");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType).isPresent();
        assertThat(contentType.get()).isEqualTo("text/plain; charset=UTF-8");
    }

    // -------------------------------------------------------------------------
    // Charset handling
    // -------------------------------------------------------------------------

    @Test
    void shouldEncodeBodyWithUtf8ByDefault() {
        // Given
        String body = "caf\u00e9=au+lait"; // contains non-ASCII character
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        method.setRequestBody(body);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<HttpRequest.BodyPublisher> bodyPublisher = request.bodyPublisher();
        assertThat(bodyPublisher).isPresent();
        assertThat(bodyPublisher.get().contentLength())
                .isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void shouldEncodeBodyWithSpecifiedCharset() {
        // Given
        String body = "hello=world";
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        method.setRequestBody(body);
        method.setCharset(StandardCharsets.ISO_8859_1);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<HttpRequest.BodyPublisher> bodyPublisher = request.bodyPublisher();
        assertThat(bodyPublisher).isPresent();
        assertThat(bodyPublisher.get().contentLength())
                .isEqualTo(body.getBytes(StandardCharsets.ISO_8859_1).length);
    }

    // -------------------------------------------------------------------------
    // Empty body edge case
    // -------------------------------------------------------------------------

    @Test
    void shouldProduceEmptyBodyPublisherWhenNoBodySet() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<HttpRequest.BodyPublisher> bodyPublisher = request.bodyPublisher();
        assertThat(bodyPublisher).isPresent();
        assertThat(bodyPublisher.get().contentLength()).isEqualTo(0);
    }

    @Test
    void shouldProduceEmptyBodyPublisherWhenEmptyBodySet() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        method.setRequestBody("");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<HttpRequest.BodyPublisher> bodyPublisher = request.bodyPublisher();
        assertThat(bodyPublisher).isPresent();
        assertThat(bodyPublisher.get().contentLength()).isEqualTo(0);
    }

    @Test
    void shouldNotSetContentTypeHeaderWhenNoBodySet() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        // Content-Type should not be present when there is no body
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType).isEmpty();
    }
}
