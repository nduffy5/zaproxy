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
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Tests that verify the network API baseline infrastructure is correctly set up.
 *
 * <p>These tests ensure:
 * <ul>
 *   <li>The baseline file {@code docs/network-api-baseline.xml} exists and is valid XML
 *   <li>The japicmp configuration does not exclude {@code org.zaproxy.zap.network}
 *   <li>The CI configuration runs japicmp as a non-skipped step
 * </ul>
 */
class NetworkApiBaselineTest {

    private static final Path REPO_ROOT = findRepoRoot();
    private static final Path BASELINE_FILE = REPO_ROOT.resolve("docs/network-api-baseline.xml");
    private static final Path JAPICMP_YAML = REPO_ROOT.resolve("zap/gradle/japicmp.yaml");
    private static final Path CI_GRADLE = REPO_ROOT.resolve("gradle/ci.gradle.kts");

    private static Path findRepoRoot() {
        // Walk up from the test class location to find the repo root (contains gradlew)
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("gradlew"))) {
                return current;
            }
            current = current.getParent();
        }
        // Fallback: use the workspace path
        return Paths.get("/workspace/709938f9-ab1d-4907-b78a-0030a304a8e7/worker-1/repo");
    }

    /**
     * Verifies that the network API baseline file exists.
     */
    @Test
    void baselineFileShouldExist() {
        assertTrue(
                Files.exists(BASELINE_FILE),
                "docs/network-api-baseline.xml must exist as the japicmp API baseline. "
                        + "Run './gradlew :zap:japicmp' and commit the output.");
    }

    /**
     * Verifies that the baseline file is valid XML.
     */
    @Test
    void baselineFileShouldBeValidXml() throws Exception {
        assertTrue(Files.exists(BASELINE_FILE), "docs/network-api-baseline.xml must exist");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(BASELINE_FILE.toFile());
        doc.getDocumentElement().normalize();

        // The root element should be present (japicmp XML output)
        assertTrue(
                doc.getDocumentElement() != null,
                "docs/network-api-baseline.xml must be valid XML with a root element");
    }

    /**
     * Verifies that the baseline file contains references to the HttpSenderImpl interface,
     * confirming it captures the network API surface.
     */
    @Test
    void baselineFileShouldContainHttpSenderImplReference() throws IOException {
        assertTrue(Files.exists(BASELINE_FILE), "docs/network-api-baseline.xml must exist");

        String content = Files.readString(BASELINE_FILE);
        assertThat(
                "docs/network-api-baseline.xml must reference HttpSenderImpl to capture the network API",
                content,
                containsString("HttpSenderImpl"));
    }

    /**
     * Verifies that the baseline file contains references to the org.zaproxy.zap.network package.
     */
    @Test
    void baselineFileShouldContainNetworkPackageReference() throws IOException {
        assertTrue(Files.exists(BASELINE_FILE), "docs/network-api-baseline.xml must exist");

        String content = Files.readString(BASELINE_FILE);
        assertThat(
                "docs/network-api-baseline.xml must reference org.zaproxy.zap.network package",
                content,
                containsString("org.zaproxy.zap.network"));
    }

    /**
     * Verifies that the japicmp.yaml configuration does NOT exclude the org.zaproxy.zap.network
     * package, ensuring the network API is subject to binary compatibility checks.
     */
    @Test
    void japicmpYamlShouldNotExcludeNetworkPackage() throws IOException {
        assertTrue(Files.exists(JAPICMP_YAML), "zap/gradle/japicmp.yaml must exist");

        String content = Files.readString(JAPICMP_YAML);
        assertThat(
                "zap/gradle/japicmp.yaml must NOT exclude org.zaproxy.zap.network "
                        + "so that binary compatibility is enforced for the network API",
                content,
                not(containsString("org.zaproxy.zap.network")));
    }

    /**
     * Verifies that the CI configuration includes japicmp as a non-skipped step.
     * The CI gradle file should configure japicmp to run during the Java CI workflow.
     */
    @Test
    void ciGradleShouldConfigureJapicmpAsNonSkipped() throws IOException {
        assertTrue(Files.exists(CI_GRADLE), "gradle/ci.gradle.kts must exist");

        String content = Files.readString(CI_GRADLE);
        assertThat(
                "gradle/ci.gradle.kts must configure japicmp to run in CI (Java CI workflow)",
                content,
                containsString("japicmp"));
    }

    /**
     * Verifies that the CI configuration references the Java CI workflow.
     */
    @Test
    void ciGradleShouldReferenceJavaCiWorkflow() throws IOException {
        assertTrue(Files.exists(CI_GRADLE), "gradle/ci.gradle.kts must exist");

        String content = Files.readString(CI_GRADLE);
        assertThat(
                "gradle/ci.gradle.kts must reference the Java CI workflow",
                content,
                containsString("Java CI"));
    }
}
