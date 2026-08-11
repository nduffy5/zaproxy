"""
Verification tests for acceptance criteria:
- The digest string is of the form zaproxy/zap-nightly@sha256:<64-hex-chars>
- The digest string is recorded for use (present in the Dockerfile)
"""

import re
import os
import unittest

DOCKERFILE_PATH = os.path.join(os.path.dirname(__file__), "Dockerfile")

# Pattern matching the exact acceptance criteria format
DIGEST_FORMAT_PATTERN = re.compile(
    r"zaproxy/zap-nightly@sha256:[0-9a-f]{64}"
)


class TestAcceptanceCriteria(unittest.TestCase):
    def _read_dockerfile(self):
        with open(DOCKERFILE_PATH, "r") as fh:
            return fh.read()

    def test_digest_format_matches_acceptance_criteria(self):
        """
        Acceptance Criterion 1:
        The digest string must be of the form zaproxy/zap-nightly@sha256:<64-hex-chars>
        """
        content = self._read_dockerfile()
        match = DIGEST_FORMAT_PATTERN.search(content)
        self.assertIsNotNone(
            match,
            "Dockerfile does not contain a digest of the form "
            "'zaproxy/zap-nightly@sha256:<64-hex-chars>'",
        )
        digest_str = match.group(0)
        # Verify it starts with the correct image name
        self.assertTrue(
            digest_str.startswith("zaproxy/zap-nightly@sha256:"),
            f"Digest must start with 'zaproxy/zap-nightly@sha256:', got: {digest_str}",
        )
        # Verify the hex part is exactly 64 lowercase hex characters
        hex_part = digest_str.split("sha256:")[1]
        self.assertEqual(len(hex_part), 64, f"SHA256 hex must be 64 chars, got {len(hex_part)}")
        self.assertTrue(
            re.fullmatch(r"[0-9a-f]{64}", hex_part),
            f"SHA256 hex must be lowercase hex only: {hex_part}",
        )

    def test_digest_is_recorded_in_dockerfile(self):
        """
        Acceptance Criterion 2:
        The digest string is recorded for use (present in the Dockerfile FROM line).
        """
        content = self._read_dockerfile()
        match = DIGEST_FORMAT_PATTERN.search(content)
        self.assertIsNotNone(match, "No digest string found in Dockerfile")
        recorded_digest = match.group(0)
        # Confirm it appears in the FROM line
        from_line_pattern = re.compile(
            r"^FROM\s+.*zaproxy/zap-nightly@sha256:[0-9a-f]{64}",
            re.MULTILINE,
        )
        self.assertRegex(
            content,
            from_line_pattern,
            f"Digest '{recorded_digest}' must appear in the FROM line of the Dockerfile",
        )

    def test_no_mutable_latest_tag(self):
        """
        The mutable ':latest' tag must NOT be used — only the pinned digest.
        """
        content = self._read_dockerfile()
        self.assertNotIn(
            "zaproxy/zap-nightly:latest",
            content,
            "Dockerfile must not reference the mutable ':latest' tag",
        )


if __name__ == "__main__":
    unittest.main()
