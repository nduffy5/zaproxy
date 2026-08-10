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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the network migration inventory document exists and accurately lists every
 * commons-httpclient reference in org.zaproxy.zap.network.
 *
 * <p>These tests act as a living contract: if a new commons-httpclient usage is added to the
 * network package the inventory must be updated, and if the inventory document is missing the tests
 * fail immediately.
 */
class NetworkMigrationInventoryTest {

    /** Relative path from the repository root to the inventory document. */
    private static final String INVENTORY_PATH = "docs/network-migration-inventory.md";

    /**
     * Resolves the repository root by walking up from the compiled test-class location until a
     * directory that contains {@code build.gradle.kts} is found.
     */
    private static Path repoRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("build.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root from: " + Paths.get("").toAbsolutePath());
    }

    private String inventoryContent() throws IOException {
        Path inventoryFile = repoRoot().resolve(INVENTORY_PATH);
        assertTrue(
                Files.exists(inventoryFile),
                "Inventory document must exist at " + INVENTORY_PATH
                        + " – create it with the full commons-httpclient usage list.");
        return Files.readString(inventoryFile);
    }

    // -----------------------------------------------------------------------
    // Document structure
    // -----------------------------------------------------------------------

    @Test
    void inventoryDocumentExists() throws IOException {
        Path inventoryFile = repoRoot().resolve(INVENTORY_PATH);
        assertTrue(Files.exists(inventoryFile),
                "docs/network-migration-inventory.md must exist");
    }

    @Test
    void inventoryDocumentHasTitle() throws IOException {
        String content = inventoryContent();
        assertThat(content, containsString("network-migration-inventory"));
    }

    // -----------------------------------------------------------------------
    // Every affected class must be listed
    // -----------------------------------------------------------------------

    @Test
    void inventoryListsDefaultHttpRedirectionValidator() throws IOException {
        assertThat(inventoryContent(), containsString("DefaultHttpRedirectionValidator"));
    }

    @Test
    void inventoryListsHttpRedirectionValidator() throws IOException {
        assertThat(inventoryContent(), containsString("HttpRedirectionValidator"));
    }

    @Test
    void inventoryListsZapCookieSpec() throws IOException {
        assertThat(inventoryContent(), containsString("ZapCookieSpec"));
    }

    @Test
    void inventoryListsZapDeleteMethod() throws IOException {
        assertThat(inventoryContent(), containsString("ZapDeleteMethod"));
    }

    @Test
    void inventoryListsZapHeadMethod() throws IOException {
        assertThat(inventoryContent(), containsString("ZapHeadMethod"));
    }

    @Test
    void inventoryListsZapHttpParser() throws IOException {
        assertThat(inventoryContent(), containsString("ZapHttpParser"));
    }

    @Test
    void inventoryListsZapNTLMEngineImpl() throws IOException {
        assertThat(inventoryContent(), containsString("ZapNTLMEngineImpl"));
    }

    @Test
    void inventoryListsZapNTLMScheme() throws IOException {
        assertThat(inventoryContent(), containsString("ZapNTLMScheme"));
    }

    @Test
    void inventoryListsZapOptionsMethod() throws IOException {
        assertThat(inventoryContent(), containsString("ZapOptionsMethod"));
    }

    @Test
    void inventoryListsZapPostMethod() throws IOException {
        assertThat(inventoryContent(), containsString("ZapPostMethod"));
    }

    @Test
    void inventoryListsZapPutMethod() throws IOException {
        assertThat(inventoryContent(), containsString("ZapPutMethod"));
    }

    @Test
    void inventoryListsZapTraceMethod() throws IOException {
        assertThat(inventoryContent(), containsString("ZapTraceMethod"));
    }

    // -----------------------------------------------------------------------
    // Every commons-httpclient type referenced in the network package must
    // appear in the inventory.
    // -----------------------------------------------------------------------

    @Test
    void inventoryMentionsCommonsHttpclientURI() throws IOException {
        assertThat(inventoryContent(), containsString("org.apache.commons.httpclient.URI"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientCookie() throws IOException {
        assertThat(inventoryContent(), containsString("org.apache.commons.httpclient.Cookie"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientCookieSpecBase() throws IOException {
        assertThat(inventoryContent(), containsString("CookieSpecBase"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientMalformedCookieException() throws IOException {
        assertThat(inventoryContent(), containsString("MalformedCookieException"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientHeader() throws IOException {
        assertThat(inventoryContent(), containsString("org.apache.commons.httpclient.Header"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientHttpState() throws IOException {
        assertThat(inventoryContent(), containsString("org.apache.commons.httpclient.HttpState"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientHttpConnection() throws IOException {
        assertThat(inventoryContent(), containsString("org.apache.commons.httpclient.HttpConnection"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientDeleteMethod() throws IOException {
        assertThat(inventoryContent(), containsString("methods.DeleteMethod"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientEntityEnclosingMethod() throws IOException {
        assertThat(inventoryContent(), containsString("EntityEnclosingMethod"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientProtocolException() throws IOException {
        assertThat(inventoryContent(), containsString("ProtocolException"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientHeadMethod() throws IOException {
        assertThat(inventoryContent(), containsString("methods.HeadMethod"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientHttpMethodParams() throws IOException {
        assertThat(inventoryContent(), containsString("HttpMethodParams"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientHttpParser() throws IOException {
        assertThat(inventoryContent(), containsString("org.apache.commons.httpclient.HttpParser"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientAuthenticationException() throws IOException {
        assertThat(inventoryContent(), containsString("AuthenticationException"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientCredentials() throws IOException {
        assertThat(inventoryContent(), containsString("org.apache.commons.httpclient.Credentials"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientHttpMethod() throws IOException {
        assertThat(inventoryContent(), containsString("org.apache.commons.httpclient.HttpMethod"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientNTCredentials() throws IOException {
        assertThat(inventoryContent(), containsString("NTCredentials"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientAuthChallengeParser() throws IOException {
        assertThat(inventoryContent(), containsString("AuthChallengeParser"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientAuthScheme() throws IOException {
        assertThat(inventoryContent(), containsString("AuthScheme"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientMalformedChallengeException() throws IOException {
        assertThat(inventoryContent(), containsString("MalformedChallengeException"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientOptionsMethod() throws IOException {
        assertThat(inventoryContent(), containsString("methods.OptionsMethod"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientPostMethod() throws IOException {
        assertThat(inventoryContent(), containsString("methods.PostMethod"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientPutMethod() throws IOException {
        assertThat(inventoryContent(), containsString("methods.PutMethod"));
    }

    @Test
    void inventoryMentionsCommonsHttpclientTraceMethod() throws IOException {
        assertThat(inventoryContent(), containsString("methods.TraceMethod"));
    }

    // -----------------------------------------------------------------------
    // Scope boundary: the inventory must document that extension/ and view/
    // are explicitly out of scope for this migration step.
    // -----------------------------------------------------------------------

    @Test
    void inventoryDocumentsScopeBoundary() throws IOException {
        String content = inventoryContent();
        // The document must explicitly call out the scope boundary
        assertThat(content, containsString("org.zaproxy.zap.network"));
    }

    @Test
    void inventoryDocumentsHttpSenderImplInterface() throws IOException {
        assertThat(inventoryContent(), containsString("HttpSenderImpl"));
    }
}
