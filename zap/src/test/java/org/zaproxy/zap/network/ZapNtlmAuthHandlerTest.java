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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zaproxy.zap.authentication.UsernamePasswordAuthenticationCredentials;

/**
 * Unit tests for {@link ZapNtlmAuthHandler}, specifying the NTLM challenge-response handshake
 * behavior that replaces Commons HttpClient's {@code NTCredentials} / {@code NTLMScheme}.
 *
 * <p>The NTLM handshake consists of three messages:
 *
 * <ol>
 *   <li>Type 1 – Negotiate: client announces capabilities (no credentials yet).
 *   <li>Type 2 – Challenge: server sends a nonce the client must sign.
 *   <li>Type 3 – Authenticate: client sends signed response using the user's password.
 * </ol>
 *
 * <p>These tests will <em>fail to compile</em> until {@code ZapNtlmAuthHandler} is created in
 * {@code org.zaproxy.zap.network}.
 */
class ZapNtlmAuthHandlerTest {

    // -----------------------------------------------------------------------
    // A minimal, well-formed Type 2 (Challenge) message encoded in Base64.
    //
    // Layout (little-endian):
    //   Bytes  0-7  : "NTLMSSP\0"          (signature)
    //   Bytes  8-11 : 0x00000002           (message type = 2)
    //   Bytes 12-19 : target security buffer (length=0, max=0, offset=56)
    //   Bytes 20-23 : flags = 0x00000201   (NTLM + Unicode)
    //   Bytes 24-31 : server challenge (8 bytes of 0x01..0x08)
    //   Bytes 32-39 : reserved (zeros)
    //   Bytes 40-47 : target-info security buffer (length=0, max=0, offset=56)
    //   Bytes 48-55 : version (ignored)
    // -----------------------------------------------------------------------
    private static final byte[] TYPE2_CHALLENGE_BYTES = buildType2Message();

    private static byte[] buildType2Message() {
        // 56-byte minimal Type 2 message
        byte[] msg = new byte[56];
        // Signature "NTLMSSP\0"
        byte[] sig = {0x4e, 0x54, 0x4c, 0x4d, 0x53, 0x53, 0x50, 0x00};
        System.arraycopy(sig, 0, msg, 0, 8);
        // Message type = 2 (little-endian)
        msg[8] = 0x02;
        msg[9] = 0x00;
        msg[10] = 0x00;
        msg[11] = 0x00;
        // Target security buffer: length=0, max=0, offset=56
        msg[12] = 0x00;
        msg[13] = 0x00;
        msg[14] = 0x00;
        msg[15] = 0x00;
        msg[16] = 0x38; // offset = 56 = 0x38
        msg[17] = 0x00;
        msg[18] = 0x00;
        msg[19] = 0x00;
        // Flags: NTLMSSP_NEGOTIATE_UNICODE (0x01) | NTLMSSP_NEGOTIATE_NTLM (0x200)
        msg[20] = 0x01;
        msg[21] = 0x02;
        msg[22] = 0x00;
        msg[23] = 0x00;
        // Server challenge: 8 bytes
        msg[24] = 0x01;
        msg[25] = 0x02;
        msg[26] = 0x03;
        msg[27] = 0x04;
        msg[28] = 0x05;
        msg[29] = 0x06;
        msg[30] = 0x07;
        msg[31] = 0x08;
        // Reserved: 8 bytes of zeros (already zero)
        // Target-info security buffer: length=0, max=0, offset=56
        msg[40] = 0x00;
        msg[41] = 0x00;
        msg[42] = 0x00;
        msg[43] = 0x00;
        msg[44] = 0x38;
        msg[45] = 0x00;
        msg[46] = 0x00;
        msg[47] = 0x00;
        // Version (bytes 48-55): zeros
        return msg;
    }

    private static final String TYPE2_CHALLENGE_B64 =
            Base64.getEncoder().encodeToString(TYPE2_CHALLENGE_BYTES);

    private ZapNtlmAuthHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ZapNtlmAuthHandler();
    }

    // -----------------------------------------------------------------------
    // Type 1 – Negotiate message construction
    // -----------------------------------------------------------------------

    @Test
    void type1NegotiateMessage_shouldNotBeNull() throws Exception {
        // Given / When
        String type1 = handler.generateType1NegotiateMessage();

        // Then
        assertNotNull(type1, "Type 1 negotiate message must not be null");
    }

    @Test
    void type1NegotiateMessage_shouldBeValidBase64() throws Exception {
        // Given / When
        String type1 = handler.generateType1NegotiateMessage();

        // Then – must be decodable Base64
        byte[] decoded = Base64.getDecoder().decode(type1);
        assertThat(decoded).isNotEmpty();
    }

    @Test
    void type1NegotiateMessage_shouldStartWithNtlmsspSignature() throws Exception {
        // Given / When
        String type1 = handler.generateType1NegotiateMessage();

        // Then – decoded bytes must begin with "NTLMSSP\0"
        byte[] decoded = Base64.getDecoder().decode(type1);
        byte[] expectedSig = {0x4e, 0x54, 0x4c, 0x4d, 0x53, 0x53, 0x50, 0x00};
        assertThat(decoded).startsWith(expectedSig);
    }

    @Test
    void type1NegotiateMessage_shouldIndicateMessageType1() throws Exception {
        // Given / When
        String type1 = handler.generateType1NegotiateMessage();

        // Then – bytes 8-11 (little-endian) must equal 1
        byte[] decoded = Base64.getDecoder().decode(type1);
        int msgType =
                (decoded[8] & 0xff)
                        | ((decoded[9] & 0xff) << 8)
                        | ((decoded[10] & 0xff) << 16)
                        | ((decoded[11] & 0xff) << 24);
        assertThat(msgType).isEqualTo(1);
    }

    @Test
    void type1NegotiateMessage_shouldBeDeterministicAcrossCallsOnSameHandler() throws Exception {
        // Given / When
        String first = handler.generateType1NegotiateMessage();
        String second = handler.generateType1NegotiateMessage();

        // Then – the negotiate message is stateless / constant
        assertThat(first).isEqualTo(second);
    }

    // -----------------------------------------------------------------------
    // Type 2 – Challenge parsing
    // -----------------------------------------------------------------------

    @Test
    void parseType2Challenge_shouldExtractNonNullChallenge() throws Exception {
        // Given / When
        byte[] challenge = handler.parseType2ChallengeBytes(TYPE2_CHALLENGE_B64);

        // Then
        assertNotNull(challenge, "Parsed challenge bytes must not be null");
    }

    @Test
    void parseType2Challenge_shouldExtractExactly8Bytes() throws Exception {
        // Given / When
        byte[] challenge = handler.parseType2ChallengeBytes(TYPE2_CHALLENGE_B64);

        // Then
        assertThat(challenge).hasSize(8);
    }

    @Test
    void parseType2Challenge_shouldExtractCorrectChallengeBytes() throws Exception {
        // Given / When
        byte[] challenge = handler.parseType2ChallengeBytes(TYPE2_CHALLENGE_B64);

        // Then – the challenge embedded in the test message is 0x01..0x08
        assertThat(challenge)
                .containsExactly(
                        (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                        (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08);
    }

    @Test
    void parseType2Challenge_shouldThrowOnInvalidBase64() {
        // Given
        String invalidBase64 = "not-valid-base64!!!";

        // When / Then
        assertThatThrownBy(() -> handler.parseType2ChallengeBytes(invalidBase64))
                .isInstanceOf(Exception.class);
    }

    @Test
    void parseType2Challenge_shouldThrowOnNonNtlmPayload() {
        // Given – a valid Base64 string that is NOT an NTLM message
        String notNtlm = Base64.getEncoder().encodeToString("Hello, World!".getBytes());

        // When / Then
        assertThatThrownBy(() -> handler.parseType2ChallengeBytes(notNtlm))
                .isInstanceOf(Exception.class);
    }

    // -----------------------------------------------------------------------
    // Type 3 – Authenticate message construction
    // -----------------------------------------------------------------------

    @Test
    void type3AuthenticateMessage_shouldNotBeNull() throws Exception {
        // Given
        String username = "testuser";
        String password = "testpass";
        String domain = "TESTDOMAIN";
        String workstation = "TESTHOST";

        // When
        String type3 =
                handler.generateType3AuthenticateMessage(
                        username, password, domain, workstation, TYPE2_CHALLENGE_B64);

        // Then
        assertNotNull(type3, "Type 3 authenticate message must not be null");
    }

    @Test
    void type3AuthenticateMessage_shouldBeValidBase64() throws Exception {
        // Given
        String username = "testuser";
        String password = "testpass";
        String domain = "TESTDOMAIN";
        String workstation = "TESTHOST";

        // When
        String type3 =
                handler.generateType3AuthenticateMessage(
                        username, password, domain, workstation, TYPE2_CHALLENGE_B64);

        // Then
        byte[] decoded = Base64.getDecoder().decode(type3);
        assertThat(decoded).isNotEmpty();
    }

    @Test
    void type3AuthenticateMessage_shouldStartWithNtlmsspSignature() throws Exception {
        // Given
        String username = "testuser";
        String password = "testpass";
        String domain = "TESTDOMAIN";
        String workstation = "TESTHOST";

        // When
        String type3 =
                handler.generateType3AuthenticateMessage(
                        username, password, domain, workstation, TYPE2_CHALLENGE_B64);

        // Then
        byte[] decoded = Base64.getDecoder().decode(type3);
        byte[] expectedSig = {0x4e, 0x54, 0x4c, 0x4d, 0x53, 0x53, 0x50, 0x00};
        assertThat(decoded).startsWith(expectedSig);
    }

    @Test
    void type3AuthenticateMessage_shouldIndicateMessageType3() throws Exception {
        // Given
        String username = "testuser";
        String password = "testpass";
        String domain = "TESTDOMAIN";
        String workstation = "TESTHOST";

        // When
        String type3 =
                handler.generateType3AuthenticateMessage(
                        username, password, domain, workstation, TYPE2_CHALLENGE_B64);

        // Then – bytes 8-11 (little-endian) must equal 3
        byte[] decoded = Base64.getDecoder().decode(type3);
        int msgType =
                (decoded[8] & 0xff)
                        | ((decoded[9] & 0xff) << 8)
                        | ((decoded[10] & 0xff) << 16)
                        | ((decoded[11] & 0xff) << 24);
        assertThat(msgType).isEqualTo(3);
    }

    @Test
    void type3AuthenticateMessage_shouldDifferForDifferentPasswords() throws Exception {
        // Given
        String username = "testuser";
        String domain = "TESTDOMAIN";
        String workstation = "TESTHOST";

        // When
        String type3WithPass1 =
                handler.generateType3AuthenticateMessage(
                        username, "password1", domain, workstation, TYPE2_CHALLENGE_B64);
        String type3WithPass2 =
                handler.generateType3AuthenticateMessage(
                        username, "password2", domain, workstation, TYPE2_CHALLENGE_B64);

        // Then – different passwords must produce different Type 3 messages
        assertThat(type3WithPass1).isNotEqualTo(type3WithPass2);
    }

    @Test
    void type3AuthenticateMessage_shouldThrowOnNullChallenge() {
        // Given
        String username = "testuser";
        String password = "testpass";
        String domain = "TESTDOMAIN";
        String workstation = "TESTHOST";

        // When / Then
        assertThatThrownBy(
                        () ->
                                handler.generateType3AuthenticateMessage(
                                        username, password, domain, workstation, null))
                .isInstanceOf(Exception.class);
    }

    // -----------------------------------------------------------------------
    // Credential extraction from ZAP's AuthenticationCredentials model
    // -----------------------------------------------------------------------

    @Test
    void credentialExtraction_shouldExtractUsernameFromUsernamePasswordCredentials() {
        // Given
        UsernamePasswordAuthenticationCredentials credentials =
                new UsernamePasswordAuthenticationCredentials("jdoe", "secret");

        // When
        String username = handler.extractUsername(credentials);

        // Then
        assertThat(username).isEqualTo("jdoe");
    }

    @Test
    void credentialExtraction_shouldExtractPasswordFromUsernamePasswordCredentials() {
        // Given
        UsernamePasswordAuthenticationCredentials credentials =
                new UsernamePasswordAuthenticationCredentials("jdoe", "secret");

        // When
        String password = handler.extractPassword(credentials);

        // Then
        assertThat(password).isEqualTo("secret");
    }

    @Test
    void credentialExtraction_shouldReturnNullUsernameWhenCredentialsNotConfigured() {
        // Given – credentials with null username (not configured)
        UsernamePasswordAuthenticationCredentials credentials =
                new UsernamePasswordAuthenticationCredentials();

        // When
        String username = handler.extractUsername(credentials);

        // Then
        assertThat(username).isNull();
    }

    @Test
    void credentialExtraction_shouldReturnNullPasswordWhenCredentialsNotConfigured() {
        // Given – credentials with null password (not configured)
        UsernamePasswordAuthenticationCredentials credentials =
                new UsernamePasswordAuthenticationCredentials();

        // When
        String password = handler.extractPassword(credentials);

        // Then
        assertThat(password).isNull();
    }

    @Test
    void credentialExtraction_shouldIndicateConfiguredWhenBothFieldsPresent() {
        // Given
        UsernamePasswordAuthenticationCredentials credentials =
                new UsernamePasswordAuthenticationCredentials("user", "pass");

        // When / Then
        assertTrue(credentials.isConfigured());
    }

    // -----------------------------------------------------------------------
    // Full handshake integration: Type1 → Type2 → Type3
    // -----------------------------------------------------------------------

    @Test
    void fullHandshake_type3ShouldBeProducedAfterType1AndType2() throws Exception {
        // Given
        String username = "alice";
        String password = "wonderland";
        String domain = "EXAMPLE";
        String workstation = "LAPTOP";

        // Step 1: generate Type 1
        String type1 = handler.generateType1NegotiateMessage();
        assertNotNull(type1);

        // Step 2: server sends Type 2 (we use our pre-built test message)
        String type2 = TYPE2_CHALLENGE_B64;

        // Step 3: generate Type 3
        String type3 =
                handler.generateType3AuthenticateMessage(username, password, domain, workstation, type2);

        // Then
        assertNotNull(type3);
        byte[] decoded = Base64.getDecoder().decode(type3);
        // Must be a valid NTLM message (starts with signature)
        byte[] expectedSig = {0x4e, 0x54, 0x4c, 0x4d, 0x53, 0x53, 0x50, 0x00};
        assertThat(decoded).startsWith(expectedSig);
        // Must be message type 3
        int msgType =
                (decoded[8] & 0xff)
                        | ((decoded[9] & 0xff) << 8)
                        | ((decoded[10] & 0xff) << 16)
                        | ((decoded[11] & 0xff) << 24);
        assertThat(msgType).isEqualTo(3);
    }

    @Test
    void fullHandshake_credentialsShouldFlowThroughHandler() throws Exception {
        // Given
        UsernamePasswordAuthenticationCredentials credentials =
                new UsernamePasswordAuthenticationCredentials("bob", "builder");
        String domain = "CORP";
        String workstation = "WORKSTATION1";

        // When
        String username = handler.extractUsername(credentials);
        String password = handler.extractPassword(credentials);
        String type3 =
                handler.generateType3AuthenticateMessage(
                        username, password, domain, workstation, TYPE2_CHALLENGE_B64);

        // Then
        assertNotNull(type3);
        assertThat(Base64.getDecoder().decode(type3)).isNotEmpty();
    }
}
