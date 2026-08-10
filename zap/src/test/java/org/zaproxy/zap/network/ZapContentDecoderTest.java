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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ZapContentDecoder}.
 *
 * <p>Verifies transparent decompression of gzip- and deflate-encoded response bodies, identity
 * (no-encoding) passthrough, unknown encoding passthrough without error, and the empty body edge
 * case.
 */
class ZapContentDecoderTest {

    private static final byte[] PLAINTEXT =
            "Hello, ZAP content decoder!".getBytes(StandardCharsets.UTF_8);

    // -----------------------------------------------------------------------
    // gzip
    // -----------------------------------------------------------------------

    @Test
    void shouldDecompressGzipEncodedBody() throws IOException {
        // Given
        byte[] compressed = gzip(PLAINTEXT);
        // When
        byte[] decoded = ZapContentDecoder.decode("gzip", compressed);
        // Then
        assertThat(decoded).isEqualTo(PLAINTEXT);
    }

    @Test
    void shouldDecompressXGzipEncodedBody() throws IOException {
        // Given
        byte[] compressed = gzip(PLAINTEXT);
        // When
        byte[] decoded = ZapContentDecoder.decode("x-gzip", compressed);
        // Then
        assertThat(decoded).isEqualTo(PLAINTEXT);
    }

    @Test
    void shouldDecompressGzipEncodedBodyCaseInsensitive() throws IOException {
        // Given
        byte[] compressed = gzip(PLAINTEXT);
        // When
        byte[] decoded = ZapContentDecoder.decode("GZIP", compressed);
        // Then
        assertThat(decoded).isEqualTo(PLAINTEXT);
    }

    // -----------------------------------------------------------------------
    // deflate
    // -----------------------------------------------------------------------

    @Test
    void shouldDecompressDeflateEncodedBody() throws IOException {
        // Given
        byte[] compressed = deflate(PLAINTEXT);
        // When
        byte[] decoded = ZapContentDecoder.decode("deflate", compressed);
        // Then
        assertThat(decoded).isEqualTo(PLAINTEXT);
    }

    @Test
    void shouldDecompressDeflateEncodedBodyCaseInsensitive() throws IOException {
        // Given
        byte[] compressed = deflate(PLAINTEXT);
        // When
        byte[] decoded = ZapContentDecoder.decode("DEFLATE", compressed);
        // Then
        assertThat(decoded).isEqualTo(PLAINTEXT);
    }

    // -----------------------------------------------------------------------
    // identity / no encoding
    // -----------------------------------------------------------------------

    @Test
    void shouldPassThroughBodyWithIdentityEncoding() throws IOException {
        // Given / When
        byte[] decoded = ZapContentDecoder.decode("identity", PLAINTEXT);
        // Then
        assertThat(decoded).isEqualTo(PLAINTEXT);
    }

    @Test
    void shouldPassThroughBodyWithNullEncoding() throws IOException {
        // Given / When
        byte[] decoded = ZapContentDecoder.decode(null, PLAINTEXT);
        // Then
        assertThat(decoded).isEqualTo(PLAINTEXT);
    }

    @Test
    void shouldPassThroughBodyWithEmptyEncoding() throws IOException {
        // Given / When
        byte[] decoded = ZapContentDecoder.decode("", PLAINTEXT);
        // Then
        assertThat(decoded).isEqualTo(PLAINTEXT);
    }

    // -----------------------------------------------------------------------
    // unknown encoding – must not throw, must return body unchanged
    // -----------------------------------------------------------------------

    @Test
    void shouldPassThroughBodyWithUnknownEncodingWithoutError() throws IOException {
        // Given
        byte[] body = "some body bytes".getBytes(StandardCharsets.UTF_8);
        // When
        byte[] decoded = ZapContentDecoder.decode("br", body);
        // Then
        assertThat(decoded).isEqualTo(body);
    }

    @Test
    void shouldPassThroughBodyWithAnotherUnknownEncodingWithoutError() throws IOException {
        // Given
        byte[] body = "some body bytes".getBytes(StandardCharsets.UTF_8);
        // When
        byte[] decoded = ZapContentDecoder.decode("zstd", body);
        // Then
        assertThat(decoded).isEqualTo(body);
    }

    // -----------------------------------------------------------------------
    // empty body edge cases
    // -----------------------------------------------------------------------

    @Test
    void shouldReturnEmptyArrayForEmptyBodyWithGzipEncoding() throws IOException {
        // Given
        byte[] empty = new byte[0];
        // When
        byte[] decoded = ZapContentDecoder.decode("gzip", empty);
        // Then
        assertThat(decoded).isEmpty();
    }

    @Test
    void shouldReturnEmptyArrayForEmptyBodyWithDeflateEncoding() throws IOException {
        // Given
        byte[] empty = new byte[0];
        // When
        byte[] decoded = ZapContentDecoder.decode("deflate", empty);
        // Then
        assertThat(decoded).isEmpty();
    }

    @Test
    void shouldReturnEmptyArrayForEmptyBodyWithIdentityEncoding() throws IOException {
        // Given
        byte[] empty = new byte[0];
        // When
        byte[] decoded = ZapContentDecoder.decode("identity", empty);
        // Then
        assertThat(decoded).isEmpty();
    }

    @Test
    void shouldReturnEmptyArrayForEmptyBodyWithNullEncoding() throws IOException {
        // Given
        byte[] empty = new byte[0];
        // When
        byte[] decoded = ZapContentDecoder.decode(null, empty);
        // Then
        assertThat(decoded).isEmpty();
    }

    @Test
    void shouldReturnEmptyArrayForEmptyBodyWithUnknownEncoding() throws IOException {
        // Given
        byte[] empty = new byte[0];
        // When
        byte[] decoded = ZapContentDecoder.decode("br", empty);
        // Then
        assertThat(decoded).isEmpty();
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private static byte[] gzip(byte[] data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStream os = new GZIPOutputStream(baos)) {
            os.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }

    private static byte[] deflate(byte[] data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStream os = new DeflaterOutputStream(baos)) {
            os.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
}
