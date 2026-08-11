"""
TDD tests for pinning chalk-action SHA in release-weekly.yml.

Acceptance criteria:
- File contains `uses: crashappsec/setup-chalk-action@<40-char-sha>` (no @main).
- An inline comment on the same line reads `# chalk-action main as of <YYYY-MM-DD>` (or equivalent).
- `grep -n 'setup-chalk-action@main'` returns no output.
- YAML syntax is valid.
- `grep -rn 'setup-chalk-action@main' .github/workflows/` returns no output (all four files patched).
"""

import re
import subprocess
import sys
from pathlib import Path

WORKFLOW_FILE = Path(__file__).parents[3] / ".github" / "workflows" / "release-weekly.yml"
WORKFLOWS_DIR = Path(__file__).parents[3] / ".github" / "workflows"


def read_workflow():
    return WORKFLOW_FILE.read_text()


def test_no_at_main_reference():
    """setup-chalk-action@main must not appear anywhere in the file."""
    content = read_workflow()
    assert "setup-chalk-action@main" not in content, (
        "Found mutable '@main' reference; it must be replaced with a pinned SHA."
    )


def test_uses_40_char_sha():
    """The chalk-action step must reference a 40-character hex SHA."""
    content = read_workflow()
    pattern = r"uses:\s+crashappsec/setup-chalk-action@([0-9a-f]{40})"
    match = re.search(pattern, content)
    assert match is not None, (
        "Could not find `uses: crashappsec/setup-chalk-action@<40-char-sha>` in the file."
    )


def test_inline_comment_present():
    """The pinned line must carry an inline comment identifying the pinned version."""
    content = read_workflow()
    for line in content.splitlines():
        if "crashappsec/setup-chalk-action@" in line:
            assert "#" in line, (
                f"No inline comment found on the chalk-action uses line:\n  {line!r}"
            )
            date_pattern = r"\d{4}-\d{2}-\d{2}"
            assert re.search(date_pattern, line), (
                f"Inline comment does not contain a YYYY-MM-DD date on line:\n  {line!r}"
            )
            return  # test passed
    raise AssertionError("No line containing 'crashappsec/setup-chalk-action@' was found.")


def test_grep_returns_no_output_for_file():
    """grep for setup-chalk-action@main in this file must return exit code 1 (no matches)."""
    result = subprocess.run(
        ["grep", "-n", "setup-chalk-action@main", str(WORKFLOW_FILE)],
        capture_output=True,
        text=True,
    )
    assert result.returncode == 1, (
        f"grep found '@main' references (exit code {result.returncode}):\n{result.stdout}"
    )
    assert result.stdout == "", f"grep output should be empty, got:\n{result.stdout}"


def test_grep_returns_no_output_across_all_workflows():
    """grep -rn 'setup-chalk-action@main' across all .yml workflows must return exit code 1."""
    result = subprocess.run(
        ["grep", "-rn", "--include=*.yml", "setup-chalk-action@main", str(WORKFLOWS_DIR)],
        capture_output=True,
        text=True,
    )
    assert result.returncode == 1, (
        f"grep found '@main' references in workflows dir (exit code {result.returncode}):\n{result.stdout}"
    )
    assert result.stdout == "", f"grep output should be empty, got:\n{result.stdout}"


def test_yaml_syntax_valid():
    """The YAML file must parse without errors."""
    result = subprocess.run(
        [sys.executable, "-c",
         f"import yaml; yaml.safe_load(open('{WORKFLOW_FILE}'))"],
        capture_output=True,
        text=True,
    )
    assert result.returncode == 0, (
        f"YAML parse failed:\n{result.stderr}"
    )


if __name__ == "__main__":
    import pytest
    sys.exit(pytest.main([__file__, "-v"]))
