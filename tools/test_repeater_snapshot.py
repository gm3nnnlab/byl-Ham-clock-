import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(__file__))
from repeater_snapshot import snapshot  # noqa: E402


def hearham(**kw):
    base = {"id": 1, "callsign": "UR0UUM", "latitude": 50.45, "longitude": 30.52, "city": "Kyiv, Ukraine",
            "mode": "DMR", "encode": "CC1", "frequency": 438800000, "offset": -7600000, "operational": 1}
    base.update(kw)
    return base


def brandmeister(**kw):
    base = {"id": 255016, "callsign": "UR0UUJ", "tx": "439.0250", "rx": "431.4250", "colorcode": 1,
            "lat": 50.42609, "lng": 30.49198, "city": "Kyiv", "last_seen": "2026-10-22 06:34:19"}
    base.update(kw)
    return base


def repeaters(hh=(), bm=()):
    return snapshot(list(hh), list(bm), "2026-10-22")["repeaters"]


class HearHamTest(unittest.TestCase):
    def test_keeps_a_listed_repeater_in_range(self):
        r = repeaters([hearham()])[0]
        self.assertEqual("hearham-1", r["id"])
        self.assertEqual((438.8, 431.2, "DMR", 1, "UNKNOWN"), (r["rxMHz"], r["txMHz"], r["mode"], r["colorCode"], r["status"]))
        self.assertIn("retrieved 2026-10-22", r["source"])

    def test_reads_an_fm_tone(self):
        r = repeaters([hearham(mode="FM", encode="88.5", frequency=145600000, offset=-600000)])[0]
        self.assertEqual(("FM", 88.5), (r["mode"], r["ctcssHz"]))
        self.assertNotIn("colorCode", r)

    def test_maps_modes_like_the_app(self):
        modes = [repeaters([hearham(mode=m, encode="")])[0]["mode"] for m in ("DMR+", "D-STAR", "YSF/FM", "NFM", "P25")]
        self.assertEqual(["DMR", "D-STAR", "C4FM", "FM", "OTHER"], modes)

    def test_skips_what_it_cannot_trust(self):
        result = snapshot([
            hearham(id=2, operational=0),
            hearham(id=3, callsign=" "),
            hearham(id=4, latitude=0, longitude=0),
            hearham(id=5, latitude=48.0, longitude=37.0),
            hearham(id=6, frequency=423475000, offset=0),
            hearham(id=7, frequency=446000000, offset=0),
        ], [], "d")
        self.assertEqual([], result["repeaters"])
        self.assertEqual(6, sum(result["sources"]["hearham"]["skipped"].values()))


class BrandMeisterTest(unittest.TestCase):
    def test_keeps_a_registered_repeater(self):
        r = repeaters(bm=[brandmeister()])[0]
        self.assertEqual(("brandmeister-255016", "UR0UUJ", "Kyiv, Ukraine"), (r["id"], r["callsign"], r["name"]))
        self.assertEqual((439.025, 431.425, "DMR", 1, "UNKNOWN"), (r["rxMHz"], r["txMHz"], r["mode"], r["colorCode"], r["status"]))
        self.assertIn("repeater ID 255016, last seen on the network 2026-10-22", r["source"])

    def test_cleans_locators_out_of_the_place_name(self):
        names = [repeaters(bm=[brandmeister(city=c)])[0]["name"] for c in ("KO50GK, Kyiv", "Zhytomyr, KO40HG")]
        self.assertEqual(["Kyiv, Ukraine", "Zhytomyr, Ukraine"], names)

    def test_keeps_only_ukrainian_stations(self):
        kept = repeaters(
            [hearham(id=8, callsign="EU7A02R"), hearham(id=9, callsign="EM1ABC"), hearham(id=10, callsign="RA3XYZ")],
            [brandmeister(id=257002, callsign="EU1A04R"), brandmeister(id=255017, callsign="US5ZAQ")],
        )
        self.assertEqual({"EM1ABC", "US5ZAQ"}, {r["callsign"] for r in kept})

    def test_drops_hotspots_and_simplex_devices(self):
        result = snapshot([], [
            brandmeister(id=255006501),
            brandmeister(id=2550029),
            brandmeister(id=255099, tx="438.5500", rx="438.5500"),
            brandmeister(id=255098, lat=0, lng=0),
        ], "d")
        self.assertEqual([], result["repeaters"])
        self.assertEqual(4, sum(result["sources"]["brandmeister"]["skipped"].values()))

    def test_lists_a_repeater_from_both_sources_once(self):
        both = repeaters([hearham(callsign="UR0UUJ", frequency=439025000, offset=-7600000)], [brandmeister()])
        self.assertEqual(1, len(both))
        self.assertTrue(both[0]["source"].endswith("also listed as operational on HearHam.live"))


if __name__ == "__main__":
    unittest.main()
