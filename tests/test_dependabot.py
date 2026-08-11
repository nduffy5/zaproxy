"""Tests for .github/dependabot.yml configuration."""
import yaml
import pytest


DEPENDABOT_PATH = ".github/dependabot.yml"


@pytest.fixture
def dependabot_config():
    with open(DEPENDABOT_PATH) as f:
        return yaml.safe_load(f)


def get_github_actions_entry(config):
    """Return the github-actions package-ecosystem entry, or None."""
    for entry in config.get("updates", []):
        if entry.get("package-ecosystem") == "github-actions":
            return entry
    return None


def test_yaml_is_valid():
    """The dependabot.yml file must be valid YAML."""
    with open(DEPENDABOT_PATH) as f:
        data = yaml.safe_load(f)
    assert data is not None


def test_github_actions_entry_exists(dependabot_config):
    """There must be a github-actions package-ecosystem entry."""
    entry = get_github_actions_entry(dependabot_config)
    assert entry is not None, "No github-actions entry found in dependabot.yml"


def test_github_actions_directory_is_root(dependabot_config):
    """The github-actions entry must cover the root directory (/)."""
    entry = get_github_actions_entry(dependabot_config)
    assert entry is not None
    assert entry.get("directory") == "/", (
        f"Expected directory '/' but got '{entry.get('directory')}'"
    )


def test_github_actions_schedule_is_weekly(dependabot_config):
    """The github-actions schedule interval must be 'weekly', not 'monthly'."""
    entry = get_github_actions_entry(dependabot_config)
    assert entry is not None
    interval = entry.get("schedule", {}).get("interval")
    assert interval == "weekly", (
        f"Expected schedule interval 'weekly' but got '{interval}'"
    )


def test_chalk_action_not_excluded(dependabot_config):
    """The chalk action (crashappsec/setup-chalk-action) must not be in the ignore list."""
    entry = get_github_actions_entry(dependabot_config)
    assert entry is not None
    ignore_list = entry.get("ignore", [])
    for ignored in ignore_list:
        dep_name = ignored.get("dependency-name", "")
        assert "setup-chalk-action" not in dep_name, (
            f"crashappsec/setup-chalk-action is explicitly excluded in dependabot.yml: {ignored}"
        )
        assert "crashappsec" not in dep_name, (
            f"crashappsec org is explicitly excluded in dependabot.yml: {ignored}"
        )
