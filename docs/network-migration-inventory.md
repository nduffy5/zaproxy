# Commons HttpClient Migration Inventory — `org.zaproxy.zap.network`

## Purpose

This document is the authoritative inventory of every class, method, and import
in `org.zaproxy.zap.network` that references `org.apache.commons.httpclient.*`.
It drives the scope of every subsequent migration step and prevents regressions
from missed call sites.

**Scope:** `zap/src/main/java/org/zaproxy/zap/network/`  
**Out of scope:** `org.zaproxy.zap.extension.*`, `org.zaproxy.zap.view.*`
(those packages are tracked separately and are not part of this migration step).

**Produced by:**
```
grep -rn 'org.apache.commons.httpclient' \
    zap/src/main/java/org/zaproxy/zap/network/
```

---

## Scope Boundary Verification

Running the same grep against the extension and view packages confirms they are
**out of scope** for this migration step:

```
grep -rn 'org.apache.commons.httpclient' \
    zap/src/main/java/org/zaproxy/zap/extension/
grep -rn 'org.apache.commons.httpclient' \
    zap/src/main/java/org/zaproxy/zap/view/
```

Those directories contain references (e.g. `URI`, `URIException`, `Cookie`) but
they are **not** addressed in this step. The inventory below covers only
`org.zaproxy.zap.network`.

---

## Affected Classes

### 1. `DefaultHttpRedirectionValidator`

**File:** `DefaultHttpRedirectionValidator.java`  
**Status:** Active (not deprecated)

| Line | Reference | Commons HttpClient Type |
|------|-----------|------------------------|
| 22 | `import org.apache.commons.httpclient.URI;` | `org.apache.commons.httpclient.URI` |
| 40 | `public boolean isValid(URI redirection)` | `org.apache.commons.httpclient.URI` (parameter type) |

**Summary:** Implements `HttpRedirectionValidator`. The single method `isValid(URI)`
accepts a `org.apache.commons.httpclient.URI` argument. The class is the default
singleton that accepts all redirections.

---

### 2. `HttpRedirectionValidator`

**File:** `HttpRedirectionValidator.java`  
**Status:** Active (functional interface, not deprecated)

| Line | Reference | Commons HttpClient Type |
|------|-----------|------------------------|
| 22 | `import org.apache.commons.httpclient.URI;` | `org.apache.commons.httpclient.URI` |
| 37 | `boolean isValid(URI redirection);` | `org.apache.commons.httpclient.URI` (parameter type) |

**Summary:** Public `@FunctionalInterface` that forms part of the public API.
The single abstract method `isValid(URI)` uses `org.apache.commons.httpclient.URI`
as its parameter type. This is the **primary API surface** that must be preserved
or migrated with a binary-compatible replacement.

---

### 3. `ZapCookieSpec`

**File:** `ZapCookieSpec.java`  
**Status:** `@Deprecated` since 2.12.0

| Line | Reference | Commons HttpClient Type |
|------|-----------|------------------------|
| 22 | `import org.apache.commons.httpclient.Cookie;` | `org.apache.commons.httpclient.Cookie` |
| 23 | `import org.apache.commons.httpclient.cookie.CookieSpecBase;` | `org.apache.commons.httpclient.cookie.CookieSpecBase` |
| 24 | `import org.apache.commons.httpclient.cookie.MalformedCookieException;` | `org.apache.commons.httpclient.cookie.MalformedCookieException` |
| 33 | `public class ZapCookieSpec extends CookieSpecBase` | Extends `CookieSpecBase` |
| 36 | `public void validate(... Cookie cookie) throws MalformedCookieException` | `Cookie`, `MalformedCookieException` |

**Summary:** Extends `org.apache.commons.httpclient.cookie.CookieSpecBase` and
overrides `validate(String, int, String, boolean, Cookie)` to skip path
validation. Directly coupled to three Commons HttpClient types.

---

### 4. `ZapDeleteMethod`

**File:** `ZapDeleteMethod.java`  
**Status:** `@Deprecated` since 2.12.0

| Line | Reference | Commons HttpClient Type |
|------|-----------|------------------------|
| 23 | `import org.apache.commons.httpclient.Header;` | `org.apache.commons.httpclient.Header` |
| 24 | `import org.apache.commons.httpclient.HttpState;` | `org.apache.commons.httpclient.HttpState` |
| 25 | `import org.apache.commons.httpclient.methods.DeleteMethod;` | `org.apache.commons.httpclient.methods.DeleteMethod` |
| 26 | `import org.apache.commons.httpclient.methods.EntityEnclosingMethod;` | `org.apache.commons.httpclient.methods.EntityEnclosingMethod` |
| 33 | `public class ZapDeleteMethod extends EntityEnclosingMethod` | Extends `EntityEnclosingMethod` |
| 68 | `protected void readResponseHeaders(HttpState state, org.apache.commons.httpclient.HttpConnection conn)` | `HttpState`, `HttpConnection` (fully-qualified inline) |

**Summary:** Extends `EntityEnclosingMethod` and overrides `readResponseHeaders`
to use `ZapHttpParser` for lenient header parsing. Uses `Header[]`, `HttpState`,
and `HttpConnection` from Commons HttpClient.

---

### 5. `ZapHeadMethod`

**File:** `ZapHeadMethod.java`  
**Status:** `@Deprecated` since 2.12.0

| Line | Reference | Commons HttpClient Type |
|------|-----------|------------------------|
| 23 | `import org.apache.commons.httpclient.Header;` | `org.apache.commons.httpclient.Header` |
| 24 | `import org.apache.commons.httpclient.HttpState;` | `org.apache.commons.httpclient.HttpState` |
| 25 | `import org.apache.commons.httpclient.ProtocolException;` | `org.apache.commons.httpclient.ProtocolException` |
| 26 | `import org.apache.commons.httpclient.methods.EntityEnclosingMethod;` | `org.apache.commons.httpclient.methods.EntityEnclosingMethod` |
| 27 | `import org.apache.commons.httpclient.methods.HeadMethod;` | `org.apache.commons.httpclient.methods.HeadMethod` |
| 28 | `import org.apache.commons.httpclient.params.HttpMethodParams;` | `org.apache.commons.httpclient.params.HttpMethodParams` |
| 72 | `protected void readResponseBody(HttpState state, org.apache.commons.httpclient.HttpConnection conn)` | `HttpState`, `HttpConnection` (fully-qualified inline) |
| 119 | `protected void readResponseHeaders(HttpState state, org.apache.commons.httpclient.HttpConnection conn)` | `HttpState`, `HttpConnection` (fully-qualified inline) |

**Summary:** Extends `EntityEnclosingMethod` and overrides both `readResponseBody`
(copied from `HeadMethod`) and `readResponseHeaders` (lenient parsing via
`ZapHttpParser`). Uses six distinct Commons HttpClient types.

---

### 6. `ZapHttpParser`

**File:** `ZapHttpParser.java`  
**Status:** `@Deprecated` since 2.12.0

| Line | Reference | Commons HttpClient Type |
|------|-----------|------------------------|
| 25 | `import org.apache.commons.httpclient.Header;` | `org.apache.commons.httpclient.Header` |
| 26 | `import org.apache.commons.httpclient.HttpParser;` | `org.apache.commons.httpclient.HttpParser` |
| 52 | `public static Header[] parseHeaders(InputStream is, String charset)` | `Header[]` return type |
| 57 | `String line = HttpParser.readLine(is, charset);` | `HttpParser.readLine()` static call |
| 72 | `headers.add(new Header(name, value.toString()));` | `Header` constructor |
| 82 | `headers.add(new Header(name, value.toString()));` | `Header` constructor |
| 86 | `return (Header[]) headers.toArray(new Header[headers.size()]);` | `Header[]` cast |

**Summary:** Utility class that wraps `HttpParser.readLine()` and constructs
`Header` objects. This is the central lenient-parsing helper used by all the
`Zap*Method` classes. Depends on `Header` and `HttpParser`.

---

### 7. `ZapNTLMEngineImpl`

**File:** `ZapNTLMEngineImpl.java`  
**Status:** `@Deprecated` since 2.12.0 (package-private class)

| Line | Reference | Commons HttpClient Type |
|------|-----------|------------------------|
| 44 | `import org.apache.commons.httpclient.auth.AuthenticationException;` | `org.apache.commons.httpclient.auth.AuthenticationException` |
| 58 | Javadoc comment: `org.apache.commons.httpclient.auth.AuthenticationException` | (documentation only) |
| Multiple | `throws AuthenticationException` in method signatures | `org.apache.commons.httpclient.auth.AuthenticationException` |
| Multiple | `throw new AuthenticationException(...)` | `org.apache.commons.httpclient.auth.AuthenticationException` |

**Key methods that throw `AuthenticationException`:**
- `getType3Message(String, String, String, String, byte[], int, String, byte[])` 
- `getType3Message(String, String, String, String, byte[], int, String, byte[], Certificate, byte[], byte[])`
- `readULong(byte[], int)`
- `readUShort(byte[], int)`
- `readSecurityBuffer(byte[], int)`
- `makeRandomChallenge(Random)`
- `makeSecondaryKey(Random)`
- `CipherGen.getClientChallenge()`, `getClientChallenge2()`, `getSecondaryKey()`, `getLMHash()`, etc.
- `hmacMD5(byte[], byte[])`
- `RC4(byte[], byte[])`
- `ntlm2SessionResponse(byte[], byte[], byte[])`
- `lmHash(String)`, `ntlmHash(String)`, `lmv2Hash(...)`, `ntlmv2Hash(...)`
- `lmResponse(byte[], byte[])`, `lmv2Response(byte[], byte[], byte[])`
- `Handle` inner class methods

**Summary:** Copied from Apache HttpComponents Client and adapted to use
`org.apache.commons.httpclient.auth.AuthenticationException` instead of the
HttpComponents-native `NTLMEngineException`. This is the only Commons HttpClient
type used, but it appears throughout the entire class as the checked exception
for all cryptographic operations.

---

### 8. `ZapNTLMScheme`

**File:** `ZapNTLMScheme.java`  
**Status:** `@Deprecated` since 2.12.0

| Line | Reference | Commons HttpClient Type |
|------|-----------|------------------------|
| 29 | `import org.apache.commons.httpclient.Credentials;` | `org.apache.commons.httpclient.Credentials` |
| 30 | `import org.apache.commons.httpclient.HttpMethod;` | `org.apache.commons.httpclient.HttpMethod` |
| 31 | `import org.apache.commons.httpclient.NTCredentials;` | `org.apache.commons.httpclient.NTCredentials` |
| 32 | `import org.apache.commons.httpclient.auth.AuthChallengeParser;` | `org.apache.commons.httpclient.auth.AuthChallengeParser` |
| 33 | `import org.apache.commons.httpclient.auth.AuthScheme;` | `org.apache.commons.httpclient.auth.AuthScheme` |
| 34 | `import org.apache.commons.httpclient.auth.AuthenticationException;` | `org.apache.commons.httpclient.auth.AuthenticationException` |
| 35 | `import org.apache.commons.httpclient.auth.MalformedChallengeException;` | `org.apache.commons.httpclient.auth.MalformedChallengeException` |
| 48 | Javadoc: `org.apache.commons.httpclient.auth.AuthScheme` | (documentation only) |
| 55 | `public class ZapNTLMScheme implements AuthScheme` | Implements `AuthScheme` |
| 90 | `public void processChallenge(String challenge) throws MalformedChallengeException` | `MalformedChallengeException` |
| 91 | `String s = AuthChallengeParser.extractScheme(challenge);` | `AuthChallengeParser` static call |
| 113 | `public String authenticate(Credentials credentials, HttpMethod method) throws AuthenticationException` | `Credentials`, `HttpMethod`, `AuthenticationException` |
| 115 | `ntcredentials = (NTCredentials) credentials;` | `NTCredentials` cast |
| 152 | `public String authenticate(Credentials credentials, String method, String uri) throws AuthenticationException` | Deprecated `AuthScheme` method |

**Summary:** Implements `org.apache.commons.httpclient.auth.AuthScheme` directly.
Uses seven distinct Commons HttpClient types. This is the most heavily coupled
class in the package.

---

### 9. `ZapOptionsMethod`

**File:** `ZapOptionsMethod.java`  
**Status:** `@Deprecated` since 2.12.0

| Line | Reference | Commons HttpClient Type |
|------|-----------|------------------------|
| 23 | `import org.apache.commons.httpclient.Header;` | `org.apache.commons.httpclient.Header` |
| 24 | `import org.apache.commons.httpclient.HttpState;` | `org.apache.commons.httpclient.HttpState` |
| 25 | `import org.apache.commons.httpclient.methods.EntityEnclosingMethod;` | `org.apache.commons.httpclient.methods.EntityEnclosingMethod` |
| 26 | `import org.apache.commons.httpclient.methods.OptionsMethod;` | `org.apache.commons.httpclient.methods.OptionsMethod` |
| 33 | `public class ZapOptionsMethod extends EntityEnclosingMethod` | Extends `EntityEnclosingMethod` |
| 62 | `protected void readResponseHeaders(HttpState state, org.apache.commons.httpclient.HttpConnection conn)` | `HttpState`, `HttpConnection` (fully-qualified inline) |

**Summary:** Extends `EntityEnclosingMethod` and overrides `readResponseHeaders`
for lenient parsing. Same pattern as `ZapDeleteMethod`, `ZapPutMethod`, and
`ZapTraceMethod`.

---

### 10. `ZapPostMethod`

**File:** `ZapPostMethod.java`  
**Status:** `@Deprecated` since 2.12.0

| Line | Reference | Commons HttpClient Type |
|------|-----------|------------------------|
| 23 | `import org.apache.commons.httpclient.Header;` | `org.apache.commons.httpclient.Header` |
| 24 | `import org.apache.commons.httpclient.HttpState;` | `org.apache.commons.httpclient.HttpState` |
| 25 | `import org.apache.commons.httpclient.methods.PostMethod;` | `org.apache.commons.httpclient.methods.PostMethod` |
| 33 | `public class ZapPostMethod extends PostMethod` | Extends `PostMethod` (not `EntityEnclosingMethod`) |
| 56 | `protected void readResponseHeaders(HttpState state, org.apache.commons.httpclient.HttpConnection conn)` | `HttpState`, `HttpConnection` (fully-qualified inline) |

**Summary:** Extends `PostMethod` directly (unlike the other Zap*Method classes
which extend `EntityEnclosingMethod`). Overrides `readResponseHeaders` for
lenient parsing.

---

### 11. `ZapPutMethod`

**File:** `ZapPutMethod.java`  
**Status:** `@Deprecated` since 2.12.0

| Line | Reference | Commons HttpClient Type |
|------|-----------|------------------------|
| 23 | `import org.apache.commons.httpclient.Header;` | `org.apache.commons.httpclient.Header` |
| 24 | `import org.apache.commons.httpclient.HttpState;` | `org.apache.commons.httpclient.HttpState` |
| 25 | `import org.apache.commons.httpclient.methods.PutMethod;` | `org.apache.commons.httpclient.methods.PutMethod` |
| 33 | `public class ZapPutMethod extends PutMethod` | Extends `PutMethod` |
| 56 | `protected void readResponseHeaders(HttpState state, org.apache.commons.httpclient.HttpConnection conn)` | `HttpState`, `HttpConnection` (fully-qualified inline) |

**Summary:** Extends `PutMethod` and overrides `readResponseHeaders` for lenient
parsing. Same pattern as `ZapPostMethod`.

---

### 12. `ZapTraceMethod`

**File:** `ZapTraceMethod.java`  
**Status:** `@Deprecated` since 2.12.0

| Line | Reference | Commons HttpClient Type |
|------|-----------|------------------------|
| 23 | `import org.apache.commons.httpclient.Header;` | `org.apache.commons.httpclient.Header` |
| 24 | `import org.apache.commons.httpclient.HttpState;` | `org.apache.commons.httpclient.HttpState` |
| 25 | `import org.apache.commons.httpclient.methods.EntityEnclosingMethod;` | `org.apache.commons.httpclient.methods.EntityEnclosingMethod` |
| 26 | `import org.apache.commons.httpclient.methods.TraceMethod;` | `org.apache.commons.httpclient.methods.TraceMethod` |
| 33 | `public class ZapTraceMethod extends EntityEnclosingMethod` | Extends `EntityEnclosingMethod` |
| 58 | `protected void readResponseHeaders(HttpState state, org.apache.commons.httpclient.HttpConnection conn)` | `HttpState`, `HttpConnection` (fully-qualified inline) |

**Summary:** Extends `EntityEnclosingMethod` and overrides `readResponseHeaders`
for lenient parsing. Same pattern as `ZapDeleteMethod` and `ZapOptionsMethod`.

---

## Complete Type Reference Matrix

| Commons HttpClient Type | Classes That Use It |
|------------------------|---------------------|
| `org.apache.commons.httpclient.URI` | `DefaultHttpRedirectionValidator`, `HttpRedirectionValidator` |
| `org.apache.commons.httpclient.Cookie` | `ZapCookieSpec` |
| `org.apache.commons.httpclient.cookie.CookieSpecBase` | `ZapCookieSpec` (superclass) |
| `org.apache.commons.httpclient.cookie.MalformedCookieException` | `ZapCookieSpec` |
| `org.apache.commons.httpclient.Header` | `ZapDeleteMethod`, `ZapHeadMethod`, `ZapHttpParser`, `ZapOptionsMethod`, `ZapPostMethod`, `ZapPutMethod`, `ZapTraceMethod` |
| `org.apache.commons.httpclient.HttpState` | `ZapDeleteMethod`, `ZapHeadMethod`, `ZapOptionsMethod`, `ZapPostMethod`, `ZapPutMethod`, `ZapTraceMethod` |
| `org.apache.commons.httpclient.HttpConnection` | `ZapDeleteMethod`, `ZapHeadMethod`, `ZapOptionsMethod`, `ZapPostMethod`, `ZapPutMethod`, `ZapTraceMethod` |
| `org.apache.commons.httpclient.methods.DeleteMethod` | `ZapDeleteMethod` (Javadoc `@see`) |
| `org.apache.commons.httpclient.methods.EntityEnclosingMethod` | `ZapDeleteMethod`, `ZapHeadMethod`, `ZapOptionsMethod`, `ZapTraceMethod` (superclass) |
| `org.apache.commons.httpclient.methods.HeadMethod` | `ZapHeadMethod` (Javadoc `@see`) |
| `org.apache.commons.httpclient.methods.OptionsMethod` | `ZapOptionsMethod` (Javadoc `@see`) |
| `org.apache.commons.httpclient.methods.PostMethod` | `ZapPostMethod` (superclass) |
| `org.apache.commons.httpclient.methods.PutMethod` | `ZapPutMethod` (superclass) |
| `org.apache.commons.httpclient.methods.TraceMethod` | `ZapTraceMethod` (Javadoc `@see`) |
| `org.apache.commons.httpclient.HttpParser` | `ZapHttpParser` |
| `org.apache.commons.httpclient.ProtocolException` | `ZapHeadMethod` |
| `org.apache.commons.httpclient.params.HttpMethodParams` | `ZapHeadMethod` |
| `org.apache.commons.httpclient.auth.AuthenticationException` | `ZapNTLMEngineImpl`, `ZapNTLMScheme` |
| `org.apache.commons.httpclient.Credentials` | `ZapNTLMScheme` |
| `org.apache.commons.httpclient.HttpMethod` | `ZapNTLMScheme` |
| `org.apache.commons.httpclient.NTCredentials` | `ZapNTLMScheme` |
| `org.apache.commons.httpclient.auth.AuthChallengeParser` | `ZapNTLMScheme` |
| `org.apache.commons.httpclient.auth.AuthScheme` | `ZapNTLMScheme` (implemented interface) |
| `org.apache.commons.httpclient.auth.MalformedChallengeException` | `ZapNTLMScheme` |

---

## Classes with NO Commons HttpClient References

The following classes in `org.zaproxy.zap.network` have **zero** references to
`org.apache.commons.httpclient.*` and are therefore out of scope for this
migration:

- `AbstractStreamHttpEncoding`
- `DomainMatcher`
- `HttpEncoding`
- `HttpEncodingDeflate`
- `HttpEncodingGzip`
- `HttpRequestBody`
- `HttpRequestConfig`
- `HttpResponseBody`
- `HttpSenderContext`
- `HttpSenderListener`
- `SocksProxy`
- `ZapAuthenticator`
- `ZapProxySelector`

---

## `HttpSenderImpl` Interface — Migration Boundary Baseline

`HttpSenderImpl` is the boundary interface between the network module and its
callers. It has **zero** direct references to `org.apache.commons.httpclient.*`
and is therefore the stable API surface for japicmp comparison in subsequent
migration steps.

**Full public API signature** (`HttpSenderImpl<T extends HttpSenderContext>`):

```java
package org.zaproxy.zap.network;

public interface HttpSenderImpl<T extends HttpSenderContext> {

    boolean isGlobalStateEnabled();

    void addListener(HttpSenderListener listener);

    void removeListener(HttpSenderListener listener);

    T createContext(HttpSender parent, int initiator);

    default T getContext(HttpSender httpSender) { return null; }

    default void sendAndReceive(T ctx, HttpRequestConfig config,
                                HttpMessage msg, Path file)
            throws IOException {}

    default void sendAndReceive(HttpSender parent, HttpRequestConfig config,
                                HttpMessage msg, Path file)
            throws IOException {}

    default Object saveState() { return null; }

    default void restoreState(Object implState) {}
}
```

**Note:** `HttpSenderImpl` is marked `/** <strong>Note:</strong> Not part of the
public API. */` in its Javadoc, but it is the internal contract that any
replacement implementation must satisfy.

---

## Migration Priority

| Priority | Class | Reason |
|----------|-------|--------|
| **High** | `HttpRedirectionValidator` | Public `@FunctionalInterface`; `URI` parameter is part of the public API |
| **High** | `DefaultHttpRedirectionValidator` | Implements `HttpRedirectionValidator`; must change with it |
| **Medium** | `ZapNTLMScheme` | Implements `AuthScheme`; most Commons HttpClient types (7) |
| **Medium** | `ZapNTLMEngineImpl` | Package-private; `AuthenticationException` used throughout |
| **Medium** | `ZapHttpParser` | Central utility; used by all `Zap*Method` classes |
| **Low** | `ZapCookieSpec` | `@Deprecated` since 2.12.0; can be removed |
| **Low** | `ZapDeleteMethod` | `@Deprecated` since 2.12.0; can be removed |
| **Low** | `ZapHeadMethod` | `@Deprecated` since 2.12.0; can be removed |
| **Low** | `ZapOptionsMethod` | `@Deprecated` since 2.12.0; can be removed |
| **Low** | `ZapPostMethod` | `@Deprecated` since 2.12.0; can be removed |
| **Low** | `ZapPutMethod` | `@Deprecated` since 2.12.0; can be removed |
| **Low** | `ZapTraceMethod` | `@Deprecated` since 2.12.0; can be removed |

---

## Raw `grep` Output

The following is the verbatim output of:
```
grep -rn 'org.apache.commons.httpclient' \
    zap/src/main/java/org/zaproxy/zap/network/
```

```
DefaultHttpRedirectionValidator.java:22:import org.apache.commons.httpclient.URI;
HttpRedirectionValidator.java:22:import org.apache.commons.httpclient.URI;
ZapCookieSpec.java:22:import org.apache.commons.httpclient.Cookie;
ZapCookieSpec.java:23:import org.apache.commons.httpclient.cookie.CookieSpecBase;
ZapCookieSpec.java:24:import org.apache.commons.httpclient.cookie.MalformedCookieException;
ZapDeleteMethod.java:23:import org.apache.commons.httpclient.Header;
ZapDeleteMethod.java:24:import org.apache.commons.httpclient.HttpState;
ZapDeleteMethod.java:25:import org.apache.commons.httpclient.methods.DeleteMethod;
ZapDeleteMethod.java:26:import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
ZapDeleteMethod.java:68:            HttpState state, org.apache.commons.httpclient.HttpConnection conn) throws IOException {
ZapHeadMethod.java:23:import org.apache.commons.httpclient.Header;
ZapHeadMethod.java:24:import org.apache.commons.httpclient.HttpState;
ZapHeadMethod.java:25:import org.apache.commons.httpclient.ProtocolException;
ZapHeadMethod.java:26:import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
ZapHeadMethod.java:27:import org.apache.commons.httpclient.methods.HeadMethod;
ZapHeadMethod.java:28:import org.apache.commons.httpclient.params.HttpMethodParams;
ZapHeadMethod.java:72:            HttpState state, org.apache.commons.httpclient.HttpConnection conn) throws IOException {
ZapHeadMethod.java:119:            HttpState state, org.apache.commons.httpclient.HttpConnection conn) throws IOException {
ZapHttpParser.java:25:import org.apache.commons.httpclient.Header;
ZapHttpParser.java:26:import org.apache.commons.httpclient.HttpParser;
ZapNTLMEngineImpl.java:44:import org.apache.commons.httpclient.auth.AuthenticationException;
ZapNTLMEngineImpl.java:58: *    org.apache.commons.httpclient.auth.AuthenticationException;
ZapNTLMScheme.java:29:import org.apache.commons.httpclient.Credentials;
ZapNTLMScheme.java:30:import org.apache.commons.httpclient.HttpMethod;
ZapNTLMScheme.java:31:import org.apache.commons.httpclient.NTCredentials;
ZapNTLMScheme.java:32:import org.apache.commons.httpclient.auth.AuthChallengeParser;
ZapNTLMScheme.java:33:import org.apache.commons.httpclient.auth.AuthScheme;
ZapNTLMScheme.java:34:import org.apache.commons.httpclient.auth.AuthenticationException;
ZapNTLMScheme.java:35:import org.apache.commons.httpclient.auth.MalformedChallengeException;
ZapNTLMScheme.java:48: *  - Changed to implement org.apache.commons.httpclient.auth.AuthScheme (instead of extending
ZapOptionsMethod.java:23:import org.apache.commons.httpclient.Header;
ZapOptionsMethod.java:24:import org.apache.commons.httpclient.HttpState;
ZapOptionsMethod.java:25:import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
ZapOptionsMethod.java:26:import org.apache.commons.httpclient.methods.OptionsMethod;
ZapOptionsMethod.java:62:            HttpState state, org.apache.commons.httpclient.HttpConnection conn) throws IOException {
ZapPostMethod.java:23:import org.apache.commons.httpclient.Header;
ZapPostMethod.java:24:import org.apache.commons.httpclient.HttpState;
ZapPostMethod.java:25:import org.apache.commons.httpclient.methods.PostMethod;
ZapPostMethod.java:56:            HttpState state, org.apache.commons.httpclient.HttpConnection conn) throws IOException {
ZapPutMethod.java:23:import org.apache.commons.httpclient.Header;
ZapPutMethod.java:24:import org.apache.commons.httpclient.HttpState;
ZapPutMethod.java:25:import org.apache.commons.httpclient.methods.PutMethod;
ZapPutMethod.java:56:            HttpState state, org.apache.commons.httpclient.HttpConnection conn) throws IOException {
ZapTraceMethod.java:23:import org.apache.commons.httpclient.Header;
ZapTraceMethod.java:24:import org.apache.commons.httpclient.HttpState;
ZapTraceMethod.java:25:import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
ZapTraceMethod.java:26:import org.apache.commons.httpclient.methods.TraceMethod;
ZapTraceMethod.java:58:            HttpState state, org.apache.commons.httpclient.HttpConnection conn) throws IOException {
```
