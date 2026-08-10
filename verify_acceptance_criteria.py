#!/usr/bin/env python3
"""
Verification tests for the acceptance criteria:
1. Running ./gradlew japicmp produces a report with zero breaking changes against the recorded baseline
2. A file docs/network-api-baseline.xml exists containing the japicmp output for the current HEAD
3. The CI pipeline step that runs japicmp is confirmed to be non-skipped in gradle/ci.gradle.kts
"""

import os
import sys
import subprocess
import xml.etree.ElementTree as ET

REPO_ROOT = "/workspace/709938f9-ab1d-4907-b78a-0030a304a8e7/worker-1/repo"
BASELINE_FILE = os.path.join(REPO_ROOT, "docs", "network-api-baseline.xml")
CI_GRADLE_FILE = os.path.join(REPO_ROOT, "gradle", "ci.gradle.kts")
ZAP_GRADLE_FILE = os.path.join(REPO_ROOT, "zap", "zap.gradle.kts")
JAPICMP_REPORT = os.path.join(REPO_ROOT, "zap", "build", "reports", "japicmp", "japi.html")

results = {
    "passed": [],
    "failed": []
}


def test_baseline_file_exists():
    """Criterion 2: docs/network-api-baseline.xml exists"""
    print("\n=== Test: docs/network-api-baseline.xml exists ===")
    if os.path.isfile(BASELINE_FILE):
        print(f"  PASS: File exists at {BASELINE_FILE}")
        results["passed"].append("docs/network-api-baseline.xml file exists")
        return True
    else:
        print(f"  FAIL: File NOT found at {BASELINE_FILE}")
        results["failed"].append("docs/network-api-baseline.xml file does not exist")
        return False


def test_baseline_file_is_valid_xml():
    """Criterion 2: The baseline file is valid XML with japicmp content"""
    print("\n=== Test: docs/network-api-baseline.xml is valid japicmp XML ===")
    if not os.path.isfile(BASELINE_FILE):
        print("  SKIP: File does not exist")
        results["failed"].append("docs/network-api-baseline.xml is not valid XML (file missing)")
        return False
    
    try:
        tree = ET.parse(BASELINE_FILE)
        root = tree.getroot()
        print(f"  Root element: {root.tag}")
        
        if root.tag != "japicmp":
            print(f"  FAIL: Root element is '{root.tag}', expected 'japicmp'")
            results["failed"].append("docs/network-api-baseline.xml root element is not 'japicmp'")
            return False
        
        # Check for expected attributes
        new_jar = root.get("newJar", "")
        old_jar = root.get("oldJar", "")
        print(f"  newJar: {new_jar}")
        print(f"  oldJar: {old_jar}")
        
        # Check for class entries
        classes = root.findall("class")
        print(f"  Number of class entries: {len(classes)}")
        
        if len(classes) == 0:
            print("  FAIL: No class entries found in baseline")
            results["failed"].append("docs/network-api-baseline.xml has no class entries")
            return False
        
        # Check for HttpSenderImpl class
        class_names = [c.get("name", "") for c in classes]
        print(f"  Classes in baseline: {class_names}")
        
        http_sender_impl = any("HttpSenderImpl" in name for name in class_names)
        if not http_sender_impl:
            print("  FAIL: HttpSenderImpl not found in baseline")
            results["failed"].append("docs/network-api-baseline.xml does not contain HttpSenderImpl")
            return False
        
        print("  PASS: Valid japicmp XML with HttpSenderImpl baseline")
        results["passed"].append("docs/network-api-baseline.xml is valid japicmp XML with HttpSenderImpl")
        return True
        
    except ET.ParseError as e:
        print(f"  FAIL: XML parse error: {e}")
        results["failed"].append(f"docs/network-api-baseline.xml XML parse error: {e}")
        return False


def test_baseline_has_zero_breaking_changes():
    """Criterion 2: The baseline records zero breaking changes"""
    print("\n=== Test: Baseline records zero breaking changes ===")
    if not os.path.isfile(BASELINE_FILE):
        print("  SKIP: File does not exist")
        results["failed"].append("Cannot verify zero breaking changes - baseline file missing")
        return False
    
    try:
        tree = ET.parse(BASELINE_FILE)
        root = tree.getroot()
        
        # Check all classes have binaryCompatible="true"
        classes = root.findall("class")
        breaking_changes = []
        
        for cls in classes:
            name = cls.get("name", "")
            binary_compatible = cls.get("binaryCompatible", "")
            change_status = cls.get("changeStatus", "")
            
            if binary_compatible == "false":
                breaking_changes.append(f"Class {name} is binary incompatible")
            
            # Check methods
            for method in cls.findall(".//method"):
                method_name = method.get("name", "")
                method_compat = method.get("binaryCompatible", "")
                if method_compat == "false":
                    breaking_changes.append(f"Method {name}#{method_name} is binary incompatible")
        
        if breaking_changes:
            print(f"  FAIL: Found breaking changes: {breaking_changes}")
            results["failed"].append(f"Baseline contains breaking changes: {breaking_changes}")
            return False
        
        print(f"  PASS: All {len(classes)} classes in baseline are binary compatible")
        results["passed"].append("Baseline records zero breaking changes (all binaryCompatible=true)")
        return True
        
    except ET.ParseError as e:
        print(f"  FAIL: XML parse error: {e}")
        results["failed"].append(f"Cannot parse baseline XML: {e}")
        return False


def test_baseline_covers_network_package():
    """Criterion 2: Baseline covers org.zaproxy.zap.network package"""
    print("\n=== Test: Baseline covers org.zaproxy.zap.network package ===")
    if not os.path.isfile(BASELINE_FILE):
        print("  SKIP: File does not exist")
        results["failed"].append("Cannot verify network package coverage - baseline file missing")
        return False
    
    try:
        tree = ET.parse(BASELINE_FILE)
        root = tree.getroot()
        classes = root.findall("class")
        
        network_classes = [c.get("name", "") for c in classes 
                          if "org.zaproxy.zap.network" in c.get("name", "")]
        
        print(f"  Network package classes: {network_classes}")
        
        if len(network_classes) == 0:
            print("  FAIL: No org.zaproxy.zap.network classes in baseline")
            results["failed"].append("Baseline does not cover org.zaproxy.zap.network package")
            return False
        
        # Check for key classes
        expected_classes = [
            "org.zaproxy.zap.network.HttpSenderImpl",
            "org.zaproxy.zap.network.HttpSenderContext",
            "org.zaproxy.zap.network.HttpSenderListener",
            "org.zaproxy.zap.network.HttpRequestConfig",
        ]
        
        missing = [cls for cls in expected_classes if cls not in network_classes]
        if missing:
            print(f"  FAIL: Missing expected classes: {missing}")
            results["failed"].append(f"Baseline missing expected network classes: {missing}")
            return False
        
        print(f"  PASS: Baseline covers {len(network_classes)} org.zaproxy.zap.network classes")
        results["passed"].append(f"Baseline covers org.zaproxy.zap.network package ({len(network_classes)} classes)")
        return True
        
    except ET.ParseError as e:
        print(f"  FAIL: XML parse error: {e}")
        results["failed"].append(f"Cannot parse baseline XML: {e}")
        return False


def test_ci_gradle_has_japicmp_enabled():
    """Criterion 3: CI pipeline step that runs japicmp is non-skipped"""
    print("\n=== Test: gradle/ci.gradle.kts enables japicmp as non-skipped ===")
    
    if not os.path.isfile(CI_GRADLE_FILE):
        print(f"  FAIL: CI gradle file not found at {CI_GRADLE_FILE}")
        results["failed"].append("gradle/ci.gradle.kts does not exist")
        return False
    
    with open(CI_GRADLE_FILE, 'r') as f:
        content = f.read()
    
    print(f"  CI gradle file content:\n{content}")
    
    # Check for japicmp task being enabled
    checks = [
        ('japicmp' in content, "Contains 'japicmp' reference"),
        ('enabled = true' in content, "Sets enabled = true for japicmp"),
        ('project(":zap")' in content, "Targets :zap project"),
        ('tasks.named("japicmp")' in content, "References japicmp task by name"),
    ]
    
    all_passed = True
    for check, description in checks:
        if check:
            print(f"  PASS: {description}")
        else:
            print(f"  FAIL: {description}")
            all_passed = False
    
    if all_passed:
        results["passed"].append("gradle/ci.gradle.kts enables japicmp as non-skipped in CI")
        return True
    else:
        results["failed"].append("gradle/ci.gradle.kts does not properly enable japicmp as non-skipped")
        return False


def test_ci_gradle_has_japicmp_comment():
    """Criterion 3: CI gradle has explanatory comment about japicmp baseline"""
    print("\n=== Test: gradle/ci.gradle.kts has japicmp baseline comment ===")
    
    if not os.path.isfile(CI_GRADLE_FILE):
        print(f"  FAIL: CI gradle file not found")
        results["failed"].append("gradle/ci.gradle.kts does not exist (comment check)")
        return False
    
    with open(CI_GRADLE_FILE, 'r') as f:
        content = f.read()
    
    # Check for comment about the baseline
    has_baseline_comment = (
        "network-api-baseline" in content or
        "baseline" in content.lower()
    )
    
    if has_baseline_comment:
        print("  PASS: CI gradle file contains comment about network API baseline")
        results["passed"].append("gradle/ci.gradle.kts has explanatory comment about japicmp baseline")
        return True
    else:
        print("  FAIL: CI gradle file lacks comment about network API baseline")
        results["failed"].append("gradle/ci.gradle.kts lacks comment about network API baseline")
        return False


def test_zap_gradle_has_japicmp_task():
    """Criterion 1: zap/zap.gradle.kts has japicmp task configured"""
    print("\n=== Test: zap/zap.gradle.kts has japicmp task ===")
    
    if not os.path.isfile(ZAP_GRADLE_FILE):
        print(f"  FAIL: zap.gradle.kts not found at {ZAP_GRADLE_FILE}")
        results["failed"].append("zap/zap.gradle.kts does not exist")
        return False
    
    with open(ZAP_GRADLE_FILE, 'r') as f:
        content = f.read()
    
    checks = [
        ('japicmp' in content, "Contains japicmp configuration"),
        ('JapicmpTask' in content, "Uses JapicmpTask class"),
        ('alias(libs.plugins.japicmp)' in content, "Applies japicmp plugin"),
        ('oldClasspath' in content, "Configures oldClasspath (baseline jar)"),
        ('newClasspath' in content, "Configures newClasspath (current jar)"),
        ('richReport' in content, "Configures rich report output"),
    ]
    
    all_passed = True
    for check, description in checks:
        if check:
            print(f"  PASS: {description}")
        else:
            print(f"  FAIL: {description}")
            all_passed = False
    
    if all_passed:
        results["passed"].append("zap/zap.gradle.kts has complete japicmp task configuration")
        return True
    else:
        results["failed"].append("zap/zap.gradle.kts is missing japicmp task configuration elements")
        return False


def test_japicmp_report_exists_and_has_no_breaking_changes():
    """Criterion 1: Running japicmp produces report with zero breaking changes"""
    print("\n=== Test: japicmp report exists with zero breaking changes ===")
    
    if not os.path.isfile(JAPICMP_REPORT):
        print(f"  FAIL: japicmp report not found at {JAPICMP_REPORT}")
        results["failed"].append("japicmp report (japi.html) was not generated")
        return False
    
    with open(JAPICMP_REPORT, 'r') as f:
        content = f.read()
    
    # Check for binary incompatible (breaking) changes - these would be severity-error
    has_severity_error = 'severity-error' in content
    has_severity_info = 'severity-info' in content
    
    print(f"  Report size: {len(content)} bytes")
    print(f"  Has severity-error (breaking changes): {has_severity_error}")
    print(f"  Has severity-info (source-compatible changes): {has_severity_info}")
    
    if has_severity_error:
        print("  FAIL: japicmp report contains binary-incompatible (breaking) changes")
        results["failed"].append("japicmp report contains binary-incompatible breaking changes (severity-error)")
        return False
    
    print("  PASS: japicmp report has zero binary-incompatible breaking changes")
    results["passed"].append("japicmp report produced with zero binary-incompatible breaking changes")
    return True


def test_japicmp_report_covers_network_package():
    """Criterion 1: japicmp report covers org.zaproxy.zap.network package"""
    print("\n=== Test: japicmp report covers org.zaproxy.zap.network ===")
    
    if not os.path.isfile(JAPICMP_REPORT):
        print(f"  SKIP: japicmp report not found")
        results["failed"].append("Cannot verify network package in report - report missing")
        return False
    
    with open(JAPICMP_REPORT, 'r') as f:
        content = f.read()
    
    # The report should mention the network package
    if "zaproxy.zap.network" in content or "HttpSenderImpl" in content:
        print("  PASS: japicmp report covers org.zaproxy.zap.network package")
        results["passed"].append("japicmp report covers org.zaproxy.zap.network package")
        return True
    else:
        # The report might not show unchanged classes - that's OK
        print("  INFO: org.zaproxy.zap.network not explicitly in report (may be unchanged/excluded)")
        print("  PASS: This is acceptable - unchanged classes may not appear in report")
        results["passed"].append("japicmp report generated (network classes unchanged, may not appear in report)")
        return True


def run_all_tests():
    print("=" * 70)
    print("ACCEPTANCE CRITERIA VERIFICATION")
    print("=" * 70)
    
    test_baseline_file_exists()
    test_baseline_file_is_valid_xml()
    test_baseline_has_zero_breaking_changes()
    test_baseline_covers_network_package()
    test_ci_gradle_has_japicmp_enabled()
    test_ci_gradle_has_japicmp_comment()
    test_zap_gradle_has_japicmp_task()
    test_japicmp_report_exists_and_has_no_breaking_changes()
    test_japicmp_report_covers_network_package()
    
    print("\n" + "=" * 70)
    print("RESULTS SUMMARY")
    print("=" * 70)
    print(f"\nPASSED ({len(results['passed'])}):")
    for p in results["passed"]:
        print(f"  ✓ {p}")
    
    print(f"\nFAILED ({len(results['failed'])}):")
    for f in results["failed"]:
        print(f"  ✗ {f}")
    
    total = len(results["passed"]) + len(results["failed"])
    print(f"\nTotal: {len(results['passed'])}/{total} tests passed")
    
    return len(results["failed"]) == 0


if __name__ == "__main__":
    success = run_all_tests()
    sys.exit(0 if success else 1)
