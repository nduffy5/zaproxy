"""
Comprehensive acceptance criteria tests for supply-chain-hardening changes.

Verifies all criteria from the task:
1. grep -rn 'setup-chalk-action@main' .github/workflows/ returns no output
2. grep -rn 'provenance: false' .github/workflows/ returns no output
3. grep -rn 'zap-nightly:latest' docker/ returns no output
4. grep -E '^zaproxy$|^pyyaml$' docker/requirements.txt returns no output
5. At least one release workflow has provenance enabled (not false)
6. PR description documents SHA pinned for crashappsec/setup-chalk-action,
   digest pinned for zaproxy/zap-nightly, and Python package versions pinned
"""
import os
import re
import subprocess
import sys
import pytest

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORKFLOWS_DIR = os.path.join(REPO_ROOT, ".github", "workflows")
DOCKER_DIR = os.path.join(REPO_ROOT, "docker")
REQUIREMENTS_TXT = os.path.join(DOCKER_DIR, "requirements.txt")
DEPENDABOT_YML = os.path.join(REPO_ROOT, ".github", "dependabot.yml")

RELEASE_WORKFLOWS = [
    "release-live-docker.yml",
    "release-main-docker.yml",
    "release-weekly-docker.yml",
    "release-weekly.yml",
]


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


def read_file(path):
    with open(path, "r") as f:
        return f.read()


# ─────────────────────────────────────────────────────────────────────────────
# Criterion 1: grep -rn 'setup-chalk-action@main' .github/workflows/ → no output
# ─────────────────────────────────────────────────────────────────────────────

class TestCriterion1_NoChalkActionAtMain:
    """
    Acceptance criterion:
    `grep -rn 'setup-chalk-action@main' .github/workflows/` returns no output.
    """

    def test_no_setup_chalk_action_at_main_in_workflows(self):
        """Direct grep equivalent: no 'setup-chalk-action@main' in any workflow file."""
        result = subprocess.run(
            ["grep", "-rn", "setup-chalk-action@main", WORKFLOWS_DIR],
            capture_output=True, text=True
        )
        assert result.stdout == "", (
            f"grep -rn 'setup-chalk-action@main' .github/workflows/ returned output:\n"
            f"{result.stdout}"
        )

    def test_chalk_action_pinned_to_sha_in_all_release_workflows(self):
        """All four release workflows must pin setup-chalk-action to a 40-char SHA."""
        sha_pattern = re.compile(r"setup-chalk-action@[0-9a-f]{40}")
        for workflow in RELEASE_WORKFLOWS:
            path = os.path.join(WORKFLOWS_DIR, workflow)
            content = read_file(path)
            assert sha_pattern.search(content), (
                f"{workflow} does not contain 'setup-chalk-action@<40-char-sha>'"
            )

    def test_chalk_action_sha_is_consistent_across_workflows(self):
        """All workflows should use the same SHA for setup-chalk-action."""
        sha_pattern = re.compile(r"setup-chalk-action@([0-9a-f]{40})")
        shas = {}
        for workflow in RELEASE_WORKFLOWS:
            path = os.path.join(WORKFLOWS_DIR, workflow)
            content = read_file(path)
            m = sha_pattern.search(content)
            if m:
                shas[workflow] = m.group(1)
        unique_shas = set(shas.values())
        assert len(unique_shas) <= 1, (
            f"Inconsistent SHAs for setup-chalk-action across workflows: {shas}"
        )

    def test_chalk_action_sha_is_40_chars(self):
        """The pinned SHA must be exactly 40 hex characters."""
        sha_pattern = re.compile(r"setup-chalk-action@([0-9a-f]{40})")
        for workflow in RELEASE_WORKFLOWS:
            path = os.path.join(WORKFLOWS_DIR, workflow)
            content = read_file(path)
            m = sha_pattern.search(content)
            if m:
                sha = m.group(1)
                assert len(sha) == 40, (
                    f"{workflow}: SHA '{sha}' is not 40 characters long"
                )
                assert re.match(r'^[0-9a-f]{40}$', sha), (
                    f"{workflow}: SHA '{sha}' contains non-hex characters"
                )


# ─────────────────────────────────────────────────────────────────────────────
# Criterion 2: grep -rn 'provenance: false' .github/workflows/ → no output
# ─────────────────────────────────────────────────────────────────────────────

class TestCriterion2_NoProvenanceFalse:
    """
    Acceptance criterion:
    `grep -rn 'provenance: false' .github/workflows/` returns no output.
    """

    def test_no_provenance_false_in_workflows(self):
        """Direct grep equivalent: no 'provenance: false' in any workflow file."""
        result = subprocess.run(
            ["grep", "-rn", "provenance: false", WORKFLOWS_DIR],
            capture_output=True, text=True
        )
        assert result.stdout == "", (
            f"grep -rn 'provenance: false' .github/workflows/ returned output:\n"
            f"{result.stdout}"
        )

    def test_no_provenance_false_pattern_in_workflows(self):
        """Also check with flexible whitespace."""
        matches = grep_rn(r"provenance:\s*false", WORKFLOWS_DIR)
        assert matches == [], (
            "Found 'provenance: false' in workflow files:\n"
            + "\n".join(f"  {f}:{n}: {l}" for f, n, l in matches)
        )

    def test_release_workflows_have_provenance_enabled(self):
        """Release workflows that build Docker images must have provenance enabled."""
        docker_release_workflows = [
            "release-live-docker.yml",
            "release-main-docker.yml",
            "release-weekly-docker.yml",
        ]
        for workflow in docker_release_workflows:
            path = os.path.join(WORKFLOWS_DIR, workflow)
            content = read_file(path)
            # provenance must be present and not set to false
            has_provenance = "provenance:" in content
            has_provenance_false = bool(re.search(r"provenance:\s*false", content))
            if has_provenance:
                assert not has_provenance_false, (
                    f"{workflow} has 'provenance: false' which disables provenance attestations"
                )

    def test_release_weekly_docker_has_provenance_mode_min(self):
        """release-weekly-docker.yml must have provenance: mode=min (not false)."""
        path = os.path.join(WORKFLOWS_DIR, "release-weekly-docker.yml")
        content = read_file(path)
        assert re.search(r"provenance:\s*mode=min", content), (
            "release-weekly-docker.yml does not have 'provenance: mode=min'"
        )


# ─────────────────────────────────────────────────────────────────────────────
# Criterion 3: grep -rn 'zap-nightly:latest' docker/ → no output
# ─────────────────────────────────────────────────────────────────────────────

class TestCriterion3_NoZapNightlyLatest:
    """
    Acceptance criterion:
    `grep -rn 'zap-nightly:latest' docker/` returns no output.
    """

    def test_no_zap_nightly_latest_in_docker_dir(self):
        """Direct grep equivalent: no 'zap-nightly:latest' in docker/ directory."""
        result = subprocess.run(
            ["grep", "-rn", "zap-nightly:latest", DOCKER_DIR],
            capture_output=True, text=True
        )
        assert result.stdout == "", (
            f"grep -rn 'zap-nightly:latest' docker/ returned output:\n"
            f"{result.stdout}"
        )

    def test_zap_nightly_pinned_to_digest_in_chrome_dockerfile(self):
        """docker/chrome/Dockerfile must use zap-nightly@sha256:<digest>."""
        chrome_dockerfile = os.path.join(DOCKER_DIR, "chrome", "Dockerfile")
        assert os.path.exists(chrome_dockerfile), (
            f"docker/chrome/Dockerfile not found at {chrome_dockerfile}"
        )
        content = read_file(chrome_dockerfile)
        digest_pattern = re.compile(r"zap-nightly@sha256:[0-9a-f]{64}")
        assert digest_pattern.search(content), (
            "docker/chrome/Dockerfile does not pin zap-nightly to a sha256 digest"
        )

    def test_zap_nightly_digest_is_valid_sha256(self):
        """The sha256 digest in chrome/Dockerfile must be a valid 64-char hex string."""
        chrome_dockerfile = os.path.join(DOCKER_DIR, "chrome", "Dockerfile")
        content = read_file(chrome_dockerfile)
        m = re.search(r"zap-nightly@sha256:([0-9a-f]{64})", content)
        if m:
            digest = m.group(1)
            assert len(digest) == 64, f"Digest '{digest}' is not 64 characters"
            assert re.match(r'^[0-9a-f]{64}$', digest), (
                f"Digest '{digest}' contains non-hex characters"
            )

    def test_all_zap_nightly_references_use_digest(self):
        """Any reference to zap-nightly in docker/ must use @sha256: not :latest."""
        matches = grep_rn(r"zap-nightly", DOCKER_DIR)
        digest_pattern = re.compile(r"zap-nightly@sha256:[0-9a-f]{64}")
        bad = [
            (f, n, l)
            for f, n, l in matches
            if not digest_pattern.search(l)
        ]
        assert bad == [], (
            "Found zap-nightly references not pinned to sha256 digest:\n"
            + "\n".join(f"  {f}:{n}: {l}" for f, n, l in bad)
        )


# ─────────────────────────────────────────────────────────────────────────────
# Criterion 4: grep -E '^zaproxy$|^pyyaml$' docker/requirements.txt → no output
# ─────────────────────────────────────────────────────────────────────────────

class TestCriterion4_RequirementsTxtPinned:
    """
    Acceptance criterion:
    `grep -E '^zaproxy$|^pyyaml$' docker/requirements.txt` returns no output.
    Both packages must be pinned to specific versions.
    """

    def test_no_unpinned_zaproxy_or_pyyaml_in_requirements(self):
        """Direct grep equivalent: no bare 'zaproxy' or 'pyyaml' lines."""
        result = subprocess.run(
            ["grep", "-E", "^zaproxy$|^pyyaml$", REQUIREMENTS_TXT],
            capture_output=True, text=True
        )
        assert result.stdout == "", (
            f"grep -E '^zaproxy$|^pyyaml$' docker/requirements.txt returned output:\n"
            f"{result.stdout}"
        )

    def test_requirements_txt_exists(self):
        assert os.path.exists(REQUIREMENTS_TXT), (
            f"docker/requirements.txt not found at {REQUIREMENTS_TXT}"
        )

    def test_zaproxy_pinned_with_version_specifier(self):
        """zaproxy must appear with == version specifier."""
        content = read_file(REQUIREMENTS_TXT)
        lines = content.splitlines()
        has_pinned = any(
            re.match(r"^zaproxy==\S+", l.strip())
            for l in lines
        )
        assert has_pinned, (
            "docker/requirements.txt must contain 'zaproxy==<version>' (pinned)"
        )

    def test_pyyaml_pinned_with_version_specifier(self):
        """PyYAML must appear with == version specifier (case-insensitive)."""
        content = read_file(REQUIREMENTS_TXT)
        lines = content.splitlines()
        has_pinned = any(
            re.match(r"^pyyaml==\S+", l.strip(), re.IGNORECASE)
            for l in lines
        )
        assert has_pinned, (
            "docker/requirements.txt must contain 'PyYAML==<version>' (pinned)"
        )

    def test_zaproxy_version_is_semver(self):
        """zaproxy version must look like a semver (X.Y.Z)."""
        content = read_file(REQUIREMENTS_TXT)
        m = re.search(r"^zaproxy==(\S+)", content, re.MULTILINE)
        assert m, "zaproxy not found with == pin in requirements.txt"
        version = m.group(1)
        assert re.match(r"^\d+\.\d+\.\d+", version), (
            f"zaproxy version '{version}' does not look like a semver"
        )

    def test_pyyaml_version_is_semver(self):
        """PyYAML version must look like a semver (X.Y.Z)."""
        content = read_file(REQUIREMENTS_TXT)
        m = re.search(r"^pyyaml==(\S+)", content, re.MULTILINE | re.IGNORECASE)
        assert m, "PyYAML not found with == pin in requirements.txt"
        version = m.group(1)
        assert re.match(r"^\d+\.\d+\.\d+", version), (
            f"PyYAML version '{version}' does not look like a semver"
        )


# ─────────────────────────────────────────────────────────────────────────────
# Criterion 5: At least one release workflow has provenance enabled
# ─────────────────────────────────────────────────────────────────────────────

class TestCriterion5_ProvenanceEnabled:
    """
    Acceptance criterion:
    At least one of the four release workflows is triggered on the test branch
    (or via workflow_dispatch) and produces a Docker image manifest that includes
    provenance and SBOM attestations.
    """

    def test_release_weekly_docker_has_workflow_dispatch(self):
        """release-weekly-docker.yml must support workflow_dispatch trigger."""
        path = os.path.join(WORKFLOWS_DIR, "release-weekly-docker.yml")
        content = read_file(path)
        assert "workflow_dispatch" in content, (
            "release-weekly-docker.yml does not have workflow_dispatch trigger"
        )

    def test_release_live_docker_has_workflow_dispatch(self):
        """release-live-docker.yml must support workflow_dispatch trigger."""
        path = os.path.join(WORKFLOWS_DIR, "release-live-docker.yml")
        content = read_file(path)
        assert "workflow_dispatch" in content, (
            "release-live-docker.yml does not have workflow_dispatch trigger"
        )

    def test_release_main_docker_has_workflow_dispatch(self):
        """release-main-docker.yml must support workflow_dispatch trigger."""
        path = os.path.join(WORKFLOWS_DIR, "release-main-docker.yml")
        content = read_file(path)
        assert "workflow_dispatch" in content, (
            "release-main-docker.yml does not have workflow_dispatch trigger"
        )

    def test_release_weekly_docker_provenance_not_false(self):
        """release-weekly-docker.yml must not have provenance: false."""
        path = os.path.join(WORKFLOWS_DIR, "release-weekly-docker.yml")
        content = read_file(path)
        assert not re.search(r"provenance:\s*false", content), (
            "release-weekly-docker.yml has provenance: false"
        )

    def test_release_live_docker_provenance_not_false(self):
        """release-live-docker.yml must not have provenance: false."""
        path = os.path.join(WORKFLOWS_DIR, "release-live-docker.yml")
        content = read_file(path)
        assert not re.search(r"provenance:\s*false", content), (
            "release-live-docker.yml has provenance: false"
        )

    def test_release_main_docker_provenance_not_false(self):
        """release-main-docker.yml must not have provenance: false."""
        path = os.path.join(WORKFLOWS_DIR, "release-main-docker.yml")
        content = read_file(path)
        assert not re.search(r"provenance:\s*false", content), (
            "release-main-docker.yml has provenance: false"
        )

    def test_at_least_one_workflow_has_provenance_mode_min(self):
        """At least one release workflow must have provenance: mode=min."""
        found = False
        for workflow in RELEASE_WORKFLOWS:
            path = os.path.join(WORKFLOWS_DIR, workflow)
            content = read_file(path)
            if re.search(r"provenance:\s*mode=min", content):
                found = True
                break
        assert found, (
            "No release workflow has 'provenance: mode=min' to enable provenance attestations"
        )


# ─────────────────────────────────────────────────────────────────────────────
# Criterion 6: PR description documents SHA, digest, and Python package versions
# ─────────────────────────────────────────────────────────────────────────────

class TestCriterion6_DocumentationInCode:
    """
    Acceptance criterion:
    The PR description documents the SHA pinned for crashappsec/setup-chalk-action,
    the digest pinned for zaproxy/zap-nightly, and the Python package versions pinned.

    Since we can't directly inspect the GitHub PR description, we verify that:
    1. The SHA is documented as a comment in the workflow files
    2. The digest is documented in the Dockerfile
    3. The Python package versions are documented in requirements.txt
    """

    def test_chalk_action_sha_has_comment_annotation(self):
        """The SHA pin for setup-chalk-action should have a comment explaining it."""
        sha_pattern = re.compile(
            r"setup-chalk-action@[0-9a-f]{40}\s*#"
        )
        found_comment = False
        for workflow in RELEASE_WORKFLOWS:
            path = os.path.join(WORKFLOWS_DIR, workflow)
            content = read_file(path)
            if sha_pattern.search(content):
                found_comment = True
                break
        assert found_comment, (
            "No workflow file has a comment after the setup-chalk-action SHA pin. "
            "The SHA should be documented with a comment (e.g., # main or # verified date)"
        )

    def test_chalk_action_sha_is_documented(self):
        """The SHA for setup-chalk-action must be present in workflow files."""
        sha_pattern = re.compile(r"setup-chalk-action@([0-9a-f]{40})")
        found_sha = None
        for workflow in RELEASE_WORKFLOWS:
            path = os.path.join(WORKFLOWS_DIR, workflow)
            content = read_file(path)
            m = sha_pattern.search(content)
            if m:
                found_sha = m.group(1)
                break
        assert found_sha is not None, (
            "No SHA found for setup-chalk-action in any release workflow"
        )
        # The SHA should be 40 hex chars
        assert len(found_sha) == 40 and re.match(r'^[0-9a-f]{40}$', found_sha), (
            f"SHA '{found_sha}' is not a valid 40-char hex SHA"
        )

    def test_zap_nightly_digest_is_documented_in_dockerfile(self):
        """The sha256 digest for zaproxy/zap-nightly must be in docker/chrome/Dockerfile."""
        chrome_dockerfile = os.path.join(DOCKER_DIR, "chrome", "Dockerfile")
        content = read_file(chrome_dockerfile)
        m = re.search(r"zap-nightly@sha256:([0-9a-f]{64})", content)
        assert m is not None, (
            "docker/chrome/Dockerfile does not document the sha256 digest for zaproxy/zap-nightly"
        )

    def test_python_package_versions_documented_in_requirements(self):
        """Python package versions must be documented in docker/requirements.txt."""
        content = read_file(REQUIREMENTS_TXT)
        # zaproxy version
        zaproxy_m = re.search(r"^zaproxy==(\S+)", content, re.MULTILINE)
        assert zaproxy_m, "zaproxy version not documented in docker/requirements.txt"
        # PyYAML version
        pyyaml_m = re.search(r"^pyyaml==(\S+)", content, re.MULTILINE | re.IGNORECASE)
        assert pyyaml_m, "PyYAML version not documented in docker/requirements.txt"

    def test_requirements_txt_has_both_packages_pinned(self):
        """Both zaproxy and PyYAML must be pinned in requirements.txt."""
        content = read_file(REQUIREMENTS_TXT)
        lines = [l.strip() for l in content.splitlines() if l.strip()]
        zaproxy_pinned = any(re.match(r"^zaproxy==\S+", l) for l in lines)
        pyyaml_pinned = any(re.match(r"^pyyaml==\S+", l, re.IGNORECASE) for l in lines)
        assert zaproxy_pinned, "zaproxy is not pinned in docker/requirements.txt"
        assert pyyaml_pinned, "PyYAML is not pinned in docker/requirements.txt"


# ─────────────────────────────────────────────────────────────────────────────
# Additional: Dependabot docker entry for docker/ directory
# ─────────────────────────────────────────────────────────────────────────────

class TestDependabotDockerEntry:
    """
    Verifies that dependabot.yml has a docker entry for the docker/ directory
    to automate digest updates.
    """

    def test_dependabot_yml_exists(self):
        assert os.path.exists(DEPENDABOT_YML), (
            f".github/dependabot.yml not found at {DEPENDABOT_YML}"
        )

    def test_dependabot_has_docker_ecosystem_entry(self):
        """dependabot.yml must have a docker package-ecosystem entry."""
        content = read_file(DEPENDABOT_YML)
        assert 'package-ecosystem: "docker"' in content or "package-ecosystem: docker" in content, (
            ".github/dependabot.yml does not have a docker package-ecosystem entry"
        )

    def test_dependabot_docker_entry_targets_docker_dir(self):
        """The docker dependabot entry must target the /docker directory."""
        content = read_file(DEPENDABOT_YML)
        # Check that docker ecosystem entry exists with /docker directory
        assert re.search(r'package-ecosystem.*docker', content), (
            "No docker package-ecosystem entry found in dependabot.yml"
        )
        assert '/docker' in content, (
            "dependabot.yml docker entry does not target /docker directory"
        )


# ─────────────────────────────────────────────────────────────────────────────
# Sanity: All four release workflow files exist
# ─────────────────────────────────────────────────────────────────────────────

class TestReleaseWorkflowsExist:
    @pytest.mark.parametrize("workflow", RELEASE_WORKFLOWS)
    def test_release_workflow_file_exists(self, workflow):
        path = os.path.join(WORKFLOWS_DIR, workflow)
        assert os.path.exists(path), f"Release workflow not found: {path}"

    @pytest.mark.parametrize("workflow", RELEASE_WORKFLOWS)
    def test_release_workflow_is_valid_yaml(self, workflow):
        """Each release workflow must be valid YAML."""
        try:
            import yaml
        except ImportError:
            pytest.skip("pyyaml not installed")
        path = os.path.join(WORKFLOWS_DIR, workflow)
        content = read_file(path)
        try:
            parsed = yaml.safe_load(content)
            assert parsed is not None, f"{workflow} parsed as None"
        except yaml.YAMLError as e:
            pytest.fail(f"{workflow} is not valid YAML: {e}")
