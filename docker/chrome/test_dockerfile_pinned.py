"""
TDD tests verifying that docker/chrome/Dockerfile has its base image
pinned to an immutable SHA256 digest instead of a mutable tag.
"""
import re
import pathlib

DOCKERFILE = pathlib.Path(__file__).parent / "Dockerfile"


def _first_line():
    return DOCKERFILE.read_text().splitlines()[0]


def test_no_mutable_latest_tag():
    """The FROM line must not contain ':latest' or any other mutable tag."""
    line = _first_line()
    assert ":latest" not in line, (
        f"Mutable ':latest' tag found in FROM line: {line!r}"
    )


def test_from_uses_sha256_digest():
    """The FROM line must reference the image via a SHA256 digest."""
    line = _first_line()
    assert "@sha256:" in line, (
        f"No '@sha256:' digest found in FROM line: {line!r}"
    )


def test_digest_is_64_hex_chars():
    """The digest value must be exactly 64 lowercase hex characters."""
    line = _first_line()
    match = re.search(r"@sha256:([0-9a-f]+)", line)
    assert match is not None, f"Could not parse sha256 digest from: {line!r}"
    digest_hex = match.group(1)
    assert len(digest_hex) == 64, (
        f"Digest hex part should be 64 chars, got {len(digest_hex)}: {digest_hex!r}"
    )


def test_platform_flag_preserved():
    """The --platform=linux/amd64 flag must still be present."""
    line = _first_line()
    assert "--platform=linux/amd64" in line, (
        f"--platform=linux/amd64 missing from FROM line: {line!r}"
    )


def test_image_name_preserved():
    """The image name zaproxy/zap-nightly must still be present."""
    line = _first_line()
    assert "zaproxy/zap-nightly" in line, (
        f"Image name 'zaproxy/zap-nightly' missing from FROM line: {line!r}"
    )


def test_pinned_date_comment():
    """The FROM line must include an inline comment recording the pin date."""
    line = _first_line()
    assert "# pinned" in line, (
        f"No '# pinned <date>' comment found on FROM line: {line!r}"
    )
    # Comment should contain a date-like string YYYY-MM-DD
    assert re.search(r"# pinned \d{4}-\d{2}-\d{2}", line), (
        f"Pin comment does not contain a YYYY-MM-DD date: {line!r}"
    )


def test_from_keyword_present():
    """The line must start with FROM."""
    line = _first_line()
    assert line.startswith("FROM "), (
        f"First line does not start with 'FROM ': {line!r}"
    )
