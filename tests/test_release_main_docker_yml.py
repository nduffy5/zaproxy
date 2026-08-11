"""
TDD tests for release-main-docker.yml provenance/SBOM changes.
Acceptance criteria:
  - `provenance: false` must NOT appear in the file.
  - `sbom: true` must appear in the docker/build-push-action step(s).
  - YAML syntax must be valid.
"""

import yaml
import re

WORKFLOW_PATH = ".github/workflows/release-main-docker.yml"


def load_raw():
    with open(WORKFLOW_PATH, "r") as f:
        return f.read()


def load_yaml():
    with open(WORKFLOW_PATH, "r") as f:
        return yaml.safe_load(f)


def test_no_provenance_false():
    """provenance: false must not appear anywhere in the file."""
    content = load_raw()
    assert "provenance: false" not in content, (
        "Found 'provenance: false' in release-main-docker.yml — it must be removed."
    )


def test_sbom_true_present():
    """sbom: true must appear in the file."""
    content = load_raw()
    assert "sbom: true" in content, (
        "'sbom: true' not found in release-main-docker.yml — it must be added."
    )


def test_yaml_valid():
    """The YAML file must parse without errors."""
    data = load_yaml()
    assert data is not None, "YAML parsed to None — file may be empty or invalid."


def test_build_push_steps_have_sbom():
    """Every docker/build-push-action step must have sbom: true in its with block."""
    data = load_yaml()
    jobs = data.get("jobs", {})
    found_build_push = False
    for job_name, job in jobs.items():
        for step in job.get("steps", []):
            uses = step.get("uses", "")
            if uses.startswith("docker/build-push-action"):
                found_build_push = True
                with_block = step.get("with", {})
                assert with_block.get("sbom") is True, (
                    f"Step '{step.get('name', uses)}' uses docker/build-push-action "
                    f"but does not have 'sbom: true' in its 'with:' block."
                )
    assert found_build_push, (
        "No docker/build-push-action step found in the workflow — check the file."
    )


def test_build_push_steps_no_provenance_false():
    """No docker/build-push-action step should have provenance: false."""
    data = load_yaml()
    jobs = data.get("jobs", {})
    for job_name, job in jobs.items():
        for step in job.get("steps", []):
            uses = step.get("uses", "")
            if uses.startswith("docker/build-push-action"):
                with_block = step.get("with", {})
                assert with_block.get("provenance") != False, (  # noqa: E712
                    f"Step '{step.get('name', uses)}' has 'provenance: false' — it must be removed."
                )
