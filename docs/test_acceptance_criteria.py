#!/usr/bin/env python3
"""
Acceptance criteria tests for: Capture HttpSenderImpl API baseline with japicmp

Criteria:
1. Running `./gradlew japicmp` (or equivalent task in zap/zap.gradle.kts) produces a report
   with zero breaking changes against the recorded baseline.
2. A file docs/network-api-baseline.xml (or .json) exists containing the japicmp output
   for the current HEAD.
3. The CI pipeline step that runs japicmp is confirmed to be non-skipped in gradle/ci.gradle.kts.
"""

import os
import sys
import xml.etree.ElementTree as ET

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

PASS = 0
FAIL = 0
failures = []


def run_test(name, condition, failure_msg):
    global PASS, FAIL
    if condition:
        print(f"  PASS: {name}")
        PASS += 1
    else:
        print(f"  FAIL: {name} — {failure_msg}")
        FAIL += 1
        failures.append(f"{name}: {failure_msg}")


# ─────────────────────────────────────────────────────────────────────────────
# CRITERION 2: docs/network-api-baseline.xml exists
# ─────────────────────────────────────────────────────────────────────────────
print("\n=== Criterion 2: Baseline file exists ===")

baseline_path = os.path.join(REPO_ROOT, "docs", "network-api-baseline.xml")
baseline_exists = os.path.isfile(baseline_path)

run_test(
    "baseline_file_exists",
    baseline_exists,
    "docs/network-api-baseline.xml does not exist",
)

# Parse the XML once for subsequent tests
root = None
if baseline_exists:
    try:
        tree = ET.parse(baseline_path)
        root = tree.getroot()
        run_test(
            "baseline_is_valid_xml",
            root.tag in ("japicmp", "api-baseline"),
            f"Root element is '{root.tag}', expected 'japicmp' or 'api-baseline'",
        )
    except ET.ParseError as e:
        run_test("baseline_is_valid_xml", False, f"XML parse error: {e}")
else:
    run_test("baseline_is_valid_xml", False, "Skipped — baseline file missing")

# Must reference HttpSenderImpl
if baseline_exists:
    with open(baseline_path) as f:
        content = f.read()
    run_test(
        "baseline_references_HttpSenderImpl",
        "HttpSenderImpl" in content,
        "Baseline does not reference HttpSenderImpl",
    )
    run_test(
        "baseline_references_network_package",
        "org.zaproxy.zap.network" in content,
        "Baseline does not reference org.zaproxy.zap.network",
    )
else:
    run_test("baseline_references_HttpSenderImpl", False, "Skipped — baseline file missing")
    run_test("baseline_references_network_package", False, "Skipped — baseline file missing")

# ─────────────────────────────────────────────────────────────────────────────
# CRITERION 1: Zero breaking changes in the baseline
# ─────────────────────────────────────────────────────────────────────────────
print("\n=== Criterion 1: Zero breaking changes in baseline ===")

if baseline_exists and root is not None:
    # Check that no class/method/field has binaryCompatible="false"
    breaking = []
    for elem in root.iter():
        if elem.get("binaryCompatible") == "false":
            fqn = elem.get("fullyQualifiedName") or elem.get("name") or elem.tag
            breaking.append(fqn)

    run_test(
        "baseline_zero_breaking_changes",
        len(breaking) == 0,
        f"Found {len(breaking)} element(s) with binaryCompatible=false: {breaking[:5]}",
    )

    # Also verify the japicmp task is wired into 'check' in zap/zap.gradle.kts
    zap_gradle = os.path.join(REPO_ROOT, "zap", "zap.gradle.kts")
    if os.path.isfile(zap_gradle):
        with open(zap_gradle) as f:
            zap_content = f.read()
        run_test(
            "japicmp_task_defined_in_zap_gradle",
            "japicmp" in zap_content and "JapicmpTask" in zap_content,
            "zap/zap.gradle.kts does not define a japicmp task using JapicmpTask",
        )
        run_test(
            "japicmp_wired_into_check",
            "dependsOn(japicmp)" in zap_content,
            "zap/zap.gradle.kts does not wire japicmp into the 'check' lifecycle",
        )
        run_test(
            "japicmp_uses_baseversion_property",
            "zap.japicmp.baseversion" in zap_content,
            "zap/zap.gradle.kts does not use 'zap.japicmp.baseversion' property",
        )
    else:
        run_test("japicmp_task_defined_in_zap_gradle", False, "zap/zap.gradle.kts not found")
        run_test("japicmp_wired_into_check", False, "zap/zap.gradle.kts not found")
        run_test("japicmp_uses_baseversion_property", False, "zap/zap.gradle.kts not found")

    # Verify zap/gradle.properties has the baseversion set
    zap_props = os.path.join(REPO_ROOT, "zap", "gradle.properties")
    if os.path.isfile(zap_props):
        with open(zap_props) as f:
            props_content = f.read()
        run_test(
            "baseversion_property_set",
            "zap.japicmp.baseversion=" in props_content,
            "zap/gradle.properties does not set zap.japicmp.baseversion",
        )
    else:
        run_test("baseversion_property_set", False, "zap/gradle.properties not found")

    # Verify japicmp.yaml does NOT exclude org.zaproxy.zap.network
    japicmp_yaml = os.path.join(REPO_ROOT, "zap", "gradle", "japicmp.yaml")
    if os.path.isfile(japicmp_yaml):
        with open(japicmp_yaml) as f:
            yaml_content = f.read()
        # Parse manually: look for non-comment lines in packageExcludes that mention the network package
        in_package_excludes = False
        network_excluded = False
        for line in yaml_content.splitlines():
            stripped = line.strip()
            if stripped.startswith("#"):
                continue
            if "packageExcludes:" in stripped:
                in_package_excludes = True
                continue
            if in_package_excludes:
                if stripped and not stripped.startswith("-") and ":" in stripped:
                    break  # left the packageExcludes section
                if stripped.startswith("-") and "org.zaproxy.zap.network" in stripped:
                    network_excluded = True
                    break
        run_test(
            "japicmp_yaml_does_not_exclude_network_package",
            not network_excluded,
            "zap/gradle/japicmp.yaml excludes org.zaproxy.zap.network in packageExcludes",
        )
    else:
        run_test(
            "japicmp_yaml_does_not_exclude_network_package",
            False,
            "zap/gradle/japicmp.yaml not found",
        )

else:
    for name in [
        "baseline_zero_breaking_changes",
        "japicmp_task_defined_in_zap_gradle",
        "japicmp_wired_into_check",
        "japicmp_uses_baseversion_property",
        "baseversion_property_set",
        "japicmp_yaml_does_not_exclude_network_package",
    ]:
        run_test(name, False, "Skipped — baseline file missing or invalid")

# ─────────────────────────────────────────────────────────────────────────────
# CRITERION 3: CI pipeline step that runs japicmp is non-skipped
# ─────────────────────────────────────────────────────────────────────────────
print("\n=== Criterion 3: CI pipeline includes japicmp (non-skipped) ===")

ci_gradle = os.path.join(REPO_ROOT, "gradle", "ci.gradle.kts")
if os.path.isfile(ci_gradle):
    with open(ci_gradle) as f:
        ci_content = f.read()

    run_test(
        "ci_gradle_exists",
        True,
        "",
    )
    run_test(
        "ci_gradle_references_japicmp",
        "japicmp" in ci_content,
        "gradle/ci.gradle.kts does not reference japicmp",
    )
    run_test(
        "ci_gradle_sets_enabled_true",
        "enabled = true" in ci_content,
        "gradle/ci.gradle.kts does not set enabled = true for japicmp (non-skipped requirement)",
    )
    # Must NOT set enabled = false for japicmp
    # Check that there's no 'enabled = false' in the japicmp block
    # Simple heuristic: if 'enabled = false' appears, it must not be in a japicmp context
    lines = ci_content.splitlines()
    in_japicmp_block = False
    enabled_false_in_japicmp = False
    for line in lines:
        if "japicmp" in line:
            in_japicmp_block = True
        if in_japicmp_block and "enabled = false" in line:
            enabled_false_in_japicmp = True
            break
        # Reset block detection on closing brace at column 0
        if in_japicmp_block and line.strip() == "}" and not line.startswith(" "):
            in_japicmp_block = False

    run_test(
        "ci_gradle_japicmp_not_disabled",
        not enabled_false_in_japicmp,
        "gradle/ci.gradle.kts sets enabled = false for japicmp (task would be skipped)",
    )
else:
    run_test("ci_gradle_exists", False, "gradle/ci.gradle.kts does not exist")
    run_test("ci_gradle_references_japicmp", False, "gradle/ci.gradle.kts does not exist")
    run_test("ci_gradle_sets_enabled_true", False, "gradle/ci.gradle.kts does not exist")
    run_test("ci_gradle_japicmp_not_disabled", False, "gradle/ci.gradle.kts does not exist")

# ─────────────────────────────────────────────────────────────────────────────
# Summary
# ─────────────────────────────────────────────────────────────────────────────
print(f"\n=== Results: {PASS} passed, {FAIL} failed ===")
if failures:
    print("\nFailures:")
    for f in failures:
        print(f"  - {f}")
    sys.exit(1)
else:
    print("All acceptance criteria met.")
    sys.exit(0)
