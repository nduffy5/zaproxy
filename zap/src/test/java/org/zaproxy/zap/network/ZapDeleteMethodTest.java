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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit test for the modernized {@link ZapDeleteMethod} that wraps {@link
 * java.net.http.HttpRequest.Builder} and produces an {@link HttpRequest} with the DELETE method and
 * an optional body publisher.
 */
class ZapDeleteMethodTest {

    private static final String TARGET_URL = "https://example.com/resource/42";

    // -------------------------------------------------------------------------
    // Correct HTTP method string
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnDeleteAsMethodName() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TARGET_URL);
        // When
        String name = method.getName();
        // Then
        assertThat(name, is(equalTo("DELETE")));
    }

    @Test
    void shouldBuildRequestWithDeleteMethod() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TARGET_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.method(), is(equalTo("DELETE")));
    }

    @Test
    void shouldSetUriFromConstructorArgument() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TARGET_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.uri(), is(equalTo(URI.create(TARGET_URL))));
    }

    // -------------------------------------------------------------------------
    // DELETE with no body
    // -------------------------------------------------------------------------

    @Test
    void shouldBuildRequestWithoutBodyByDefault() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TARGET_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        // A DELETE with no body should either have no publisher or a zero-length publisher
        Optional<HttpRequest.BodyPublisher> publisher = request.bodyPublisher();
        boolean hasNoBody =
                publisher.isEmpty() || publisher.get().contentLength() == 0;
        assertThat(hasNoBody, is(true));
    }

    @Test
    void shouldBuildRequestSuccessfullyWithNoBody() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TARGET_URL);
        // When / Then
        assertDoesNotThrow(method::buildRequest);
    }

    @Test
    void shouldHaveEmptyBodyStringWhenNoBodySet() throws Exception {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TARGET_URL);
        // When
        HttpRequest request = method.buildRequest();
        String body = readBodyAsString(request);
        // Then
        assertThat(body, is(equalTo("")));
    }

    // -------------------------------------------------------------------------
    // DELETE with JSON body
    // -------------------------------------------------------------------------

    @Test
    void shouldBuildRequestWithJsonBody() throws Exception {
        // Given
        String jsonBody = "{\"id\":42}";
        ZapDeleteMethod method = new ZapDeleteMethod(TARGET_URL);
        method.setRequestBody(jsonBody, "application/json");
        // When
        HttpRequest request = method.buildRequest();
        String body = readBodyAsString(request);
        // Then
        assertThat(body, is(equalTo(jsonBody)));
    }

    @Test
    void shouldSetContentTypeHeaderForJsonBody() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TARGET_URL);
        method.setRequestBody("{\"id\":42}", "application/json");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType.isPresent(), is(true));
        assertThat(contentType.get(), is(equalTo("application/json")));
    }

    @Test
    void shouldStillUseDeleteMethodWhenBodyIsPresent() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TARGET_URL);
        method.setRequestBody("{\"id\":42}", "application/json");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.method(), is(equalTo("DELETE")));
    }

    @Test
    void shouldHaveNonEmptyBodyPublisherWhenBodyIsSet() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TARGET_URL);
        method.setRequestBody("{\"id\":42}", "application/json");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<HttpRequest.BodyPublisher> publisher = request.bodyPublisher();
        assertThat(publisher.isPresent(), is(true));
        assertThat(publisher.get().contentLength() > 0, is(true));
    }

    @Test
    void shouldPreserveUriWhenBodyIsSet() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TARGET_URL);
        method.setRequestBody("{\"id\":42}", "application/json");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.uri(), is(equalTo(URI.create(TARGET_URL))));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Reads the body of an {@link HttpRequest} as a UTF-8 string by subscribing to the body
     * publisher.
     */
    private static String readBodyAsString(HttpRequest request) throws Exception {
        Optional<HttpRequest.BodyPublisher> publisherOpt = request.bodyPublisher();
        if (publisherOpt.isEmpty()) {
            return "";
        }
        HttpRequest.BodyPublisher publisher = publisherOpt.get();
        if (publisher.contentLength() == 0) {
            return "";
        }
        BodyCollector collector = new BodyCollector();
        publisher.subscribe(collector);
        return new String(collector.getBytes(), StandardCharsets.UTF_8);
    }

    /** Simple {@link java.util.concurrent.Flow.Subscriber} that collects body bytes. */
    private static class BodyCollector
            implements java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer> {

        private final java.util.concurrent.CompletableFuture<byte[]> future =
                new java.util.concurrent.CompletableFuture<>();
        private final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        @Override
        public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(java.nio.ByteBuffer item) {
            byte[] bytes = new byte[item.remaining()];
            item.get(bytes);
            baos.write(bytes, 0, bytes.length);
        }

        @Override
        public void onError(Throwable throwable) {
            future.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            future.complete(baos.toByteArray());
        }

        public byte[] getBytes() throws Exception {
            return future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
    }
}
