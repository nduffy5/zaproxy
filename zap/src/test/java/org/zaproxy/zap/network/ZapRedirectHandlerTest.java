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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ZapRedirectHandler}. */
class ZapRedirectHandlerTest {

    @Test
    void shouldNotFollowRedirectWhenStatusIsNot3xx() throws IOException {
        // Given
        List<URI> visited = new ArrayList<>();
        ZapRedirectHandler handler =
                new ZapRedirectHandler(
                        (uri, statusCode, locationHeader) -> {
                            visited.add(uri);
                            return new ZapRedirectHandler.Response(200, null);
                        });
        URI start = URI.create("http://example.com/");

        // When
        ZapRedirectHandler.Response response = handler.execute(start);

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(visited).hasSize(1);
    }

    @Test
    void shouldFollowSingleRedirect() throws IOException {
        // Given
        URI first = URI.create("http://example.com/");
        URI second = URI.create("http://example.com/final");
        List<URI> visited = new ArrayList<>();

        ZapRedirectHandler handler =
                new ZapRedirectHandler(
                        (uri, statusCode, locationHeader) -> {
                            visited.add(uri);
                            if (uri.equals(first)) {
                                return new ZapRedirectHandler.Response(302, second.toString());
                            }
                            return new ZapRedirectHandler.Response(200, null);
                        });

        // When
        ZapRedirectHandler.Response response = handler.execute(first);

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(visited).containsExactly(first, second);
    }

    @Test
    void shouldFollowMultipleRedirects() throws IOException {
        // Given
        URI first = URI.create("http://example.com/a");
        URI second = URI.create("http://example.com/b");
        URI third = URI.create("http://example.com/c");
        List<URI> visited = new ArrayList<>();

        ZapRedirectHandler handler =
                new ZapRedirectHandler(
                        (uri, statusCode, locationHeader) -> {
                            visited.add(uri);
                            if (uri.equals(first)) {
                                return new ZapRedirectHandler.Response(301, second.toString());
                            }
                            if (uri.equals(second)) {
                                return new ZapRedirectHandler.Response(302, third.toString());
                            }
                            return new ZapRedirectHandler.Response(200, null);
                        });

        // When
        ZapRedirectHandler.Response response = handler.execute(first);

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(visited).containsExactly(first, second, third);
    }

    @Test
    void shouldThrowWhenRedirectLimitExceeded() {
        // Given - a chain of unique URIs that exceeds the limit
        ZapRedirectHandler handler =
                new ZapRedirectHandler(
                        (uri, statusCode, locationHeader) -> {
                            int n = Integer.parseInt(uri.getPath().substring(1));
                            return new ZapRedirectHandler.Response(
                                    302, "http://example.com/" + (n + 1));
                        });

        // When / Then
        assertThatThrownBy(() -> handler.execute(URI.create("http://example.com/0")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("redirect");
    }

    @Test
    void shouldThrowOnRedirectLoop() {
        // Given
        URI first = URI.create("http://example.com/a");
        URI second = URI.create("http://example.com/b");

        ZapRedirectHandler handler =
                new ZapRedirectHandler(
                        (uri, statusCode, locationHeader) -> {
                            if (uri.equals(first)) {
                                return new ZapRedirectHandler.Response(302, second.toString());
                            }
                            return new ZapRedirectHandler.Response(302, first.toString());
                        });

        // When / Then
        assertThatThrownBy(() -> handler.execute(first))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("loop");
    }

    @Test
    void shouldAllowHttpToHttpsUpgrade() throws IOException {
        // Given
        URI httpUri = URI.create("http://example.com/");
        URI httpsUri = URI.create("https://example.com/");
        List<URI> visited = new ArrayList<>();

        ZapRedirectHandler handler =
                new ZapRedirectHandler(
                        (uri, statusCode, locationHeader) -> {
                            visited.add(uri);
                            if (uri.equals(httpUri)) {
                                return new ZapRedirectHandler.Response(301, httpsUri.toString());
                            }
                            return new ZapRedirectHandler.Response(200, null);
                        });

        // When
        ZapRedirectHandler.Response response = handler.execute(httpUri);

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(visited).containsExactly(httpUri, httpsUri);
    }

    @Test
    void shouldRejectHttpsToHttpDowngrade() {
        // Given
        URI httpsUri = URI.create("https://example.com/");
        URI httpUri = URI.create("http://example.com/");

        ZapRedirectHandler handler =
                new ZapRedirectHandler(
                        (uri, statusCode, locationHeader) -> {
                            if (uri.equals(httpsUri)) {
                                return new ZapRedirectHandler.Response(301, httpUri.toString());
                            }
                            return new ZapRedirectHandler.Response(200, null);
                        });

        // When / Then
        assertThatThrownBy(() -> handler.execute(httpsUri))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("scheme");
    }

    @Test
    void shouldNotifyCallbackForEachHop() throws IOException {
        // Given
        URI first = URI.create("http://example.com/a");
        URI second = URI.create("http://example.com/b");
        List<URI> callbackUris = new ArrayList<>();

        ZapRedirectHandler handler =
                new ZapRedirectHandler(
                        (uri, statusCode, locationHeader) -> {
                            callbackUris.add(uri);
                            if (uri.equals(first)) {
                                return new ZapRedirectHandler.Response(302, second.toString());
                            }
                            return new ZapRedirectHandler.Response(200, null);
                        });

        // When
        handler.execute(first);

        // Then - callback is called for each hop (first and second)
        assertThat(callbackUris).containsExactly(first, second);
    }

    @Test
    void shouldHandleAllRedirectStatusCodes() throws IOException {
        for (int statusCode : new int[] {301, 302, 303, 307, 308}) {
            // Given
            URI first = URI.create("http://example.com/");
            URI second = URI.create("http://example.com/final");
            List<URI> visited = new ArrayList<>();

            ZapRedirectHandler handler =
                    new ZapRedirectHandler(
                            (uri, statusCode2, locationHeader) -> {
                                visited.add(uri);
                                if (uri.equals(first)) {
                                    return new ZapRedirectHandler.Response(
                                            statusCode, second.toString());
                                }
                                return new ZapRedirectHandler.Response(200, null);
                            });

            // When
            ZapRedirectHandler.Response response = handler.execute(first);

            // Then
            assertThat(response.statusCode())
                    .as("Status code %d should be followed", statusCode)
                    .isEqualTo(200);
            assertThat(visited).containsExactly(first, second);
        }
    }

    @Test
    void shouldNotFollowNon3xxStatusCodes() throws IOException {
        for (int statusCode : new int[] {200, 201, 400, 404, 500}) {
            // Given
            URI uri = URI.create("http://example.com/");
            List<URI> visited = new ArrayList<>();

            ZapRedirectHandler handler =
                    new ZapRedirectHandler(
                            (u, sc, loc) -> {
                                visited.add(u);
                                return new ZapRedirectHandler.Response(statusCode, null);
                            });

            // When
            ZapRedirectHandler.Response response = handler.execute(uri);

            // Then
            assertThat(response.statusCode())
                    .as("Status code %d should not be followed", statusCode)
                    .isEqualTo(statusCode);
            assertThat(visited).hasSize(1);
        }
    }

    @Test
    void shouldStopWhenNoLocationHeader() throws IOException {
        // Given
        URI uri = URI.create("http://example.com/");
        List<URI> visited = new ArrayList<>();

        ZapRedirectHandler handler =
                new ZapRedirectHandler(
                        (u, sc, loc) -> {
                            visited.add(u);
                            return new ZapRedirectHandler.Response(302, null);
                        });

        // When
        ZapRedirectHandler.Response response = handler.execute(uri);

        // Then
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(visited).hasSize(1);
    }

    @Test
    void shouldAllowSameSchemeRedirect() throws IOException {
        // Given
        URI first = URI.create("https://example.com/a");
        URI second = URI.create("https://example.com/b");
        List<URI> visited = new ArrayList<>();

        ZapRedirectHandler handler =
                new ZapRedirectHandler(
                        (uri, statusCode, locationHeader) -> {
                            visited.add(uri);
                            if (uri.equals(first)) {
                                return new ZapRedirectHandler.Response(301, second.toString());
                            }
                            return new ZapRedirectHandler.Response(200, null);
                        });

        // When
        ZapRedirectHandler.Response response = handler.execute(first);

        // Then
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(visited).containsExactly(first, second);
    }

    @Test
    void shouldEnforceMaxRedirectsOfOneHundred() {
        // Given - a chain of unique URIs that would go on forever
        ZapRedirectHandler handler =
                new ZapRedirectHandler(
                        (uri, statusCode, locationHeader) -> {
                            int n = Integer.parseInt(uri.getPath().substring(1));
                            return new ZapRedirectHandler.Response(
                                    302, "http://example.com/" + (n + 1));
                        });

        // When / Then
        assertThatThrownBy(() -> handler.execute(URI.create("http://example.com/0")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("redirect");
    }
}
