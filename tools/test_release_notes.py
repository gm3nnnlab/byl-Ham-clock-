import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(__file__))
from release_notes import render, version_code  # noqa: E402

CHANGES = {"en-US": "* Real repeaters\n* No ban notice\n", "uk": "* Реальні репітери\n"}


class ReleaseNotesTest(unittest.TestCase):
    def test_names_the_author_and_the_changes(self):
        notes = render("v0.3.3", CHANGES, debug_only=False)
        self.assertTrue(notes.startswith('Chornobyl HamDash v0.3.3 by Yevhen "CUBE" Honchar (@gm3nnnlab).\n'))
        self.assertIn("## What's new\n\n* Real repeaters\n* No ban notice\n", notes)
        self.assertIn("## Що нового\n\n* Реальні репітери\n", notes)
        self.assertLess(notes.index("## What's new"), notes.index("## Що нового"))

    def test_lists_the_signed_files(self):
        notes = render("v0.3.3", CHANGES, debug_only=False)
        for name in ("chornobyl-hamdash-v0.3.3.apk", "chornobyl-hamdash-v0.3.3.aab", "chornobyl-hamdash-v0.3.3-debug.apk"):
            self.assertIn(f"`{name}`", notes)
        self.assertIn("keystore/release-cert-sha256.txt", notes)

    def test_debug_only_release_has_no_signed_files(self):
        notes = render("v0.3.4", {}, debug_only=True)
        self.assertNotIn("chornobyl-hamdash-v0.3.4.apk`", notes)
        self.assertIn("`chornobyl-hamdash-v0.3.4-debug.apk`", notes)
        self.assertIn("Do not submit this file to a store.", notes)
        self.assertNotIn("## What's new", notes)

    def test_reads_the_version_code(self):
        self.assertEqual(9, version_code('        versionCode = 9\n        versionName = "0.3.3"\n'))


if __name__ == "__main__":
    unittest.main()
