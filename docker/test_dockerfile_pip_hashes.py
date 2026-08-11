"""
TDD tests verifying that all Dockerfiles under docker/ that contain pip install
lines use --require-hashes and reference requirements.lock (not bare package names
or requirements.txt).
"""
import re
import os
import pytest

DOCKER_DIR = os.path.join(os.path.dirname(__file__))

DOCKERFILES = [
    "Dockerfile-stable",
    "Dockerfile-live",
    "Dockerfile-weekly",
    "Dockerfile-bare",
    "Dockerfile-tests",
]


def get_dockerfile_content(dockerfile_name):
    path = os.path.join(DOCKER_DIR, dockerfile_name)
    with open(path) as f:
        return f.read()


def get_run_blocks_with_pip(content):
    """
    Extract full RUN command blocks (handling line continuations) that contain
    'pip install' or 'pip3 install'.
    Returns a list of the full RUN block strings.
    """
    # Join continuation lines: lines ending with \ are continued
    # We'll parse the file line by line, accumulating RUN blocks
    lines = content.splitlines()
    blocks = []
    current_block = None

    for line in lines:
        stripped = line.strip()
        if current_block is not None:
            # We're inside a multi-line RUN command
            current_block += " " + stripped.rstrip("\\").strip()
            if not stripped.endswith("\\"):
                # End of the block
                blocks.append(current_block)
                current_block = None
        else:
            if stripped.startswith("RUN ") or stripped == "RUN":
                if stripped.endswith("\\"):
                    current_block = stripped.rstrip("\\").strip()
                else:
                    blocks.append(stripped)

    if current_block is not None:
        blocks.append(current_block)

    # Filter to only blocks containing pip install
    pip_blocks = [b for b in blocks if "pip install" in b or "pip3 install" in b]
    return pip_blocks


class TestDockerfilePipInstallUsesRequireHashes:
    """Every pip install block that installs packages must use --require-hashes."""

    @pytest.mark.parametrize("dockerfile", DOCKERFILES)
    def test_pip_install_blocks_use_require_hashes(self, dockerfile):
        content = get_dockerfile_content(dockerfile)
        pip_blocks = get_run_blocks_with_pip(content)
        if not pip_blocks:
            pytest.skip(f"{dockerfile} has no pip install blocks")
        for block in pip_blocks:
            assert "--require-hashes" in block, (
                f"{dockerfile}: pip install block missing --require-hashes:\n{block!r}"
            )

    @pytest.mark.parametrize("dockerfile", DOCKERFILES)
    def test_pip_install_blocks_reference_requirements_lock(self, dockerfile):
        content = get_dockerfile_content(dockerfile)
        pip_blocks = get_run_blocks_with_pip(content)
        if not pip_blocks:
            pytest.skip(f"{dockerfile} has no pip install blocks")
        for block in pip_blocks:
            assert "requirements.lock" in block, (
                f"{dockerfile}: pip install block does not reference requirements.lock:\n{block!r}"
            )

    @pytest.mark.parametrize("dockerfile", DOCKERFILES)
    def test_pip_install_does_not_use_bare_requirements_txt(self, dockerfile):
        content = get_dockerfile_content(dockerfile)
        pip_blocks = get_run_blocks_with_pip(content)
        if not pip_blocks:
            pytest.skip(f"{dockerfile} has no pip install blocks")
        for block in pip_blocks:
            assert "-r requirements.txt" not in block, (
                f"{dockerfile}: pip install still references requirements.txt:\n{block!r}"
            )

    @pytest.mark.parametrize("dockerfile", DOCKERFILES)
    def test_requirements_lock_is_copied_before_pip_install(self, dockerfile):
        content = get_dockerfile_content(dockerfile)
        pip_blocks = get_run_blocks_with_pip(content)
        if not pip_blocks:
            pytest.skip(f"{dockerfile} has no pip install blocks")
        # requirements.lock must be COPYed somewhere in the Dockerfile
        assert "requirements.lock" in content, (
            f"{dockerfile}: requirements.lock is not referenced in the Dockerfile"
        )
        # The COPY of requirements.lock must appear before the RUN pip install line
        copy_pos = content.find("requirements.lock")
        pip_pos = content.find("pip install")
        if pip_pos == -1:
            pip_pos = content.find("pip3 install")
        assert copy_pos < pip_pos, (
            f"{dockerfile}: COPY requirements.lock (pos {copy_pos}) must appear "
            f"before pip install (pos {pip_pos})"
        )


class TestRequirementsLockExists:
    def test_requirements_lock_file_exists(self):
        lock_path = os.path.join(DOCKER_DIR, "requirements.lock")
        assert os.path.exists(lock_path), (
            "docker/requirements.lock does not exist"
        )

    def test_requirements_lock_contains_hashes(self):
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
            "requirements.lock does not contain --require-hashes flag"
        )
