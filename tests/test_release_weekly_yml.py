"""
TDD tests for release-weekly.yml provenance/SBOM changes.
Acceptance criteria:
1. grep 'provenance: false' .github/workflows/release-weekly.yml returns no output
2. The docker/build-push-action step in release-weekly-docker.yml contains sbom: true
   (release-weekly.yml is the Gradle build workflow; the docker step is in release-weekly-docker.yml)
3. grep -rn 'provenance: false' .github/workflows/ returns no output
4. YAML syntax is valid
"""
import subprocess
import yaml
import pytest


WEEKLY_YML = ".github/workflows/release-weekly.yml"
WEEKLY_DOCKER_YML = ".github/workflows/release-weekly-docker.yml"
WORKFLOWS_DIR = ".github/workflows"


def test_no_provenance_false_in_release_weekly_yml():
    """release-weekly.yml must not contain 'provenance: false'."""
    result = subprocess.run(
        ["grep", "provenance: false", WEEKLY_YML],
        capture_output=True, text=True
    )
    assert result.stdout == "", (
        f"Found 'provenance: false' in {WEEKLY_YML}:\n{result.stdout}"
    )


def test_sbom_true_in_release_weekly_docker_yml():
    """The docker/build-push-action step in release-weekly-docker.yml must contain sbom: true."""
    with open(WEEKLY_DOCKER_YML) as f:
        content = yaml.safe_load(f)

    jobs = content.get("jobs", {})
    found_build_push = False
    found_sbom = False

    for job_name, job in jobs.items():
        steps = job.get("steps", [])
        for step in steps:
            uses = step.get("uses", "")
            if "docker/build-push-action" in uses:
                found_build_push = True
                with_params = step.get("with", {})
                if with_params.get("sbom") is True:
                    found_sbom = True

    assert found_build_push, (
        f"No docker/build-push-action step found in {WEEKLY_DOCKER_YML}"
    )
    assert found_sbom, (
        f"docker/build-push-action step in {WEEKLY_DOCKER_YML} does not have 'sbom: true'"
    )


def test_no_provenance_false_in_release_weekly_docker_yml():
    """release-weekly-docker.yml must not contain 'provenance: false'."""
    result = subprocess.run(
        ["grep", "provenance: false", WEEKLY_DOCKER_YML],
        capture_output=True, text=True
    )
    assert result.stdout == "", (
        f"Found 'provenance: false' in {WEEKLY_DOCKER_YML}:\n{result.stdout}"
    )


def test_no_provenance_false_anywhere_in_workflows():
    """grep -rn 'provenance: false' .github/workflows/ must return no output."""
    result = subprocess.run(
        ["grep", "-rn", "provenance: false", WORKFLOWS_DIR],
        capture_output=True, text=True
    )
    assert result.stdout == "", (
        f"Found 'provenance: false' in workflows directory:\n{result.stdout}"
    )


def test_release_weekly_yml_valid_yaml():
    """release-weekly.yml must be valid YAML."""
    with open(WEEKLY_YML) as f:
        content = yaml.safe_load(f)
    assert content is not None, f"{WEEKLY_YML} parsed as None (empty file?)"


def test_release_weekly_docker_yml_valid_yaml():
    """release-weekly-docker.yml must be valid YAML."""
    with open(WEEKLY_DOCKER_YML) as f:
        content = yaml.safe_load(f)
    assert content is not None, f"{WEEKLY_DOCKER_YML} parsed as None (empty file?)"
