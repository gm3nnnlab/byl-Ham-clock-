#!/usr/bin/env python3
"""Make a dated snapshot of public amateur repeater listings near Chornobyl.

Two public sources are read:
* the HearHam.live repeater list (https://hearham.com/api/repeaters/v1, worldwide, about 10 MB), the
  source of the earlier HamDash app; entries listed as operational are kept;
* the BrandMeister DMR network device list (https://api.brandmeister.network/v2/device); only devices
  with a 6-digit repeater ID are kept. Hotspots and other personal devices carry a longer ID and are
  dropped: their coordinates are usually someone's home.

Kept entries need a Ukrainian callsign, coordinates within RADIUS_KM of Chornobyl, and both frequencies
inside IARU Region 1 amateur bands. A repeater listed by both sources appears once. The output uses the app's
catalog repeater format, with status UNKNOWN: a directory listing does not prove a repeater is on air.

Usage: repeater_snapshot.py OUTPUT.json [--hearham saved.json] [--brandmeister saved.json]
"""
import argparse
import datetime
import json
import math
import re
import sys
import urllib.request

HEARHAM_URL = "https://hearham.com/api/repeaters/v1"
BRANDMEISTER_URL = "https://api.brandmeister.network/v2/device"
CENTER_LAT, CENTER_LON = 51.2763, 30.2219
RADIUS_KM = 300.0
AMATEUR_BANDS_MHZ = [(28.0, 29.7), (50.0, 54.0), (144.0, 146.0), (430.0, 440.0), (1240.0, 1300.0)]
COLOR_CODE = re.compile(r"CC\s*(\d+)", re.IGNORECASE)
# ITU prefixes allocated to Ukraine: EM-EO and UR-UZ.
UKRAINIAN_CALLSIGN = re.compile(r"^(E[M-O]|U[R-Z])\d", re.IGNORECASE)
LOCATOR = re.compile(r"^[A-R]{2}\d{2}([A-X]{2}(\d{2})?)?$", re.IGNORECASE)
# DMR IDs start with the ITU mobile country code.
COUNTRY_BY_MCC = {"250": "Russia", "255": "Ukraine", "257": "Belarus", "259": "Moldova", "260": "Poland"}


def distance_km(lat, lon):
    p1, p2 = math.radians(CENTER_LAT), math.radians(lat)
    dp, dl = p2 - p1, math.radians(lon - CENTER_LON)
    h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * 6371.0 * math.asin(math.sqrt(h))


def in_band(mhz):
    return any(lo <= mhz <= hi for lo, hi in AMATEUR_BANDS_MHZ)


def place_problem(callsign, lat, lon):
    if not callsign:
        return "no callsign"
    if not UKRAINIAN_CALLSIGN.match(callsign):
        return "not a Ukrainian station"
    if lat is None or lon is None or (lat == 0 and lon == 0):
        return "no coordinates"
    if distance_km(lat, lon) > RADIUS_KM:
        return "outside the radius"
    return None


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


def entry_tail(lat, lon, source, retrieved):
    return {"lat": lat, "lon": lon, "source": source, "lastUpdated": retrieved, "status": "UNKNOWN"}


def from_hearham(entry, retrieved):
    """Returns (repeater, None) or (None, reason it was skipped)."""
    if entry.get("operational") != 1:
        return None, "not listed as operational"
    callsign = (entry.get("callsign") or "").strip()
    lat, lon = entry.get("latitude"), entry.get("longitude")
    problem = place_problem(callsign, lat, lon)
    if problem:
        return None, problem
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
    source = f"HearHam.live repeater list (hearham.com), listed as operational, retrieved {retrieved}"
    repeater.update(entry_tail(lat, lon, source, retrieved))
    return repeater, None


def brandmeister_place(device_id, city, callsign):
    parts = [p.strip() for p in (city or "").split(",")]
    parts = [p for p in parts if p and not LOCATOR.match(p) and p.upper() != "L0C4T0R"]
    name = ", ".join(parts) or callsign
    country = COUNTRY_BY_MCC.get(str(device_id)[:3])
    if country and country.lower() not in name.lower():
        name = f"{name}, {country}"
    return name


def from_brandmeister(device, retrieved):
    """Returns (repeater, None) or (None, reason it was skipped)."""
    device_id = device.get("id") or 0
    if not 100000 <= device_id <= 999999:
        return None, "hotspot or personal device, not a repeater ID"
    callsign = (device.get("callsign") or "").strip()
    try:
        lat, lon = float(device.get("lat") or 0), float(device.get("lng") or 0)
        output, listen = float(device.get("tx") or 0), float(device.get("rx") or 0)
    except (TypeError, ValueError):
        return None, "unreadable record"
    problem = place_problem(callsign, lat, lon)
    if problem:
        return None, problem
    if not (in_band(output) and in_band(listen)):
        return None, "frequency outside the amateur bands"
    if output == listen:
        return None, "simplex device, not a repeater"
    repeater = {
        "id": f"brandmeister-{device_id}",
        "name": brandmeister_place(device_id, device.get("city"), callsign),
        "callsign": callsign,
        "rxMHz": round(output, 6),
        "txMHz": round(listen, 6),
        "mode": "DMR",
    }
    cc = device.get("colorcode")
    if isinstance(cc, int) and 0 <= cc <= 15:
        repeater["colorCode"] = cc
    seen = (device.get("last_seen") or "")[:10]
    seen_text = f", last seen on the network {seen}" if seen else ""
    source = (f"BrandMeister DMR network device list (brandmeister.network), repeater ID {device_id}{seen_text}, "
              f"retrieved {retrieved}")
    repeater.update(entry_tail(lat, lon, source, retrieved))
    return repeater, None


def collect(records, convert, retrieved):
    kept, skipped = [], {}
    for record in records:
        repeater, reason = convert(record, retrieved)
        if repeater:
            kept.append(repeater)
        else:
            skipped[reason] = skipped.get(reason, 0) + 1
    return kept, dict(sorted(skipped.items()))


def snapshot(hearham, brandmeister, retrieved):
    from_hh, skipped_hh = collect(hearham, from_hearham, retrieved)
    from_bm, skipped_bm = collect(brandmeister, from_brandmeister, retrieved)
    by_key = {(r["callsign"].upper(), round(r["rxMHz"], 4)): r for r in from_bm}
    merged = list(from_bm)
    for r in from_hh:
        twin = by_key.get((r["callsign"].upper(), round(r["rxMHz"], 4)))
        if twin:
            twin["source"] += "; also listed as operational on HearHam.live"
        else:
            merged.append(r)
    merged.sort(key=lambda r: (round(distance_km(r["lat"], r["lon"])), r["callsign"]))
    return {
        "retrieved": retrieved,
        "center": [CENTER_LAT, CENTER_LON],
        "radiusKm": RADIUS_KM,
        "sources": {
            "hearham": {"url": HEARHAM_URL, "listed": len(hearham), "kept": len(from_hh), "skipped": skipped_hh},
            "brandmeister": {"url": BRANDMEISTER_URL, "listed": len(brandmeister), "kept": len(from_bm),
                             "skipped": skipped_bm},
        },
        "repeaters": merged,
    }


def read_list(path, url):
    if path:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    request = urllib.request.Request(url, headers={"User-Agent": "ChornobylHamDash-snapshot/1.0"})
    with urllib.request.urlopen(request, timeout=180) as response:
        return json.load(response)


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("output")
    parser.add_argument("--hearham", help="read a saved copy of the HearHam list instead of downloading it")
    parser.add_argument("--brandmeister", help="read a saved copy of the BrandMeister list instead of downloading it")
    args = parser.parse_args()
    retrieved = datetime.date.today().isoformat()
    result = snapshot(read_list(args.hearham, HEARHAM_URL), read_list(args.brandmeister, BRANDMEISTER_URL), retrieved)
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
        f.write("\n")
    for name, info in result["sources"].items():
        print(f"{name}: {info['kept']} of {info['listed']} kept; skipped: {info['skipped']}")
    for r in result["repeaters"]:
        print(f"  {r['callsign']:<10} {r['mode']:<7} {r['rxMHz']:>10} / {r['txMHz']:<10} {r['name']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
