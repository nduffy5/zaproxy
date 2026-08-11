"""
TDD tests for docker/requirements.txt pinning and lockfile generation.
Run with: python3 -m pytest docker/test_requirements.py -v
"""
import os
import re
import subprocess
import sys

DOCKER_DIR = os.path.dirname(os.path.abspath(__file__))
REQUIREMENTS_TXT = os.path.join(DOCKER_DIR, "requirements.txt")
REQUIREMENTS_LOCK = os.path.join(DOCKER_DIR, "requirements.lock")


def read_requirements_txt():
    with open(REQUIREMENTS_TXT) as f:
        return f.read()


def read_requirements_lock():
    with open(REQUIREMENTS_LOCK) as f:
        return f.read()


# ---------------------------------------------------------------------------
# Tests for requirements.txt
# ---------------------------------------------------------------------------

def test_zaproxy_is_pinned():
    """requirements.txt must contain zaproxy==X.Y.Z (not bare 'zaproxy')."""
    content = read_requirements_txt()
    assert re.search(r'^zaproxy==\d+\.\d+\.\d+', content, re.MULTILINE), (
        "zaproxy must be pinned with ==X.Y.Z in requirements.txt"
    )


def test_pyyaml_is_pinned():
    """requirements.txt must contain pyyaml==A.B.C (not bare 'pyyaml')."""
    content = read_requirements_txt()
    assert re.search(r'^pyyaml==\d+\.\d+', content, re.MULTILINE | re.IGNORECASE), (
        "pyyaml must be pinned with ==A.B.C in requirements.txt"
    )


def test_no_bare_zaproxy():
    """Bare 'zaproxy' (without version pin) must not appear in requirements.txt."""
    content = read_requirements_txt()
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.lower() == 'zaproxy':
            raise AssertionError(
                "Bare unpinned 'zaproxy' found in requirements.txt"
            )


def test_no_bare_pyyaml():
    """Bare 'pyyaml' (without version pin) must not appear in requirements.txt."""
    content = read_requirements_txt()
    for line in content.splitlines():
        stripped = line.strip()
        if stripped.lower() == 'pyyaml':
            raise AssertionError(
                "Bare unpinned 'pyyaml' found in requirements.txt"
            )


def test_grep_no_bare_names():
    """grep -E '^zaproxy$|^pyyaml$' docker/requirements.txt returns no output."""
    result = subprocess.run(
        ['grep', '-E', '^zaproxy$|^pyyaml$', REQUIREMENTS_TXT],
        capture_output=True, text=True
    )
    assert result.stdout == '', (
        f"Bare unpinned package names found: {result.stdout!r}"
    )


# ---------------------------------------------------------------------------
# Tests for requirements.lock
# ---------------------------------------------------------------------------

def test_lockfile_exists():
    """docker/requirements.lock must exist."""
    assert os.path.isfile(REQUIREMENTS_LOCK), (
        "docker/requirements.lock does not exist"
    )


def test_lockfile_contains_hashes():
    """requirements.lock must contain --hash=sha256: entries."""
    content = read_requirements_lock()
    assert '--hash=sha256:' in content, (
        "requirements.lock must contain --hash=sha256: entries"
    )


def test_lockfile_contains_zaproxy():
    """requirements.lock must pin zaproxy."""
    content = read_requirements_lock()
    assert re.search(r'zaproxy==\d+\.\d+\.\d+', content), (
        "requirements.lock must contain a pinned zaproxy entry"
    )


def test_lockfile_contains_pyyaml():
    """requirements.lock must pin pyyaml."""
    content = read_requirements_lock()
    assert re.search(r'[Pp][Yy][Yy][Aa][Mm][Ll]==\d+\.\d+', content), (
        "requirements.lock must contain a pinned PyYAML entry"
    )


def test_lockfile_dry_run_install():
    """pip install --dry-run --require-hashes -r requirements.lock exits 0."""
    result = subprocess.run(
        [sys.executable, '-m', 'pip', 'install',
         '--dry-run', '--require-hashes', '-r', REQUIREMENTS_LOCK],
        capture_output=True, text=True
    )
    assert result.returncode == 0, (
        f"pip install --dry-run --require-hashes failed:\n"
        f"STDOUT: {result.stdout}\nSTDERR: {result.stderr}"
    )
