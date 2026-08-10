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

import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * A cookie spec implementation backed by {@link java.net.CookieManager} configured with {@link
 * CookiePolicy#ACCEPT_ALL}, matching ZAP's permissive scanning posture.
 *
 * @since 2.7.0
 * @deprecated (2.12.0) Implementation details, do not use.
 */
@Deprecated
public class ZapCookieSpec {

    private final CookieManager cookieManager;

    /** Creates a new {@code ZapCookieSpec} with an {@code ACCEPT_ALL} cookie policy. */
    public ZapCookieSpec() {
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    }

    /**
     * Returns the underlying {@link CookieManager} configured with {@link CookiePolicy#ACCEPT_ALL}.
     *
     * @return the cookie manager, never {@code null}.
     */
    public CookieManager getCookieManager() {
        return cookieManager;
    }
}
