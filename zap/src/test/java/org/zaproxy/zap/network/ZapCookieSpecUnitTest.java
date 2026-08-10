/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2017 The ZAP Development Team
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.CookieManager;
import org.junit.jupiter.api.Test;

/** Unit test for {@link ZapCookieSpec}. */
@SuppressWarnings("deprecation")
class ZapCookieSpecUnitTest {

    @Test
    void shouldBeInstantiable() {
        assertDoesNotThrow(ZapCookieSpec::new);
    }

    @Test
    void shouldProvideCookieManager() {
        // Given
        ZapCookieSpec cookieSpec = new ZapCookieSpec();
        // When
        CookieManager manager = cookieSpec.getCookieManager();
        // Then
        assertNotNull(manager);
    }

    @Test
    void shouldBeValidEvenIfCookiePathIsDifferentThanOrigin() {
        // Given / When / Then
        assertDoesNotThrow(ZapCookieSpec::new);
    }
}
