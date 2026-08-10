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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit test for the modernized {@link ZapPostMethod} that wraps {@link
 * java.net.http.HttpRequest.Builder} and produces an {@link HttpRequest} with the correct method,
 * body publisher, and Content-Type header.
 */
class ZapPostMethodTest {

    private static final String TARGET_URL = "https://example.com/form";

    // -------------------------------------------------------------------------
    // URL-encoded form body
    // -------------------------------------------------------------------------

    @Test
    void shouldSetMethodToPost() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.method(), is(equalTo("POST")));
    }

    @Test
    void shouldSetUriFromConstructorArgument() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.uri(), is(equalTo(URI.create(TARGET_URL))));
    }

    @Test
    void shouldProduceUrlEncodedContentTypeForFormBody() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        method.addFormParameter("user", "alice");
        method.addFormParameter("pass", "secret");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType.isPresent(), is(true));
        assertThat(contentType.get(), startsWith("application/x-www-form-urlencoded"));
    }

    @Test
    void shouldEncodeFormParametersInBody() throws Exception {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        method.addFormParameter("name", "John Doe");
        method.addFormParameter("city", "New York");
        // When
        HttpRequest request = method.buildRequest();
        String body = readBodyAsString(request);
        // Then
        assertThat(body, containsString("name=John+Doe"));
        assertThat(body, containsString("city=New+York"));
    }

    @Test
    void shouldEncodeFormParametersWithAmpersandSeparator() throws Exception {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        method.addFormParameter("a", "1");
        method.addFormParameter("b", "2");
        // When
        HttpRequest request = method.buildRequest();
        String body = readBodyAsString(request);
        // Then
        assertThat(body, containsString("a=1"));
        assertThat(body, containsString("b=2"));
        assertThat(body, containsString("&"));
    }

    // -------------------------------------------------------------------------
    // Charset handling
    // -------------------------------------------------------------------------

    @Test
    void shouldIncludeCharsetInContentTypeWhenExplicitlySet() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        method.addFormParameter("key", "value");
        method.setCharset(StandardCharsets.UTF_8);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType.isPresent(), is(true));
        assertThat(contentType.get(), containsString("charset=UTF-8"));
    }

    // -------------------------------------------------------------------------
    // Multipart body
    // -------------------------------------------------------------------------

    @Test
    void shouldProduceMultipartContentTypeForMultipartBody() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        method.setMultipartBody(true);
        method.addFormParameter("field", "value");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType.isPresent(), is(true));
        assertThat(contentType.get(), startsWith("multipart/form-data"));
    }

    @Test
    void shouldIncludeBoundaryInMultipartContentType() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        method.setMultipartBody(true);
        method.addFormParameter("field", "value");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType.isPresent(), is(true));
        assertThat(contentType.get(), containsString("boundary="));
    }

    @Test
    void shouldIncludeFieldNameInMultipartBody() throws Exception {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        method.setMultipartBody(true);
        method.addFormParameter("myField", "myValue");
        // When
        HttpRequest request = method.buildRequest();
        String body = readBodyAsString(request);
        // Then
        assertThat(body, containsString("name=\"myField\""));
        assertThat(body, containsString("myValue"));
    }

    // -------------------------------------------------------------------------
    // Explicit Content-Type override
    // -------------------------------------------------------------------------

    @Test
    void shouldRespectExplicitContentTypeOverride() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        method.setRequestBody("{\"key\":\"value\"}", "application/json");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        Optional<String> contentType = request.headers().firstValue("Content-Type");
        assertThat(contentType.isPresent(), is(true));
        assertThat(contentType.get(), is(equalTo("application/json")));
    }

    @Test
    void shouldSendRawBodyWhenContentTypeIsOverridden() throws Exception {
        // Given
        String jsonBody = "{\"key\":\"value\"}";
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        method.setRequestBody(jsonBody, "application/json");
        // When
        HttpRequest request = method.buildRequest();
        String body = readBodyAsString(request);
        // Then
        assertThat(body, is(equalTo(jsonBody)));
    }

    // -------------------------------------------------------------------------
    // Empty body edge case
    // -------------------------------------------------------------------------

    @Test
    void shouldBuildRequestWithEmptyBodyWhenNoParametersAdded() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        // When / Then
        assertDoesNotThrow(method::buildRequest);
    }

    @Test
    void shouldHaveEmptyBodyWhenNoParametersAdded() throws Exception {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        // When
        HttpRequest request = method.buildRequest();
        String body = readBodyAsString(request);
        // Then
        assertThat(body, is(equalTo("")));
    }

    @Test
    void shouldStillBePostMethodWithEmptyBody() {
        // Given
        ZapPostMethod method = new ZapPostMethod(TARGET_URL);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertThat(request.method(), is(equalTo("POST")));
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
        java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer> subscriber =
                new java.util.concurrent.Flow.Subscriber<>() {
                    private final java.util.concurrent.CompletableFuture<byte[]> future =
                            new java.util.concurrent.CompletableFuture<>();
                    private final java.io.ByteArrayOutputStream baos =
                            new java.io.ByteArrayOutputStream();
                    private java.util.concurrent.Flow.Subscription subscription;

                    @Override
                    public void onSubscribe(java.util.concurrent.Flow.Subscription s) {
                        this.subscription = s;
                        s.request(Long.MAX_VALUE);
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
                };

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
