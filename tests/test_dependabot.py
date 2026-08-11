"""Tests for .github/dependabot.yml configuration."""
import yaml
import pytest


DEPENDABOT_FILE = ".github/dependabot.yml"


def load_dependabot():
    with open(DEPENDABOT_FILE) as f:
        return yaml.safe_load(f)


def test_dependabot_yaml_is_valid():
    """The dependabot.yml file must be valid YAML."""
    config = load_dependabot()
    assert config is not None


def test_pip_entry_for_docker_directory_exists():
    """There must be a pip ecosystem entry targeting /docker."""
    config = load_dependabot()
    updates = config.get("updates", [])
    pip_docker_entries = [
        entry for entry in updates
        if entry.get("package-ecosystem") == "pip"
        and entry.get("directory") == "/docker"
    ]
    assert len(pip_docker_entries) == 1, (
        "Expected exactly one pip entry with directory '/docker', "
        f"found {len(pip_docker_entries)}"
    )


def test_pip_entry_has_schedule():
    """The pip /docker entry must have a defined schedule."""
    config = load_dependabot()
    updates = config.get("updates", [])
    pip_docker_entries = [
        entry for entry in updates
        if entry.get("package-ecosystem") == "pip"
        and entry.get("directory") == "/docker"
    ]
    assert len(pip_docker_entries) == 1
    entry = pip_docker_entries[0]
    assert "schedule" in entry, "pip /docker entry must have a 'schedule' key"
    assert "interval" in entry["schedule"], "schedule must have an 'interval' key"
    assert entry["schedule"]["interval"] in ("daily", "weekly", "monthly"), (
        f"interval must be daily, weekly, or monthly; got {entry['schedule']['interval']!r}"
    )


def test_no_duplicate_pip_entries():
    """There must not be duplicate pip ecosystem entries for /docker."""
    config = load_dependabot()
    updates = config.get("updates", [])
    pip_docker_entries = [
        entry for entry in updates
        if entry.get("package-ecosystem") == "pip"
        and entry.get("directory") == "/docker"
    ]
    assert len(pip_docker_entries) == 1, (
        f"Duplicate pip /docker entries found: {len(pip_docker_entries)}"
    )
