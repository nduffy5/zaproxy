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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Decodes the body of an {@link HttpResponse} based on the {@code Content-Encoding} header, using
 * {@link java.util.zip} classes.
 *
 * <p>Supported encodings:
 *
 * <ul>
 *   <li>{@code gzip} – decompressed with {@link GZIPInputStream}
 *   <li>{@code deflate} – decompressed with {@link InflaterInputStream} (auto-detects zlib vs raw
 *       deflate)
 *   <li>{@code identity} or absent – body is read as-is
 * </ul>
 *
 * <p><strong>Note:</strong> Not part of the public API.
 */
public final class ZapContentDecoder {

    private static final int BUFFER_SIZE = 4096;

    private ZapContentDecoder() {}

    /**
     * Decodes the body of the given HTTP response.
     *
     * <p>The {@code Content-Encoding} header is inspected to determine the compression algorithm.
     * If the header is absent or set to {@code identity}, the raw body bytes are returned. For
     * {@code gzip} the body is decompressed with {@link GZIPInputStream}; for {@code deflate} the
     * body is decompressed with {@link InflaterInputStream} using nowrap=true (raw deflate).
     *
     * @param response the HTTP response whose body should be decoded; must not be {@code null}.
     * @return the decoded body bytes.
     * @throws IOException if an I/O error occurs while reading or decompressing the body.
     */
    public static byte[] decode(HttpResponse<InputStream> response) throws IOException {
        String encoding =
                response.headers().firstValue("Content-Encoding").orElse("").trim().toLowerCase();

        InputStream body = response.body();

        InputStream decodingStream;
        switch (encoding) {
            case "gzip":
                decodingStream = new GZIPInputStream(body);
                break;
            case "deflate":
                decodingStream = createDeflateStream(body);
                break;
            default:
                decodingStream = body;
                break;
        }

        try (InputStream in = decodingStream) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = in.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Creates an {@link InflaterInputStream} for deflate-encoded content.
     *
     * <p>Auto-detects whether the stream uses a zlib wrapper (RFC 1950) or raw deflate (RFC 1951)
     * by inspecting the first two bytes, following the same heuristic used by Apache HttpClient's
     * {@code DeflateInputStream}.
     *
     * @param body the raw deflate-encoded input stream.
     * @return an {@link InflaterInputStream} configured appropriately.
     * @throws IOException if an I/O error occurs while reading the stream header.
     */
    private static InputStream createDeflateStream(InputStream body) throws IOException {
        if (!body.markSupported()) {
            body = new java.io.BufferedInputStream(body);
        }
        body.mark(2);
        int b1 = body.read();
        int b2 = body.read();
        body.reset();

        boolean nowrap = true;
        if (b1 != -1 && b2 != -1) {
            nowrap = !isZlibWrapped(b1, b2);
        }
        return new InflaterInputStream(body, new Inflater(nowrap));
    }

    /**
     * Returns {@code true} if the two header bytes indicate a zlib-wrapped (RFC 1950) deflate
     * stream.
     *
     * <p>Logic adapted from Apache HttpClient's {@code DeflateInputStream}.
     */
    private static boolean isZlibWrapped(int b1, int b2) {
        int compressionMethod = b1 & 0xF;
        int compressionInfo = (b1 >> 4) & 0xF;
        if (compressionMethod == 8 && compressionInfo <= 7 && ((b1 << 8) | b2) % 31 == 0) {
            return true;
        }
        return false;
    }
}
