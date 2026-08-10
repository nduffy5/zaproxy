# network-migration-inventory

## Commons HttpClient Usage Inventory — `org.zaproxy.zap.network`

**Purpose:** Complete, authoritative list of every class, method, and import in
`org.zaproxy.zap.network` that references `org.apache.commons.httpclient.*`.
This inventory drives the scope of every subsequent migration step and prevents
regressions from missed call sites.

**Dependency being replaced:** `commons-httpclient-3.1.jar` (EOL, known TLS
vulnerabilities — see `LEGALNOTICE.md`).

**Scope:** `zap/src/main/java/org/zaproxy/zap/network/` only.  
References in `org.zaproxy.zap.extension.*` and `org.zaproxy.zap.view.*` are
explicitly **out of scope** for this migration step and are tracked separately.

---

## Grep Baseline

The following command reproduces the complete list of references:

```
grep -rn 'org.apache.commons.httpclient' \
  zap/src/main/java/org/zaproxy/zap/network/
```

Output (sorted by file, then line number):

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

---

## Affected Classes — Detail

### 1. `DefaultHttpRedirectionValidator`

**File:** `DefaultHttpRedirectionValidator.java`  
**Status:** `@since 2.6.0`

| Line | Commons HttpClient Type | Usage |
|------|------------------------|-------|
| 22 | `org.apache.commons.httpclient.URI` | Import; parameter type of `isValid(URI)` |

**Method signatures referencing commons-httpclient:**
```java
public boolean isValid(URI redirection)   // URI = org.apache.commons.httpclient.URI
```

---

### 2. `HttpRedirectionValidator`

**File:** `HttpRedirectionValidator.java`  
**Status:** `@FunctionalInterface`, `@since 2.6.0`

| Line | Commons HttpClient Type | Usage |
|------|------------------------|-------|
| 22 | `org.apache.commons.httpclient.URI` | Import; parameter type of `isValid(URI)` |

**Method signatures referencing commons-httpclient:**
```java
boolean isValid(URI redirection)   // URI = org.apache.commons.httpclient.URI
```

---

### 3. `ZapCookieSpec`

**File:** `ZapCookieSpec.java`  
**Status:** `@Deprecated (2.12.0)`

| Line | Commons HttpClient Type | Usage |
|------|------------------------|-------|
| 22 | `org.apache.commons.httpclient.Cookie` | Import; parameter type of `validate(...)` |
| 23 | `org.apache.commons.httpclient.cookie.CookieSpecBase` | Import; superclass (`extends CookieSpecBase`) |
| 24 | `org.apache.commons.httpclient.cookie.MalformedCookieException` | Import; checked exception thrown by `validate(...)` |

**Method signatures referencing commons-httpclient:**
```java
// extends org.apache.commons.httpclient.cookie.CookieSpecBase
public void validate(String host, int port, String path, boolean secure, Cookie cookie)
    throws MalformedCookieException
```

---

### 4. `ZapDeleteMethod`

**File:** `ZapDeleteMethod.java`  
**Status:** `@Deprecated (2.12.0)`

| Line | Commons HttpClient Type | Usage |
|------|------------------------|-------|
| 23 | `org.apache.commons.httpclient.Header` | Import; return type of `ZapHttpParser.parseHeaders(...)` |
| 24 | `org.apache.commons.httpclient.HttpState` | Import; parameter type of `readResponseHeaders(...)` |
| 25 | `org.apache.commons.httpclient.methods.DeleteMethod` | Import; Javadoc `@see` reference |
| 26 | `org.apache.commons.httpclient.methods.EntityEnclosingMethod` | Import; superclass (`extends EntityEnclosingMethod`) |
| 68 | `org.apache.commons.httpclient.HttpConnection` | Inline fully-qualified type in method signature |

**Method signatures referencing commons-httpclient:**
```java
// extends org.apache.commons.httpclient.methods.EntityEnclosingMethod
@Override
protected void readResponseHeaders(
    HttpState state,
    org.apache.commons.httpclient.HttpConnection conn) throws IOException
```

---

### 5. `ZapHeadMethod`

**File:** `ZapHeadMethod.java`  
**Status:** `@Deprecated (2.12.0)`

| Line | Commons HttpClient Type | Usage |
|------|------------------------|-------|
| 23 | `org.apache.commons.httpclient.Header` | Import; return type of `ZapHttpParser.parseHeaders(...)` |
| 24 | `org.apache.commons.httpclient.HttpState` | Import; parameter type of overridden methods |
| 25 | `org.apache.commons.httpclient.ProtocolException` | Import; thrown in `readResponseBody(...)` |
| 26 | `org.apache.commons.httpclient.methods.EntityEnclosingMethod` | Import; superclass (`extends EntityEnclosingMethod`) |
| 27 | `org.apache.commons.httpclient.methods.HeadMethod` | Import; Javadoc `@see` reference |
| 28 | `org.apache.commons.httpclient.params.HttpMethodParams` | Import; used to read `HEAD_BODY_CHECK_TIMEOUT` and `REJECT_HEAD_BODY` params |
| 72 | `org.apache.commons.httpclient.HttpConnection` | Inline fully-qualified type in `readResponseBody(...)` |
| 119 | `org.apache.commons.httpclient.HttpConnection` | Inline fully-qualified type in `readResponseHeaders(...)` |

**Method signatures referencing commons-httpclient:**
```java
// extends org.apache.commons.httpclient.methods.EntityEnclosingMethod
@Override
protected void readResponseBody(
    HttpState state,
    org.apache.commons.httpclient.HttpConnection conn) throws IOException

@Override
protected void readResponseHeaders(
    HttpState state,
    org.apache.commons.httpclient.HttpConnection conn) throws IOException
```

---

### 6. `ZapHttpParser`

**File:** `ZapHttpParser.java`  
**Status:** `@Deprecated (2.12.0)`

| Line | Commons HttpClient Type | Usage |
|------|------------------------|-------|
| 25 | `org.apache.commons.httpclient.Header` | Import; return type of `parseHeaders(...)` and used to construct header objects |
| 26 | `org.apache.commons.httpclient.HttpParser` | Import; `HttpParser.readLine(InputStream, String)` called inside `parseHeaders(...)` |

**Method signatures referencing commons-httpclient:**
```java
public static Header[] parseHeaders(InputStream is, String charset) throws IOException
//   ^^^^^^ org.apache.commons.httpclient.Header
```

**Internal call sites:**
```java
String line = HttpParser.readLine(is, charset);   // org.apache.commons.httpclient.HttpParser
headers.add(new Header(name, value.toString()));  // org.apache.commons.httpclient.Header
return (Header[]) headers.toArray(new Header[headers.size()]);
```

---

### 7. `ZapNTLMEngineImpl`

**File:** `ZapNTLMEngineImpl.java`  
**Status:** `@Deprecated (2.12.0)`, package-private (`final class`)

| Line | Commons HttpClient Type | Usage |
|------|------------------------|-------|
| 44 | `org.apache.commons.httpclient.auth.AuthenticationException` | Import; replaces `NTLMEngineException` throughout the class |
| 58 | `org.apache.commons.httpclient.auth.AuthenticationException` | Javadoc comment (adaptation note) |

**Method signatures referencing commons-httpclient:**
```java
static String getType3Message(...) throws AuthenticationException
static String getType3Message(..., Certificate, byte[], byte[]) throws AuthenticationException
// AuthenticationException = org.apache.commons.httpclient.auth.AuthenticationException

// Also used as the exception type in:
//   CipherGen.getClientChallenge()       throws AuthenticationException
//   CipherGen.getLMHash()                throws AuthenticationException
//   CipherGen.getNTLMHash()              throws AuthenticationException
//   CipherGen.getLMResponse()            throws AuthenticationException
//   CipherGen.getNTLMResponse()          throws AuthenticationException
//   CipherGen.getLMv2Hash()              throws AuthenticationException
//   CipherGen.getNTLMv2Hash()            throws AuthenticationException
//   CipherGen.getNTLMv2Blob()            throws AuthenticationException
//   CipherGen.getNTLMv2Response()        throws AuthenticationException
//   CipherGen.getLMv2Response()          throws AuthenticationException
//   CipherGen.getNTLM2SessionResponse()  throws AuthenticationException
//   CipherGen.getLM2SessionResponse()    throws AuthenticationException
//   CipherGen.getLMUserSessionKey()      throws AuthenticationException
//   CipherGen.getNTLMUserSessionKey()    throws AuthenticationException
//   CipherGen.getNTLMv2UserSessionKey()  throws AuthenticationException
//   CipherGen.getNTLM2SessionResponseUserSessionKey() throws AuthenticationException
//   CipherGen.getLanManagerSessionKey()  throws AuthenticationException
//   Handle constructor                   throws AuthenticationException
//   static hmacMD5(...)                  throws AuthenticationException
//   static RC4(...)                      throws AuthenticationException
//   static ntlm2SessionResponse(...)     throws AuthenticationException
//   private static lmHash(...)           throws AuthenticationException
//   private static ntlmHash(...)         throws AuthenticationException
//   private static lmv2Hash(...)         throws AuthenticationException
//   private static ntlmv2Hash(...)       throws AuthenticationException
//   private static lmResponse(...)       throws AuthenticationException
//   private static lmv2Response(...)     throws AuthenticationException
//   NTLMMessage constructors             throws AuthenticationException
//   Type3Message constructors            throws AuthenticationException
```

---

### 8. `ZapNTLMScheme`

**File:** `ZapNTLMScheme.java`  
**Status:** `@Deprecated (2.12.0)`

| Line | Commons HttpClient Type | Usage |
|------|------------------------|-------|
| 29 | `org.apache.commons.httpclient.Credentials` | Import; parameter type of `authenticate(Credentials, HttpMethod)` |
| 30 | `org.apache.commons.httpclient.HttpMethod` | Import; parameter type of `authenticate(Credentials, HttpMethod)` |
| 31 | `org.apache.commons.httpclient.NTCredentials` | Import; cast target inside `authenticate(...)` |
| 32 | `org.apache.commons.httpclient.auth.AuthChallengeParser` | Import; `AuthChallengeParser.extractScheme(String)` called in `processChallenge(...)` |
| 33 | `org.apache.commons.httpclient.auth.AuthScheme` | Import; implemented interface (`implements AuthScheme`) |
| 34 | `org.apache.commons.httpclient.auth.AuthenticationException` | Import; thrown by `authenticate(...)` |
| 35 | `org.apache.commons.httpclient.auth.MalformedChallengeException` | Import; thrown by `processChallenge(...)` |
| 48 | `org.apache.commons.httpclient.auth.AuthScheme` | Javadoc comment (adaptation note) |

**Method signatures referencing commons-httpclient:**
```java
// implements org.apache.commons.httpclient.auth.AuthScheme
@Override
public void processChallenge(String challenge) throws MalformedChallengeException
//                                                     ^^^ org.apache.commons.httpclient.auth.MalformedChallengeException

@Override
public String authenticate(Credentials credentials, HttpMethod method) throws AuthenticationException
//                         ^^^ o.a.c.h.Credentials  ^^^ o.a.c.h.HttpMethod  ^^^ o.a.c.h.auth.AuthenticationException

@Override @Deprecated
public String authenticate(Credentials credentials, String method, String uri) throws AuthenticationException
```

**Internal call sites:**
```java
String s = AuthChallengeParser.extractScheme(challenge);  // org.apache.commons.httpclient.auth.AuthChallengeParser
ntcredentials = (NTCredentials) credentials;              // org.apache.commons.httpclient.NTCredentials
```

---

### 9. `ZapOptionsMethod`

**File:** `ZapOptionsMethod.java`  
**Status:** `@Deprecated (2.12.0)`

| Line | Commons HttpClient Type | Usage |
|------|------------------------|-------|
| 23 | `org.apache.commons.httpclient.Header` | Import; return type of `ZapHttpParser.parseHeaders(...)` |
| 24 | `org.apache.commons.httpclient.HttpState` | Import; parameter type of `readResponseHeaders(...)` |
| 25 | `org.apache.commons.httpclient.methods.EntityEnclosingMethod` | Import; superclass (`extends EntityEnclosingMethod`) |
| 26 | `org.apache.commons.httpclient.methods.OptionsMethod` | Import; Javadoc `@see` reference |
| 62 | `org.apache.commons.httpclient.HttpConnection` | Inline fully-qualified type in method signature |

**Method signatures referencing commons-httpclient:**
```java
// extends org.apache.commons.httpclient.methods.EntityEnclosingMethod
@Override
protected void readResponseHeaders(
    HttpState state,
    org.apache.commons.httpclient.HttpConnection conn) throws IOException
```

---

### 10. `ZapPostMethod`

**File:** `ZapPostMethod.java`  
**Status:** `@Deprecated (2.12.0)`

| Line | Commons HttpClient Type | Usage |
|------|------------------------|-------|
| 23 | `org.apache.commons.httpclient.Header` | Import; return type of `ZapHttpParser.parseHeaders(...)` |
| 24 | `org.apache.commons.httpclient.HttpState` | Import; parameter type of `readResponseHeaders(...)` |
| 25 | `org.apache.commons.httpclient.methods.PostMethod` | Import; superclass (`extends PostMethod`) |
| 56 | `org.apache.commons.httpclient.HttpConnection` | Inline fully-qualified type in method signature |

**Method signatures referencing commons-httpclient:**
```java
// extends org.apache.commons.httpclient.methods.PostMethod
@Override
protected void readResponseHeaders(
    HttpState state,
    org.apache.commons.httpclient.HttpConnection conn) throws IOException
```

---

### 11. `ZapPutMethod`

**File:** `ZapPutMethod.java`  
**Status:** `@Deprecated (2.12.0)`

| Line | Commons HttpClient Type | Usage |
|------|------------------------|-------|
| 23 | `org.apache.commons.httpclient.Header` | Import; return type of `ZapHttpParser.parseHeaders(...)` |
| 24 | `org.apache.commons.httpclient.HttpState` | Import; parameter type of `readResponseHeaders(...)` |
| 25 | `org.apache.commons.httpclient.methods.PutMethod` | Import; superclass (`extends PutMethod`) |
| 56 | `org.apache.commons.httpclient.HttpConnection` | Inline fully-qualified type in method signature |

**Method signatures referencing commons-httpclient:**
```java
// extends org.apache.commons.httpclient.methods.PutMethod
@Override
protected void readResponseHeaders(
    HttpState state,
    org.apache.commons.httpclient.HttpConnection conn) throws IOException
```

---

### 12. `ZapTraceMethod`

**File:** `ZapTraceMethod.java`  
**Status:** `@Deprecated (2.12.0)`

| Line | Commons HttpClient Type | Usage |
|------|------------------------|-------|
| 23 | `org.apache.commons.httpclient.Header` | Import; return type of `ZapHttpParser.parseHeaders(...)` |
| 24 | `org.apache.commons.httpclient.HttpState` | Import; parameter type of `readResponseHeaders(...)` |
| 25 | `org.apache.commons.httpclient.methods.EntityEnclosingMethod` | Import; superclass (`extends EntityEnclosingMethod`) |
| 26 | `org.apache.commons.httpclient.methods.TraceMethod` | Import; Javadoc `@see` reference |
| 58 | `org.apache.commons.httpclient.HttpConnection` | Inline fully-qualified type in method signature |

**Method signatures referencing commons-httpclient:**
```java
// extends org.apache.commons.httpclient.methods.EntityEnclosingMethod
@Override
protected void readResponseHeaders(
    HttpState state,
    org.apache.commons.httpclient.HttpConnection conn) throws IOException
```

---

## Commons HttpClient Types Referenced — Master List

| Commons HttpClient Type (FQN) | Used In |
|-------------------------------|---------|
| `org.apache.commons.httpclient.URI` | `DefaultHttpRedirectionValidator`, `HttpRedirectionValidator` |
| `org.apache.commons.httpclient.Cookie` | `ZapCookieSpec` |
| `org.apache.commons.httpclient.cookie.CookieSpecBase` | `ZapCookieSpec` (superclass) |
| `org.apache.commons.httpclient.cookie.MalformedCookieException` | `ZapCookieSpec` |
| `org.apache.commons.httpclient.Header` | `ZapDeleteMethod`, `ZapHeadMethod`, `ZapHttpParser`, `ZapOptionsMethod`, `ZapPostMethod`, `ZapPutMethod`, `ZapTraceMethod` |
| `org.apache.commons.httpclient.HttpState` | `ZapDeleteMethod`, `ZapHeadMethod`, `ZapOptionsMethod`, `ZapPostMethod`, `ZapPutMethod`, `ZapTraceMethod` |
| `org.apache.commons.httpclient.HttpConnection` | `ZapDeleteMethod`, `ZapHeadMethod`, `ZapOptionsMethod`, `ZapPostMethod`, `ZapPutMethod`, `ZapTraceMethod` |
| `org.apache.commons.httpclient.methods.DeleteMethod` | `ZapDeleteMethod` (Javadoc `@see`) |
| `org.apache.commons.httpclient.methods.EntityEnclosingMethod` | `ZapDeleteMethod`, `ZapHeadMethod`, `ZapOptionsMethod`, `ZapTraceMethod` (superclass) |
| `org.apache.commons.httpclient.ProtocolException` | `ZapHeadMethod` |
| `org.apache.commons.httpclient.methods.HeadMethod` | `ZapHeadMethod` (Javadoc `@see`) |
| `org.apache.commons.httpclient.params.HttpMethodParams` | `ZapHeadMethod` |
| `org.apache.commons.httpclient.HttpParser` | `ZapHttpParser` |
| `org.apache.commons.httpclient.auth.AuthenticationException` | `ZapNTLMEngineImpl`, `ZapNTLMScheme` |
| `org.apache.commons.httpclient.Credentials` | `ZapNTLMScheme` |
| `org.apache.commons.httpclient.HttpMethod` | `ZapNTLMScheme` |
| `org.apache.commons.httpclient.NTCredentials` | `ZapNTLMScheme` |
| `org.apache.commons.httpclient.auth.AuthChallengeParser` | `ZapNTLMScheme` |
| `org.apache.commons.httpclient.auth.AuthScheme` | `ZapNTLMScheme` (implemented interface) |
| `org.apache.commons.httpclient.auth.MalformedChallengeException` | `ZapNTLMScheme` |
| `org.apache.commons.httpclient.methods.OptionsMethod` | `ZapOptionsMethod` (Javadoc `@see`) |
| `org.apache.commons.httpclient.methods.PostMethod` | `ZapPostMethod` (superclass) |
| `org.apache.commons.httpclient.methods.PutMethod` | `ZapPutMethod` (superclass) |
| `org.apache.commons.httpclient.methods.TraceMethod` | `ZapTraceMethod` (Javadoc `@see`) |

---

## Classes NOT Affected (zero commons-httpclient references)

The following classes in `org.zaproxy.zap.network` contain **no** references to
`org.apache.commons.httpclient.*`:

| Class | Notes |
|-------|-------|
| `AbstractStreamHttpEncoding` | Uses only JDK streams |
| `DomainMatcher` | Pure string/regex logic |
| `HttpEncoding` | Interface, no external deps |
| `HttpEncodingDeflate` | Uses only JDK streams |
| `HttpEncodingGzip` | Uses only JDK streams |
| `HttpRequestBody` | Internal model class |
| `HttpRequestConfig` | Internal configuration class |
| `HttpResponseBody` | Internal model class |
| `HttpSenderContext` | Interface, no external deps |
| `HttpSenderImpl` | Interface, no external deps (see below) |
| `HttpSenderListener` | Interface, no external deps |
| `SocksProxy` | SOCKS proxy configuration |
| `ZapAuthenticator` | Uses `java.net.Authenticator` |
| `ZapProxySelector` | Uses `java.net.ProxySelector` |

---

## `HttpSenderImpl` — Public API Baseline

`HttpSenderImpl` is the boundary interface between the network package and its
callers. It contains **zero** commons-httpclient references and its full public
API is documented here as a baseline for japicmp comparison in step 2.

```java
package org.zaproxy.zap.network;

/** <strong>Note:</strong> Not part of the public API. */
public interface HttpSenderImpl<T extends HttpSenderContext> {

    boolean isGlobalStateEnabled();

    void addListener(HttpSenderListener listener);

    void removeListener(HttpSenderListener listener);

    T createContext(HttpSender parent, int initiator);

    default T getContext(HttpSender httpSender);

    default void sendAndReceive(T ctx, HttpRequestConfig config, HttpMessage msg, Path file)
            throws IOException;

    default void sendAndReceive(
            HttpSender parent, HttpRequestConfig config, HttpMessage msg, Path file)
            throws IOException;

    default Object saveState();

    default void restoreState(Object implState);
}
```

---

## Scope Boundary Confirmation

| Directory | Command | Expected Result |
|-----------|---------|-----------------|
| `org.zaproxy.zap.network` | `grep -rn 'org.apache.commons.httpclient' zap/src/main/java/org/zaproxy/zap/network/` | **48 matches** across 12 files (see Grep Baseline above) |
| `org.zaproxy.zap.extension` | `grep -rn 'org.apache.commons.httpclient' zap/src/main/java/org/zaproxy/zap/extension/` | Out of scope — tracked separately |
| `org.zaproxy.zap.view` | `grep -rn 'org.apache.commons.httpclient' zap/src/main/java/org/zaproxy/zap/view/` | Out of scope — tracked separately |

> **Note:** The `extension/` and `view/` directories currently contain
> commons-httpclient references (e.g. `URI`, `URIException`, `Cookie`), but
> these are **explicitly out of scope** for this migration step. The scope
> boundary for this step is `org.zaproxy.zap.network` only.

---

## Migration Priority

Based on deprecation status and coupling:

| Priority | Class | Reason |
|----------|-------|--------|
| 1 | `ZapHttpParser` | Central utility; all HTTP-method classes depend on it |
| 2 | `ZapPostMethod`, `ZapPutMethod`, `ZapDeleteMethod`, `ZapHeadMethod`, `ZapOptionsMethod`, `ZapTraceMethod` | All extend commons-httpclient base classes; all call `ZapHttpParser` |
| 3 | `ZapNTLMEngineImpl`, `ZapNTLMScheme` | NTLM auth; `ZapNTLMScheme` implements `AuthScheme` |
| 4 | `ZapCookieSpec` | Extends `CookieSpecBase` |
| 5 | `HttpRedirectionValidator`, `DefaultHttpRedirectionValidator` | Public API; `URI` parameter type is the only coupling |

All 12 affected classes are already annotated `@Deprecated(since="2.12.0")`,
confirming they are implementation details not intended for external use.
