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
 * Unit tests for {@link ZapDeleteMethod} request-building behaviour.
 *
 * <p>These tests verify that {@code ZapDeleteMethod} produces an {@link HttpRequest} with method
 * {@code DELETE}, the correct URI, and an optional body when one is provided.
 */
class ZapDeleteMethodTest {

    private static final String TEST_URL = "http://example.com/resource/42";

    // -------------------------------------------------------------------------
    // Correct HTTP method string
    // -------------------------------------------------------------------------

    @Test
    void shouldSetMethodToDelete() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.method()).isEqualTo("DELETE");
    }

    @Test
    void shouldSetUriFromConstructorArgument() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.uri()).isEqualTo(URI.create(TEST_URL));
    }

    // -------------------------------------------------------------------------
    // DELETE with no body
    // -------------------------------------------------------------------------

    @Test
    void shouldProduceNoBodyPublisherWhenNoBodySet() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        // A DELETE with no body should have no body publisher (or an empty one with length 0)
        Optional<HttpRequest.BodyPublisher> bodyPublisher = request.bodyPublisher();
        if (bodyPublisher.isPresent()) {
            assertThat(bodyPublisher.get().contentLength()).isEqualTo(0);
        } else {
            assertThat(bodyPublisher).isEmpty();
        }
    }

    @Test
    void shouldNotSetContentTypeHeaderWhenNoBodySet() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType).isEmpty();
    }

    @Test
    void shouldStillUseDeleteMethodWhenNoBodySet() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.method()).isEqualTo("DELETE");
    }

    // -------------------------------------------------------------------------
    // DELETE with JSON body
    // -------------------------------------------------------------------------

    @Test
    void shouldIncludeJsonBodyInRequest() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URL);
        String jsonBody = "{\"id\":42}";
        method.setRequestBody(jsonBody);
        method.setContentType("application/json");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<HttpRequest.BodyPublisher> bodyPublisher = request.bodyPublisher();
        assertThat(bodyPublisher).isPresent();
        assertThat(bodyPublisher.get().contentLength())
                .isEqualTo(jsonBody.getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void shouldSetContentTypeHeaderWhenJsonBodyProvided() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URL);
        method.setRequestBody("{\"id\":42}");
        method.setContentType("application/json");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType).isPresent();
        assertThat(contentType.get()).isEqualTo("application/json");
    }

    @Test
    void shouldStillUseDeleteMethodWhenJsonBodySet() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URL);
        method.setRequestBody("{\"id\":42}");
        method.setContentType("application/json");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.method()).isEqualTo("DELETE");
    }

    @Test
    void shouldEncodeJsonBodyWithUtf8() {
        // Given
        String jsonBody = "{\"name\":\"caf\u00e9\"}"; // contains non-ASCII character
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URL);
        method.setRequestBody(jsonBody);
        method.setContentType("application/json; charset=UTF-8");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<HttpRequest.BodyPublisher> bodyPublisher = request.bodyPublisher();
        assertThat(bodyPublisher).isPresent();
        assertThat(bodyPublisher.get().contentLength())
                .isEqualTo(jsonBody.getBytes(StandardCharsets.UTF_8).length);
    }

    // -------------------------------------------------------------------------
    // Additional edge cases
    // -------------------------------------------------------------------------

    @Test
    void shouldHandleEmptyBodyAsNoBody() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URL);
        method.setRequestBody("");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<HttpRequest.BodyPublisher> bodyPublisher = request.bodyPublisher();
        if (bodyPublisher.isPresent()) {
            assertThat(bodyPublisher.get().contentLength()).isEqualTo(0);
        } else {
            assertThat(bodyPublisher).isEmpty();
        }
    }

    @Test
    void shouldPreserveUriWithQueryParameters() {
        // Given
        String urlWithQuery = "http://example.com/resource?filter=active&page=1";
        ZapDeleteMethod method = new ZapDeleteMethod(urlWithQuery);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.uri()).isEqualTo(URI.create(urlWithQuery));
        assertThat(request.method()).isEqualTo("DELETE");
    }
}
