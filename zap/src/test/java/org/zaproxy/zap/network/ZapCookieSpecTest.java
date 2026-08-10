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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit test for {@link ZapCookieSpec}. */
@SuppressWarnings("deprecation")
class ZapCookieSpecTest {

    @Test
    void shouldBeInstantiable() {
        assertDoesNotThrow(ZapCookieSpec::new);
    }

    @Test
    void shouldProvideCookieManager() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        // When
        CookieManager manager = spec.getCookieManager();
        // Then
        assertNotNull(manager);
    }

    @Test
    void shouldReturnNonNullCookieStore() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        // When
        CookieStore store = spec.getCookieManager().getCookieStore();
        // Then
        assertNotNull(store);
    }

    @Test
    void shouldReturnEmptyCookieStoreInitially() {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        // When
        List<HttpCookie> cookies = spec.getCookieManager().getCookieStore().getCookies();
        // Then
        assertNotNull(cookies);
        assertTrue(cookies.isEmpty());
    }

    @Test
    void shouldCreateIndependentCookieManagerPerInstance() {
        // Given
        ZapCookieSpec spec1 = new ZapCookieSpec();
        ZapCookieSpec spec2 = new ZapCookieSpec();
        // When / Then
        assertFalse(spec1.getCookieManager() == spec2.getCookieManager());
    }

    @Test
    void shouldAcceptCookiesFromAnyDomainWithAcceptAllPolicy() throws Exception {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        CookieManager manager = spec.getCookieManager();
        URI uri = new URI("http://example.com/");
        // When - put a Set-Cookie header from a different domain
        Map<String, List<String>> responseHeaders =
                Map.of("Set-Cookie", List.of("session=abc123; Domain=other.example.com"));
        manager.put(uri, responseHeaders);
        // Then - ACCEPT_ALL policy stores the cookie regardless of domain mismatch
        List<HttpCookie> cookies = manager.getCookieStore().getCookies();
        assertFalse(cookies.isEmpty());
    }

    @Test
    void shouldStoreCookieInCookieStore() throws Exception {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        CookieManager manager = spec.getCookieManager();
        URI uri = new URI("http://example.com/");
        // When
        Map<String, List<String>> responseHeaders = Map.of("Set-Cookie", List.of("name=value"));
        manager.put(uri, responseHeaders);
        // Then
        List<HttpCookie> cookies = manager.getCookieStore().getCookies();
        assertFalse(cookies.isEmpty());
        assertTrue(cookies.stream().anyMatch(c -> "name".equals(c.getName())));
    }

    @Test
    void shouldRetrieveCookiesForMatchingUri() throws Exception {
        // Given
        ZapCookieSpec spec = new ZapCookieSpec();
        CookieManager manager = spec.getCookieManager();
        URI uri = new URI("http://example.com/");
        Map<String, List<String>> responseHeaders = Map.of("Set-Cookie", List.of("session=xyz"));
        manager.put(uri, responseHeaders);
        // When
        Map<String, List<String>> requestHeaders = manager.get(uri, Map.of());
        // Then
        assertNotNull(requestHeaders);
        assertTrue(requestHeaders.containsKey("Cookie"));
        assertTrue(requestHeaders.get("Cookie").stream().anyMatch(v -> v.contains("session=xyz")));
    }
}
