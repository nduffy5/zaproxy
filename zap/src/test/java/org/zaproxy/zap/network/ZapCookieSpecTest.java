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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit test for {@link ZapCookieSpec}. */
@SuppressWarnings("deprecation")
class ZapCookieSpecTest {

    private static final String HOST = "example.com";
    private static final int PORT = 8443;
    private static final String PATH = "/path/file";
    private static final boolean SECURE = true;

    @Test
    void shouldInstantiateWithCookieManager() {
        // Given / When
        ZapCookieSpec spec = new ZapCookieSpec();
        // Then
        assertNotNull(spec.getCookieManager());
    }

    @Test
    void shouldUseCookiePolicyAcceptAll() throws Exception {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        CookieManager manager = spec.getCookieManager();
        // When - add a cookie from any domain
        HttpCookie cookie = new HttpCookie("test", "value");
        cookie.setDomain("any.domain.com");
        cookie.setPath("/");
        cookie.setVersion(0);
        manager.getCookieStore().add(new URI("http://any.domain.com/"), cookie);
        // Then - cookie is accepted (ACCEPT_ALL policy)
        List<HttpCookie> cookies = manager.getCookieStore().getCookies();
        assertNotNull(cookies);
        assertFalse(cookies.isEmpty());
    }

    @Test
    void shouldProvideCookieStore() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        // When
        CookieStore store = spec.getCookieManager().getCookieStore();
        // Then
        assertNotNull(store);
    }

    @Test
    void shouldStoreCookiesInCookieStore() throws Exception {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        CookieManager manager = spec.getCookieManager();
        HttpCookie cookie1 = new HttpCookie("name1", "value1");
        cookie1.setDomain("example.com");
        cookie1.setPath("/");
        cookie1.setVersion(0);
        HttpCookie cookie2 = new HttpCookie("name2", "value2");
        cookie2.setDomain("other.com");
        cookie2.setPath("/");
        cookie2.setVersion(0);
        // When
        manager.getCookieStore().add(new URI("http://example.com/"), cookie1);
        manager.getCookieStore().add(new URI("http://other.com/"), cookie2);
        // Then
        List<HttpCookie> cookies = manager.getCookieStore().getCookies();
        assertNotNull(cookies);
        org.junit.jupiter.api.Assertions.assertEquals(2, cookies.size());
    }

    @Test
    void shouldThrowWhenValidatingWithNullHost() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        HttpCookie cookie = new HttpCookie("name", "value");
        // When / Then
        assertThrows(
                IllegalArgumentException.class,
                () -> spec.validate(null, PORT, PATH, SECURE, cookie));
    }

    @Test
    void shouldThrowWhenValidatingWithEmptyHost() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        HttpCookie cookie = new HttpCookie("name", "value");
        // When / Then
        assertThrows(
                IllegalArgumentException.class,
                () -> spec.validate("", PORT, PATH, SECURE, cookie));
    }

    @Test
    void shouldThrowWhenValidatingWithNegativePort() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        HttpCookie cookie = new HttpCookie("name", "value");
        // When / Then
        assertThrows(
                IllegalArgumentException.class,
                () -> spec.validate(HOST, -1, PATH, SECURE, cookie));
    }

    @Test
    void shouldThrowWhenValidatingWithNullPath() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        HttpCookie cookie = new HttpCookie("name", "value");
        // When / Then
        assertThrows(
                IllegalArgumentException.class,
                () -> spec.validate(HOST, PORT, null, SECURE, cookie));
    }

    @Test
    void shouldThrowWhenValidatingWithNullCookie() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        // When / Then
        assertThrows(
                NullPointerException.class,
                () -> spec.validate(HOST, PORT, PATH, SECURE, (HttpCookie) null));
    }

    @Test
    void shouldBeValidEvenIfCookiePathIsDifferentThanOrigin() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        HttpCookie cookie = new HttpCookie("name", "value");
        cookie.setPath("/other/path/");
        cookie.setDomain(HOST);
        // When / Then
        assertDoesNotThrow(() -> spec.validate(HOST, PORT, PATH, SECURE, cookie));
    }

    @Test
    void shouldBeValidWhenDomainMatchesHost() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        HttpCookie cookie = new HttpCookie("name", "value");
        cookie.setDomain(HOST);
        // When / Then
        assertDoesNotThrow(() -> spec.validate(HOST, PORT, PATH, SECURE, cookie));
    }

    @Test
    void shouldBeValidWhenDomainIsSuffixOfHost() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        HttpCookie cookie = new HttpCookie("name", "value");
        cookie.setDomain(".example.com");
        // When / Then
        assertDoesNotThrow(() -> spec.validate("sub.example.com", PORT, PATH, SECURE, cookie));
    }

    @Test
    void shouldThrowIllegalArgumentWhenDomainDoesNotMatchHost() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        HttpCookie cookie = new HttpCookie("name", "value");
        cookie.setDomain("other.com");
        // When / Then
        assertThrows(
                IllegalArgumentException.class,
                () -> spec.validate(HOST, PORT, PATH, SECURE, cookie));
    }
}
