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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ZapNtlmAuthHandler}. */
class ZapNtlmAuthHandlerTest {

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "testpass";
    private static final String DOMAIN = "TESTDOMAIN";
    private static final String WORKSTATION = "TESTHOST";

    @Test
    void shouldCreateHandlerWithCredentials() {
        // Given / When / Then
        assertThatCode(() -> new ZapNtlmAuthHandler(USERNAME, PASSWORD, DOMAIN, WORKSTATION))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldCreateHandlerWithNullDomain() {
        // Given / When / Then
        assertThatCode(() -> new ZapNtlmAuthHandler(USERNAME, PASSWORD, null, WORKSTATION))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldCreateHandlerWithNullWorkstation() {
        // Given / When / Then
        assertThatCode(() -> new ZapNtlmAuthHandler(USERNAME, PASSWORD, DOMAIN, null))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldGenerateType1Message() throws Exception {
        // Given
        ZapNtlmAuthHandler handler =
                new ZapNtlmAuthHandler(USERNAME, PASSWORD, DOMAIN, WORKSTATION);
        // When
        String type1 = handler.generateType1Msg();
        // Then
        assertThat(type1).isNotNull();
        assertThat(type1).isNotEmpty();
        // Verify it's valid Base64
        byte[] decoded = Base64.getDecoder().decode(type1);
        // NTLM signature: "NTLMSSP\0"
        assertThat(decoded)
                .startsWith(
                        (byte) 'N',
                        (byte) 'T',
                        (byte) 'L',
                        (byte) 'M',
                        (byte) 'S',
                        (byte) 'S',
                        (byte) 'P',
                        (byte) 0x00);
        // Type 1 message type indicator
        assertThat(decoded[8]).isEqualTo((byte) 0x01);
        assertThat(decoded[9]).isEqualTo((byte) 0x00);
        assertThat(decoded[10]).isEqualTo((byte) 0x00);
        assertThat(decoded[11]).isEqualTo((byte) 0x00);
    }

    @Test
    void shouldGenerateType3MessageFromType2Challenge() throws Exception {
        // Given
        ZapNtlmAuthHandler handler =
                new ZapNtlmAuthHandler(USERNAME, PASSWORD, DOMAIN, WORKSTATION);
        // A minimal valid Type 2 NTLM challenge message (base64 encoded)
        String type2Challenge = buildType2Message();
        // When
        String type3 = handler.generateType3Msg(type2Challenge);
        // Then
        assertThat(type3).isNotNull();
        assertThat(type3).isNotEmpty();
        // Verify it's valid Base64
        byte[] decoded = Base64.getDecoder().decode(type3);
        // NTLM signature: "NTLMSSP\0"
        assertThat(decoded)
                .startsWith(
                        (byte) 'N',
                        (byte) 'T',
                        (byte) 'L',
                        (byte) 'M',
                        (byte) 'S',
                        (byte) 'S',
                        (byte) 'P',
                        (byte) 0x00);
        // Type 3 message type indicator
        assertThat(decoded[8]).isEqualTo((byte) 0x03);
        assertThat(decoded[9]).isEqualTo((byte) 0x00);
        assertThat(decoded[10]).isEqualTo((byte) 0x00);
        assertThat(decoded[11]).isEqualTo((byte) 0x00);
    }

    @Test
    void shouldReturnNtlmSchemePrefix() {
        // Given
        ZapNtlmAuthHandler handler =
                new ZapNtlmAuthHandler(USERNAME, PASSWORD, DOMAIN, WORKSTATION);
        // When
        String schemeName = handler.getSchemeName();
        // Then
        assertThat(schemeName).isEqualToIgnoringCase("ntlm");
    }

    @Test
    void shouldHandleUsernameWithDomainPrefix() throws Exception {
        // Given - username in DOMAIN backslash user format
        ZapNtlmAuthHandler handler =
                new ZapNtlmAuthHandler(DOMAIN + "\\" + USERNAME, PASSWORD, null, WORKSTATION);
        // When
        String type1 = handler.generateType1Msg();
        // Then
        assertThat(type1).isNotNull();
        assertThat(type1).isNotEmpty();
        byte[] decoded = Base64.getDecoder().decode(type1);
        assertThat(decoded)
                .startsWith(
                        (byte) 'N',
                        (byte) 'T',
                        (byte) 'L',
                        (byte) 'M',
                        (byte) 'S',
                        (byte) 'S',
                        (byte) 'P',
                        (byte) 0x00);
    }

    @Test
    void shouldHandleNullDomainAndWorkstation() throws Exception {
        // Given
        ZapNtlmAuthHandler handler = new ZapNtlmAuthHandler(USERNAME, PASSWORD, null, null);
        // When
        String type1 = handler.generateType1Msg();
        // Then
        assertThat(type1).isNotNull();
        assertThat(type1).isNotEmpty();
    }

    @Test
    void shouldGenerateType3MessageWithNullDomain() throws Exception {
        // Given
        ZapNtlmAuthHandler handler = new ZapNtlmAuthHandler(USERNAME, PASSWORD, null, WORKSTATION);
        String type2Challenge = buildType2Message();
        // When
        String type3 = handler.generateType3Msg(type2Challenge);
        // Then
        assertThat(type3).isNotNull();
        assertThat(type3).isNotEmpty();
        byte[] decoded = Base64.getDecoder().decode(type3);
        assertThat(decoded)
                .startsWith(
                        (byte) 'N',
                        (byte) 'T',
                        (byte) 'L',
                        (byte) 'M',
                        (byte) 'S',
                        (byte) 'S',
                        (byte) 'P',
                        (byte) 0x00);
        assertThat(decoded[8]).isEqualTo((byte) 0x03);
    }

    @Test
    void shouldBeConnectionBased() {
        // Given
        ZapNtlmAuthHandler handler =
                new ZapNtlmAuthHandler(USERNAME, PASSWORD, DOMAIN, WORKSTATION);
        // When / Then
        assertThat(handler.isConnectionBased()).isTrue();
    }

    /**
     * Builds a minimal valid NTLM Type 2 (challenge) message.
     *
     * <p>Structure:
     *
     * <ul>
     *   <li>Bytes 0-7: Signature "NTLMSSP\0"
     *   <li>Bytes 8-11: Message type (2)
     *   <li>Bytes 12-19: Target name security buffer
     *   <li>Bytes 20-23: Negotiate flags
     *   <li>Bytes 24-31: Server challenge (8 bytes)
     *   <li>Bytes 32-39: Reserved (8 bytes of zeros)
     *   <li>Bytes 40-47: Target info security buffer
     * </ul>
     */
    private static String buildType2Message() {
        // Build a minimal Type 2 message
        byte[] signature = {'N', 'T', 'L', 'M', 'S', 'S', 'P', 0x00};
        // Message type 2
        byte[] msgType = {0x02, 0x00, 0x00, 0x00};
        // Target name: empty (length=0, maxLength=0, offset=48)
        byte[] targetNameSecBuf = {
            0x00,
            0x00, // length
            0x00,
            0x00, // max length
            0x30,
            0x00,
            0x00,
            0x00 // offset = 48
        };
        // Flags: NTLM v1 | Unicode | Target | NTLMv2 session
        int flags = 0x00000001 | 0x00000004 | 0x00000200 | 0x00080000;
        byte[] negotiateFlags = {
            (byte) (flags & 0xff),
            (byte) ((flags >> 8) & 0xff),
            (byte) ((flags >> 16) & 0xff),
            (byte) ((flags >> 24) & 0xff)
        };
        // Server challenge (8 bytes)
        byte[] challenge = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        // Reserved (8 bytes)
        byte[] reserved = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        // Target info: empty (length=0, maxLength=0, offset=48)
        byte[] targetInfoSecBuf = {
            0x00,
            0x00, // length
            0x00,
            0x00, // max length
            0x30,
            0x00,
            0x00,
            0x00 // offset = 48
        };

        // Total: 8 + 4 + 8 + 4 + 8 + 8 + 8 = 48 bytes header
        byte[] msg = new byte[48];
        int pos = 0;
        System.arraycopy(signature, 0, msg, pos, signature.length);
        pos += signature.length;
        System.arraycopy(msgType, 0, msg, pos, msgType.length);
        pos += msgType.length;
        System.arraycopy(targetNameSecBuf, 0, msg, pos, targetNameSecBuf.length);
        pos += targetNameSecBuf.length;
        System.arraycopy(negotiateFlags, 0, msg, pos, negotiateFlags.length);
        pos += negotiateFlags.length;
        System.arraycopy(challenge, 0, msg, pos, challenge.length);
        pos += challenge.length;
        System.arraycopy(reserved, 0, msg, pos, reserved.length);
        pos += reserved.length;
        System.arraycopy(targetInfoSecBuf, 0, msg, pos, targetInfoSecBuf.length);

        return Base64.getEncoder().encodeToString(msg);
    }
}
