#!/usr/bin/env python3
"""Make a dated snapshot of the public HearHam.live repeater list near Chornobyl.

Downloads https://hearham.com/api/repeaters/v1 (worldwide, about 10 MB) and keeps entries that are
listed as operational, have a callsign and coordinates, lie within RADIUS_KM of Chornobyl and whose
receive and transmit frequencies are both inside IARU Region 1 amateur bands. The result uses the
app's catalog repeater format, with status UNKNOWN: a directory listing does not prove a repeater is on air.

Usage: hearham_snapshot.py OUTPUT.json [--input saved-list.json]
"""
import argparse
import datetime
import json
import math
import re
import sys
import urllib.request

URL = "https://hearham.com/api/repeaters/v1"
CENTER_LAT, CENTER_LON = 51.2763, 30.2219
RADIUS_KM = 300.0
AMATEUR_BANDS_MHZ = [(28.0, 29.7), (50.0, 54.0), (144.0, 146.0), (430.0, 440.0), (1240.0, 1300.0)]
COLOR_CODE = re.compile(r"CC\s*(\d+)", re.IGNORECASE)


def distance_km(lat, lon):
    p1, p2 = math.radians(CENTER_LAT), math.radians(lat)
    dp, dl = p2 - p1, math.radians(lon - CENTER_LON)
    h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * 6371.0 * math.asin(math.sqrt(h))


def in_band(mhz):
    return any(lo <= mhz <= hi for lo, hi in AMATEUR_BANDS_MHZ)


def catalog_mode(raw):
    primary = raw.upper().split("/")[0].replace("-", "").strip()
    if primary.startswith("DMR"):
        return "DMR"
    if primary == "DSTAR":
        return "D-STAR"
    if primary in ("YSF", "C4FM", "FUSION"):
        return "C4FM"
    if primary in ("FM", "NFM"):
        return "FM"
    return "OTHER"


def to_repeater(entry, retrieved):
    """Returns (repeater, None) or (None, reason it was skipped)."""
    if entry.get("operational") != 1:
        return None, "not listed as operational"
    callsign = (entry.get("callsign") or "").strip()
    if not callsign:
        return None, "no callsign"
    lat, lon = entry.get("latitude"), entry.get("longitude")
    if lat is None or lon is None or (lat == 0 and lon == 0):
        return None, "no coordinates"
    if distance_km(lat, lon) > RADIUS_KM:
        return None, "outside the radius"
    output_hz = entry.get("frequency") or 0
    if output_hz <= 0:
        return None, "no frequency"
    rx = output_hz / 1e6
    tx = (output_hz + (entry.get("offset") or 0)) / 1e6
    if not (in_band(rx) and in_band(tx)):
        return None, "frequency outside the amateur bands"
    mode = catalog_mode(entry.get("mode") or "")
    encode = (entry.get("encode") or "").strip()
    repeater = {
        "id": f"hearham-{entry['id']}",
        "name": (entry.get("city") or "").strip() or callsign,
        "callsign": callsign,
        "rxMHz": round(rx, 6),
        "txMHz": round(tx, 6),
        "mode": mode,
    }
    if mode == "DMR":
        cc = COLOR_CODE.search(encode)
        if cc and 0 <= int(cc.group(1)) <= 15:
            repeater["colorCode"] = int(cc.group(1))
    else:
        try:
            tone = float(encode)
            if tone > 0:
                repeater["ctcssHz"] = round(tone, 1)
        except ValueError:
            pass
    repeater.update({
        "lat": lat,
        "lon": lon,
        "source": f"HearHam.live repeater list (hearham.com), listed as operational, retrieved {retrieved}",
        "lastUpdated": retrieved,
        "status": "UNKNOWN",
    })
    return repeater, None


def snapshot(entries, retrieved):
    kept, skipped = [], {}
    for entry in entries:
        repeater, reason = to_repeater(entry, retrieved)
        if repeater:
            kept.append(repeater)
        else:
            skipped[reason] = skipped.get(reason, 0) + 1
    kept.sort(key=lambda r: (distance_km(r["lat"], r["lon"]), r["callsign"]))
    return {
        "source": URL,
        "retrieved": retrieved,
        "center": [CENTER_LAT, CENTER_LON],
        "radiusKm": RADIUS_KM,
        "listed": len(entries),
        "skipped": dict(sorted(skipped.items())),
        "repeaters": kept,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("output")
    parser.add_argument("--input", help="read a saved copy of the list instead of downloading it")
    args = parser.parse_args()
    if args.input:
        with open(args.input, encoding="utf-8") as f:
            entries = json.load(f)
    else:
        request = urllib.request.Request(URL, headers={"User-Agent": "ChornobylHamDash-snapshot/1.0"})
        with urllib.request.urlopen(request, timeout=120) as response:
            entries = json.load(response)
    retrieved = datetime.date.today().isoformat()
    result = snapshot(entries, retrieved)
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"{len(result['repeaters'])} of {result['listed']} listings kept; skipped: {result['skipped']}")
    for r in result["repeaters"]:
        print(f"  {r['callsign']:<10} {r['mode']:<7} {r['rxMHz']:>10} / {r['txMHz']:<10} {r['name']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
