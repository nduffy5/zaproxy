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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

/** Unit test for {@link ZapContentDecoder}. */
class ZapContentDecoderTest {

    private static final byte[] PLAIN_CONTENT = "Hello, World!".getBytes(StandardCharsets.UTF_8);

    @Test
    void shouldReturnRawBytesWhenNoContentEncodingHeader() throws IOException {
        // Given
        HttpResponse<InputStream> response = mockResponse(PLAIN_CONTENT, null);
        // When
        byte[] result = ZapContentDecoder.decode(response);
        // Then
        assertThat(result, is(equalTo(PLAIN_CONTENT)));
    }

    @Test
    void shouldReturnRawBytesWhenContentEncodingIsIdentity() throws IOException {
        // Given
        HttpResponse<InputStream> response = mockResponse(PLAIN_CONTENT, "identity");
        // When
        byte[] result = ZapContentDecoder.decode(response);
        // Then
        assertThat(result, is(equalTo(PLAIN_CONTENT)));
    }

    @Test
    void shouldDecodeGzipContent() throws IOException {
        // Given
        byte[] gzipped = gzip(PLAIN_CONTENT);
        HttpResponse<InputStream> response = mockResponse(gzipped, "gzip");
        // When
        byte[] result = ZapContentDecoder.decode(response);
        // Then
        assertThat(result, is(equalTo(PLAIN_CONTENT)));
    }

    @Test
    void shouldDecodeDeflateContent() throws IOException {
        // Given
        byte[] deflated = deflate(PLAIN_CONTENT);
        HttpResponse<InputStream> response = mockResponse(deflated, "deflate");
        // When
        byte[] result = ZapContentDecoder.decode(response);
        // Then
        assertThat(result, is(equalTo(PLAIN_CONTENT)));
    }

    @Test
    void shouldReturnRawBytesForUnknownEncoding() throws IOException {
        // Given
        HttpResponse<InputStream> response = mockResponse(PLAIN_CONTENT, "br");
        // When
        byte[] result = ZapContentDecoder.decode(response);
        // Then
        assertThat(result, is(equalTo(PLAIN_CONTENT)));
    }

    @Test
    void shouldDecodeEmptyGzipContent() throws IOException {
        // Given
        byte[] gzipped = gzip(new byte[0]);
        HttpResponse<InputStream> response = mockResponse(gzipped, "gzip");
        // When
        byte[] result = ZapContentDecoder.decode(response);
        // Then
        assertThat(result, is(equalTo(new byte[0])));
    }

    @Test
    void shouldDecodeEmptyDeflateContent() throws IOException {
        // Given
        byte[] deflated = deflate(new byte[0]);
        HttpResponse<InputStream> response = mockResponse(deflated, "deflate");
        // When
        byte[] result = ZapContentDecoder.decode(response);
        // Then
        assertThat(result, is(equalTo(new byte[0])));
    }

    @Test
    void shouldReturnEmptyBytesWhenBodyIsEmpty() throws IOException {
        // Given
        HttpResponse<InputStream> response = mockResponse(new byte[0], null);
        // When
        byte[] result = ZapContentDecoder.decode(response);
        // Then
        assertThat(result, is(equalTo(new byte[0])));
    }

    // --- helpers ---

    private static HttpResponse<InputStream> mockResponse(byte[] body, String contentEncoding) {
        Map<String, List<String>> headersMap;
        if (contentEncoding != null) {
            headersMap = Map.of("Content-Encoding", List.of(contentEncoding));
        } else {
            headersMap = Map.of();
        }
        HttpHeaders headers = HttpHeaders.of(headersMap, (a, b) -> true);

        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return 200;
            }

            @Override
            public java.net.http.HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<InputStream>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return headers;
            }

            @Override
            public InputStream body() {
                return new ByteArrayInputStream(body);
            }

            @Override
            public Optional<javax.net.ssl.SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public java.net.URI uri() {
                return null;
            }

            @Override
            public java.net.http.HttpClient.Version version() {
                return java.net.http.HttpClient.Version.HTTP_1_1;
            }
        };
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(data);
        }
        return baos.toByteArray();
    }

    private static byte[] deflate(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
            dos.write(data);
        }
        return baos.toByteArray();
    }
}
