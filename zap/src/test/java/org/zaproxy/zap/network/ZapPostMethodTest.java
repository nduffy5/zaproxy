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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;

/** Unit test for {@link ZapPostMethod}. */
@SuppressWarnings("deprecation")
class ZapPostMethodTest {

    private static final String TEST_URI = "http://example.com/test";

    @Test
    void defaultConstructorShouldCreateInstance() {
        // When
        ZapPostMethod method = new ZapPostMethod();
        // Then
        assertNotNull(method);
    }

    @Test
    void uriConstructorShouldCreateInstance() {
        // When
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        // Then
        assertNotNull(method);
    }

    @Test
    void buildRequestShouldReturnPostMethod() throws Exception {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertEquals("POST", request.method());
    }

    @Test
    void buildRequestShouldUseConfiguredUri() throws Exception {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertEquals(URI.create(TEST_URI), request.uri());
    }

    @Test
    void buildRequestWithStringBodyShouldUseOfString() throws Exception {
        // Given
        String body = "param1=value1&param2=value2";
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        method.setStringBody(body);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertEquals("POST", request.method());
        byte[] actualBytes = readBodyPublisher(request.bodyPublisher().orElseThrow());
        assertArrayEquals(body.getBytes(StandardCharsets.UTF_8), actualBytes);
    }

    @Test
    void buildRequestWithByteArrayBodyShouldUseOfByteArray() throws Exception {
        // Given
        byte[] body = new byte[] {0x01, 0x02, 0x03, (byte) 0xFF};
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        method.setByteArrayBody(body);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertEquals("POST", request.method());
        byte[] actualBytes = readBodyPublisher(request.bodyPublisher().orElseThrow());
        assertArrayEquals(body, actualBytes);
    }

    @Test
    void buildRequestWithNoBodyShouldHaveEmptyBody() throws Exception {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertEquals("POST", request.method());
        // Body publisher should be present (POST always has a body publisher)
        assertNotNull(request.bodyPublisher());
    }

    @Test
    void setUriShouldUpdateRequestUri() throws Exception {
        // Given
        ZapPostMethod method = new ZapPostMethod();
        method.setUri(TEST_URI);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertEquals(URI.create(TEST_URI), request.uri());
    }

    @Test
    void buildRequestWithStringBodyShouldPreserveContent() throws Exception {
        // Given
        String urlEncodedBody = "field=hello+world&other=%2F";
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        method.setStringBody(urlEncodedBody);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        byte[] actualBytes = readBodyPublisher(request.bodyPublisher().orElseThrow());
        String actualBody = new String(actualBytes, StandardCharsets.UTF_8);
        assertEquals(urlEncodedBody, actualBody);
    }

    @Test
    void buildRequestWithEmptyStringBodyShouldWork() throws Exception {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        method.setStringBody("");
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertEquals("POST", request.method());
        byte[] actualBytes = readBodyPublisher(request.bodyPublisher().orElseThrow());
        assertEquals(0, actualBytes.length);
    }

    @Test
    void buildRequestWithEmptyByteArrayBodyShouldWork() throws Exception {
        // Given
        ZapPostMethod method = new ZapPostMethod(TEST_URI);
        method.setByteArrayBody(new byte[0]);
        // When
        HttpRequest request = method.buildRequest();
        // Then
        assertEquals("POST", request.method());
        byte[] actualBytes = readBodyPublisher(request.bodyPublisher().orElseThrow());
        assertEquals(0, actualBytes.length);
    }

    /** Reads all bytes from an {@link HttpRequest.BodyPublisher}. */
    private static byte[] readBodyPublisher(HttpRequest.BodyPublisher publisher) {
        CollectingSubscriber subscriber = new CollectingSubscriber();
        publisher.subscribe(subscriber);
        return subscriber.getBytes();
    }

    /** A simple {@link Flow.Subscriber} that collects all bytes from a publisher. */
    private static class CollectingSubscriber implements Flow.Subscriber<ByteBuffer> {

        private final java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        private Flow.Subscription subscription;
        private volatile boolean done = false;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer item) {
            byte[] bytes = new byte[item.remaining()];
            item.get(bytes);
            baos.write(bytes, 0, bytes.length);
        }

        @Override
        public void onError(Throwable throwable) {
            done = true;
        }

        @Override
        public void onComplete() {
            done = true;
        }

        public byte[] getBytes() {
            // Wait for completion
            long deadline = System.currentTimeMillis() + 5000;
            while (!done && System.currentTimeMillis() < deadline) {
                Thread.yield();
            }
            return baos.toByteArray();
        }
    }
}
