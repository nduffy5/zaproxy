"""
Tests to verify acceptance criteria for the Dependabot pip entry for docker/ directory.
"""
import sys
import yaml
import unittest


DEPENDABOT_PATH = ".github/dependabot.yml"


def load_dependabot():
    with open(DEPENDABOT_PATH, "r") as f:
        return yaml.safe_load(f)


class TestDependabotPipEntry(unittest.TestCase):

    def setUp(self):
        self.config = load_dependabot()
        self.updates = self.config.get("updates", [])

    # -----------------------------------------------------------------------
    # Criterion 1: YAML is valid and safe_load exits 0
    # -----------------------------------------------------------------------
    def test_yaml_is_valid(self):
        """python3 -c "import yaml; yaml.safe_load(open('.github/dependabot.yml'))" exits 0."""
        # If we got here, loading succeeded – just assert the result is not None
        self.assertIsNotNone(self.config, "dependabot.yml should parse to a non-None value")

    # -----------------------------------------------------------------------
    # Criterion 2: pip entry with directory /docker exists
    # -----------------------------------------------------------------------
    def test_pip_docker_entry_exists(self):
        """There must be a package-ecosystem: pip entry with directory: /docker."""
        pip_docker_entries = [
            entry for entry in self.updates
            if entry.get("package-ecosystem") == "pip"
            and entry.get("directory") == "/docker"
        ]
        self.assertTrue(
            len(pip_docker_entries) >= 1,
            "Expected at least one pip entry with directory '/docker', found none."
        )

    # -----------------------------------------------------------------------
    # Criterion 3: The pip /docker entry has a defined schedule
    # -----------------------------------------------------------------------
    def test_pip_docker_entry_has_schedule(self):
        """The pip /docker entry must have a schedule with an interval."""
        pip_docker_entries = [
            entry for entry in self.updates
            if entry.get("package-ecosystem") == "pip"
            and entry.get("directory") == "/docker"
        ]
        self.assertTrue(pip_docker_entries, "No pip /docker entry found to check schedule.")
        entry = pip_docker_entries[0]
        schedule = entry.get("schedule")
        self.assertIsNotNone(schedule, "pip /docker entry must have a 'schedule' key.")
        interval = schedule.get("interval")
        self.assertIsNotNone(interval, "pip /docker schedule must have an 'interval' key.")
        self.assertIn(
            interval,
            ["daily", "weekly", "monthly"],
            f"interval '{interval}' is not a valid Dependabot schedule interval."
        )

    def test_pip_docker_schedule_is_weekly(self):
        """The pip /docker entry schedule interval should be 'weekly' (as specified)."""
        pip_docker_entries = [
            entry for entry in self.updates
            if entry.get("package-ecosystem") == "pip"
            and entry.get("directory") == "/docker"
        ]
        self.assertTrue(pip_docker_entries, "No pip /docker entry found.")
        entry = pip_docker_entries[0]
        interval = entry.get("schedule", {}).get("interval")
        self.assertEqual(
            interval, "weekly",
            f"Expected schedule interval 'weekly', got '{interval}'."
        )

    # -----------------------------------------------------------------------
    # Criterion 4: No duplicate pip entries
    # -----------------------------------------------------------------------
    def test_no_duplicate_pip_entries(self):
        """There must be exactly one pip entry with directory /docker (no duplicates)."""
        pip_docker_entries = [
            entry for entry in self.updates
            if entry.get("package-ecosystem") == "pip"
            and entry.get("directory") == "/docker"
        ]
        self.assertEqual(
            len(pip_docker_entries), 1,
            f"Expected exactly 1 pip /docker entry, found {len(pip_docker_entries)}."
        )

    def test_no_duplicate_pip_entries_any_directory(self):
        """All pip entries should have unique directories (no duplicate pip ecosystem+directory combos)."""
        pip_entries = [
            (entry.get("package-ecosystem"), entry.get("directory"))
            for entry in self.updates
            if entry.get("package-ecosystem") == "pip"
        ]
        unique_pip_entries = set(pip_entries)
        self.assertEqual(
            len(pip_entries), len(unique_pip_entries),
            f"Duplicate pip entries found: {pip_entries}"
        )

    # -----------------------------------------------------------------------
    # Sanity: top-level structure
    # -----------------------------------------------------------------------
    def test_top_level_version(self):
        """dependabot.yml must have version: 2."""
        self.assertEqual(self.config.get("version"), 2)

    def test_updates_is_list(self):
        """updates must be a list."""
        self.assertIsInstance(self.updates, list)

    def test_updates_not_empty(self):
        """updates list must not be empty."""
        self.assertGreater(len(self.updates), 0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
