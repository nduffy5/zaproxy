#!/usr/bin/env bash
# TDD test: Verify commons-httpclient is removed from LEGALNOTICE.md
# and that the updateLegalNotice task does not re-add it.

set -e

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
LEGALNOTICE="$REPO_ROOT/LEGALNOTICE.md"
PASS=0
FAIL=0

run_test() {
    local name="$1"
    local result="$2"
    if [ "$result" -eq 0 ]; then
        echo "PASS: $name"
        PASS=$((PASS + 1))
    else
        echo "FAIL: $name"
        FAIL=$((FAIL + 1))
    fi
}

# Test 1: LEGALNOTICE.md must not contain 'commons-httpclient'
if grep -q 'commons-httpclient' "$LEGALNOTICE"; then
    run_test "LEGALNOTICE.md does not contain commons-httpclient" 1
else
    run_test "LEGALNOTICE.md does not contain commons-httpclient" 0
fi

# Test 2: thirdPartyLicenses.yaml must not contain 'commons-httpclient'
LICENSES_YAML="$REPO_ROOT/zap/gradle/thirdPartyLicenses.yaml"
if grep -q 'commons-httpclient' "$LICENSES_YAML"; then
    run_test "thirdPartyLicenses.yaml does not contain commons-httpclient" 1
else
    run_test "thirdPartyLicenses.yaml does not contain commons-httpclient" 0
fi

# Test 3: zap.gradle.kts must not declare commons-httpclient as a dependency
ZAP_GRADLE="$REPO_ROOT/zap/zap.gradle.kts"
if grep -q 'commons.httpclient\|commons-httpclient' "$ZAP_GRADLE"; then
    run_test "zap.gradle.kts does not declare commons-httpclient dependency" 1
else
    run_test "zap.gradle.kts does not declare commons-httpclient dependency" 0
fi

echo ""
echo "Results: $PASS passed, $FAIL failed"
if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
exit 0
