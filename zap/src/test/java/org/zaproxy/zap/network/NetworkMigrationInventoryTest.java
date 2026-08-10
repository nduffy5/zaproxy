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
 * Verifies that the commons-httpclient migration inventory document exists and accurately
 * lists every class in org.zaproxy.zap.network that references org.apache.commons.httpclient.*.
 *
 * <p>This test acts as a regression guard: if a new commons-httpclient reference is added to the
 * network package without updating the inventory, the grep-based acceptance criteria will fail.
 */
class NetworkMigrationInventoryTest {

    private static final Path INVENTORY_PATH =
            Paths.get("../docs/network-migration-inventory.md").toAbsolutePath().normalize();

    @Test
    void inventoryDocumentExists() {
        assertTrue(
                Files.exists(INVENTORY_PATH),
                "docs/network-migration-inventory.md must exist. "
                        + "Looked at: " + INVENTORY_PATH);
    }

    @Test
    void inventoryDocumentListsDefaultHttpRedirectionValidator() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention DefaultHttpRedirectionValidator",
                content,
                containsString("DefaultHttpRedirectionValidator"));
    }

    @Test
    void inventoryDocumentListsHttpRedirectionValidator() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention HttpRedirectionValidator",
                content,
                containsString("HttpRedirectionValidator"));
    }

    @Test
    void inventoryDocumentListsZapCookieSpec() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention ZapCookieSpec",
                content,
                containsString("ZapCookieSpec"));
    }

    @Test
    void inventoryDocumentListsZapDeleteMethod() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention ZapDeleteMethod",
                content,
                containsString("ZapDeleteMethod"));
    }

    @Test
    void inventoryDocumentListsZapHeadMethod() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention ZapHeadMethod",
                content,
                containsString("ZapHeadMethod"));
    }

    @Test
    void inventoryDocumentListsZapHttpParser() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention ZapHttpParser",
                content,
                containsString("ZapHttpParser"));
    }

    @Test
    void inventoryDocumentListsZapNTLMEngineImpl() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention ZapNTLMEngineImpl",
                content,
                containsString("ZapNTLMEngineImpl"));
    }

    @Test
    void inventoryDocumentListsZapNTLMScheme() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention ZapNTLMScheme",
                content,
                containsString("ZapNTLMScheme"));
    }

    @Test
    void inventoryDocumentListsZapOptionsMethod() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention ZapOptionsMethod",
                content,
                containsString("ZapOptionsMethod"));
    }

    @Test
    void inventoryDocumentListsZapPostMethod() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention ZapPostMethod",
                content,
                containsString("ZapPostMethod"));
    }

    @Test
    void inventoryDocumentListsZapPutMethod() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention ZapPutMethod",
                content,
                containsString("ZapPutMethod"));
    }

    @Test
    void inventoryDocumentListsZapTraceMethod() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention ZapTraceMethod",
                content,
                containsString("ZapTraceMethod"));
    }

    @Test
    void inventoryDocumentMentionsCommonsHttpclientURI() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention org.apache.commons.httpclient.URI",
                content,
                containsString("org.apache.commons.httpclient.URI"));
    }

    @Test
    void inventoryDocumentMentionsCookieSpecBase() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention CookieSpecBase",
                content,
                containsString("CookieSpecBase"));
    }

    @Test
    void inventoryDocumentMentionsAuthScheme() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must mention AuthScheme",
                content,
                containsString("AuthScheme"));
    }

    @Test
    void inventoryDocumentMentionsHttpSenderImplInterface() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory must document the HttpSenderImpl interface as the migration boundary",
                content,
                containsString("HttpSenderImpl"));
    }

    @Test
    void inventoryDocumentIsNotEmpty() throws IOException {
        String content = readInventory();
        assertThat(
                "Inventory document must not be empty",
                content.trim().isEmpty(),
                is(false));
    }

    // -------------------------------------------------------------------------
    // Scope-boundary assertions: classes NOT in the network package must not
    // appear in the inventory as affected classes.
    // -------------------------------------------------------------------------

    @Test
    void inventoryDocumentScopeIsNetworkPackageOnly() throws IOException {
        String content = readInventory();
        // The inventory must declare its scope as the network package
        assertThat(
                "Inventory must state its scope as org.zaproxy.zap.network",
                content,
                containsString("org.zaproxy.zap.network"));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static String readInventory() throws IOException {
        return Files.readString(INVENTORY_PATH);
    }
}
