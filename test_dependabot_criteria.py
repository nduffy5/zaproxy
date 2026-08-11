"""
Tests to verify acceptance criteria for adding crashappsec/setup-chalk-action
to Dependabot github-actions config.
"""

import subprocess
import sys
import yaml
import pytest


DEPENDABOT_PATH = ".github/dependabot.yml"


@pytest.fixture(scope="module")
def dependabot_config():
    with open(DEPENDABOT_PATH, "r") as f:
        return yaml.safe_load(f)


@pytest.fixture(scope="module")
def github_actions_entry(dependabot_config):
    updates = dependabot_config.get("updates", [])
    for entry in updates:
        if entry.get("package-ecosystem") == "github-actions":
            return entry
    return None


# Criterion 1: YAML is valid and parses without error
def test_yaml_is_valid():
    """python3 -c "import yaml; yaml.safe_load(open('.github/dependabot.yml'))" exits 0"""
    result = subprocess.run(
        [
            sys.executable,
            "-c",
            f"import yaml; yaml.safe_load(open('{DEPENDABOT_PATH}'))",
        ],
        capture_output=True,
        text=True,
    )
    assert result.returncode == 0, (
        f"YAML parsing failed with exit code {result.returncode}.\n"
        f"stderr: {result.stderr}"
    )


# Criterion 2: github-actions package-ecosystem entry exists with directory: /
def test_github_actions_entry_exists(github_actions_entry):
    """dependabot.yml contains a package-ecosystem: github-actions entry"""
    assert github_actions_entry is not None, (
        "No 'package-ecosystem: github-actions' entry found in dependabot.yml"
    )


def test_github_actions_directory_is_root(github_actions_entry):
    """github-actions entry has directory: / (covers the whole repo)"""
    assert github_actions_entry is not None, "No github-actions entry found"
    directory = github_actions_entry.get("directory")
    assert directory == "/", (
        f"Expected directory '/' but got '{directory}'"
    )


# Criterion 3: The entry is NOT scoped to exclude the chalk action
def test_github_actions_entry_does_not_exclude_chalk_action(github_actions_entry):
    """The github-actions entry does not exclude crashappsec/setup-chalk-action"""
    assert github_actions_entry is not None, "No github-actions entry found"

    # Check ignore list
    ignore_list = github_actions_entry.get("ignore", [])
    for ignore_entry in ignore_list:
        dep_name = ignore_entry.get("dependency-name", "")
        assert "crashappsec" not in dep_name and "setup-chalk-action" not in dep_name, (
            f"crashappsec/setup-chalk-action is excluded via ignore: {ignore_entry}"
        )

    # Check exclude-paths
    exclude_paths = github_actions_entry.get("exclude-paths", [])
    assert len(exclude_paths) == 0 or all(
        "crashappsec" not in p and "setup-chalk-action" not in p
        for p in exclude_paths
    ), f"Chalk action is excluded via exclude-paths: {exclude_paths}"

    # Check that patterns (if any) don't explicitly exclude chalk
    groups = github_actions_entry.get("groups", {})
    for group_name, group_config in groups.items():
        exclude_patterns = group_config.get("exclude-patterns", [])
        for pattern in exclude_patterns:
            assert "crashappsec" not in pattern and "setup-chalk-action" not in pattern, (
                f"Chalk action is excluded via group '{group_name}' exclude-patterns: {pattern}"
            )


# Criterion 4: Schedule interval is weekly (not monthly)
def test_github_actions_schedule_is_weekly(github_actions_entry):
    """The Dependabot github-actions schedule interval is 'weekly' (not 'monthly')"""
    assert github_actions_entry is not None, "No github-actions entry found"
    schedule = github_actions_entry.get("schedule", {})
    interval = schedule.get("interval")
    assert interval == "weekly", (
        f"Expected schedule interval 'weekly' but got '{interval}'"
    )


# Criterion 5: crashappsec/setup-chalk-action is used in workflows (sanity check)
def test_chalk_action_is_used_in_workflows():
    """Verify crashappsec/setup-chalk-action is actually referenced in workflows"""
    result = subprocess.run(
        ["grep", "-r", "crashappsec/setup-chalk-action", ".github/workflows/"],
        capture_output=True,
        text=True,
    )
    assert result.returncode == 0, (
        "crashappsec/setup-chalk-action is not referenced in any workflow file. "
        "The Dependabot entry may be unnecessary."
    )
    assert "crashappsec/setup-chalk-action" in result.stdout


# Criterion 6: The github-actions entry covers the directory where workflows live
def test_github_actions_covers_workflows_directory(github_actions_entry):
    """The directory '/' covers .github/workflows/ where chalk action is used"""
    assert github_actions_entry is not None, "No github-actions entry found"
    directory = github_actions_entry.get("directory", "")
    # directory "/" covers all paths including .github/workflows/
    assert directory == "/", (
        f"Directory '{directory}' may not cover .github/workflows/ where chalk action is used"
    )


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
