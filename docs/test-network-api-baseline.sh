#!/bin/bash
# Test script for network API baseline acceptance criteria (TDD)
# Run from repo root: bash docs/test-network-api-baseline.sh

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PASS=0
FAIL=0
ERRORS=()

run_test() {
    local name="$1"
    local result="$2"
    local message="$3"
    if [ "$result" = "0" ]; then
        echo "  PASS: $name"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $name - $message"
        ERRORS+=("$name: $message")
        FAIL=$((FAIL + 1))
    fi
}

echo "=== Network API Baseline Tests ==="
echo ""

# Test 1: docs/network-api-baseline.xml must exist
BASELINE="$REPO_ROOT/docs/network-api-baseline.xml"
if [ -f "$BASELINE" ]; then
    run_test "baseline_file_exists" "0" ""
else
    run_test "baseline_file_exists" "1" \
        "docs/network-api-baseline.xml does not exist. Run './gradlew :zap:japicmpBaseline' to generate it."
fi

# Test 2: baseline file must be valid XML
if [ -f "$BASELINE" ]; then
    python3 -c "
import xml.etree.ElementTree as ET
import sys
try:
    tree = ET.parse('$BASELINE')
    root = tree.getroot()
    if root.tag not in ('japicmp', 'api-baseline'):
        print(f'ERROR: root element is {root.tag!r}, expected japicmp or api-baseline', file=sys.stderr)
        sys.exit(1)
    sys.exit(0)
except Exception as e:
    print(f'ERROR: {e}', file=sys.stderr)
    sys.exit(1)
" 2>/dev/null
    run_test "baseline_is_valid_xml" "$?" \
        "docs/network-api-baseline.xml is not valid XML with root element 'japicmp' or 'api-baseline'"
else
    run_test "baseline_is_valid_xml" "1" \
        "Skipped - baseline file does not exist"
fi

# Test 3: baseline must reference HttpSenderImpl
if [ -f "$BASELINE" ]; then
    if grep -q "HttpSenderImpl" "$BASELINE"; then
        run_test "baseline_contains_HttpSenderImpl" "0" ""
    else
        run_test "baseline_contains_HttpSenderImpl" "1" \
            "Baseline must reference HttpSenderImpl"
    fi
else
    run_test "baseline_contains_HttpSenderImpl" "1" \
        "Skipped - baseline file does not exist"
fi

# Test 4: baseline must reference org.zaproxy.zap.network package
if [ -f "$BASELINE" ]; then
    if grep -q "org.zaproxy.zap.network" "$BASELINE"; then
        run_test "baseline_contains_network_package" "0" ""
    else
        run_test "baseline_contains_network_package" "1" \
            "Baseline must reference org.zaproxy.zap.network package"
    fi
else
    run_test "baseline_contains_network_package" "1" \
        "Skipped - baseline file does not exist"
fi

# Test 5: japicmp.yaml must NOT list org.zaproxy.zap.network in packageExcludes
# (comments mentioning the package are acceptable; only the YAML list entries matter)
JAPICMP_YAML="$REPO_ROOT/zap/gradle/japicmp.yaml"
if [ -f "$JAPICMP_YAML" ]; then
    # Check that no non-comment line in packageExcludes contains org.zaproxy.zap.network
    EXCLUDED=$(python3 -c "
import sys
try:
    import yaml
    with open('$JAPICMP_YAML') as f:
        data = yaml.safe_load(f)
    excludes = data.get('packageExcludes', [])
    network_excluded = [e for e in excludes if 'org.zaproxy.zap.network' in e]
    if network_excluded:
        print('EXCLUDED: ' + ', '.join(network_excluded))
        sys.exit(1)
    sys.exit(0)
except ImportError:
    # Fallback: grep only non-comment lines in the packageExcludes section
    import re
    with open('$JAPICMP_YAML') as f:
        content = f.read()
    # Extract packageExcludes section
    in_section = False
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.startswith('#'):
            continue
        if 'packageExcludes:' in stripped:
            in_section = True
            continue
        if in_section:
            if stripped and not stripped.startswith('-') and ':' in stripped:
                break
            if 'org.zaproxy.zap.network' in stripped and stripped.startswith('-'):
                print('EXCLUDED: ' + stripped)
                sys.exit(1)
    sys.exit(0)
" 2>&1)
    run_test "japicmp_yaml_does_not_exclude_network" "$?" \
        "zap/gradle/japicmp.yaml must NOT exclude org.zaproxy.zap.network in packageExcludes: $EXCLUDED"
else
    run_test "japicmp_yaml_does_not_exclude_network" "1" \
        "zap/gradle/japicmp.yaml does not exist"
fi

# Test 6: gradle/ci.gradle.kts must include japicmp
CI_GRADLE="$REPO_ROOT/gradle/ci.gradle.kts"
if [ -f "$CI_GRADLE" ]; then
    if grep -q "japicmp" "$CI_GRADLE"; then
        run_test "ci_gradle_includes_japicmp" "0" ""
    else
        run_test "ci_gradle_includes_japicmp" "1" \
            "gradle/ci.gradle.kts must include japicmp in the CI pipeline"
    fi
else
    run_test "ci_gradle_includes_japicmp" "1" \
        "gradle/ci.gradle.kts does not exist"
fi

# Test 7: baseline must not contain binaryCompatible="false" (zero breaking changes)
if [ -f "$BASELINE" ]; then
    if grep -q 'binaryCompatible="false"' "$BASELINE"; then
        run_test "baseline_zero_breaking_changes" "1" \
            "Baseline must not contain binaryCompatible=\"false\" - zero breaking changes required"
    else
        run_test "baseline_zero_breaking_changes" "0" ""
    fi
else
    run_test "baseline_zero_breaking_changes" "1" \
        "Skipped - baseline file does not exist"
fi

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="

if [ ${#ERRORS[@]} -gt 0 ]; then
    echo ""
    echo "Failures:"
    for err in "${ERRORS[@]}"; do
        echo "  - $err"
    done
    exit 1
fi

exit 0
