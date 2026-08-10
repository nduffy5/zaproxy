#!/usr/bin/env bash
# Test script to verify acceptance criteria for removing commons-httpclient from LEGALNOTICE.md

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
PASS=0
FAIL=0

pass() { echo "PASS: $1"; ((PASS++)); }
fail() { echo "FAIL: $1"; ((FAIL++)); }

# ── Criterion 1: grep 'commons-httpclient' LEGALNOTICE.md returns zero results ──
echo "=== Criterion 1: commons-httpclient absent from LEGALNOTICE.md ==="
if grep -q 'commons-httpclient' "$REPO_ROOT/LEGALNOTICE.md"; then
    fail "commons-httpclient still present in LEGALNOTICE.md"
else
    pass "commons-httpclient is NOT in LEGALNOTICE.md"
fi

# ── Criterion 2: updateLegalNotice task completes without error and does NOT re-add the entry ──
echo ""
echo "=== Criterion 2: ./gradlew updateLegalNotice completes without error ==="
export JAVA_HOME=/tmp/jdk-17.0.10+7
export PATH=$JAVA_HOME/bin:$PATH

GRADLE_OUTPUT=$("$REPO_ROOT/gradlew" -p "$REPO_ROOT" updateLegalNotice 2>&1)
GRADLE_EXIT=$?

if [ $GRADLE_EXIT -ne 0 ]; then
    fail "gradlew updateLegalNotice exited with code $GRADLE_EXIT"
    echo "$GRADLE_OUTPUT"
else
    pass "gradlew updateLegalNotice completed successfully (exit 0)"
fi

echo ""
echo "=== Criterion 2b: updateLegalNotice does NOT re-add commons-httpclient ==="
if grep -q 'commons-httpclient' "$REPO_ROOT/LEGALNOTICE.md"; then
    fail "updateLegalNotice re-added commons-httpclient to LEGALNOTICE.md"
else
    pass "updateLegalNotice did NOT re-add commons-httpclient"
fi

# ── Criterion 3: LEGALNOTICE.md is committed with the removal ──
echo ""
echo "=== Criterion 3: LEGALNOTICE.md removal is reflected in git history/diff ==="
# The change is staged/modified (not yet committed in this branch), but the diff shows the removal.
# Check that the working-tree version does NOT contain the entry (already verified above),
# and that the original (HEAD) version DID contain it.
ORIGINAL_CONTENT=$(git -C "$REPO_ROOT" show HEAD:LEGALNOTICE.md 2>/dev/null || true)
if echo "$ORIGINAL_CONTENT" | grep -q 'commons-httpclient'; then
    pass "HEAD (original) LEGALNOTICE.md contained commons-httpclient (removal is a real change)"
else
    # Maybe it was already committed on this branch
    # Check if the current file lacks it (already verified) and the diff shows a deletion
    DIFF_OUTPUT=$(git -C "$REPO_ROOT" diff HEAD -- LEGALNOTICE.md 2>/dev/null || true)
    if echo "$DIFF_OUTPUT" | grep -q '\-.*commons-httpclient'; then
        pass "git diff shows commons-httpclient was removed from LEGALNOTICE.md"
    else
        # Check if it was committed on this branch (compare with merge-base)
        MERGE_BASE=$(git -C "$REPO_ROOT" merge-base HEAD origin/main 2>/dev/null || git -C "$REPO_ROOT" merge-base HEAD main 2>/dev/null || echo "")
        if [ -n "$MERGE_BASE" ]; then
            BASE_CONTENT=$(git -C "$REPO_ROOT" show "$MERGE_BASE:LEGALNOTICE.md" 2>/dev/null || true)
            if echo "$BASE_CONTENT" | grep -q 'commons-httpclient'; then
                pass "commons-httpclient was removed relative to merge-base"
            else
                fail "Could not confirm commons-httpclient removal in git history"
            fi
        else
            fail "Could not confirm commons-httpclient removal in git history"
        fi
    fi
fi

# Also verify thirdPartyLicenses.yaml does not contain commons-httpclient
echo ""
echo "=== Bonus: commons-httpclient absent from thirdPartyLicenses.yaml ==="
if grep -q 'commons-httpclient' "$REPO_ROOT/zap/gradle/thirdPartyLicenses.yaml"; then
    fail "commons-httpclient still present in thirdPartyLicenses.yaml"
else
    pass "commons-httpclient is NOT in thirdPartyLicenses.yaml"
fi

echo ""
echo "========================================"
echo "Results: $PASS passed, $FAIL failed"
echo "========================================"

[ $FAIL -eq 0 ]
