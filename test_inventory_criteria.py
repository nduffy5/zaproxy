#!/usr/bin/env python3
"""
Verification tests for acceptance criteria:
  1. grep -rn 'org.apache.commons.httpclient' network/ matches inventory document
  2. Same grep against extension/ and view/ produces zero results
  3. docs/network-migration-inventory.md exists listing every affected class and types
"""

import subprocess
import os
import sys

REPO_ROOT = "/workspace/709938f9-ab1d-4907-b78a-0030a304a8e7/worker-0/repo"
NETWORK_DIR = os.path.join(REPO_ROOT, "zap/src/main/java/org/zaproxy/zap/network")
EXTENSION_DIR = os.path.join(REPO_ROOT, "zap/src/main/java/org/zaproxy/zap/extension")
VIEW_DIR = os.path.join(REPO_ROOT, "zap/src/main/java/org/zaproxy/zap/view")
INVENTORY_FILE = os.path.join(REPO_ROOT, "docs/network-migration-inventory.md")

PASS = []
FAIL = []


def run_grep(directory):
    """Run grep for commons-httpclient in a directory."""
    result = subprocess.run(
        ["grep", "-rn", "org.apache.commons.httpclient", directory],
        capture_output=True, text=True
    )
    return result.stdout.strip()


def test_inventory_file_exists():
    """CRITERION 3: docs/network-migration-inventory.md exists"""
    if os.path.isfile(INVENTORY_FILE):
        PASS.append("docs/network-migration-inventory.md exists")
        return True
    else:
        FAIL.append("docs/network-migration-inventory.md does NOT exist")
        return False


def test_network_grep_matches_inventory():
    """CRITERION 1: grep output from network/ matches inventory document"""
    if not os.path.isfile(INVENTORY_FILE):
        FAIL.append("Cannot verify criterion 1: inventory file missing")
        return False

    with open(INVENTORY_FILE, "r") as f:
        inventory_content = f.read()

    grep_output = run_grep(NETWORK_DIR)
    if not grep_output:
        FAIL.append("No grep results found in network/ - unexpected")
        return False

    # Extract unique filenames from grep output
    grep_files = set()
    for line in grep_output.splitlines():
        # line format: /full/path/to/File.java:lineno:content
        parts = line.split(":")
        if parts:
            filepath = parts[0]
            filename = os.path.basename(filepath)
            grep_files.add(filename)

    print(f"\nFiles found by grep in network/: {sorted(grep_files)}")

    # Check each file is in the inventory
    all_found = True
    for filename in sorted(grep_files):
        classname = filename.replace(".java", "")
        if classname in inventory_content or filename in inventory_content:
            PASS.append(f"Inventory documents {filename}")
        else:
            FAIL.append(f"Inventory MISSING {filename}")
            all_found = False

    # Also check that the inventory doesn't claim classes that don't exist in grep
    # (i.e., no phantom entries)
    expected_classes = [
        "DefaultHttpRedirectionValidator",
        "HttpRedirectionValidator",
        "ZapCookieSpec",
        "ZapDeleteMethod",
        "ZapHeadMethod",
        "ZapHttpParser",
        "ZapNTLMEngineImpl",
        "ZapNTLMScheme",
        "ZapOptionsMethod",
        "ZapPostMethod",
        "ZapPutMethod",
        "ZapTraceMethod",
    ]

    for cls in expected_classes:
        if cls in inventory_content:
            if cls in grep_files or f"{cls}.java" in grep_files:
                PASS.append(f"Inventory entry for {cls} matches grep result")
            else:
                # Check if the class is actually in grep output
                if cls in grep_output:
                    PASS.append(f"Inventory entry for {cls} matches grep result")
                else:
                    FAIL.append(f"Inventory lists {cls} but grep doesn't find it")
                    all_found = False
        else:
            FAIL.append(f"Inventory missing section for {cls}")
            all_found = False

    if all_found:
        PASS.append("CRITERION 1: grep output from network/ matches inventory document")
    return all_found


def test_scope_boundary_extension():
    """CRITERION 2a: grep against extension/ produces zero results"""
    grep_output = run_grep(EXTENSION_DIR)
    count = len(grep_output.splitlines()) if grep_output else 0
    if count == 0:
        PASS.append("CRITERION 2a: extension/ has zero commons-httpclient references")
        return True
    else:
        FAIL.append(f"CRITERION 2a FAILED: extension/ has {count} commons-httpclient references (expected 0)")
        print(f"\nExtension grep results ({count} lines):")
        for line in grep_output.splitlines()[:5]:
            print(f"  {line}")
        if count > 5:
            print(f"  ... and {count - 5} more")
        return False


def test_scope_boundary_view():
    """CRITERION 2b: grep against view/ produces zero results"""
    grep_output = run_grep(VIEW_DIR)
    count = len(grep_output.splitlines()) if grep_output else 0
    if count == 0:
        PASS.append("CRITERION 2b: view/ has zero commons-httpclient references")
        return True
    else:
        FAIL.append(f"CRITERION 2b FAILED: view/ has {count} commons-httpclient references (expected 0)")
        print(f"\nView grep results ({count} lines):")
        for line in grep_output.splitlines():
            print(f"  {line}")
        return False


def test_inventory_lists_specific_types():
    """CRITERION 3: Inventory lists specific Commons HttpClient types for each class"""
    if not os.path.isfile(INVENTORY_FILE):
        FAIL.append("Cannot verify criterion 3: inventory file missing")
        return False

    with open(INVENTORY_FILE, "r") as f:
        inventory_content = f.read()

    # These are the specific types that must be documented
    required_types = [
        "org.apache.commons.httpclient.URI",
        "org.apache.commons.httpclient.Cookie",
        "org.apache.commons.httpclient.cookie.CookieSpecBase",
        "org.apache.commons.httpclient.cookie.MalformedCookieException",
        "org.apache.commons.httpclient.Header",
        "org.apache.commons.httpclient.HttpState",
        "org.apache.commons.httpclient.HttpConnection",
        "org.apache.commons.httpclient.HttpParser",
        "org.apache.commons.httpclient.ProtocolException",
        "org.apache.commons.httpclient.params.HttpMethodParams",
        "org.apache.commons.httpclient.auth.AuthenticationException",
        "org.apache.commons.httpclient.Credentials",
        "org.apache.commons.httpclient.HttpMethod",
        "org.apache.commons.httpclient.NTCredentials",
        "org.apache.commons.httpclient.auth.AuthChallengeParser",
        "org.apache.commons.httpclient.auth.AuthScheme",
        "org.apache.commons.httpclient.auth.MalformedChallengeException",
        "org.apache.commons.httpclient.methods.EntityEnclosingMethod",
        "org.apache.commons.httpclient.methods.PostMethod",
        "org.apache.commons.httpclient.methods.PutMethod",
    ]

    all_found = True
    for type_name in required_types:
        if type_name in inventory_content:
            PASS.append(f"Inventory documents type: {type_name}")
        else:
            FAIL.append(f"Inventory MISSING type: {type_name}")
            all_found = False

    if all_found:
        PASS.append("CRITERION 3: Inventory lists all specific Commons HttpClient types")
    return all_found


def test_inventory_has_raw_grep_section():
    """CRITERION 1 supplement: Inventory contains the raw grep output section"""
    if not os.path.isfile(INVENTORY_FILE):
        FAIL.append("Cannot verify: inventory file missing")
        return False

    with open(INVENTORY_FILE, "r") as f:
        inventory_content = f.read()

    # The inventory should contain the raw grep output
    if "Raw `grep` Output" in inventory_content or "Raw grep Output" in inventory_content:
        PASS.append("Inventory contains raw grep output section")
        return True
    else:
        FAIL.append("Inventory missing raw grep output section")
        return False


def main():
    print("=" * 60)
    print("VERIFICATION: network-migration-inventory.md acceptance criteria")
    print("=" * 60)

    print("\n--- Test 1: Inventory file exists ---")
    test_inventory_file_exists()

    print("\n--- Test 2: grep output matches inventory ---")
    test_network_grep_matches_inventory()

    print("\n--- Test 3: Scope boundary (extension/) ---")
    test_scope_boundary_extension()

    print("\n--- Test 4: Scope boundary (view/) ---")
    test_scope_boundary_view()

    print("\n--- Test 5: Inventory lists specific types ---")
    test_inventory_lists_specific_types()

    print("\n--- Test 6: Inventory has raw grep section ---")
    test_inventory_has_raw_grep_section()

    print("\n" + "=" * 60)
    print("RESULTS")
    print("=" * 60)
    print(f"\nPASSED ({len(PASS)}):")
    for p in PASS:
        print(f"  ✓ {p}")

    print(f"\nFAILED ({len(FAIL)}):")
    for f in FAIL:
        print(f"  ✗ {f}")

    print(f"\nTotal: {len(PASS)} passed, {len(FAIL)} failed")

    if FAIL:
        sys.exit(1)
    else:
        sys.exit(0)


if __name__ == "__main__":
    main()
