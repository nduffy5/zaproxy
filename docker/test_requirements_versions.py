#!/usr/bin/env python3
"""
TDD tests to verify that docker/requirements.txt pins zaproxy and pyyaml
to specific stable versions compatible with the docker scan scripts.
"""

import os
import re
import sys
import unittest


REQUIREMENTS_FILE = os.path.join(os.path.dirname(__file__), 'requirements.txt')

# Latest stable versions as queried from PyPI
EXPECTED_ZAPROXY_VERSION = '0.6.0'
EXPECTED_PYYAML_VERSION = '6.0.2'  # minimum acceptable 6.x version


class TestRequirementsPinned(unittest.TestCase):
    """Verify docker/requirements.txt pins both packages to exact stable versions."""

    def _parse_requirements(self):
        """Return a dict of {package_name_lower: version_string} from requirements.txt."""
        packages = {}
        with open(REQUIREMENTS_FILE, 'r') as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                # Match lines like: package==version
                m = re.match(r'^([A-Za-z0-9_\-]+)\s*==\s*([^\s#]+)', line)
                if m:
                    packages[m.group(1).lower()] = m.group(2)
        return packages

    def test_requirements_file_exists(self):
        """docker/requirements.txt must exist."""
        self.assertTrue(
            os.path.isfile(REQUIREMENTS_FILE),
            f'requirements.txt not found at {REQUIREMENTS_FILE}'
        )

    def test_zaproxy_is_pinned(self):
        """zaproxy must be pinned with == in requirements.txt."""
        packages = self._parse_requirements()
        self.assertIn(
            'zaproxy', packages,
            'zaproxy must be pinned with == in docker/requirements.txt'
        )

    def test_pyyaml_is_pinned(self):
        """pyyaml must be pinned with == in requirements.txt."""
        packages = self._parse_requirements()
        self.assertIn(
            'pyyaml', packages,
            'pyyaml must be pinned with == in docker/requirements.txt'
        )

    def test_zaproxy_version_is_latest_stable(self):
        """zaproxy must be pinned to the latest stable version 0.6.0."""
        packages = self._parse_requirements()
        self.assertIn('zaproxy', packages)
        self.assertEqual(
            packages['zaproxy'],
            EXPECTED_ZAPROXY_VERSION,
            f'zaproxy should be pinned to {EXPECTED_ZAPROXY_VERSION}, '
            f'got {packages.get("zaproxy")}'
        )

    def test_pyyaml_version_is_latest_stable(self):
        """pyyaml must be pinned to a stable 6.x version (6.0.2 or 6.0.3)."""
        packages = self._parse_requirements()
        self.assertIn('pyyaml', packages)
        pinned = packages.get('pyyaml', '')
        self.assertRegex(
            pinned,
            r'^6\.',
            f'pyyaml should be pinned to a 6.x stable version, got {pinned}'
        )

    def test_zaproxy_api_compatibility(self):
        """
        zaproxy 0.6.0 must expose all API namespaces used by the docker scan scripts.
        This test imports the installed zaproxy package and checks for required attributes.
        """
        try:
            import zapv2
        except ImportError:
            self.skipTest('zaproxy package not installed in this environment')

        required_namespaces = [
            'core', 'spider', 'ascan', 'pscan', 'ajaxSpider',
            'clientSpider', 'context', 'users', 'openapi', 'graphql',
            'script',
        ]
        try:
            zap = zapv2.ZAPv2(
                proxies={'http': 'http://localhost:19999',
                         'https': 'http://localhost:19999'}
            )
        except Exception:
            self.skipTest('Could not instantiate ZAPv2')

        for ns in required_namespaces:
            self.assertTrue(
                hasattr(zap, ns),
                f'zaproxy 0.6.0 is missing namespace: {ns}'
            )

    def test_no_unpinned_packages(self):
        """All packages in requirements.txt must use == pinning (no bare names)."""
        with open(REQUIREMENTS_FILE, 'r') as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                self.assertIn(
                    '==', line,
                    f'Package line is not pinned with ==: {line!r}'
                )


if __name__ == '__main__':
    unittest.main()
