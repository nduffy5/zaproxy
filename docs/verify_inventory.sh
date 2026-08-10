#!/usr/bin/env bash
# Verification script for acceptance criteria of network-migration-inventory.md
# Run from repo root.

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NETWORK_DIR="$REPO_ROOT/zap/src/main/java/org/zaproxy/zap/network"
EXTENSION_DIR="$REPO_ROOT/zap/src/main/java/org/zaproxy/zap/extension"
VIEW_DIR="$REPO_ROOT/zap/src/main/java/org/zaproxy/zap/view"
INVENTORY="$REPO_ROOT/docs/network-migration-inventory.md"

PASS=0
FAIL=0

pass() { echo "PASS: $1"; PASS=$((PASS+1)); }
fail() { echo "FAIL: $1"; FAIL=$((FAIL+1)); }

echo "=== Criterion 1: Inventory file exists ==="
if [ -f "$INVENTORY" ]; then
  pass "docs/network-migration-inventory.md exists"
else
  fail "docs/network-migration-inventory.md does NOT exist"
fi

echo ""
echo "=== Criterion 2: Grep produces results in network directory ==="

ACTUAL_GREP=$(grep -rn 'org.apache.commons.httpclient' "$NETWORK_DIR" 2>/dev/null | sed "s|$NETWORK_DIR/||")

if [ -z "$ACTUAL_GREP" ]; then
  fail "No grep results found in network directory"
else
  MATCH_COUNT=$(echo "$ACTUAL_GREP" | wc -l)
  pass "Grep produces $MATCH_COUNT results in network directory"
fi

echo ""
echo "=== Criterion 3: All 12 affected files appear in grep output ==="

INVENTORY_FILES="DefaultHttpRedirectionValidator.java HttpRedirectionValidator.java ZapCookieSpec.java ZapDeleteMethod.java ZapHeadMethod.java ZapHttpParser.java ZapNTLMEngineImpl.java ZapNTLMScheme.java ZapOptionsMethod.java ZapPostMethod.java ZapPutMethod.java ZapTraceMethod.java"

for f in $INVENTORY_FILES; do
  if echo "$ACTUAL_GREP" | grep -q "$f"; then
    pass "File $f appears in grep output"
  else
    fail "File $f missing from grep output"
  fi
done

echo ""
echo "=== Criterion 4: Grep output matches inventory baseline ==="

# Check each specific line from the inventory baseline
BASELINE_CHECKS=(
  "DefaultHttpRedirectionValidator.java:22:import org.apache.commons.httpclient.URI"
  "HttpRedirectionValidator.java:22:import org.apache.commons.httpclient.URI"
  "ZapCookieSpec.java:22:import org.apache.commons.httpclient.Cookie"
  "ZapCookieSpec.java:23:import org.apache.commons.httpclient.cookie.CookieSpecBase"
  "ZapCookieSpec.java:24:import org.apache.commons.httpclient.cookie.MalformedCookieException"
  "ZapDeleteMethod.java:23:import org.apache.commons.httpclient.Header"
  "ZapDeleteMethod.java:24:import org.apache.commons.httpclient.HttpState"
  "ZapDeleteMethod.java:25:import org.apache.commons.httpclient.methods.DeleteMethod"
  "ZapDeleteMethod.java:26:import org.apache.commons.httpclient.methods.EntityEnclosingMethod"
  "ZapHeadMethod.java:23:import org.apache.commons.httpclient.Header"
  "ZapHeadMethod.java:24:import org.apache.commons.httpclient.HttpState"
  "ZapHeadMethod.java:25:import org.apache.commons.httpclient.ProtocolException"
  "ZapHeadMethod.java:26:import org.apache.commons.httpclient.methods.EntityEnclosingMethod"
  "ZapHeadMethod.java:27:import org.apache.commons.httpclient.methods.HeadMethod"
  "ZapHeadMethod.java:28:import org.apache.commons.httpclient.params.HttpMethodParams"
  "ZapHttpParser.java:25:import org.apache.commons.httpclient.Header"
  "ZapHttpParser.java:26:import org.apache.commons.httpclient.HttpParser"
  "ZapNTLMEngineImpl.java:44:import org.apache.commons.httpclient.auth.AuthenticationException"
  "ZapNTLMScheme.java:29:import org.apache.commons.httpclient.Credentials"
  "ZapNTLMScheme.java:30:import org.apache.commons.httpclient.HttpMethod"
  "ZapNTLMScheme.java:31:import org.apache.commons.httpclient.NTCredentials"
  "ZapNTLMScheme.java:32:import org.apache.commons.httpclient.auth.AuthChallengeParser"
  "ZapNTLMScheme.java:33:import org.apache.commons.httpclient.auth.AuthScheme"
  "ZapNTLMScheme.java:34:import org.apache.commons.httpclient.auth.AuthenticationException"
  "ZapNTLMScheme.java:35:import org.apache.commons.httpclient.auth.MalformedChallengeException"
  "ZapOptionsMethod.java:23:import org.apache.commons.httpclient.Header"
  "ZapOptionsMethod.java:24:import org.apache.commons.httpclient.HttpState"
  "ZapOptionsMethod.java:25:import org.apache.commons.httpclient.methods.EntityEnclosingMethod"
  "ZapOptionsMethod.java:26:import org.apache.commons.httpclient.methods.OptionsMethod"
  "ZapPostMethod.java:23:import org.apache.commons.httpclient.Header"
  "ZapPostMethod.java:24:import org.apache.commons.httpclient.HttpState"
  "ZapPostMethod.java:25:import org.apache.commons.httpclient.methods.PostMethod"
  "ZapPutMethod.java:23:import org.apache.commons.httpclient.Header"
  "ZapPutMethod.java:24:import org.apache.commons.httpclient.HttpState"
  "ZapPutMethod.java:25:import org.apache.commons.httpclient.methods.PutMethod"
  "ZapTraceMethod.java:23:import org.apache.commons.httpclient.Header"
  "ZapTraceMethod.java:24:import org.apache.commons.httpclient.HttpState"
  "ZapTraceMethod.java:25:import org.apache.commons.httpclient.methods.EntityEnclosingMethod"
  "ZapTraceMethod.java:26:import org.apache.commons.httpclient.methods.TraceMethod"
)

for check in "${BASELINE_CHECKS[@]}"; do
  if echo "$ACTUAL_GREP" | grep -qF "$check"; then
    pass "Baseline line present: $check"
  else
    fail "Baseline line MISSING: $check"
  fi
done

echo ""
echo "=== Criterion 5: Inventory lists all affected classes ==="

for f in $INVENTORY_FILES; do
  CLASS="${f%.java}"
  if grep -q "$CLASS" "$INVENTORY"; then
    pass "Inventory mentions class $CLASS"
  else
    fail "Inventory does NOT mention class $CLASS"
  fi
done

echo ""
echo "=== Criterion 6: Inventory lists specific Commons HttpClient types ==="

EXPECTED_TYPES="org.apache.commons.httpclient.URI org.apache.commons.httpclient.Cookie org.apache.commons.httpclient.cookie.CookieSpecBase org.apache.commons.httpclient.cookie.MalformedCookieException org.apache.commons.httpclient.Header org.apache.commons.httpclient.HttpState org.apache.commons.httpclient.HttpConnection org.apache.commons.httpclient.methods.DeleteMethod org.apache.commons.httpclient.methods.EntityEnclosingMethod org.apache.commons.httpclient.ProtocolException org.apache.commons.httpclient.methods.HeadMethod org.apache.commons.httpclient.params.HttpMethodParams org.apache.commons.httpclient.HttpParser org.apache.commons.httpclient.auth.AuthenticationException org.apache.commons.httpclient.Credentials org.apache.commons.httpclient.HttpMethod org.apache.commons.httpclient.NTCredentials org.apache.commons.httpclient.auth.AuthChallengeParser org.apache.commons.httpclient.auth.AuthScheme org.apache.commons.httpclient.auth.MalformedChallengeException org.apache.commons.httpclient.methods.OptionsMethod org.apache.commons.httpclient.methods.PostMethod org.apache.commons.httpclient.methods.PutMethod org.apache.commons.httpclient.methods.TraceMethod"

for t in $EXPECTED_TYPES; do
  if grep -q "$t" "$INVENTORY"; then
    pass "Inventory mentions type $t"
  else
    fail "Inventory does NOT mention type $t"
  fi
done

echo ""
echo "=== Criterion 7: Scope boundary — extension/ and view/ ==="

EXT_COUNT=$(grep -rn 'org.apache.commons.httpclient' "$EXTENSION_DIR" 2>/dev/null | wc -l)
VIEW_COUNT=$(grep -rn 'org.apache.commons.httpclient' "$VIEW_DIR" 2>/dev/null | wc -l)

echo "  extension/ references: $EXT_COUNT"
echo "  view/ references: $VIEW_COUNT"

# Check inventory documents scope boundary
if grep -q "out of scope" "$INVENTORY" && grep -q "extension" "$INVENTORY" && grep -q "view" "$INVENTORY"; then
  pass "Inventory explicitly documents extension/ and view/ as out of scope"
else
  fail "Inventory does not document scope boundary for extension/ and view/"
fi

# The acceptance criterion literally says "produces zero results"
if [ "$EXT_COUNT" -eq 0 ] && [ "$VIEW_COUNT" -eq 0 ]; then
  pass "extension/ and view/ produce zero grep results (scope boundary confirmed)"
else
  fail "extension/ ($EXT_COUNT matches) and/or view/ ($VIEW_COUNT matches) produce non-zero grep results — literal 'zero results' criterion NOT met"
fi

echo ""
echo "=== Summary ==="
echo "PASSED: $PASS"
echo "FAILED: $FAIL"
echo ""
if [ "$FAIL" -eq 0 ]; then
  echo "ALL CRITERIA MET"
  exit 0
else
  echo "SOME CRITERIA FAILED"
  exit 1
fi
