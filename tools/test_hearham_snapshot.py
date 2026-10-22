import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(__file__))
from hearham_snapshot import snapshot  # noqa: E402


def entry(**kw):
    base = {"id": 1, "callsign": "UR0UUM", "latitude": 50.45, "longitude": 30.52, "city": "Kyiv, Ukraine",
            "mode": "DMR", "encode": "CC1", "frequency": 438800000, "offset": -7600000, "operational": 1}
    base.update(kw)
    return base


class SnapshotTest(unittest.TestCase):
    def test_keeps_a_listed_repeater_in_range(self):
        r = snapshot([entry()], "2026-10-22")["repeaters"][0]
        self.assertEqual("hearham-1", r["id"])
        self.assertEqual((438.8, 431.2, "DMR", 1, "UNKNOWN"), (r["rxMHz"], r["txMHz"], r["mode"], r["colorCode"], r["status"]))
        self.assertIn("retrieved 2026-10-22", r["source"])

    def test_reads_an_fm_tone(self):
        r = snapshot([entry(mode="FM", encode="88.5", frequency=145600000, offset=-600000)], "d")["repeaters"][0]
        self.assertEqual(("FM", 88.5), (r["mode"], r["ctcssHz"]))
        self.assertNotIn("colorCode", r)

    def test_maps_modes_like_the_app(self):
        modes = [snapshot([entry(mode=m, encode="")], "d")["repeaters"][0]["mode"]
                 for m in ("DMR+", "D-STAR", "YSF/FM", "NFM", "P25")]
        self.assertEqual(["DMR", "D-STAR", "C4FM", "FM", "OTHER"], modes)

    def test_skips_what_it_cannot_trust(self):
        result = snapshot([
            entry(id=2, operational=0),
            entry(id=3, callsign=" "),
            entry(id=4, latitude=0, longitude=0),
            entry(id=5, latitude=48.0, longitude=37.0),
            entry(id=6, frequency=423475000, offset=0),
            entry(id=7, frequency=446000000, offset=0),
        ], "d")
        self.assertEqual([], result["repeaters"])
        self.assertEqual(6, sum(result["skipped"].values()))


if __name__ == "__main__":
    unittest.main()
