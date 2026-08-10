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
import java.net.HttpCookie;

/**
 * A cookie spec implementation backed by {@link CookieManager} configured with {@link
 * CookiePolicy#ACCEPT_ALL}, matching ZAP's permissive scanning posture. Path validation is skipped
 * to allow cookies from any path.
 *
 * @since 2.7.0
 * @deprecated (2.12.0) Implementation details, do not use.
 */
@Deprecated
public class ZapCookieSpec {

    private final CookieManager cookieManager;

    /**
     * Constructs a {@code ZapCookieSpec} backed by a {@link CookieManager} accepting all cookies.
     */
    public ZapCookieSpec() {
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    }

    /**
     * Returns the underlying {@link CookieManager} used for cookie storage and policy.
     *
     * @return the cookie manager, never {@code null}.
     */
    public CookieManager getCookieManager() {
        return cookieManager;
    }

    /**
     * Validates the given cookie against the origin host, port, and path.
     *
     * <p>Path validation is intentionally skipped to allow cookies from any path. Domain validation
     * ensures the cookie domain matches or is a suffix of the origin host.
     *
     * @param host the origin host, must not be {@code null} or blank.
     * @param port the origin port, must be non-negative.
     * @param path the origin path, must not be {@code null}.
     * @param secure whether the connection is secure.
     * @param cookie the cookie to validate, must not be {@code null}.
     * @throws IllegalArgumentException if host is null/blank, port is negative, path is null, or
     *     the cookie domain does not match the host.
     * @throws NullPointerException if cookie is {@code null}.
     */
    public void validate(String host, int port, String path, boolean secure, HttpCookie cookie) {
        if (host == null) {
            throw new IllegalArgumentException("Host of origin may not be null");
        }
        if (host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host of origin may not be blank");
        }
        if (port < 0) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        if (path == null) {
            throw new IllegalArgumentException("Path of origin may not be null.");
        }
        // NullPointerException if cookie is null (matches original behaviour)
        String cookieDomain = cookie.getDomain();
        host = host.toLowerCase();

        // Validate the cookie's domain attribute. Domains without dots are allowed to support
        // hosts on private LANs that don't have DNS names.
        if (host.indexOf('.') >= 0) {
            if (!host.endsWith(cookieDomain)) {
                String domain = cookieDomain;
                if (domain.startsWith(".")) {
                    domain = domain.substring(1);
                }
                if (!host.equals(domain)) {
                    throw new IllegalArgumentException(
                            "Illegal domain attribute \""
                                    + cookieDomain
                                    + "\". Domain of origin: \""
                                    + host
                                    + "\"");
                }
            }
        } else {
            if (!host.equals(cookieDomain)) {
                throw new IllegalArgumentException(
                        "Illegal domain attribute \""
                                + cookieDomain
                                + "\". Domain of origin: \""
                                + host
                                + "\"");
            }
        }
    }
}
