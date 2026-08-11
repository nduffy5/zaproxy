#!/usr/bin/env python3
"""
Comprehensive tests to verify all acceptance criteria for:
- Determining current stable versions of zaproxy and pyyaml Python packages
- Verifying zaproxy API compatibility with docker scan scripts
"""

import json
import os
import re
import subprocess
import sys
import unittest
from urllib.request import urlopen
from urllib.error import URLError


REQUIREMENTS_FILE = os.path.join(os.path.dirname(__file__), 'requirements.txt')

# Known stable versions as of the implementation
ZAPROXY_STABLE_VERSION = '0.6.0'
PYYAML_STABLE_VERSION_PREFIX = '6.'  # 6.x series


def _has_attr_or_property(obj, name):
    """Check if obj has attribute 'name' either as instance attr or class property."""
    # Check on the class (catches properties)
    if hasattr(type(obj), name):
        return True
    # Check on the instance
    return hasattr(obj, name)


class TestCriterion1_ZapraxyVersionFromPip(unittest.TestCase):
    """
    Criterion 1: Running `pip index versions zaproxy` returns a version string.
    """

    def test_pip_index_versions_zaproxy_returns_version(self):
        """pip index versions zaproxy must return a version string."""
        result = subprocess.run(
            [sys.executable, '-m', 'pip', 'index', 'versions', 'zaproxy'],
            capture_output=True, text=True
        )
        output = result.stdout + result.stderr
        # Should contain a version number like 0.6.0
        self.assertRegex(
            output,
            r'\d+\.\d+\.\d+',
            'pip index versions zaproxy did not return a version string'
        )

    def test_pip_index_versions_zaproxy_shows_latest(self):
        """pip index versions zaproxy must show the latest version."""
        result = subprocess.run(
            [sys.executable, '-m', 'pip', 'index', 'versions', 'zaproxy'],
            capture_output=True, text=True
        )
        output = result.stdout + result.stderr
        # Should contain LATEST line
        self.assertIn('LATEST', output, 'pip index versions zaproxy did not show LATEST version')

    def test_zaproxy_version_string_is_recorded_in_requirements(self):
        """The zaproxy version string must be recorded in docker/requirements.txt."""
        self.assertTrue(os.path.isfile(REQUIREMENTS_FILE))
        with open(REQUIREMENTS_FILE) as f:
            content = f.read()
        # Must contain zaproxy==<version>
        self.assertRegex(
            content,
            r'zaproxy==\d+\.\d+\.\d+',
            'zaproxy version not recorded in requirements.txt'
        )


class TestCriterion2_PyYamlVersionFromPip(unittest.TestCase):
    """
    Criterion 2: Running `pip index versions pyyaml` returns a version string.
    """

    def test_pip_index_versions_pyyaml_returns_version(self):
        """pip index versions pyyaml must return a version string."""
        result = subprocess.run(
            [sys.executable, '-m', 'pip', 'index', 'versions', 'pyyaml'],
            capture_output=True, text=True
        )
        output = result.stdout + result.stderr
        self.assertRegex(
            output,
            r'\d+\.\d+',
            'pip index versions pyyaml did not return a version string'
        )

    def test_pip_index_versions_pyyaml_shows_latest(self):
        """pip index versions pyyaml must show the latest version."""
        result = subprocess.run(
            [sys.executable, '-m', 'pip', 'index', 'versions', 'pyyaml'],
            capture_output=True, text=True
        )
        output = result.stdout + result.stderr
        self.assertIn('LATEST', output, 'pip index versions pyyaml did not show LATEST version')

    def test_pyyaml_version_string_is_recorded_in_requirements(self):
        """The pyyaml version string must be recorded in docker/requirements.txt."""
        self.assertTrue(os.path.isfile(REQUIREMENTS_FILE))
        with open(REQUIREMENTS_FILE) as f:
            content = f.read()
        self.assertRegex(
            content,
            r'[Pp][Yy][Yy][Aa][Mm][Ll]==\d+\.\d+',
            'pyyaml version not recorded in requirements.txt'
        )


class TestCriterion3_VersionsRecorded(unittest.TestCase):
    """
    Criterion 3: Both version strings are recorded for use in step 11.
    """

    def _parse_requirements(self):
        packages = {}
        with open(REQUIREMENTS_FILE, 'r') as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
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

    def test_zaproxy_version_recorded(self):
        """zaproxy version must be pinned with == in requirements.txt."""
        packages = self._parse_requirements()
        self.assertIn('zaproxy', packages, 'zaproxy not found in requirements.txt')
        version = packages['zaproxy']
        self.assertRegex(version, r'^\d+\.\d+\.\d+$', f'zaproxy version {version!r} is not a valid semver')

    def test_pyyaml_version_recorded(self):
        """pyyaml version must be pinned with == in requirements.txt."""
        packages = self._parse_requirements()
        self.assertIn('pyyaml', packages, 'pyyaml not found in requirements.txt')
        version = packages['pyyaml']
        self.assertRegex(version, r'^\d+\.\d+', f'pyyaml version {version!r} is not a valid version')

    def test_zaproxy_pinned_to_stable_version(self):
        """zaproxy must be pinned to the current stable version (0.6.0)."""
        packages = self._parse_requirements()
        self.assertIn('zaproxy', packages)
        self.assertEqual(
            packages['zaproxy'],
            ZAPROXY_STABLE_VERSION,
            f'zaproxy should be {ZAPROXY_STABLE_VERSION}, got {packages.get("zaproxy")}'
        )

    def test_pyyaml_pinned_to_stable_6x_version(self):
        """pyyaml must be pinned to a stable 6.x version."""
        packages = self._parse_requirements()
        self.assertIn('pyyaml', packages)
        self.assertTrue(
            packages['pyyaml'].startswith(PYYAML_STABLE_VERSION_PREFIX),
            f'pyyaml should be a 6.x version, got {packages.get("pyyaml")}'
        )

    def test_both_packages_use_exact_pinning(self):
        """Both packages must use == (exact) pinning."""
        with open(REQUIREMENTS_FILE) as f:
            content = f.read()
        for pkg in ['zaproxy', 'pyyaml']:
            self.assertRegex(
                content,
                rf'(?i){pkg}==',
                f'{pkg} must use == pinning in requirements.txt'
            )


class TestCriterion4_ZapraxyApiCompatibility(unittest.TestCase):
    """
    Criterion 4: The zaproxy version is compatible with API calls in the docker scan scripts.
    Verifies by reviewing the zaproxy Python client's exposed namespaces and methods.
    
    Note: Properties that make HTTP calls are checked on the class (type), not the instance,
    to avoid connection errors when no ZAP server is running.
    """

    def setUp(self):
        try:
            import zapv2
            self.zapv2 = zapv2
            # Instantiate with a dummy proxy (won't connect, just checks structure)
            self.zap = zapv2.ZAPv2(
                proxies={'http': 'http://localhost:19999',
                         'https': 'http://localhost:19999'}
            )
        except ImportError:
            self.skipTest('zaproxy package not installed')
        except Exception as e:
            self.skipTest(f'Could not instantiate ZAPv2: {e}')

    def _has_method(self, obj, name):
        """Check if obj has a callable method named 'name'."""
        return callable(getattr(obj, name, None))

    def _has_property_or_attr(self, obj, name):
        """Check if obj has attribute 'name' as property on class or instance attr."""
        # Properties are defined on the class
        if hasattr(type(obj), name):
            return True
        return hasattr(obj, name)

    def test_zaproxy_version_is_0_6_0(self):
        """Installed zaproxy must be version 0.6.0."""
        version = getattr(self.zapv2, '__version__', None)
        self.assertIsNotNone(version, 'zaproxy __version__ not found')
        self.assertEqual(version, ZAPROXY_STABLE_VERSION,
                         f'Expected zaproxy {ZAPROXY_STABLE_VERSION}, got {version}')

    # --- Namespaces used in zap_common.py ---

    def test_core_namespace_exists(self):
        """zap.core must exist (used in zap_common.py: zap.core.version, zap.core.urls, etc.)"""
        self.assertTrue(hasattr(self.zap, 'core'), 'zap.core namespace missing')

    def test_core_version_is_property(self):
        """zap.core.version must be a property on the class (used in wait_for_zap_start)."""
        self.assertTrue(
            self._has_property_or_attr(self.zap.core, 'version'),
            'zap.core.version missing'
        )

    def test_core_urls_method(self):
        """zap.core.urls must be callable (used in multiple scan scripts)."""
        self.assertTrue(self._has_method(self.zap.core, 'urls'), 'zap.core.urls not callable')

    def test_core_alerts_method(self):
        """zap.core.alerts must be callable (used in zap_get_alerts)."""
        self.assertTrue(self._has_method(self.zap.core, 'alerts'), 'zap.core.alerts not callable')

    def test_core_message_method(self):
        """zap.core.message must be callable (used in print_rule)."""
        self.assertTrue(self._has_method(self.zap.core, 'message'), 'zap.core.message not callable')

    def test_core_shutdown_method(self):
        """zap.core.shutdown must be callable (used in all scan scripts)."""
        self.assertTrue(self._has_method(self.zap.core, 'shutdown'), 'zap.core.shutdown not callable')

    def test_core_htmlreport_method(self):
        """zap.core.htmlreport must be callable (used in report generation)."""
        self.assertTrue(self._has_method(self.zap.core, 'htmlreport'), 'zap.core.htmlreport not callable')

    def test_core_jsonreport_method(self):
        """zap.core.jsonreport must be callable."""
        self.assertTrue(self._has_method(self.zap.core, 'jsonreport'), 'zap.core.jsonreport not callable')

    def test_core_xmlreport_method(self):
        """zap.core.xmlreport must be callable."""
        self.assertTrue(self._has_method(self.zap.core, 'xmlreport'), 'zap.core.xmlreport not callable')

    def test_core_number_of_messages_method(self):
        """zap.core.number_of_messages must be callable (used in zap-api-scan.py)."""
        self.assertTrue(self._has_method(self.zap.core, 'number_of_messages'),
                         'zap.core.number_of_messages not callable')

    def test_urlopen_method(self):
        """zap.urlopen must be callable (used in zap_access_target)."""
        self.assertTrue(self._has_method(self.zap, 'urlopen'), 'zap.urlopen not callable')

    # --- pscan namespace ---

    def test_pscan_namespace_exists(self):
        """zap.pscan must exist (used in zap_tune, zap_wait_for_passive_scan)."""
        self.assertTrue(hasattr(self.zap, 'pscan'), 'zap.pscan namespace missing')

    def test_pscan_disable_all_tags_method(self):
        """zap.pscan.disable_all_tags must be callable (used in zap_tune)."""
        self.assertTrue(self._has_method(self.zap.pscan, 'disable_all_tags'),
                         'zap.pscan.disable_all_tags not callable')

    def test_pscan_set_max_alerts_per_rule_method(self):
        """zap.pscan.set_max_alerts_per_rule must be callable (used in zap_tune)."""
        self.assertTrue(self._has_method(self.zap.pscan, 'set_max_alerts_per_rule'),
                         'zap.pscan.set_max_alerts_per_rule not callable')

    def test_pscan_records_to_scan_is_property(self):
        """zap.pscan.records_to_scan must be a property (used in zap_wait_for_passive_scan)."""
        self.assertTrue(
            self._has_property_or_attr(self.zap.pscan, 'records_to_scan'),
            'zap.pscan.records_to_scan missing'
        )

    def test_pscan_scanners_is_property(self):
        """zap.pscan.scanners must be a property (used in zap-baseline.py and zap-full-scan.py)."""
        self.assertTrue(
            self._has_property_or_attr(self.zap.pscan, 'scanners'),
            'zap.pscan.scanners missing'
        )

    # --- spider namespace ---

    def test_spider_namespace_exists(self):
        """zap.spider must exist (used in zap_spider)."""
        self.assertTrue(hasattr(self.zap, 'spider'), 'zap.spider namespace missing')

    def test_spider_scan_method(self):
        """zap.spider.scan must be callable (used in zap_spider)."""
        self.assertTrue(self._has_method(self.zap.spider, 'scan'), 'zap.spider.scan not callable')

    def test_spider_scan_as_user_method(self):
        """zap.spider.scan_as_user must be callable (used in zap_spider with user)."""
        self.assertTrue(self._has_method(self.zap.spider, 'scan_as_user'),
                         'zap.spider.scan_as_user not callable')

    def test_spider_status_method(self):
        """zap.spider.status must be callable (used in zap_spider polling loop)."""
        self.assertTrue(self._has_method(self.zap.spider, 'status'), 'zap.spider.status not callable')

    # --- ajaxSpider namespace ---

    def test_ajax_spider_namespace_exists(self):
        """zap.ajaxSpider must exist (used in zap_ajax_spider)."""
        self.assertTrue(hasattr(self.zap, 'ajaxSpider'), 'zap.ajaxSpider namespace missing')

    def test_ajax_spider_scan_method(self):
        """zap.ajaxSpider.scan must be callable."""
        self.assertTrue(self._has_method(self.zap.ajaxSpider, 'scan'), 'zap.ajaxSpider.scan not callable')

    def test_ajax_spider_scan_as_user_method(self):
        """zap.ajaxSpider.scan_as_user must be callable."""
        self.assertTrue(self._has_method(self.zap.ajaxSpider, 'scan_as_user'),
                         'zap.ajaxSpider.scan_as_user not callable')

    def test_ajax_spider_set_option_max_duration_method(self):
        """zap.ajaxSpider.set_option_max_duration must be callable."""
        self.assertTrue(self._has_method(self.zap.ajaxSpider, 'set_option_max_duration'),
                         'zap.ajaxSpider.set_option_max_duration not callable')

    def test_ajax_spider_status_is_property(self):
        """zap.ajaxSpider.status must be a property (used in polling loop)."""
        self.assertTrue(
            self._has_property_or_attr(self.zap.ajaxSpider, 'status'),
            'zap.ajaxSpider.status missing'
        )

    def test_ajax_spider_number_of_results_is_property(self):
        """zap.ajaxSpider.number_of_results must be a property."""
        self.assertTrue(
            self._has_property_or_attr(self.zap.ajaxSpider, 'number_of_results'),
            'zap.ajaxSpider.number_of_results missing'
        )

    # --- clientSpider namespace ---

    def test_client_spider_namespace_exists(self):
        """zap.clientSpider must exist (used in zap_client_spider)."""
        self.assertTrue(hasattr(self.zap, 'clientSpider'), 'zap.clientSpider namespace missing')

    def test_client_spider_scan_method(self):
        """zap.clientSpider.scan must be callable."""
        self.assertTrue(self._has_method(self.zap.clientSpider, 'scan'),
                         'zap.clientSpider.scan not callable')

    def test_client_spider_set_option_max_duration_method(self):
        """zap.clientSpider.set_option_max_duration must be callable."""
        self.assertTrue(self._has_method(self.zap.clientSpider, 'set_option_max_duration'),
                         'zap.clientSpider.set_option_max_duration not callable')

    def test_client_spider_status_method(self):
        """zap.clientSpider.status must be callable."""
        self.assertTrue(self._has_method(self.zap.clientSpider, 'status'),
                         'zap.clientSpider.status not callable')

    # --- ascan namespace ---

    def test_ascan_namespace_exists(self):
        """zap.ascan must exist (used in zap_active_scan)."""
        self.assertTrue(hasattr(self.zap, 'ascan'), 'zap.ascan namespace missing')

    def test_ascan_scan_method(self):
        """zap.ascan.scan must be callable."""
        self.assertTrue(self._has_method(self.zap.ascan, 'scan'), 'zap.ascan.scan not callable')

    def test_ascan_scan_as_user_method(self):
        """zap.ascan.scan_as_user must be callable."""
        self.assertTrue(self._has_method(self.zap.ascan, 'scan_as_user'),
                         'zap.ascan.scan_as_user not callable')

    def test_ascan_status_method(self):
        """zap.ascan.status must be callable."""
        self.assertTrue(self._has_method(self.zap.ascan, 'status'), 'zap.ascan.status not callable')

    def test_ascan_scan_progress_method(self):
        """zap.ascan.scan_progress must be callable."""
        self.assertTrue(self._has_method(self.zap.ascan, 'scan_progress'),
                         'zap.ascan.scan_progress not callable')

    def test_ascan_enable_all_scanners_method(self):
        """zap.ascan.enable_all_scanners must be callable (used in zap-full-scan.py)."""
        self.assertTrue(self._has_method(self.zap.ascan, 'enable_all_scanners'),
                         'zap.ascan.enable_all_scanners not callable')

    def test_ascan_set_scanner_alert_threshold_method(self):
        """zap.ascan.set_scanner_alert_threshold must be callable."""
        self.assertTrue(self._has_method(self.zap.ascan, 'set_scanner_alert_threshold'),
                         'zap.ascan.set_scanner_alert_threshold not callable')

    def test_ascan_scanners_method(self):
        """zap.ascan.scanners must be callable (used in zap-full-scan.py and zap-api-scan.py)."""
        self.assertTrue(self._has_method(self.zap.ascan, 'scanners'), 'zap.ascan.scanners not callable')

    # --- context namespace ---

    def test_context_namespace_exists(self):
        """zap.context must exist (used in zap_import_context)."""
        self.assertTrue(hasattr(self.zap, 'context'), 'zap.context namespace missing')

    def test_context_import_context_method(self):
        """zap.context.import_context must be callable."""
        self.assertTrue(self._has_method(self.zap.context, 'import_context'),
                         'zap.context.import_context not callable')

    def test_context_context_list_is_property(self):
        """zap.context.context_list must be a property (used in zap_import_context)."""
        self.assertTrue(
            self._has_property_or_attr(self.zap.context, 'context_list'),
            'zap.context.context_list missing'
        )

    # --- users namespace ---

    def test_users_namespace_exists(self):
        """zap.users must exist (used in zap_import_context)."""
        self.assertTrue(hasattr(self.zap, 'users'), 'zap.users namespace missing')

    def test_users_users_list_method(self):
        """zap.users.users_list must be callable."""
        self.assertTrue(self._has_method(self.zap.users, 'users_list'),
                         'zap.users.users_list not callable')

    # --- openapi namespace (used in zap-api-scan.py) ---

    def test_openapi_namespace_exists(self):
        """zap.openapi must exist (used in zap-api-scan.py for OpenAPI imports)."""
        self.assertTrue(hasattr(self.zap, 'openapi'), 'zap.openapi namespace missing')

    def test_openapi_import_url_method(self):
        """zap.openapi.import_url must be callable."""
        self.assertTrue(self._has_method(self.zap.openapi, 'import_url'),
                         'zap.openapi.import_url not callable')

    def test_openapi_import_file_method(self):
        """zap.openapi.import_file must be callable."""
        self.assertTrue(self._has_method(self.zap.openapi, 'import_file'),
                         'zap.openapi.import_file not callable')

    # --- graphql namespace (used in zap-api-scan.py) ---

    def test_graphql_namespace_exists(self):
        """zap.graphql must exist (used in zap-api-scan.py for GraphQL imports)."""
        self.assertTrue(hasattr(self.zap, 'graphql'), 'zap.graphql namespace missing')

    def test_graphql_import_url_method(self):
        """zap.graphql.import_url must be callable."""
        self.assertTrue(self._has_method(self.zap.graphql, 'import_url'),
                         'zap.graphql.import_url not callable')

    def test_graphql_import_file_method(self):
        """zap.graphql.import_file must be callable."""
        self.assertTrue(self._has_method(self.zap.graphql, 'import_file'),
                         'zap.graphql.import_file not callable')

    # --- script namespace (used in zap-api-scan.py) ---

    def test_script_namespace_exists(self):
        """zap.script must exist (used in zap-api-scan.py)."""
        self.assertTrue(hasattr(self.zap, 'script'), 'zap.script namespace missing')

    def test_script_load_method(self):
        """zap.script.load must be callable."""
        self.assertTrue(self._has_method(self.zap.script, 'load'), 'zap.script.load not callable')

    def test_script_enable_method(self):
        """zap.script.enable must be callable."""
        self.assertTrue(self._has_method(self.zap.script, 'enable'), 'zap.script.enable not callable')

    def test_script_list_engines_is_property(self):
        """zap.script.list_engines must be a property (used in get_script_engine)."""
        self.assertTrue(
            self._has_property_or_attr(self.zap.script, 'list_engines'),
            'zap.script.list_engines missing'
        )

    # --- _request method (used in zap-api-scan.py for SOAP) ---

    def test_request_method_exists(self):
        """zap._request must be callable (used in zap-api-scan.py for SOAP imports)."""
        self.assertTrue(self._has_method(self.zap, '_request'), 'zap._request not callable')

    def test_base_attribute_exists(self):
        """zap.base must be accessible (used in zap-api-scan.py for SOAP URL construction)."""
        self.assertTrue(hasattr(self.zap, 'base'), 'zap.base attribute missing')


class TestPyYamlCompatibility(unittest.TestCase):
    """Verify pyyaml is importable and works correctly."""

    def test_yaml_importable(self):
        """yaml module must be importable."""
        try:
            import yaml
        except ImportError:
            self.fail('yaml (pyyaml) is not importable')

    def test_yaml_dump_works(self):
        """yaml.dump must work correctly (used in zap-baseline.py)."""
        import yaml
        data = {'env': {'contexts': [{'name': 'test', 'urls': ['http://example.com']}]}}
        result = yaml.dump(data)
        self.assertIsInstance(result, str)
        self.assertIn('contexts', result)

    def test_yaml_version_is_6x(self):
        """Installed pyyaml must be version 6.x."""
        import yaml
        version = getattr(yaml, '__version__', None)
        self.assertIsNotNone(version, 'yaml.__version__ not found')
        self.assertTrue(
            version.startswith('6.'),
            f'pyyaml version should be 6.x, got {version}'
        )


class TestPyPiVersionQuery(unittest.TestCase):
    """
    Verify that PyPI can be queried for version information.
    Tests the curl/PyPI JSON API approach mentioned in acceptance criteria.
    """

    def _query_pypi(self, package_name):
        """Query PyPI JSON API for package version info."""
        try:
            url = f'https://pypi.org/pypi/{package_name}/json'
            response = urlopen(url, timeout=15)
            data = json.loads(response.read().decode('utf-8'))
            return data
        except URLError as e:
            self.skipTest(f'Cannot reach PyPI: {e}')
        except Exception as e:
            self.skipTest(f'PyPI query failed: {e}')

    def test_zaproxy_pypi_returns_version(self):
        """PyPI JSON API for zaproxy must return a version string."""
        data = self._query_pypi('zaproxy')
        self.assertIn('info', data, 'PyPI response missing "info" key')
        self.assertIn('version', data['info'], 'PyPI response missing "version" in info')
        version = data['info']['version']
        self.assertRegex(version, r'^\d+\.\d+\.\d+$',
                          f'zaproxy PyPI version {version!r} is not valid semver')

    def test_pyyaml_pypi_returns_version(self):
        """PyPI JSON API for pyyaml must return a version string."""
        data = self._query_pypi('pyyaml')
        self.assertIn('info', data, 'PyPI response missing "info" key')
        self.assertIn('version', data['info'], 'PyPI response missing "version" in info')
        version = data['info']['version']
        self.assertRegex(version, r'^\d+\.\d+',
                          f'pyyaml PyPI version {version!r} is not valid')

    def test_zaproxy_stable_version_is_0_6_0(self):
        """The current stable zaproxy version on PyPI must be 0.6.0."""
        data = self._query_pypi('zaproxy')
        latest = data['info']['version']
        self.assertEqual(latest, ZAPROXY_STABLE_VERSION,
                          f'Expected zaproxy stable {ZAPROXY_STABLE_VERSION}, PyPI says {latest}')

    def test_pyyaml_stable_version_is_6x(self):
        """The current stable pyyaml version on PyPI must be 6.x."""
        data = self._query_pypi('pyyaml')
        latest = data['info']['version']
        self.assertTrue(latest.startswith('6.'),
                         f'Expected pyyaml 6.x, PyPI says {latest}')


if __name__ == '__main__':
    unittest.main(verbosity=2)
