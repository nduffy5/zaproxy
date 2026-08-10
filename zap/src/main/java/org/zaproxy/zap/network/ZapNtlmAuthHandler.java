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

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * An {@link Authenticator} that performs NTLM authentication using the three-way handshake (Type 1
 * negotiate, Type 2 challenge, Type 3 authenticate).
 *
 * <p>This handler uses {@link ZapNTLMEngineImpl} for NTLM token encoding and decoding.
 *
 * @since 2.16.0
 */
public class ZapNtlmAuthHandler extends Authenticator {

    private static final String SCHEME_NAME = "ntlm";

    private final String username;
    private final String password;
    private final String domain;
    private final String workstation;

    @SuppressWarnings("deprecation")
    private final ZapNTLMEngineImpl engine;

    /**
     * Creates a new NTLM auth handler with the given credentials.
     *
     * <p>The {@code username} may include the domain prefix in the form {@code
     * DOMAIN\u005cusername}. If a domain prefix is present and {@code domain} is {@code null}, the
     * prefix is used as the domain.
     *
     * @param username the username (optionally prefixed with domain and backslash)
     * @param password the password
     * @param domain the Windows domain, may be {@code null}
     * @param workstation the workstation name, may be {@code null}
     */
    @SuppressWarnings("deprecation")
    public ZapNtlmAuthHandler(String username, String password, String domain, String workstation) {
        String resolvedDomain = domain;
        String resolvedUsername = username;

        if (username != null && username.contains("\\")) {
            int idx = username.indexOf('\\');
            if (resolvedDomain == null || resolvedDomain.isEmpty()) {
                resolvedDomain = username.substring(0, idx);
            }
            resolvedUsername = username.substring(idx + 1);
        }

        this.username = resolvedUsername;
        this.password = password;
        this.domain = resolvedDomain != null ? resolvedDomain : "";
        this.workstation = workstation != null ? workstation : "";
        this.engine = new ZapNTLMEngineImpl();
    }

    /**
     * Returns the scheme name for this handler.
     *
     * @return {@code "ntlm"}
     */
    public String getSchemeName() {
        return SCHEME_NAME;
    }

    /**
     * Returns {@code true} because NTLM is a connection-based authentication scheme.
     *
     * @return {@code true}
     */
    public boolean isConnectionBased() {
        return true;
    }

    /**
     * Generates the NTLM Type 1 (negotiate) message.
     *
     * @return the Base64-encoded Type 1 message
     * @throws Exception if the message cannot be generated
     */
    @SuppressWarnings("deprecation")
    public String generateType1Msg() throws Exception {
        return engine.generateType1Msg(domain, workstation);
    }

    /**
     * Generates the NTLM Type 3 (authenticate) message in response to a Type 2 challenge.
     *
     * @param type2Challenge the Base64-encoded Type 2 challenge message received from the server
     * @return the Base64-encoded Type 3 message
     * @throws Exception if the message cannot be generated
     */
    @SuppressWarnings("deprecation")
    public String generateType3Msg(String type2Challenge) throws Exception {
        return engine.generateType3Msg(username, password, domain, workstation, type2Challenge);
    }

    /**
     * Returns a {@link PasswordAuthentication} for the configured credentials.
     *
     * <p>This is used when the JDK's HTTP client requests credentials for NTLM authentication.
     *
     * @return a {@link PasswordAuthentication} with the username and password
     */
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if (!SCHEME_NAME.equalsIgnoreCase(getRequestingScheme())) {
            return null;
        }
        return new PasswordAuthentication(
                username, password != null ? password.toCharArray() : new char[0]);
    }
}
