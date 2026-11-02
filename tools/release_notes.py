#!/usr/bin/env python3
"""Write the GitHub release notes for a tag: author, what's new and the attached files.

The changes come from the Fastlane changelog of the release's versionCode (English and Ukrainian), read
from the given git ref, so the notes of an older release can be rewritten from a newer checkout.

Usage: release_notes.py TAG [--ref REF] [--debug-only]
"""
import argparse
import re
import subprocess
import sys

AUTHOR = 'Yevhen "CUBE" Honchar (@gm3nnnlab)'
CHANGELOG_SECTIONS = [("en-US", "What's new"), ("uk", "Що нового")]


def version_code(gradle_text):
    match = re.search(r"versionCode\s*=\s*(\d+)", gradle_text)
    if not match:
        raise ValueError("versionCode not found in app/build.gradle.kts")
    return int(match.group(1))


def render(tag, changelogs, debug_only):
    if debug_only:
        lines = [f"Chornobyl HamDash {tag}: test build by {AUTHOR}.", ""]
    else:
        lines = [f"Chornobyl HamDash {tag} by {AUTHOR}.", ""]
    for locale, heading in CHANGELOG_SECTIONS:
        text = changelogs.get(locale, "").strip()
        if text:
            lines += [f"## {heading}", "", text, ""]
    lines += ["## Files", ""]
    if debug_only:
        lines += [
            f"* `chornobyl-hamdash-{tag}-debug.apk`: signed with the public debug key in `keystore/debug.keystore`. "
            "It installs over builds from 0.2.3 on and keeps your data.",
            "",
            "This release has no store-signed APK or App Bundle yet: it was published with the release key skipped. "
            "Do not submit this file to a store.",
        ]
    else:
        lines += [
            f"* `chornobyl-hamdash-{tag}.apk`: release build signed with the project's release key (use this one).",
            f"* `chornobyl-hamdash-{tag}.aab`: the same build as an Android App Bundle for Google Play.",
            f"* `chornobyl-hamdash-{tag}-debug.apk`: test build signed with the public debug key in "
            "`keystore/debug.keystore`.",
            "",
            "The release and debug builds use different keys, so Android will not install one over the other.",
            "Release key SHA-256: see `keystore/release-cert-sha256.txt`.",
        ]
    return "\n".join(lines) + "\n"


def show(ref, path):
    result = subprocess.run(["git", "show", f"{ref}:{path}"], capture_output=True, text=True)
    return result.stdout if result.returncode == 0 else None


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("tag")
    parser.add_argument("--ref", default="HEAD", help="git ref to read the version and changelogs from")
    parser.add_argument("--debug-only", action="store_true", help="the release carries only the debug APK")
    args = parser.parse_args()
    gradle = show(args.ref, "app/build.gradle.kts")
    if gradle is None:
        sys.exit(f"Cannot read app/build.gradle.kts at {args.ref}. Fetch the tag first (actions/checkout with fetch-depth: 0).")
    code = version_code(gradle)
    changelogs = {}
    for locale, _ in CHANGELOG_SECTIONS:
        text = show(args.ref, f"fastlane/metadata/android/{locale}/changelogs/{code}.txt")
        if text:
            changelogs[locale] = text
    sys.stdout.write(render(args.tag, changelogs, args.debug_only))
    return 0


if __name__ == "__main__":
    sys.exit(main())
