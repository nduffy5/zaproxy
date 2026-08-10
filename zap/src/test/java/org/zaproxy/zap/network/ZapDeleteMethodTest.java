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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.net.URI;
import java.net.http.HttpRequest;
import org.junit.jupiter.api.Test;

/** Unit test for {@link ZapDeleteMethod}. */
@SuppressWarnings("deprecation")
class ZapDeleteMethodTest {

    private static final String TEST_URI = "http://example.com/resource";

    @Test
    void noArgConstructorShouldCreateInstance() {
        // When
        ZapDeleteMethod method = new ZapDeleteMethod();
        // Then
        assertThat(method, is(notNullValue()));
    }

    @Test
    void uriConstructorShouldCreateInstance() {
        // When
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URI);
        // Then
        assertThat(method, is(notNullValue()));
    }

    @Test
    void getNameShouldReturnDelete() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod();
        // When
        String name = method.getName();
        // Then
        assertThat(name, is(equalTo("DELETE")));
    }

    @Test
    void getNameShouldReturnDeleteForUriConstructor() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URI);
        // When
        String name = method.getName();
        // Then
        assertThat(name, is(equalTo("DELETE")));
    }

    @Test
    void builderShouldNotBeNull() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod();
        // When
        HttpRequest.Builder builder = method.getBuilder();
        // Then
        assertThat(builder, is(notNullValue()));
    }

    @Test
    void builderFromUriConstructorShouldNotBeNull() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URI);
        // When
        HttpRequest.Builder builder = method.getBuilder();
        // Then
        assertThat(builder, is(notNullValue()));
    }

    @Test
    void builtRequestShouldHaveDeleteMethod() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URI);
        // When
        HttpRequest request = method.getBuilder().build();
        // Then
        assertThat(request.method(), is(equalTo("DELETE")));
    }

    @Test
    void builtRequestShouldHaveCorrectUri() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URI);
        // When
        HttpRequest request = method.getBuilder().build();
        // Then
        assertThat(request.uri(), is(equalTo(URI.create(TEST_URI))));
    }

    @Test
    void builtRequestWithBodyShouldHaveDeleteMethod() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod(TEST_URI);
        // When
        HttpRequest request =
                method.getBuilder()
                        .method("DELETE", HttpRequest.BodyPublishers.ofString("body content"))
                        .build();
        // Then
        assertThat(request.method(), is(equalTo("DELETE")));
    }

    @Test
    void noArgBuilderShouldAllowSettingUri() {
        // Given
        ZapDeleteMethod method = new ZapDeleteMethod();
        URI uri = URI.create(TEST_URI);
        // When
        HttpRequest request = method.getBuilder().uri(uri).build();
        // Then
        assertThat(request.method(), is(equalTo("DELETE")));
        assertThat(request.uri(), is(equalTo(uri)));
    }
}
