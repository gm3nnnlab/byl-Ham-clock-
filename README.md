# Chornobyl HamDash

Native Android dashboard for licensed radio amateurs working in the Chornobyl region, in the spirit of HamClock.
It collects public amateur radio information in one place: repeaters (DMR, FM, D-STAR, C4FM), DMR parameters and
talkgroups, a band reference, a digital mode reference, UTC and local clocks with sunrise and sunset, basic
propagation data, a map, a weather forecast with a drone flight weather rating, an FPV reference (5.8 GHz video
channels, control links, checklist, legal notice), and the history and public landmarks of the Exclusion Zone with
clearly labeled S.T.A.L.K.E.R. game references. Every repeater shows whether it is on air, and a notice reminds that
amateur transmitting is banned in Ukraine under martial law. Everything saved works offline.

It is not a navigator and is not intended for military use. See "Safety limits" below.

| Dashboard | Map | Repeaters | FPV | More |
| --- | --- | --- | --- | --- |
| ![Dashboard](docs/screenshots/1.png) | ![Map with Zone places](docs/screenshots/2.png) | ![Repeaters](docs/screenshots/3.png) | ![FPV and drones](docs/screenshots/4.png) | ![More](docs/screenshots/5.png) |

## Download

The current version is **0.3.1**. Get `chornobyl-hamdash-v0.3.1-debug.apk` from the
[latest release](https://github.com/gm3nnnlab/Chernobyl-Ham-clock-/releases/latest), allow installs from unknown
sources and open the file. To get updates automatically, add this repository to
[Obtainium](https://github.com/ImranR98/Obtainium). Store listings (F-Droid, IzzyOnDroid, Google Play and others) are
in progress, see `PUBLISHING.md`.

Version 0.3.1 is published as a test build only: the release key for store builds is not set up yet, so the release has
just the debug-signed APK. It installs over 0.2.3 and later and keeps your data. Once the release key secrets are set,
releases also carry a store-signed APK and App Bundle. Those two kinds of APK are signed with different keys, so Android
will not install one over the other:

* `chornobyl-hamdash-vX.apk`: release build signed with the private release key (certificate SHA-256 in
  `keystore/release-cert-sha256.txt`). Use this one.
* `chornobyl-hamdash-vX-debug.apk`: test build signed with the public debug key in `keystore/debug.keystore`.

Versions 0.1.0 to 0.2.2 were each signed with a different, randomly generated key, so nothing installs over them
("App not installed"): uninstall such a version once. Versions installed from F-Droid or Google Play are signed by
those stores and also update only from the same store.

| Version | Changes |
| --- | --- |
| 0.3.1 | Radiation background layer and screen from Safecast open data (volunteer readings, grouped into 2 km cells, no safety verdict). Published as a test build |
| 0.3.0 | Targets Android 16 (API 36), signed release APK and App Bundle for stores, privacy policy |
| 0.2.3 | All builds signed with one fixed key, so updates install over each other (uninstall 0.2.2 or older once) |
| 0.2.2 | Repeater on-air status (on air, off air, unknown), martial-law transmit-ban notice, database migration that keeps favorites |
| 0.2.1 | Fixed a missing space in the database status line; screenshots in the store listing |
| 0.2.0 | Weather, drone flight weather rating, FPV reference, Exclusion Zone history and landmarks, S.T.A.L.K.E.R. references |
| 0.1.0 | First release: dashboard, map, repeaters, DMR, bands, digital modes, propagation, English and Ukrainian |

## Build and run

Requirements: JDK 17 or newer, Android SDK with platform 36 (Android Studio installs both).

```bash
./gradlew :app:testDebugUnitTest      # JVM unit tests
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug           # install on a connected device or emulator
```

Or open the folder in Android Studio and press Run. A GitHub Actions workflow (`.github/workflows/build.yml`)
builds the APK and runs the tests on every push.

Minimum Android 8.0 (API 26), target and compile SDK 36.

## Languages

The interface is available in English and Ukrainian (Settings > Display > Language). On first launch the app follows the
device language (Ukrainian if the device is set to Ukrainian, English otherwise). Strings live in
`app/src/main/res/values/strings.xml` and `values-uk/strings.xml`; to add a language, add a `values-<code>/strings.xml`,
an entry in `settings/AppLanguage.kt`, and a `fastlane/metadata/android/<code>/` folder.

Reference content that comes from the catalog (band purposes, digital mode descriptions, talkgroup descriptions and source
names) is shown as stored in the catalog data, so the bundled catalog texts are in English.

## Stack and layout

Kotlin, Jetpack Compose, Material 3, coroutines and Flow, ViewModel, Room, DataStore, Retrofit and OkHttp,
kotlinx.serialization, osmdroid (OpenStreetMap, no API key). Architecture: MVVM with a repository.

```
app/src/main/java/com/hamdash/chornobyl/
  domain/     models, filters, sun calculator, formatting (no Android dependencies)
  data/       repositories (radio data, propagation, weather, Zone), data sources, catalog and Zone parsers
  database/   Room entities, DAOs, database and migrations (schema version 2)
  network/    Retrofit RawApi, NOAA and Open-Meteo parsers, connectivity monitor
  location/   LocationTracker (platform LocationManager, no Google Play services)
  settings/   DataStore settings
  ui/         theme, navigation, screens and ViewModels (dashboard, map, repeaters, dmr, weather, zone, more, settings)
app/src/main/assets/catalog.json   bundled offline catalog
app/src/main/assets/zone.json      bundled Zone history, landmarks and game notes
```

## Data sources

The app never invents values. Missing data is shown as `NO DATA`, and every repeater and talkgroup shows its source.

| Data | Source |
| --- | --- |
| Repeaters, talkgroups | Bundled `catalog.json`. **The bundled repeaters are fictional demo entries marked `DEMO DATA`, with status unknown.** Replace them with a real public source. |
| Bands, digital modes | Bundled general reference (IARU Region 1 overview, official project sites). Always check your national allocation plan. |
| Propagation | NOAA SWPC public JSON (`services.swpc.noaa.gov`): 10.7 cm flux, planetary K and A index, latest X-ray flare. |
| Weather | Open-Meteo forecast API (`api.open-meteo.com`, no key, CC BY 4.0): current conditions, 3-day hourly and daily forecast, wind at 10 m and 80 m. |
| Radiation background | Safecast open data (`api.safecast.org`, CC0): volunteer dose-rate readings within 35 km of the plant, last 12 months, grouped into 2 km cells with the median shown. The request uses the plant's fixed coordinates, never the user's position. Counts per minute are converted with 334 CPM per uSv/h (bGeigie Nano) and flagged as estimates. Unverified by any authority; shown as `NO DATA` when nothing usable arrives. |
| Zone history, landmarks, game notes | Bundled `zone.json`, compiled from Wikipedia and IAEA articles. Every entry carries its source; only public civilian and historical landmarks. Game notes are labeled as fiction. |
| Map | OpenStreetMap tiles through osmdroid, cached on the device. The default public OSM tile server is meant for light use; set your own https tile URL in Settings > Map tiles for heavier use. |

### Plugging in a real source

`RadioDataRepository` talks to a `LocalDataSource` (Room) and a `RemoteDataSource`. The bundled implementation,
`HttpCatalogRemoteDataSource`, downloads a catalog JSON from the https URL entered in Settings > Data source.
Refresh flow: fetch, parse and validate, store the valid sections in Room, otherwise keep the local data, and show the
time of the last sync. To use a different API, implement `RemoteDataSource` and pass it in `AppContainer`.

Catalog format (`schemaVersion` 1), see `app/src/main/assets/catalog.json` for a full example:

```json
{
  "schemaVersion": 1,
  "updated": "2026-10-06",
  "repeaters": [{
    "id": "unique-id", "name": "...", "callsign": "...",
    "rxMHz": 438.4, "txMHz": 430.8, "mode": "DMR",
    "colorCode": 1, "timeslot": 2, "talkgroups": [255],
    "lat": 50.45, "lon": 30.52, "elevationM": 180,
    "source": "where this entry comes from", "lastUpdated": "2026-10-06",
    "status": "OFF_AIR"
  }],
  "talkgroups": [], "bands": [], "digitalModes": []
}
```

`rxMHz` is the frequency you listen on (repeater output), `txMHz` the frequency you transmit on (repeater input).
`status` is `ACTIVE`, `OFF_AIR` or `UNKNOWN` (the default when the field is missing; the app never assumes a
repeater works). Ukraine has banned amateur transmitters under martial law since 2022, so the app shows a transmit-ban
notice next to every repeater list, and real entries should be marked `OFF_AIR` unless the source confirms otherwise.
Invalid entries are skipped and reported; entries without a `source` are rejected. A section with no valid entries
never replaces the stored one.

## Author

Yevhen "CUBE" Honchar

## License

GPL-3.0-or-later, see `LICENSE`. Change it before the first public release if you prefer another free license
(also update `fdroid/com.hamdash.chornobyl.yml`).

## Publishing

`PUBLISHING.md` has the step-by-step guide for F-Droid, IzzyOnDroid, Obtainium, Google Play, Samsung Galaxy Store and
Huawei AppGallery, with the answers for the Google Play forms. Store texts, the icon, the feature graphic and
screenshots live in `fastlane/metadata/android/` (en-US, uk and ru-RU; images are in en-US, which F-Droid uses as the
fallback for every language).

For a new version: bump `versionCode` and `versionName`, add `changelogs/<versionCode>.txt` in each language, add a
`Builds` entry to `fdroid/com.hamdash.chornobyl.yml`, and publish the release (it creates the `v<version>` tag).

## Privacy

Full policy: `PRIVACY.md`.


GPS feeds the map, distance labels and sun times. It is sent anywhere only if you pick "My location" as the weather
place, and then rounded to 0.1 degree (about 10 km) before the request to Open-Meteo. The default weather place is the
town of Chornobyl. Permission is requested only when you tap something that needs it, and "Show my location" can be
turned off in Settings.

## Safety limits

Not implemented on purpose: interception of private communications, decoding of protected systems, key recovery,
searching for military frequencies, detection of military sites, tracking of people, and sharing of the user's
position without consent. The map shows only public civilian and historical landmarks. The radiation layer shows volunteer
measurements with a "not a dosimeter" warning and no safety verdict, and never uses the user's position. The drone section rates the
weather only; it never says a flight is permitted and points to martial-law and Exclusion Zone access rules. Repeater
lists carry a notice that amateur transmitting is banned in Ukraine under martial law (Presidential Decrees
No. 133/2022, 259/2022 and 341/2022), and no repeater is shown as on air unless its source says so. Use only
public, lawful amateur radio data and transmit only where your license allows it.

## Status

The pure Kotlin logic (domain, flight rating, repeater status, catalog, Zone and weather parsers, NOAA parsers) is
covered by 46 JVM unit tests that run on every push. Version 0.2.2 has been installed over 0.2.1 on a real Android
phone and checked, including the database migration. The repeater catalog still holds demo entries; real public
data is the next step.
