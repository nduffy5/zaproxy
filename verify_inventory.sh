#!/usr/bin/env bash
# Verification script for acceptance criteria of network-migration-inventory.md

REPO_ROOT="/workspace/709938f9-ab1d-4907-b78a-0030a304a8e7/worker-0/repo"
NETWORK_DIR="$REPO_ROOT/zap/src/main/java/org/zaproxy/zap/network"
EXTENSION_DIR="$REPO_ROOT/zap/src/main/java/org/zaproxy/zap/extension"
VIEW_DIR="$REPO_ROOT/zap/src/main/java/org/zaproxy/zap/view"
INVENTORY_FILE="$REPO_ROOT/docs/network-migration-inventory.md"

PASS=0
FAIL=0

echo "============================================================"
echo "CRITERION 1: Inventory file exists"
echo "============================================================"
if [ -f "$INVENTORY_FILE" ]; then
    echo "PASS: docs/network-migration-inventory.md exists"
    PASS=$((PASS+1))
else
    echo "FAIL: docs/network-migration-inventory.md does NOT exist"
    FAIL=$((FAIL+1))
fi

echo ""
echo "============================================================"
echo "CRITERION 2: grep output matches inventory document"
echo "============================================================"

# Run actual grep
ACTUAL_GREP=$(grep -rn 'org.apache.commons.httpclient' "$NETWORK_DIR" 2>/dev/null | sed "s|$NETWORK_DIR/||")

echo "--- Actual grep output (relative paths) ---"
echo "$ACTUAL_GREP"
echo ""

# Classes that should be in the inventory
EXPECTED_CLASSES=(
    "DefaultHttpRedirectionValidator.java"
    "HttpRedirectionValidator.java"
    "ZapCookieSpec.java"
    "ZapDeleteMethod.java"
    "ZapHeadMethod.java"
    "ZapHttpParser.java"
    "ZapNTLMEngineImpl.java"
    "ZapNTLMScheme.java"
    "ZapOptionsMethod.java"
    "ZapPostMethod.java"
    "ZapPutMethod.java"
    "ZapTraceMethod.java"
)

ALL_CLASSES_IN_INVENTORY=true
for cls in "${EXPECTED_CLASSES[@]}"; do
    if grep -q "$cls" "$INVENTORY_FILE"; then
        echo "PASS: $cls is listed in inventory"
    else
        echo "FAIL: $cls is NOT listed in inventory"
        ALL_CLASSES_IN_INVENTORY=false
        FAIL=$((FAIL+1))
    fi
done

if $ALL_CLASSES_IN_INVENTORY; then
    echo "PASS: All grep-found classes are listed in the inventory document"
    PASS=$((PASS+1))
fi

# Verify each class found by grep is in the inventory
echo ""
echo "--- Verifying each grep result class appears in inventory ---"
GREP_CLASSES=$(grep -rn 'org.apache.commons.httpclient' "$NETWORK_DIR" 2>/dev/null | sed "s|$NETWORK_DIR/||" | cut -d: -f1 | sort -u)
INVENTORY_MATCH=true
while IFS= read -r cls; do
    basename_cls=$(basename "$cls")
    if grep -q "$basename_cls" "$INVENTORY_FILE"; then
        echo "PASS: $basename_cls found in inventory"
    else
        echo "FAIL: $basename_cls from grep NOT found in inventory"
        INVENTORY_MATCH=false
        FAIL=$((FAIL+1))
    fi
done <<< "$GREP_CLASSES"

if $INVENTORY_MATCH; then
    echo "PASS: All grep-found files are documented in inventory"
    PASS=$((PASS+1))
fi

echo ""
echo "============================================================"
echo "CRITERION 3: Scope boundary - extension/ grep results"
echo "============================================================"
EXT_COUNT=$(grep -rn 'org.apache.commons.httpclient' "$EXTENSION_DIR" 2>/dev/null | wc -l)
echo "grep results in extension/: $EXT_COUNT"
if [ "$EXT_COUNT" -eq 0 ]; then
    echo "PASS: extension/ has zero commons-httpclient references"
    PASS=$((PASS+1))
else
    echo "FAIL: extension/ has $EXT_COUNT commons-httpclient references (expected 0)"
    FAIL=$((FAIL+1))
fi

echo ""
echo "============================================================"
echo "CRITERION 4: Scope boundary - view/ grep results"
echo "============================================================"
VIEW_COUNT=$(grep -rn 'org.apache.commons.httpclient' "$VIEW_DIR" 2>/dev/null | wc -l)
echo "grep results in view/: $VIEW_COUNT"
if [ "$VIEW_COUNT" -eq 0 ]; then
    echo "PASS: view/ has zero commons-httpclient references"
    PASS=$((PASS+1))
else
    echo "FAIL: view/ has $VIEW_COUNT commons-httpclient references (expected 0)"
    FAIL=$((FAIL+1))
fi

echo ""
echo "============================================================"
echo "CRITERION 5: Inventory lists every affected class with specific types"
echo "============================================================"

# Check that specific Commons HttpClient types are documented
EXPECTED_TYPES=(
    "org.apache.commons.httpclient.URI"
    "org.apache.commons.httpclient.Cookie"
    "org.apache.commons.httpclient.cookie.CookieSpecBase"
    "org.apache.commons.httpclient.cookie.MalformedCookieException"
    "org.apache.commons.httpclient.Header"
    "org.apache.commons.httpclient.HttpState"
    "org.apache.commons.httpclient.HttpConnection"
    "org.apache.commons.httpclient.HttpParser"
    "org.apache.commons.httpclient.ProtocolException"
    "org.apache.commons.httpclient.params.HttpMethodParams"
    "org.apache.commons.httpclient.auth.AuthenticationException"
    "org.apache.commons.httpclient.Credentials"
    "org.apache.commons.httpclient.HttpMethod"
    "org.apache.commons.httpclient.NTCredentials"
    "org.apache.commons.httpclient.auth.AuthChallengeParser"
    "org.apache.commons.httpclient.auth.AuthScheme"
    "org.apache.commons.httpclient.auth.MalformedChallengeException"
    "org.apache.commons.httpclient.methods.EntityEnclosingMethod"
    "org.apache.commons.httpclient.methods.PostMethod"
    "org.apache.commons.httpclient.methods.PutMethod"
)

ALL_TYPES_FOUND=true
for type in "${EXPECTED_TYPES[@]}"; do
    if grep -q "$type" "$INVENTORY_FILE"; then
        echo "PASS: '$type' is documented in inventory"
    else
        echo "FAIL: '$type' is NOT documented in inventory"
        ALL_TYPES_FOUND=false
        FAIL=$((FAIL+1))
    fi
done

if $ALL_TYPES_FOUND; then
    echo "PASS: All specific Commons HttpClient types are documented"
    PASS=$((PASS+1))
fi

echo ""
echo "============================================================"
echo "SUMMARY"
echo "============================================================"
echo "PASS: $PASS"
echo "FAIL: $FAIL"

if [ "$FAIL" -eq 0 ]; then
    echo "ALL CRITERIA MET"
    exit 0
else
    echo "SOME CRITERIA FAILED"
    exit 1
fi
