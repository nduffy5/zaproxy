"""
TDD tests for release-live-docker.yml workflow changes:
- provenance: false must be removed
- sbom: true must be present in the docker/build-push-action step
- YAML must be valid
"""

import yaml
import re
import pytest

WORKFLOW_PATH = ".github/workflows/release-live-docker.yml"


def load_workflow():
    with open(WORKFLOW_PATH, "r") as f:
        return f.read()


def load_workflow_yaml():
    with open(WORKFLOW_PATH, "r") as f:
        return yaml.safe_load(f)


def test_yaml_is_valid():
    """YAML syntax must be valid."""
    data = load_workflow_yaml()
    assert data is not None, "Workflow YAML failed to parse"


def test_provenance_false_is_removed():
    """provenance: false must not appear anywhere in the workflow file."""
    content = load_workflow()
    assert "provenance: false" not in content, (
        "Found 'provenance: false' in workflow — it must be removed"
    )


def test_sbom_true_is_present():
    """sbom: true must appear in the workflow file."""
    content = load_workflow()
    assert "sbom: true" in content, (
        "Did not find 'sbom: true' in workflow — it must be added"
    )


def test_sbom_true_in_build_push_action_step():
    """sbom: true must be in the with: block of the docker/build-push-action step."""
    data = load_workflow_yaml()
    steps = data["jobs"]["publish"]["steps"]
    build_push_steps = [
        s for s in steps
        if isinstance(s.get("uses", ""), str) and "docker/build-push-action" in s.get("uses", "")
    ]
    assert build_push_steps, "No docker/build-push-action step found"
    for step in build_push_steps:
        with_block = step.get("with", {})
        assert with_block.get("sbom") is True, (
            f"Expected sbom: true in step '{step.get('name', step.get('uses'))}', "
            f"got: {with_block.get('sbom')}"
        )


def test_provenance_false_not_in_build_push_action_step():
    """provenance: false must not be in the with: block of the docker/build-push-action step."""
    data = load_workflow_yaml()
    steps = data["jobs"]["publish"]["steps"]
    build_push_steps = [
        s for s in steps
        if isinstance(s.get("uses", ""), str) and "docker/build-push-action" in s.get("uses", "")
    ]
    assert build_push_steps, "No docker/build-push-action step found"
    for step in build_push_steps:
        with_block = step.get("with", {})
        provenance = with_block.get("provenance")
        assert provenance is not False, (
            f"Found provenance: false in step '{step.get('name', step.get('uses'))}' — must be removed"
        )
