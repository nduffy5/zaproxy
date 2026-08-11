"""
Acceptance criteria verification tests for:
"Update Dockerfiles to install Python deps with --require-hashes"

Criteria:
1. Every `pip install` invocation in any Dockerfile under `docker/` that installs
   from requirements.txt now uses `pip install --require-hashes -r requirements.lock`
   (or equivalent).
2. `grep 'pip install' docker/Dockerfile-stable docker/Dockerfile-live
   docker/Dockerfile-weekly docker/Dockerfile-bare docker/Dockerfile-tests`
   shows `--require-hashes` in every pip install line that references requirements files.
"""
import os
import re
import subprocess
import pytest

DOCKER_DIR = os.path.dirname(os.path.abspath(__file__))

DOCKERFILES_WITH_PIP = [
    "Dockerfile-stable",
    "Dockerfile-live",
    "Dockerfile-weekly",
]

ALL_DOCKERFILES = [
    "Dockerfile-stable",
    "Dockerfile-live",
    "Dockerfile-weekly",
    "Dockerfile-bare",
    "Dockerfile-tests",
]


def read_dockerfile(name):
    path = os.path.join(DOCKER_DIR, name)
    with open(path) as f:
        return f.read()


def get_pip_install_lines(content):
    """Return all lines containing 'pip install' or 'pip3 install'."""
    return [
        line.strip()
        for line in content.splitlines()
        if re.search(r'pip3?\s+install', line)
    ]


# ─────────────────────────────────────────────────────────────────────────────
# Criterion 1: Every pip install that references a requirements file uses
#              --require-hashes and references requirements.lock (not .txt)
# ─────────────────────────────────────────────────────────────────────────────

class TestCriterion1_RequireHashesOnAllPipInstalls:

    @pytest.mark.parametrize("dockerfile", ALL_DOCKERFILES)
    def test_no_pip_install_references_requirements_txt(self, dockerfile):
        """No pip install line should reference requirements.txt."""
        content = read_dockerfile(dockerfile)
        pip_lines = get_pip_install_lines(content)
        for line in pip_lines:
            assert "requirements.txt" not in line, (
                f"{dockerfile}: pip install references requirements.txt (should use "
                f"requirements.lock): {line!r}"
            )

    @pytest.mark.parametrize("dockerfile", ALL_DOCKERFILES)
    def test_pip_install_with_requirements_file_uses_require_hashes(self, dockerfile):
        """Any pip install that uses -r <file> must include --require-hashes."""
        content = read_dockerfile(dockerfile)
        pip_lines = get_pip_install_lines(content)
        for line in pip_lines:
            if "-r " in line:
                assert "--require-hashes" in line, (
                    f"{dockerfile}: pip install with -r flag is missing "
                    f"--require-hashes: {line!r}"
                )

    @pytest.mark.parametrize("dockerfile", ALL_DOCKERFILES)
    def test_pip_install_with_requirements_file_uses_requirements_lock(self, dockerfile):
        """Any pip install that uses -r <file> must reference requirements.lock."""
        content = read_dockerfile(dockerfile)
        pip_lines = get_pip_install_lines(content)
        for line in pip_lines:
            if "-r " in line:
                assert "requirements.lock" in line, (
                    f"{dockerfile}: pip install with -r flag does not reference "
                    f"requirements.lock: {line!r}"
                )

    @pytest.mark.parametrize("dockerfile", DOCKERFILES_WITH_PIP)
    def test_dockerfiles_that_install_python_deps_have_pip_install(self, dockerfile):
        """Dockerfiles known to install Python deps must have a pip install line."""
        content = read_dockerfile(dockerfile)
        pip_lines = get_pip_install_lines(content)
        assert len(pip_lines) > 0, (
            f"{dockerfile}: expected at least one pip install line but found none"
        )

    @pytest.mark.parametrize("dockerfile", DOCKERFILES_WITH_PIP)
    def test_requirements_lock_is_copied_before_pip_install(self, dockerfile):
        """COPY requirements.lock must appear before the pip install RUN command."""
        content = read_dockerfile(dockerfile)
        copy_pos = content.find("requirements.lock")
        pip_pos = min(
            (content.find(kw) for kw in ("pip install", "pip3 install")
             if content.find(kw) != -1),
            default=-1
        )
        assert copy_pos != -1, (
            f"{dockerfile}: requirements.lock is not referenced at all"
        )
        assert pip_pos != -1, (
            f"{dockerfile}: no pip install found"
        )
        assert copy_pos < pip_pos, (
            f"{dockerfile}: COPY requirements.lock (pos {copy_pos}) must appear "
            f"before pip install (pos {pip_pos})"
        )


# ─────────────────────────────────────────────────────────────────────────────
# Criterion 2: grep 'pip install' across all five Dockerfiles shows
#              --require-hashes in every pip install line referencing req files
# ─────────────────────────────────────────────────────────────────────────────

class TestCriterion2_GrepShowsRequireHashes:

    def test_grep_pip_install_shows_require_hashes_in_all_matching_lines(self):
        """
        Simulate: grep 'pip install' Dockerfile-stable Dockerfile-live
                  Dockerfile-weekly Dockerfile-bare Dockerfile-tests
        Every line returned that also references a requirements file must
        contain --require-hashes.
        """
        dockerfile_paths = [
            os.path.join(DOCKER_DIR, df) for df in ALL_DOCKERFILES
        ]
        result = subprocess.run(
            ["grep", "pip install"] + dockerfile_paths,
            capture_output=True, text=True
        )
        # grep exits 1 if no matches found; that's acceptable (bare/tests have none)
        matched_lines = result.stdout.strip().splitlines()

        failures = []
        for line in matched_lines:
            # line format: "path/Dockerfile-xxx:RUN pip install ..."
            content_part = line.split(":", 1)[-1] if ":" in line else line
            if "-r " in content_part and "--require-hashes" not in content_part:
                failures.append(line)

        assert not failures, (
            "The following pip install lines reference a requirements file but "
            "are missing --require-hashes:\n" + "\n".join(failures)
        )

    def test_grep_pip_install_stable_has_require_hashes(self):
        """Dockerfile-stable pip install line must contain --require-hashes."""
        content = read_dockerfile("Dockerfile-stable")
        pip_lines = get_pip_install_lines(content)
        assert pip_lines, "Dockerfile-stable has no pip install lines"
        for line in pip_lines:
            assert "--require-hashes" in line, (
                f"Dockerfile-stable pip install missing --require-hashes: {line!r}"
            )

    def test_grep_pip_install_live_has_require_hashes(self):
        """Dockerfile-live pip install line must contain --require-hashes."""
        content = read_dockerfile("Dockerfile-live")
        pip_lines = get_pip_install_lines(content)
        assert pip_lines, "Dockerfile-live has no pip install lines"
        for line in pip_lines:
            assert "--require-hashes" in line, (
                f"Dockerfile-live pip install missing --require-hashes: {line!r}"
            )

    def test_grep_pip_install_weekly_has_require_hashes(self):
        """Dockerfile-weekly pip install line must contain --require-hashes."""
        content = read_dockerfile("Dockerfile-weekly")
        pip_lines = get_pip_install_lines(content)
        assert pip_lines, "Dockerfile-weekly has no pip install lines"
        for line in pip_lines:
            assert "--require-hashes" in line, (
                f"Dockerfile-weekly pip install missing --require-hashes: {line!r}"
            )

    def test_grep_pip_install_bare_has_no_pip_install_lines(self):
        """Dockerfile-bare has no Python deps and should have no pip install lines."""
        content = read_dockerfile("Dockerfile-bare")
        pip_lines = get_pip_install_lines(content)
        # If there are pip install lines, they must all have --require-hashes
        for line in pip_lines:
            if "-r " in line:
                assert "--require-hashes" in line, (
                    f"Dockerfile-bare pip install missing --require-hashes: {line!r}"
                )

    def test_grep_pip_install_tests_has_no_pip_install_lines(self):
        """Dockerfile-tests has no pip install lines (inherits from base image)."""
        content = read_dockerfile("Dockerfile-tests")
        pip_lines = get_pip_install_lines(content)
        # If there are pip install lines, they must all have --require-hashes
        for line in pip_lines:
            if "-r " in line:
                assert "--require-hashes" in line, (
                    f"Dockerfile-tests pip install missing --require-hashes: {line!r}"
                )


# ─────────────────────────────────────────────────────────────────────────────
# Bonus: requirements.lock file integrity checks
# ─────────────────────────────────────────────────────────────────────────────

class TestRequirementsLockIntegrity:

    def test_requirements_lock_exists(self):
        lock_path = os.path.join(DOCKER_DIR, "requirements.lock")
        assert os.path.exists(lock_path), "docker/requirements.lock does not exist"

    def test_requirements_lock_contains_hash_entries(self):
        lock_path = os.path.join(DOCKER_DIR, "requirements.lock")
        with open(lock_path) as f:
            content = f.read()
        assert "--hash=sha256:" in content, (
            "requirements.lock does not contain any --hash=sha256: entries"
        )

    def test_requirements_lock_has_require_hashes_flag(self):
        lock_path = os.path.join(DOCKER_DIR, "requirements.lock")
        with open(lock_path) as f:
            content = f.read()
        assert "--require-hashes" in content, (
            "requirements.lock does not contain the --require-hashes flag"
        )

    def test_requirements_lock_is_not_empty(self):
        lock_path = os.path.join(DOCKER_DIR, "requirements.lock")
        assert os.path.getsize(lock_path) > 0, "requirements.lock is empty"
