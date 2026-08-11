"""
Tests verifying the Dependabot docker entry acceptance criteria.
"""
import subprocess
import sys
import yaml
import pytest


DEPENDABOT_PATH = ".github/dependabot.yml"


@pytest.fixture(scope="module")
def dependabot_config():
    with open(DEPENDABOT_PATH) as f:
        return yaml.safe_load(f)


@pytest.fixture(scope="module")
def docker_entries(dependabot_config):
    updates = dependabot_config.get("updates", [])
    return [e for e in updates if e.get("package-ecosystem") == "docker"]


def test_yaml_is_valid():
    """Criterion 3: YAML file parses without error."""
    result = subprocess.run(
        [sys.executable, "-c",
         f"import yaml; yaml.safe_load(open('{DEPENDABOT_PATH}'))"],
        capture_output=True,
        text=True,
    )
    assert result.returncode == 0, (
        f"YAML parse failed:\nstdout: {result.stdout}\nstderr: {result.stderr}"
    )


def test_docker_entry_exists_with_correct_directory(docker_entries):
    """Criterion 1: A docker entry with directory /docker or /docker/chrome exists."""
    valid_dirs = {"/docker", "/docker/chrome"}
    matching = [e for e in docker_entries if e.get("directory") in valid_dirs]
    assert matching, (
        f"No docker entry found with directory in {valid_dirs}. "
        f"Found docker entries: {docker_entries}"
    )


def test_docker_entry_has_daily_or_weekly_schedule(docker_entries):
    """Criterion 2: The docker entry has schedule interval of 'daily' or 'weekly'."""
    valid_dirs = {"/docker", "/docker/chrome"}
    valid_intervals = {"daily", "weekly"}
    matching = [
        e for e in docker_entries
        if e.get("directory") in valid_dirs
        and e.get("schedule", {}).get("interval") in valid_intervals
    ]
    assert matching, (
        f"No docker entry with directory in {valid_dirs} has a schedule interval "
        f"in {valid_intervals}. Found docker entries: {docker_entries}"
    )


def test_docker_entry_prefers_daily_schedule(docker_entries):
    """Preferred: The docker entry uses 'daily' schedule (preferred over weekly)."""
    valid_dirs = {"/docker", "/docker/chrome"}
    daily_entries = [
        e for e in docker_entries
        if e.get("directory") in valid_dirs
        and e.get("schedule", {}).get("interval") == "daily"
    ]
    assert daily_entries, (
        f"No docker entry with directory in {valid_dirs} uses 'daily' schedule. "
        f"Found docker entries: {docker_entries}"
    )


def test_no_duplicate_docker_entries(docker_entries):
    """Criterion 4: No duplicate docker entries (same directory listed twice)."""
    directories = [e.get("directory") for e in docker_entries]
    duplicates = [d for d in set(directories) if directories.count(d) > 1]
    assert not duplicates, (
        f"Duplicate docker entries found for directories: {duplicates}"
    )
