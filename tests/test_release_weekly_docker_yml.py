"""
TDD tests for release-weekly-docker.yml modernization:
- Remove provenance: false
- Add sbom: true
- YAML syntax must be valid
"""
import yaml

WORKFLOW_FILE = ".github/workflows/release-weekly-docker.yml"


def load_raw():
    with open(WORKFLOW_FILE, "r") as f:
        return f.read()


def load_yaml():
    with open(WORKFLOW_FILE, "r") as f:
        return yaml.safe_load(f)


def test_yaml_is_valid():
    """YAML syntax must be valid."""
    doc = load_yaml()
    assert doc is not None


def test_no_provenance_false():
    """provenance: false must not appear in the file."""
    raw = load_raw()
    assert "provenance: false" not in raw, (
        "Found 'provenance: false' in the workflow file; it should be removed."
    )


def test_sbom_true_present():
    """sbom: true must be present in the docker/build-push-action step."""
    raw = load_raw()
    assert "sbom: true" in raw, (
        "'sbom: true' was not found in the workflow file; it should be added."
    )


def test_build_push_step_has_sbom():
    """The docker/build-push-action step must have sbom: true in its 'with' block."""
    doc = load_yaml()
    steps = doc["jobs"]["publish"]["steps"]
    build_push_steps = [
        s for s in steps
        if isinstance(s, dict)
        and isinstance(s.get("uses", ""), str)
        and s.get("uses", "").startswith("docker/build-push-action")
    ]
    assert build_push_steps, "No docker/build-push-action step found."
    for step in build_push_steps:
        assert step.get("with", {}).get("sbom") is True, (
            f"Step '{step.get('name', step.get('uses'))}' is missing 'sbom: true'."
        )


def test_build_push_step_has_no_provenance_false():
    """The docker/build-push-action step must NOT have provenance: false."""
    doc = load_yaml()
    steps = doc["jobs"]["publish"]["steps"]
    build_push_steps = [
        s for s in steps
        if isinstance(s, dict)
        and isinstance(s.get("uses", ""), str)
        and s.get("uses", "").startswith("docker/build-push-action")
    ]
    assert build_push_steps, "No docker/build-push-action step found."
    for step in build_push_steps:
        provenance = step.get("with", {}).get("provenance")
        assert provenance is not False, (
            f"Step '{step.get('name', step.get('uses'))}' still has 'provenance: false'."
        )
