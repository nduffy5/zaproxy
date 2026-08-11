"""
Tests to verify acceptance criteria for pinned zaproxy and pyyaml versions
in docker/requirements.txt and docker/requirements.lock.
"""

import re
import subprocess
import sys
import os
import pytest

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REQUIREMENTS_TXT = os.path.join(REPO_ROOT, "docker", "requirements.txt")
REQUIREMENTS_LOCK = os.path.join(REPO_ROOT, "docker", "requirements.lock")


def read_file(path):
    with open(path, "r") as f:
        return f.read()


# ── Criterion 1 ──────────────────────────────────────────────────────────────
# docker/requirements.txt contains zaproxy==<X.Y.Z> and pyyaml==<A.B.C>
# with no bare package names.

class TestRequirementsTxt:

    def test_requirements_txt_exists(self):
        assert os.path.isfile(REQUIREMENTS_TXT), \
            f"docker/requirements.txt not found at {REQUIREMENTS_TXT}"

    def test_zaproxy_is_pinned(self):
        content = read_file(REQUIREMENTS_TXT)
        # Must match zaproxy==X.Y.Z (with at least one dot-separated version component)
        assert re.search(r'^zaproxy==\d+\.\d+', content, re.MULTILINE), \
            "zaproxy is not pinned with a version in docker/requirements.txt"

    def test_pyyaml_is_pinned(self):
        content = read_file(REQUIREMENTS_TXT)
        assert re.search(r'^pyyaml==\d+\.\d+', content, re.MULTILINE | re.IGNORECASE), \
            "pyyaml is not pinned with a version in docker/requirements.txt"

    def test_no_bare_zaproxy(self):
        """Criterion 4: grep -E '^zaproxy$' docker/requirements.txt returns no output."""
        content = read_file(REQUIREMENTS_TXT)
        bare = re.findall(r'^zaproxy$', content, re.MULTILINE)
        assert bare == [], \
            f"Bare unpinned 'zaproxy' found in docker/requirements.txt: {bare}"

    def test_no_bare_pyyaml(self):
        """Criterion 4: grep -E '^pyyaml$' docker/requirements.txt returns no output."""
        content = read_file(REQUIREMENTS_TXT)
        bare = re.findall(r'^pyyaml$', content, re.MULTILINE | re.IGNORECASE)
        assert bare == [], \
            f"Bare unpinned 'pyyaml' found in docker/requirements.txt: {bare}"


# ── Criterion 2 ──────────────────────────────────────────────────────────────
# docker/requirements.lock exists with --hash=sha256:... entries for every
# package and its transitive dependencies.

class TestRequirementsLock:

    def test_requirements_lock_exists(self):
        assert os.path.isfile(REQUIREMENTS_LOCK), \
            f"docker/requirements.lock not found at {REQUIREMENTS_LOCK}"

    def test_lock_contains_hash_entries(self):
        content = read_file(REQUIREMENTS_LOCK)
        hashes = re.findall(r'--hash=sha256:[a-f0-9]{64}', content)
        assert len(hashes) > 0, \
            "docker/requirements.lock contains no --hash=sha256:... entries"

    def test_lock_contains_zaproxy_with_hash(self):
        content = read_file(REQUIREMENTS_LOCK)
        # zaproxy entry must appear and be followed by at least one hash
        assert re.search(r'zaproxy==\d+\.\d+.*--hash=sha256:', content, re.DOTALL), \
            "zaproxy entry with hash not found in docker/requirements.lock"

    def test_lock_contains_pyyaml_with_hash(self):
        content = read_file(REQUIREMENTS_LOCK)
        assert re.search(r'pyyaml==\d+\.\d+.*--hash=sha256:', content, re.DOTALL | re.IGNORECASE), \
            "pyyaml entry with hash not found in docker/requirements.lock"

    def test_lock_contains_transitive_deps_with_hashes(self):
        """Every package block in the lock file should have at least one hash."""
        content = read_file(REQUIREMENTS_LOCK)
        # Find all package==version lines (not comment lines)
        packages = re.findall(r'^(\S+==\S+)', content, re.MULTILINE)
        # Filter out lines that are part of hash continuations
        packages = [p for p in packages if not p.startswith('--')]
        assert len(packages) >= 2, \
            "Lock file should contain at least zaproxy and pyyaml entries"
        # Verify each package block has at least one hash
        for pkg in packages:
            pkg_name = pkg.split('==')[0].rstrip(' \\')
            assert re.search(
                rf'{re.escape(pkg_name)}.*--hash=sha256:', content, re.DOTALL | re.IGNORECASE
            ), f"Package '{pkg_name}' in lock file has no --hash=sha256: entry"


# ── Criterion 3 ──────────────────────────────────────────────────────────────
# pip install --dry-run --require-hashes -r docker/requirements.lock exits 0
# in a clean virtual environment.

class TestPipDryRun:

    def test_pip_dry_run_with_require_hashes_exits_zero(self):
        """
        Run pip install --dry-run --require-hashes against the lock file
        and assert exit code is 0.
        """
        result = subprocess.run(
            [sys.executable, "-m", "pip", "install",
             "--dry-run", "--require-hashes",
             "-r", REQUIREMENTS_LOCK],
            capture_output=True,
            text=True,
        )
        assert result.returncode == 0, (
            f"pip install --dry-run --require-hashes -r docker/requirements.lock "
            f"failed with exit code {result.returncode}.\n"
            f"STDOUT: {result.stdout}\n"
            f"STDERR: {result.stderr}"
        )


# ── Criterion 4 (shell-level) ─────────────────────────────────────────────────
# grep -E '^zaproxy$|^pyyaml$' docker/requirements.txt returns no output.

class TestGrepBareNames:

    def test_grep_bare_names_returns_no_output(self):
        result = subprocess.run(
            ["grep", "-E", r"^zaproxy$|^pyyaml$", REQUIREMENTS_TXT],
            capture_output=True,
            text=True,
        )
        # grep exits 1 when no match is found (which is what we want)
        assert result.returncode == 1, (
            f"grep found bare unpinned package names in docker/requirements.txt:\n"
            f"{result.stdout}"
        )
        assert result.stdout.strip() == "", (
            f"Expected no output from grep, but got:\n{result.stdout}"
        )
