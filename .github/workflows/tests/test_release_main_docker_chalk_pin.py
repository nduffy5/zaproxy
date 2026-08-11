"""
TDD tests for pinning crashappsec/setup-chalk-action SHA in release-main-docker.yml.

Acceptance Criteria:
1. File contains `uses: crashappsec/setup-chalk-action@<40-char-sha>` (no @main).
2. `grep -n 'setup-chalk-action@main'` returns no output.
3. YAML syntax is valid.
"""

import re
import os
import unittest
import yaml

WORKFLOW_PATH = os.path.join(
    os.path.dirname(__file__),
    "..",
    "release-main-docker.yml",
)

# Pattern: 40 lowercase hex characters
SHA_PATTERN = re.compile(r"crashappsec/setup-chalk-action@([0-9a-f]{40})")


class TestChalkActionSHAPin(unittest.TestCase):
    def _read_file(self):
        with open(WORKFLOW_PATH, "r") as fh:
            return fh.read()

    def test_no_at_main_reference(self):
        """setup-chalk-action@main must NOT appear in the file."""
        content = self._read_file()
        self.assertNotIn(
            "setup-chalk-action@main",
            content,
            "Found 'setup-chalk-action@main' — must be replaced with a pinned SHA.",
        )

    def test_sha_pin_present(self):
        """File must contain setup-chalk-action@<40-char-hex-sha>."""
        content = self._read_file()
        match = SHA_PATTERN.search(content)
        self.assertIsNotNone(
            match,
            "Did not find 'crashappsec/setup-chalk-action@<40-char-sha>' in the file.",
        )
        sha = match.group(1)
        self.assertEqual(
            len(sha),
            40,
            f"SHA must be exactly 40 characters, got {len(sha)}: {sha}",
        )
        self.assertTrue(
            re.fullmatch(r"[0-9a-f]{40}", sha),
            f"SHA must be lowercase hex only: {sha}",
        )

    def test_yaml_syntax_valid(self):
        """The YAML file must parse without errors."""
        content = self._read_file()
        try:
            parsed = yaml.safe_load(content)
        except yaml.YAMLError as exc:
            self.fail(f"YAML syntax error: {exc}")
        self.assertIsNotNone(parsed, "Parsed YAML must not be None.")

    def test_uses_line_format(self):
        """The uses line must follow the exact format: uses: crashappsec/setup-chalk-action@<sha>"""
        content = self._read_file()
        # Check that the uses line with SHA exists
        uses_sha_pattern = re.compile(
            r"uses:\s+crashappsec/setup-chalk-action@[0-9a-f]{40}"
        )
        self.assertRegex(
            content,
            uses_sha_pattern,
            "Expected 'uses: crashappsec/setup-chalk-action@<40-char-sha>' line not found.",
        )


if __name__ == "__main__":
    unittest.main()
