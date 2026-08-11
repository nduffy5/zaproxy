#!/usr/bin/env bash
# TDD test: Verify that the setup-chalk-action@main SHA is resolved and recorded.
#
# Acceptance criteria:
#   1. git ls-remote returns exit code 0 and a 40-character SHA
#   2. The SHA is recorded in .chalk-action-sha scratch file

set -euo pipefail

SCRATCH_FILE="$(dirname "$0")/../.chalk-action-sha"
REPO_URL="https://github.com/crashappsec/setup-chalk-action"
REF="refs/heads/main"
SHA_REGEX='^[0-9a-f]{40}$'

PASS=0
FAIL=0

run_test() {
    local description="$1"
    local result="$2"   # "pass" or "fail"
    if [ "$result" = "pass" ]; then
        echo "  PASS: $description"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $description"
        FAIL=$((FAIL + 1))
    fi
}

echo "=== Test Suite: setup-chalk-action SHA resolution ==="
echo ""

# Test 1: git ls-remote exits with code 0 and returns output
echo "Test 1: git ls-remote exits successfully"
if git ls-remote "$REPO_URL" "$REF" > /tmp/ls_remote_output 2>&1; then
    run_test "git ls-remote exits with code 0" "pass"
else
    run_test "git ls-remote exits with code 0" "fail"
fi

# Test 2: Output contains a 40-character hex SHA
echo "Test 2: Output contains a 40-character hex SHA"
REMOTE_SHA=$(awk '{print $1}' /tmp/ls_remote_output)
if echo "$REMOTE_SHA" | grep -qE "$SHA_REGEX"; then
    run_test "SHA is exactly 40 hex characters" "pass"
else
    run_test "SHA is exactly 40 hex characters (got: '$REMOTE_SHA')" "fail"
fi

# Test 3: Scratch file exists
echo "Test 3: Scratch file .chalk-action-sha exists"
if [ -f "$SCRATCH_FILE" ]; then
    run_test "Scratch file .chalk-action-sha exists" "pass"
else
    run_test "Scratch file .chalk-action-sha exists" "fail"
fi

# Test 4: Scratch file contains a valid 40-character SHA
echo "Test 4: Scratch file contains a valid 40-character SHA"
if [ -f "$SCRATCH_FILE" ]; then
    RECORDED_SHA=$(cat "$SCRATCH_FILE" | tr -d '[:space:]')
    if echo "$RECORDED_SHA" | grep -qE "$SHA_REGEX"; then
        run_test "Recorded SHA is exactly 40 hex characters" "pass"
    else
        run_test "Recorded SHA is exactly 40 hex characters (got: '$RECORDED_SHA')" "fail"
    fi
else
    run_test "Recorded SHA is exactly 40 hex characters (file missing)" "fail"
fi

# Test 5: Recorded SHA matches the live SHA from git ls-remote
echo "Test 5: Recorded SHA matches live SHA from git ls-remote"
if [ -f "$SCRATCH_FILE" ]; then
    RECORDED_SHA=$(cat "$SCRATCH_FILE" | tr -d '[:space:]')
    if [ "$RECORDED_SHA" = "$REMOTE_SHA" ]; then
        run_test "Recorded SHA matches live SHA ($REMOTE_SHA)" "pass"
    else
        run_test "Recorded SHA matches live SHA (recorded='$RECORDED_SHA', live='$REMOTE_SHA')" "fail"
    fi
else
    run_test "Recorded SHA matches live SHA (file missing)" "fail"
fi

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
exit 0
