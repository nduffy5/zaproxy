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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ZapCookieHandler} — the replacement for the deprecated {@link
 * ZapCookieSpec} that is built on {@code java.net.CookieManager} / {@code java.net.CookiePolicy}
 * rather than Commons HttpClient's {@code CookieSpecBase}.
 *
 * <p>These tests intentionally reference {@link ZapCookieHandler}, which does not yet exist, so
 * that the TDD cycle fails at compilation until the implementation is provided.
 */
class ZapCookieSpecTest {

    private ZapCookieHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ZapCookieHandler();
    }

    // -------------------------------------------------------------------------
    // Domain matching
    // -------------------------------------------------------------------------

    @Test
    void shouldAcceptCookieWhenDomainMatchesRequestHost() throws Exception {
        // Given
        URI uri = new URI("https://example.com/path");
        HttpCookie cookie = new HttpCookie("session", "abc123");
        cookie.setDomain("example.com");
        cookie.setPath("/");
        cookie.setVersion(0);

        // When
        boolean accepted = handler.isCookieAccepted(uri, cookie);

        // Then
        assertTrue(accepted, "Cookie whose domain equals the request host must be accepted");
    }

    @Test
    void shouldAcceptCookieWhenDomainIsDotPrefixedAndMatchesRequestHost() throws Exception {
        // Given
        URI uri = new URI("https://sub.example.com/path");
        HttpCookie cookie = new HttpCookie("session", "abc123");
        cookie.setDomain(".example.com");
        cookie.setPath("/");
        cookie.setVersion(0);

        // When
        boolean accepted = handler.isCookieAccepted(uri, cookie);

        // Then
        assertTrue(accepted, "Cookie with domain '.example.com' must match 'sub.example.com'");
    }

    @Test
    void shouldRejectCookieWhenDomainDoesNotMatchRequestHost() throws Exception {
        // Given
        URI uri = new URI("https://other.com/path");
        HttpCookie cookie = new HttpCookie("session", "abc123");
        cookie.setDomain("example.com");
        cookie.setPath("/");
        cookie.setVersion(0);

        // When
        boolean accepted = handler.isCookieAccepted(uri, cookie);

        // Then
        assertFalse(accepted, "Cookie whose domain does not match the request host must be rejected");
    }

    @Test
    void shouldRejectCookieWhenDomainIsSubdomainOfRequestHost() throws Exception {
        // Given – cookie claims a *more specific* domain than the request host
        URI uri = new URI("https://example.com/path");
        HttpCookie cookie = new HttpCookie("session", "abc123");
        cookie.setDomain("sub.example.com");
        cookie.setPath("/");
        cookie.setVersion(0);

        // When
        boolean accepted = handler.isCookieAccepted(uri, cookie);

        // Then
        assertFalse(
                accepted,
                "Cookie with domain 'sub.example.com' must not be accepted for host 'example.com'");
    }

    // -------------------------------------------------------------------------
    // Path matching
    // -------------------------------------------------------------------------

    @Test
    void shouldAcceptCookieWhenPathMatchesRequestPath() throws Exception {
        // Given
        URI uri = new URI("https://example.com/app/resource");
        HttpCookie cookie = new HttpCookie("token", "xyz");
        cookie.setDomain("example.com");
        cookie.setPath("/app");
        cookie.setVersion(0);

        // When
        boolean accepted = handler.isCookieAccepted(uri, cookie);

        // Then
        assertTrue(accepted, "Cookie path '/app' must match request path '/app/resource'");
    }

    @Test
    void shouldAcceptCookieWithRootPath() throws Exception {
        // Given
        URI uri = new URI("https://example.com/any/deep/path");
        HttpCookie cookie = new HttpCookie("token", "xyz");
        cookie.setDomain("example.com");
        cookie.setPath("/");
        cookie.setVersion(0);

        // When
        boolean accepted = handler.isCookieAccepted(uri, cookie);

        // Then
        assertTrue(accepted, "Cookie with path '/' must match any request path");
    }

    @Test
    void shouldAcceptCookieEvenWhenCookiePathDiffersFromRequestPath() throws Exception {
        // Given – ZAP intentionally does NOT enforce path restrictions (mirrors old ZapCookieSpec)
        URI uri = new URI("https://example.com/other/path");
        HttpCookie cookie = new HttpCookie("token", "xyz");
        cookie.setDomain("example.com");
        cookie.setPath("/app");
        cookie.setVersion(0);

        // When / Then – must not throw and must be accepted (no path enforcement)
        assertDoesNotThrow(
                () -> {
                    boolean accepted = handler.isCookieAccepted(uri, cookie);
                    assertTrue(
                            accepted,
                            "ZapCookieHandler must not enforce path restrictions, matching old ZapCookieSpec behaviour");
                });
    }

    // -------------------------------------------------------------------------
    // Secure-flag enforcement
    // -------------------------------------------------------------------------

    @Test
    void shouldAcceptSecureCookieOverHttpsConnection() throws Exception {
        // Given
        URI uri = new URI("https://example.com/secure");
        HttpCookie cookie = new HttpCookie("secureToken", "s3cr3t");
        cookie.setDomain("example.com");
        cookie.setPath("/");
        cookie.setSecure(true);
        cookie.setVersion(0);

        // When
        boolean accepted = handler.isCookieAccepted(uri, cookie);

        // Then
        assertTrue(accepted, "Secure cookie must be accepted over HTTPS");
    }

    @Test
    void shouldRejectSecureCookieOverHttpConnection() throws Exception {
        // Given
        URI uri = new URI("http://example.com/page");
        HttpCookie cookie = new HttpCookie("secureToken", "s3cr3t");
        cookie.setDomain("example.com");
        cookie.setPath("/");
        cookie.setSecure(true);
        cookie.setVersion(0);

        // When
        boolean accepted = handler.isCookieAccepted(uri, cookie);

        // Then
        assertFalse(accepted, "Secure cookie must NOT be accepted over plain HTTP");
    }

    @Test
    void shouldAcceptNonSecureCookieOverHttpConnection() throws Exception {
        // Given
        URI uri = new URI("http://example.com/page");
        HttpCookie cookie = new HttpCookie("ordinary", "value");
        cookie.setDomain("example.com");
        cookie.setPath("/");
        cookie.setSecure(false);
        cookie.setVersion(0);

        // When
        boolean accepted = handler.isCookieAccepted(uri, cookie);

        // Then
        assertTrue(accepted, "Non-secure cookie must be accepted over plain HTTP");
    }

    // -------------------------------------------------------------------------
    // SameSite attribute passthrough
    // -------------------------------------------------------------------------

    @Test
    void shouldPreserveSameSiteStrictAttribute() throws Exception {
        // Given
        String headerValue = "id=abc; Path=/; SameSite=Strict";

        // When
        List<HttpCookie> cookies = handler.parseCookieHeader("example.com", headerValue);

        // Then
        assertThat(cookies, is(notNullValue()));
        assertThat(cookies.size(), is(equalTo(1)));
        HttpCookie cookie = cookies.get(0);
        assertThat(
                "SameSite=Strict must be preserved",
                handler.getSameSite(cookie),
                is(equalTo("Strict")));
    }

    @Test
    void shouldPreserveSameSiteLaxAttribute() throws Exception {
        // Given
        String headerValue = "id=abc; Path=/; SameSite=Lax";

        // When
        List<HttpCookie> cookies = handler.parseCookieHeader("example.com", headerValue);

        // Then
        assertThat(cookies, is(notNullValue()));
        assertThat(cookies.size(), is(equalTo(1)));
        HttpCookie cookie = cookies.get(0);
        assertThat(
                "SameSite=Lax must be preserved",
                handler.getSameSite(cookie),
                is(equalTo("Lax")));
    }

    @Test
    void shouldPreserveSameSiteNoneAttribute() throws Exception {
        // Given
        String headerValue = "id=abc; Path=/; Secure; SameSite=None";

        // When
        List<HttpCookie> cookies = handler.parseCookieHeader("example.com", headerValue);

        // Then
        assertThat(cookies, is(notNullValue()));
        assertThat(cookies.size(), is(equalTo(1)));
        HttpCookie cookie = cookies.get(0);
        assertThat(
                "SameSite=None must be preserved",
                handler.getSameSite(cookie),
                is(equalTo("None")));
    }

    @Test
    void shouldReturnNullSameSiteWhenAttributeAbsent() throws Exception {
        // Given
        String headerValue = "id=abc; Path=/";

        // When
        List<HttpCookie> cookies = handler.parseCookieHeader("example.com", headerValue);

        // Then
        assertThat(cookies, is(notNullValue()));
        assertThat(cookies.size(), is(equalTo(1)));
        HttpCookie cookie = cookies.get(0);
        assertThat(
                "SameSite must be null when not present in Set-Cookie header",
                handler.getSameSite(cookie),
                is(equalTo(null)));
    }

    // -------------------------------------------------------------------------
    // Malformed Set-Cookie header rejection
    // -------------------------------------------------------------------------

    @Test
    void shouldRejectMalformedSetCookieHeaderWithNoName() {
        // Given – a Set-Cookie value that has no cookie name (just '=value')
        String malformedHeader = "=noname; Path=/";

        // When / Then
        assertThrows(
                IllegalArgumentException.class,
                () -> handler.parseCookieHeader("example.com", malformedHeader),
                "A Set-Cookie header with no cookie name must be rejected");
    }

    @Test
    void shouldRejectNullSetCookieHeader() {
        // When / Then
        assertThrows(
                IllegalArgumentException.class,
                () -> handler.parseCookieHeader("example.com", null),
                "A null Set-Cookie header must be rejected");
    }

    @Test
    void shouldRejectEmptySetCookieHeader() {
        // When / Then
        assertThrows(
                IllegalArgumentException.class,
                () -> handler.parseCookieHeader("example.com", ""),
                "An empty Set-Cookie header must be rejected");
    }

    @Test
    void shouldRejectSetCookieHeaderWithNullHost() {
        // Given
        String headerValue = "id=abc; Path=/";

        // When / Then
        assertThrows(
                IllegalArgumentException.class,
                () -> handler.parseCookieHeader(null, headerValue),
                "A null host must be rejected when parsing a Set-Cookie header");
    }

    // -------------------------------------------------------------------------
    // Round-trip: parse then accept
    // -------------------------------------------------------------------------

    @Test
    void shouldParseAndAcceptValidSetCookieHeader() throws Exception {
        // Given
        String headerValue = "session=tok123; Domain=example.com; Path=/; Secure";
        URI uri = new URI("https://example.com/app");

        // When
        List<HttpCookie> cookies = handler.parseCookieHeader("example.com", headerValue);

        // Then
        assertThat(cookies, is(notNullValue()));
        assertFalse(cookies.isEmpty(), "Parsed cookie list must not be empty");
        HttpCookie cookie = cookies.get(0);
        assertThat(cookie.getName(), is(equalTo("session")));
        assertThat(cookie.getValue(), is(equalTo("tok123")));
        assertTrue(cookie.getSecure(), "Secure flag must be parsed correctly");
        assertTrue(
                handler.isCookieAccepted(uri, cookie),
                "A validly parsed cookie must be accepted for its own origin URI");
    }
}
