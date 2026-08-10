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
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Tests that verify the japicmp network API baseline is properly recorded and configured.
 *
 * <p>These tests are the regression gate for the HttpSenderImpl interface contract.
 */
class NetworkApiBaselineTest {

    private static final Path REPO_ROOT = findRepoRoot();
    private static final Path BASELINE_FILE = REPO_ROOT.resolve("docs/network-api-baseline.xml");
    private static final Path JAPICMP_YAML = REPO_ROOT.resolve("zap/gradle/japicmp.yaml");
    private static final Path CI_GRADLE = REPO_ROOT.resolve("gradle/ci.gradle.kts");

    @Test
    void baselineFileShouldExist() {
        assertTrue(
                Files.exists(BASELINE_FILE),
                "docs/network-api-baseline.xml must exist as the japicmp API baseline. "
                        + "Run './gradlew :zap:japicmp' to generate it.");
    }

    @Test
    void baselineFileShouldBeValidXml() throws Exception {
        assertTrue(Files.exists(BASELINE_FILE), "Baseline file must exist before XML validation");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(BASELINE_FILE.toFile());
        doc.getDocumentElement().normalize();

        String rootElement = doc.getDocumentElement().getNodeName();
        assertTrue(
                rootElement.equals("japicmp") || rootElement.equals("api-baseline"),
                "Root element should be 'japicmp' or 'api-baseline', but was: " + rootElement);
    }

    @Test
    void baselineFileShouldContainHttpSenderImplEntry() throws IOException {
        assertTrue(Files.exists(BASELINE_FILE), "Baseline file must exist");

        String content = Files.readString(BASELINE_FILE, StandardCharsets.UTF_8);
        assertThat(
                "Baseline must reference HttpSenderImpl",
                content,
                containsString("HttpSenderImpl"));
    }

    @Test
    void baselineFileShouldContainNetworkPackageEntry() throws IOException {
        assertTrue(Files.exists(BASELINE_FILE), "Baseline file must exist");

        String content = Files.readString(BASELINE_FILE, StandardCharsets.UTF_8);
        assertThat(
                "Baseline must reference org.zaproxy.zap.network package",
                content,
                containsString("org.zaproxy.zap.network"));
    }

    @Test
    void japicmpYamlShouldNotExcludeNetworkPackageInPackageExcludes() throws IOException {
        assertTrue(Files.exists(JAPICMP_YAML), "japicmp.yaml must exist");

        List<String> lines = Files.readAllLines(JAPICMP_YAML, StandardCharsets.UTF_8);

        // Parse the packageExcludes section and verify org.zaproxy.zap.network is not listed.
        // Comments (lines starting with #) are ignored.
        boolean inPackageExcludes = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                continue; // skip comments
            }
            if (trimmed.startsWith("packageExcludes:")) {
                inPackageExcludes = true;
                continue;
            }
            if (inPackageExcludes) {
                // End of packageExcludes section when we hit another top-level key
                if (!trimmed.isEmpty() && !trimmed.startsWith("-") && trimmed.contains(":")) {
                    break;
                }
                assertFalse(
                        trimmed.startsWith("-") && trimmed.contains("org.zaproxy.zap.network"),
                        "japicmp.yaml packageExcludes must NOT contain org.zaproxy.zap.network "
                                + "so that API changes in that package are detected. "
                                + "Found: "
                                + trimmed);
            }
        }
    }

    @Test
    void ciGradleShouldRunJapicmpInPipeline() throws IOException {
        assertTrue(Files.exists(CI_GRADLE), "gradle/ci.gradle.kts must exist");

        String content = Files.readString(CI_GRADLE, StandardCharsets.UTF_8);
        assertThat(
                "gradle/ci.gradle.kts must include japicmp in the CI pipeline",
                content,
                containsString("japicmp"));
    }

    @Test
    void baselineFileShouldIndicateZeroBreakingChanges() throws IOException {
        assertTrue(Files.exists(BASELINE_FILE), "Baseline file must exist");

        String content = Files.readString(BASELINE_FILE, StandardCharsets.UTF_8);
        // The baseline represents the current HEAD state - it should not contain
        // any breaking-change markers that would indicate regressions
        assertThat(
                "Baseline should not contain binary-incompatible markers",
                content,
                not(containsString("binaryCompatible=\"false\"")));
    }

    private static Path findRepoRoot() {
        // Walk up from the current working directory to find the repo root.
        // The repo root contains both build.gradle.kts and settings.gradle.kts.
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))
                    && Files.exists(current.resolve("build.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        // Fallback: assume we're running from the zap subproject directory
        return Paths.get("").toAbsolutePath().getParent();
    }
}
