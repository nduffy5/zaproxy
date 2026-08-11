"""
TDD tests for pinned SHA digest in docker/chrome/Dockerfile.

Acceptance criteria:
- The Dockerfile FROM line must reference zaproxy/zap-nightly by an immutable
  SHA256 digest rather than the mutable ':latest' tag.
- The digest must match the pattern:
      zaproxy/zap-nightly@sha256:<64 hex characters>
"""

import re
import os
import unittest

DOCKERFILE_PATH = os.path.join(os.path.dirname(__file__), "Dockerfile")

SHA256_PATTERN = re.compile(
    r"^FROM\s+--platform=linux/amd64\s+zaproxy/zap-nightly@sha256:[0-9a-f]{64}\s*$",
    re.MULTILINE,
)

DIGEST_RECORD_PATTERN = re.compile(
    r"zaproxy/zap-nightly@sha256:[0-9a-f]{64}"
)


class TestDockerfileDigest(unittest.TestCase):
    def _read_dockerfile(self):
        with open(DOCKERFILE_PATH, "r") as fh:
            return fh.read()

    def test_from_line_uses_digest_not_latest_tag(self):
        """The FROM line must NOT use the mutable ':latest' tag."""
        content = self._read_dockerfile()
        self.assertNotIn(
            "zaproxy/zap-nightly:latest",
            content,
            "Dockerfile still references the mutable ':latest' tag — pin it to a SHA digest.",
        )

    def test_from_line_contains_sha256_digest(self):
        """The FROM line must reference the image via an immutable sha256 digest."""
        content = self._read_dockerfile()
        self.assertRegex(
            content,
            SHA256_PATTERN,
            "Dockerfile FROM line does not contain a valid sha256 digest reference "
            "(expected: FROM --platform=linux/amd64 zaproxy/zap-nightly@sha256:<64-hex-chars>).",
        )

    def test_digest_string_is_recorded(self):
        """The full digest string must be present and well-formed."""
        content = self._read_dockerfile()
        match = DIGEST_RECORD_PATTERN.search(content)
        self.assertIsNotNone(
            match,
            "Could not find a digest string of the form "
            "'zaproxy/zap-nightly@sha256:<64-hex-chars>' in the Dockerfile.",
        )
        # Verify the hex portion is exactly 64 characters
        digest_ref = match.group(0)
        hex_part = digest_ref.split("sha256:")[1]
        self.assertEqual(
            len(hex_part),
            64,
            f"SHA256 hex digest must be exactly 64 characters, got {len(hex_part)}: {hex_part}",
        )
        self.assertTrue(
            re.fullmatch(r"[0-9a-f]{64}", hex_part),
            f"SHA256 hex digest must contain only lowercase hex characters: {hex_part}",
        )


if __name__ == "__main__":
    unittest.main()
