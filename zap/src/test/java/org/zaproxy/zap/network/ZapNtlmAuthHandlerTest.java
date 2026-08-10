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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zaproxy.zap.authentication.UsernamePasswordAuthenticationCredentials;

/**
 * Unit tests for {@link ZapNtlmAuthHandler}.
 *
 * <p>Covers the three-way NTLM challenge-response handshake:
 *
 * <ol>
 *   <li>Type 1 (Negotiate) message construction
 *   <li>Type 2 (Challenge) message parsing
 *   <li>Type 3 (Authenticate) message construction
 *   <li>Credential extraction from ZAP's {@link UsernamePasswordAuthenticationCredentials} model
 * </ol>
 */
class ZapNtlmAuthHandlerTest {

    // A minimal, valid NTLM Type 2 (Challenge) message encoded in Base64.
    // Structure (little-endian):
    //   Signature:  "NTLMSSP" + NUL              (8 bytes)
    //   Type:       0x00000002                    (4 bytes)
    //   TargetName: length=0, maxLen=0, offset=56 (8 bytes)
    //   Flags:      0x00000201 (NTLMv1 + Unicode) (4 bytes)
    //   Challenge:  0x0102030405060708            (8 bytes)
    //   Reserved:   0x0000000000000000            (8 bytes)
    //   TargetInfo: length=0, maxLen=0, offset=56 (8 bytes)
    //   Version:    0x0000000000000000            (8 bytes)
    // Total: 56 bytes
    private static final byte[] TYPE2_CHALLENGE_BYTES = buildType2Message();

    private static final String TYPE2_CHALLENGE_BASE64 =
            Base64.getEncoder().encodeToString(TYPE2_CHALLENGE_BYTES);

    // A Type 2 challenge that also includes target-info (NTLMv2 path)
    private static final byte[] TYPE2_CHALLENGE_WITH_TARGET_INFO_BYTES =
            buildType2MessageWithTargetInfo();

    private static final String TYPE2_CHALLENGE_WITH_TARGET_INFO_BASE64 =
            Base64.getEncoder().encodeToString(TYPE2_CHALLENGE_WITH_TARGET_INFO_BYTES);

    private ZapNtlmAuthHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ZapNtlmAuthHandler();
    }

    // -------------------------------------------------------------------------
    // Type 1 - Negotiate message
    // -------------------------------------------------------------------------

    @Test
    void type1MessageShouldNotBeNull() {
        // Given / When
        String type1 = handler.generateType1Message("WORKGROUP", "WORKSTATION");

        // Then
        assertNotNull(type1);
    }

    @Test
    void type1MessageShouldBeBase64Encoded() {
        // Given / When
        String type1 = handler.generateType1Message("WORKGROUP", "WORKSTATION");

        // Then - must be decodable as Base64
        byte[] decoded = Base64.getDecoder().decode(type1);
        assertThat(decoded.length, is(not(equalTo(0))));
    }

    @Test
    void type1MessageShouldStartWithNtlmSignature() {
        // Given / When
        String type1 = handler.generateType1Message("WORKGROUP", "WORKSTATION");

        // Then - decoded bytes must begin with "NTLMSSP" followed by a NUL byte
        byte[] decoded = Base64.getDecoder().decode(type1);
        String signature = new String(decoded, 0, 7, StandardCharsets.US_ASCII);
        assertThat(signature, is(equalTo("NTLMSSP")));
        assertThat(decoded[7], is(equalTo((byte) 0x00)));
    }

    @Test
    void type1MessageShouldHaveMessageTypeOne() {
        // Given / When
        String type1 = handler.generateType1Message("WORKGROUP", "WORKSTATION");

        // Then - bytes 8-11 (little-endian UInt32) must equal 1
        byte[] decoded = Base64.getDecoder().decode(type1);
        int messageType = readUInt32LE(decoded, 8);
        assertThat(messageType, is(equalTo(1)));
    }

    @Test
    void type1MessageShouldBeConsistentAcrossCallsWithSameArguments() {
        // Given / When
        String first = handler.generateType1Message("DOMAIN", "HOST");
        String second = handler.generateType1Message("DOMAIN", "HOST");

        // Then - the negotiate message is deterministic (no random nonce in Type 1)
        assertThat(first, is(equalTo(second)));
    }

    @Test
    void type1MessageShouldBeNonEmptyForNullDomainAndHost() {
        // Given / When
        String type1 = handler.generateType1Message(null, null);

        // Then
        assertNotNull(type1);
        assertThat(type1.length(), is(not(equalTo(0))));
    }

    // -------------------------------------------------------------------------
    // Type 2 - Challenge parsing
    // -------------------------------------------------------------------------

    @Test
    void shouldParseType2ChallengeWithoutError() {
        // Given / When / Then - must not throw
        ZapNtlmAuthHandler.NtlmChallenge challenge =
                handler.parseType2Challenge(TYPE2_CHALLENGE_BASE64);
        assertNotNull(challenge);
    }

    @Test
    void shouldExtractServerChallengeFromType2Message() {
        // Given / When
        ZapNtlmAuthHandler.NtlmChallenge challenge =
                handler.parseType2Challenge(TYPE2_CHALLENGE_BASE64);

        // Then - the 8-byte server nonce must match what we embedded
        byte[] nonce = challenge.getServerChallenge();
        assertThat(nonce, is(notNullValue()));
        assertThat(nonce.length, is(equalTo(8)));
        assertThat(nonce[0], is(equalTo((byte) 0x01)));
        assertThat(nonce[1], is(equalTo((byte) 0x02)));
        assertThat(nonce[2], is(equalTo((byte) 0x03)));
        assertThat(nonce[3], is(equalTo((byte) 0x04)));
        assertThat(nonce[4], is(equalTo((byte) 0x05)));
        assertThat(nonce[5], is(equalTo((byte) 0x06)));
        assertThat(nonce[6], is(equalTo((byte) 0x07)));
        assertThat(nonce[7], is(equalTo((byte) 0x08)));
    }

    @Test
    void shouldExtractFlagsFromType2Message() {
        // Given / When
        ZapNtlmAuthHandler.NtlmChallenge challenge =
                handler.parseType2Challenge(TYPE2_CHALLENGE_BASE64);

        // Then - flags must be non-zero
        assertThat(challenge.getFlags(), is(not(equalTo(0))));
    }

    @Test
    void shouldThrowOnInvalidType2Message() {
        // Given
        String invalidBase64 = Base64.getEncoder().encodeToString("not-ntlm".getBytes());

        // When / Then
        assertThrows(
                IllegalArgumentException.class,
                () -> handler.parseType2Challenge(invalidBase64));
    }

    @Test
    void shouldThrowOnNullType2Message() {
        // When / Then
        assertThrows(
                IllegalArgumentException.class,
                () -> handler.parseType2Challenge(null));
    }

    @Test
    void shouldParseType2ChallengeWithTargetInfo() {
        // Given / When
        ZapNtlmAuthHandler.NtlmChallenge challenge =
                handler.parseType2Challenge(TYPE2_CHALLENGE_WITH_TARGET_INFO_BASE64);

        // Then
        assertNotNull(challenge);
        assertThat(challenge.getServerChallenge().length, is(equalTo(8)));
    }

    // -------------------------------------------------------------------------
    // Type 3 - Authenticate message
    // -------------------------------------------------------------------------

    @Test
    void type3MessageShouldNotBeNull() throws Exception {
        // Given
        ZapNtlmAuthHandler.NtlmChallenge challenge =
                handler.parseType2Challenge(TYPE2_CHALLENGE_BASE64);

        // When
        String type3 = handler.generateType3Message("user", "password", "DOMAIN", "HOST", challenge);

        // Then
        assertNotNull(type3);
    }

    @Test
    void type3MessageShouldBeBase64Encoded() throws Exception {
        // Given
        ZapNtlmAuthHandler.NtlmChallenge challenge =
                handler.parseType2Challenge(TYPE2_CHALLENGE_BASE64);

        // When
        String type3 = handler.generateType3Message("user", "password", "DOMAIN", "HOST", challenge);

        // Then
        byte[] decoded = Base64.getDecoder().decode(type3);
        assertThat(decoded.length, is(not(equalTo(0))));
    }

    @Test
    void type3MessageShouldStartWithNtlmSignature() throws Exception {
        // Given
        ZapNtlmAuthHandler.NtlmChallenge challenge =
                handler.parseType2Challenge(TYPE2_CHALLENGE_BASE64);

        // When
        String type3 = handler.generateType3Message("user", "password", "DOMAIN", "HOST", challenge);

        // Then
        byte[] decoded = Base64.getDecoder().decode(type3);
        String signature = new String(decoded, 0, 7, StandardCharsets.US_ASCII);
        assertThat(signature, is(equalTo("NTLMSSP")));
        assertThat(decoded[7], is(equalTo((byte) 0x00)));
    }

    @Test
    void type3MessageShouldHaveMessageTypeThree() throws Exception {
        // Given
        ZapNtlmAuthHandler.NtlmChallenge challenge =
                handler.parseType2Challenge(TYPE2_CHALLENGE_BASE64);

        // When
        String type3 = handler.generateType3Message("user", "password", "DOMAIN", "HOST", challenge);

        // Then - bytes 8-11 (little-endian UInt32) must equal 3
        byte[] decoded = Base64.getDecoder().decode(type3);
        int messageType = readUInt32LE(decoded, 8);
        assertThat(messageType, is(equalTo(3)));
    }

    @Test
    void type3MessageShouldDifferForDifferentPasswords() throws Exception {
        // Given
        ZapNtlmAuthHandler.NtlmChallenge challenge =
                handler.parseType2Challenge(TYPE2_CHALLENGE_BASE64);

        // When
        String type3WithPassword1 =
                handler.generateType3Message("user", "password1", "DOMAIN", "HOST", challenge);
        String type3WithPassword2 =
                handler.generateType3Message("user", "password2", "DOMAIN", "HOST", challenge);

        // Then - different passwords must produce different authenticate messages
        assertThat(type3WithPassword1, is(not(equalTo(type3WithPassword2))));
    }

    @Test
    void type3MessageShouldDifferForDifferentUsers() throws Exception {
        // Given
        ZapNtlmAuthHandler.NtlmChallenge challenge =
                handler.parseType2Challenge(TYPE2_CHALLENGE_BASE64);

        // When
        String type3User1 =
                handler.generateType3Message("alice", "password", "DOMAIN", "HOST", challenge);
        String type3User2 =
                handler.generateType3Message("bob", "password", "DOMAIN", "HOST", challenge);

        // Then
        assertThat(type3User1, is(not(equalTo(type3User2))));
    }

    @Test
    void fullHandshakeShouldProduceValidAuthorizationHeader() throws Exception {
        // Given
        handler.generateType1Message("DOMAIN", "HOST");
        ZapNtlmAuthHandler.NtlmChallenge challenge =
                handler.parseType2Challenge(TYPE2_CHALLENGE_BASE64);
        String type3 = handler.generateType3Message("user", "password", "DOMAIN", "HOST", challenge);

        // When
        String authorizationHeader = handler.buildAuthorizationHeader(type3);

        // Then
        assertThat(authorizationHeader, startsWith("NTLM "));
        assertThat(authorizationHeader.length(), is(not(equalTo("NTLM ".length()))));
    }

    // -------------------------------------------------------------------------
    // Credential extraction from ZAP's AuthenticationCredentials model
    // -------------------------------------------------------------------------

    @Test
    void shouldExtractUsernameFromCredentials() {
        // Given
        UsernamePasswordAuthenticationCredentials credentials =
                new UsernamePasswordAuthenticationCredentials("alice", "s3cr3t");

        // When
        String username = handler.extractUsername(credentials);

        // Then
        assertThat(username, is(equalTo("alice")));
    }

    @Test
    void shouldExtractPasswordFromCredentials() {
        // Given
        UsernamePasswordAuthenticationCredentials credentials =
                new UsernamePasswordAuthenticationCredentials("alice", "s3cr3t");

        // When
        String password = handler.extractPassword(credentials);

        // Then
        assertThat(password, is(equalTo("s3cr3t")));
    }

    @Test
    void shouldExtractDomainFromUsernameWithBackslash() {
        // Given - Windows-style domain-prefixed username: CORP\alice
        String domainPrefixedUser = "CORP" + "\\" + "alice";
        UsernamePasswordAuthenticationCredentials credentials =
                new UsernamePasswordAuthenticationCredentials(domainPrefixedUser, "s3cr3t");

        // When
        String domain = handler.extractDomain(credentials);
        String username = handler.extractUsername(credentials);

        // Then
        assertThat(domain, is(equalTo("CORP")));
        assertThat(username, is(equalTo("alice")));
    }

    @Test
    void shouldReturnEmptyDomainWhenNoBackslashInUsername() {
        // Given
        UsernamePasswordAuthenticationCredentials credentials =
                new UsernamePasswordAuthenticationCredentials("alice", "s3cr3t");

        // When
        String domain = handler.extractDomain(credentials);

        // Then
        assertThat(domain, is(equalTo("")));
    }

    @Test
    void shouldHandleNullCredentials() {
        // When / Then
        assertThrows(
                IllegalArgumentException.class,
                () -> handler.extractUsername(null));
    }

    @Test
    void shouldHandleEmptyUsername() {
        // Given
        UsernamePasswordAuthenticationCredentials credentials =
                new UsernamePasswordAuthenticationCredentials("", "password");

        // When
        String username = handler.extractUsername(credentials);
        String domain = handler.extractDomain(credentials);

        // Then
        assertThat(username, is(equalTo("")));
        assertThat(domain, is(equalTo("")));
    }

    @Test
    void shouldPerformFullHandshakeUsingCredentials() throws Exception {
        // Given - domain-prefixed username: CORP\alice
        String domainPrefixedUser = "CORP" + "\\" + "alice";
        UsernamePasswordAuthenticationCredentials credentials =
                new UsernamePasswordAuthenticationCredentials(domainPrefixedUser, "s3cr3t");
        ZapNtlmAuthHandler.NtlmChallenge challenge =
                handler.parseType2Challenge(TYPE2_CHALLENGE_BASE64);

        // When
        String type3 = handler.generateType3MessageFromCredentials(credentials, "HOST", challenge);

        // Then
        assertNotNull(type3);
        byte[] decoded = Base64.getDecoder().decode(type3);
        // Must be a valid NTLM message
        String signature = new String(decoded, 0, 7, StandardCharsets.US_ASCII);
        assertThat(signature, is(equalTo("NTLMSSP")));
        int messageType = readUInt32LE(decoded, 8);
        assertThat(messageType, is(equalTo(3)));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Reads a 32-bit unsigned integer from {@code bytes} at {@code offset} in little-endian order.
     */
    private static int readUInt32LE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    /**
     * Builds a minimal NTLM Type 2 (Challenge) message.
     *
     * <p>Layout (all multi-byte values are little-endian):
     *
     * <pre>
     * Offset  Length  Field
     *  0       8      Signature "NTLMSSP" + NUL
     *  8       4      MessageType = 2
     * 12       8      TargetNameFields (len=0, maxLen=0, offset=56)
     * 20       4      NegotiateFlags = 0x00000201
     * 24       8      ServerChallenge = {0x01..0x08}
     * 32       8      Reserved = zeros
     * 40       8      TargetInfoFields (len=0, maxLen=0, offset=56)
     * 48       8      Version (zeros)
     * 56       0      (payload - empty)
     * </pre>
     */
    private static byte[] buildType2Message() {
        byte[] msg = new byte[56];
        // Signature: "NTLMSSP" + NUL
        byte[] sig = "NTLMSSP\0".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(sig, 0, msg, 0, 8);
        // MessageType = 2
        writeUInt32LE(msg, 8, 2);
        // TargetNameFields: len=0, maxLen=0, offset=56
        writeUInt16LE(msg, 12, 0);
        writeUInt16LE(msg, 14, 0);
        writeUInt32LE(msg, 16, 56);
        // NegotiateFlags = NTLMSSP_NEGOTIATE_UNICODE (0x01) | NTLMSSP_NEGOTIATE_NTLM (0x200)
        writeUInt32LE(msg, 20, 0x00000201);
        // ServerChallenge
        msg[24] = 0x01;
        msg[25] = 0x02;
        msg[26] = 0x03;
        msg[27] = 0x04;
        msg[28] = 0x05;
        msg[29] = 0x06;
        msg[30] = 0x07;
        msg[31] = 0x08;
        // Reserved (8 bytes of zeros) - already zero
        // TargetInfoFields: len=0, maxLen=0, offset=56
        writeUInt16LE(msg, 40, 0);
        writeUInt16LE(msg, 42, 0);
        writeUInt32LE(msg, 44, 56);
        // Version (8 bytes of zeros) - already zero
        return msg;
    }

    /**
     * Builds a minimal NTLM Type 2 message that includes a non-empty TargetInfo block so that the
     * NTLMv2 code path is exercised.
     */
    private static byte[] buildType2MessageWithTargetInfo() {
        // TargetInfo: a single MsvAvEOL entry (AvId=0, AvLen=0) = 4 bytes
        byte[] targetInfo = new byte[] {0x00, 0x00, 0x00, 0x00};
        // TargetName: "TEST" in UTF-16LE = 8 bytes
        byte[] targetName = "TEST".getBytes(Charset.forName("UTF-16LE"));

        int payloadOffset = 56;
        int totalLength = payloadOffset + targetName.length + targetInfo.length;
        byte[] msg = new byte[totalLength];

        // Signature: "NTLMSSP" + NUL
        byte[] sig = "NTLMSSP\0".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(sig, 0, msg, 0, 8);
        // MessageType = 2
        writeUInt32LE(msg, 8, 2);
        // TargetNameFields
        writeUInt16LE(msg, 12, targetName.length);
        writeUInt16LE(msg, 14, targetName.length);
        writeUInt32LE(msg, 16, payloadOffset);
        // NegotiateFlags: Unicode + NTLM + TargetInfo present (0x00800201)
        writeUInt32LE(msg, 20, 0x00800201);
        // ServerChallenge
        msg[24] = 0x01;
        msg[25] = 0x02;
        msg[26] = 0x03;
        msg[27] = 0x04;
        msg[28] = 0x05;
        msg[29] = 0x06;
        msg[30] = 0x07;
        msg[31] = 0x08;
        // Reserved (zeros)
        // TargetInfoFields
        int targetInfoOffset = payloadOffset + targetName.length;
        writeUInt16LE(msg, 40, targetInfo.length);
        writeUInt16LE(msg, 42, targetInfo.length);
        writeUInt32LE(msg, 44, targetInfoOffset);
        // Version (zeros)
        // Payload
        System.arraycopy(targetName, 0, msg, payloadOffset, targetName.length);
        System.arraycopy(targetInfo, 0, msg, targetInfoOffset, targetInfo.length);
        return msg;
    }

    private static void writeUInt32LE(byte[] buf, int offset, int value) {
        buf[offset]     = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
        buf[offset + 2] = (byte) ((value >> 16) & 0xFF);
        buf[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    private static void writeUInt16LE(byte[] buf, int offset, int value) {
        buf[offset]     = (byte) (value & 0xFF);
        buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }
}
