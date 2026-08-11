#!/usr/bin/env python3
"""
TDD tests for pinning crashappsec/setup-chalk-action SHA in release-weekly-docker.yml.

Acceptance Criteria:
- .github/workflows/release-weekly-docker.yml contains
  `uses: crashappsec/setup-chalk-action@<40-char-sha>` with no remaining `@main` reference.
- grep -n 'setup-chalk-action@main' returns no output.
- YAML syntax is valid.
"""

import os
import re
import subprocess
import sys
import unittest
import yaml

WORKFLOW_FILE = os.path.join(
    os.path.dirname(__file__),
    '..', '.github', 'workflows', 'release-weekly-docker.yml'
)

SHA_PATTERN = re.compile(r'[0-9a-f]{40}')


class TestChalkActionSHAPin(unittest.TestCase):
    """Tests that setup-chalk-action is pinned to a 40-char SHA in release-weekly-docker.yml."""

    def setUp(self):
        self.workflow_path = os.path.abspath(WORKFLOW_FILE)
        self.assertTrue(
            os.path.isfile(self.workflow_path),
            f"Workflow file not found: {self.workflow_path}"
        )
        with open(self.workflow_path, 'r') as f:
            self.content = f.read()

    def test_no_setup_chalk_action_at_main(self):
        """There must be no 'setup-chalk-action@main' reference in the file."""
        self.assertNotIn(
            'setup-chalk-action@main',
            self.content,
            "Found 'setup-chalk-action@main' in release-weekly-docker.yml - must be replaced with SHA pin"
        )

    def test_setup_chalk_action_uses_40_char_sha(self):
        """The setup-chalk-action reference must use a 40-character hex SHA."""
        # Find all uses: crashappsec/setup-chalk-action@... lines
        matches = re.findall(r'crashappsec/setup-chalk-action@([^\s#]+)', self.content)
        self.assertTrue(
            len(matches) > 0,
            "No 'crashappsec/setup-chalk-action@...' reference found in release-weekly-docker.yml"
        )
        for ref in matches:
            self.assertRegex(
                ref,
                r'^[0-9a-f]{40}$',
                f"setup-chalk-action reference '{ref}' is not a 40-char hex SHA"
            )

    def test_grep_setup_chalk_action_at_main_returns_no_output(self):
        """grep -n 'setup-chalk-action@main' must return no output (exit code 1 = no match)."""
        result = subprocess.run(
            ['grep', '-n', 'setup-chalk-action@main', self.workflow_path],
            capture_output=True,
            text=True
        )
        self.assertEqual(
            result.stdout.strip(),
            '',
            f"grep found 'setup-chalk-action@main' in file:\n{result.stdout}"
        )

    def test_yaml_syntax_is_valid(self):
        """The YAML file must be syntactically valid."""
        try:
            with open(self.workflow_path, 'r') as f:
                yaml.safe_load(f)
        except yaml.YAMLError as e:
            self.fail(f"YAML syntax error in release-weekly-docker.yml: {e}")

    def test_yaml_syntax_valid_via_subprocess(self):
        """python3 -c 'import yaml; yaml.safe_load(open(...))' must exit 0."""
        result = subprocess.run(
            [
                sys.executable, '-c',
                f"import yaml; yaml.safe_load(open('{self.workflow_path}'))"
            ],
            capture_output=True,
            text=True
        )
        self.assertEqual(
            result.returncode,
            0,
            f"YAML validation failed:\n{result.stderr}"
        )


if __name__ == '__main__':
    unittest.main()
