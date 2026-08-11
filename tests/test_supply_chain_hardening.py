"""
Supply chain hardening integration tests.
These tests verify that all acceptance criteria for the supply-chain-hardening
branch are met before merging to main.
"""
import os
import re
import glob
import pytest

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORKFLOWS_DIR = os.path.join(REPO_ROOT, ".github", "workflows")
DOCKER_DIR = os.path.join(REPO_ROOT, "docker")


def read_file(path):
    with open(path, "r") as f:
        return f.read()


def grep_rn(pattern, directory):
    """Return list of (filepath, lineno, line) tuples matching pattern."""
    matches = []
    for root, dirs, files in os.walk(directory):
        for fname in files:
            fpath = os.path.join(root, fname)
            try:
                with open(fpath, "r", errors="replace") as f:
                    for lineno, line in enumerate(f, 1):
                        if re.search(pattern, line):
                            matches.append((fpath, lineno, line.rstrip()))
            except (IOError, OSError):
                pass
    return matches


class TestSetupChalkActionPinned:
    """
    Acceptance criterion:
    `grep -rn 'setup-chalk-action@main' .github/workflows/` returns no output.
    All uses of crashappsec/setup-chalk-action must be pinned to a full 40-char SHA,
    not the floating @main tag.
    """

    def test_no_setup_chalk_action_at_main(self):
        matches = grep_rn(r"setup-chalk-action@main", WORKFLOWS_DIR)
        assert matches == [], (
            "Found 'setup-chalk-action@main' (unpinned) in workflow files:\n"
            + "\n".join(f"  {f}:{n}: {l}" for f, n, l in matches)
        )

    def test_chalk_action_uses_sha_pin(self):
        """All setup-chalk-action references must use a 40-char hex SHA."""
        matches = grep_rn(r"setup-chalk-action@", WORKFLOWS_DIR)
        sha_pattern = re.compile(r"setup-chalk-action@[0-9a-f]{40}")
        bad = [
            (f, n, l)
            for f, n, l in matches
            if not sha_pattern.search(l)
        ]
        assert bad == [], (
            "Found setup-chalk-action references not pinned to a 40-char SHA:\n"
            + "\n".join(f"  {f}:{n}: {l}" for f, n, l in bad)
        )


class TestProvenanceNotDisabled:
    """
    Acceptance criterion:
    `grep -rn 'provenance: false' .github/workflows/` returns no output.
    Provenance must not be explicitly disabled in any workflow.
    """

    def test_no_provenance_false(self):
        matches = grep_rn(r"provenance:\s*false", WORKFLOWS_DIR)
        assert matches == [], (
            "Found 'provenance: false' in workflow files (provenance must be enabled):\n"
            + "\n".join(f"  {f}:{n}: {l}" for f, n, l in matches)
        )


class TestZapNightlyNotLatest:
    """
    Acceptance criterion:
    `grep -rn 'zap-nightly:latest' docker/` returns no output.
    The zaproxy/zap-nightly:latest floating tag must be replaced with a pinned digest.
    """

    def test_no_zap_nightly_latest_in_docker(self):
        matches = grep_rn(r"zap-nightly:latest", DOCKER_DIR)
        assert matches == [], (
            "Found 'zap-nightly:latest' (unpinned) in docker/ files:\n"
            + "\n".join(f"  {f}:{n}: {l}" for f, n, l in matches)
        )

    def test_zap_nightly_uses_digest_if_present(self):
        """If zap-nightly is referenced, it must use a sha256 digest."""
        matches = grep_rn(r"zap-nightly", DOCKER_DIR)
        digest_pattern = re.compile(r"zap-nightly@sha256:[0-9a-f]{64}")
        bad = [
            (f, n, l)
            for f, n, l in matches
            if not digest_pattern.search(l)
        ]
        assert bad == [], (
            "Found zap-nightly references not pinned to a sha256 digest:\n"
            + "\n".join(f"  {f}:{n}: {l}" for f, n, l in bad)
        )


class TestRequirementsTxtPinned:
    """
    Acceptance criterion:
    `grep -E '^zaproxy$|^pyyaml$' docker/requirements.txt` returns no output.
    Both zaproxy and pyyaml must be pinned to specific versions (e.g. zaproxy==X.Y.Z).
    """

    def test_no_unpinned_zaproxy(self):
        req_path = os.path.join(DOCKER_DIR, "requirements.txt")
        content = read_file(req_path)
        lines = content.splitlines()
        unpinned = [l for l in lines if re.match(r"^zaproxy\s*$", l.strip())]
        assert unpinned == [], (
            "Found unpinned 'zaproxy' in docker/requirements.txt. "
            "Pin it to a specific version, e.g. zaproxy==0.3.5"
        )

    def test_no_unpinned_pyyaml(self):
        req_path = os.path.join(DOCKER_DIR, "requirements.txt")
        content = read_file(req_path)
        lines = content.splitlines()
        unpinned = [l for l in lines if re.match(r"^pyyaml\s*$", l.strip(), re.IGNORECASE)]
        assert unpinned == [], (
            "Found unpinned 'pyyaml' in docker/requirements.txt. "
            "Pin it to a specific version, e.g. PyYAML==6.0.2"
        )

    def test_zaproxy_pinned_with_version(self):
        req_path = os.path.join(DOCKER_DIR, "requirements.txt")
        content = read_file(req_path)
        # zaproxy must appear with == version specifier
        has_pinned = any(
            re.match(r"^zaproxy==\S+", l.strip())
            for l in content.splitlines()
        )
        assert has_pinned, (
            "docker/requirements.txt must contain 'zaproxy==<version>' (pinned)"
        )

    def test_pyyaml_pinned_with_version(self):
        req_path = os.path.join(DOCKER_DIR, "requirements.txt")
        content = read_file(req_path)
        # PyYAML (case-insensitive) must appear with == version specifier
        has_pinned = any(
            re.match(r"^pyyaml==\S+", l.strip(), re.IGNORECASE)
            for l in content.splitlines()
        )
        assert has_pinned, (
            "docker/requirements.txt must contain 'PyYAML==<version>' (pinned)"
        )


class TestWorkflowFilesExist:
    """Sanity checks that the four release workflow files exist."""

    @pytest.mark.parametrize("workflow", [
        "release-live-docker.yml",
        "release-main-docker.yml",
        "release-weekly-docker.yml",
        "release-weekly.yml",
    ])
    def test_release_workflow_exists(self, workflow):
        path = os.path.join(WORKFLOWS_DIR, workflow)
        assert os.path.exists(path), f"Release workflow not found: {path}"


class TestDockerfilesPipInstallUsesRequirements:
    """
    Dockerfiles that install Python packages should reference requirements.txt
    rather than listing packages inline, OR the inline packages must match
    the pinned versions in requirements.txt.
    This test checks that the Dockerfiles do not install zaproxy or pyyaml
    without version pins inline.
    """

    @pytest.mark.parametrize("dockerfile", [
        "Dockerfile-live",
        "Dockerfile-stable",
        "Dockerfile-weekly",
    ])
    def test_dockerfile_no_unpinned_zaproxy_inline(self, dockerfile):
        path = os.path.join(DOCKER_DIR, dockerfile)
        content = read_file(path)
        # Look for pip install lines that include 'zaproxy' without a version
        pip_lines = [
            l.strip() for l in content.splitlines()
            if "pip" in l and "zaproxy" in l
        ]
        bad = [l for l in pip_lines if re.search(r"\bzaproxy\b(?!=)", l)]
        assert bad == [], (
            f"{dockerfile} has inline pip install of 'zaproxy' without version pin:\n"
            + "\n".join(f"  {l}" for l in bad)
        )

    @pytest.mark.parametrize("dockerfile", [
        "Dockerfile-live",
        "Dockerfile-stable",
        "Dockerfile-weekly",
    ])
    def test_dockerfile_no_unpinned_pyyaml_inline(self, dockerfile):
        path = os.path.join(DOCKER_DIR, dockerfile)
        content = read_file(path)
        pip_lines = [
            l.strip() for l in content.splitlines()
            if "pip" in l and re.search(r"pyyaml", l, re.IGNORECASE)
        ]
        bad = [l for l in pip_lines if re.search(r"\bpyyaml\b(?!=)", l, re.IGNORECASE)]
        assert bad == [], (
            f"{dockerfile} has inline pip install of 'pyyaml' without version pin:\n"
            + "\n".join(f"  {l}" for l in bad)
        )
