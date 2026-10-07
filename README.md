# Chornobyl HamDash

Native Android dashboard for licensed radio amateurs working in the Chornobyl region, in the spirit of HamClock.
It collects public amateur radio information in one place: repeaters (DMR, FM, D-STAR, C4FM), DMR parameters and
talkgroups, a band reference, a digital mode reference, UTC and local clocks with sunrise and sunset, basic
propagation data, and a map. Everything saved works offline.

It is not a navigator and is not intended for military use. See "Safety limits" below.

## Build and run

Requirements: JDK 17 or newer, Android SDK with platform 35 (Android Studio installs both).

```bash
./gradlew :app:testDebugUnitTest      # JVM unit tests
./gradlew :app:assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:installDebug           # install on a connected device or emulator
```

Or open the folder in Android Studio and press Run. A GitHub Actions workflow (`.github/workflows/build.yml`)
builds the APK and runs the tests on every push.

Minimum Android 8.0 (API 26), target and compile SDK 35.

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
  data/       RadioDataRepository implementation, Local/Remote data sources, catalog parser and validator
  database/   Room entities, DAOs, database
  network/    Retrofit RawApi, NOAA parsers, connectivity monitor
  location/   LocationTracker (platform LocationManager, no Google Play services)
  settings/   DataStore settings
  ui/         theme, navigation, screens and ViewModels (dashboard, map, repeaters, dmr, more, settings)
app/src/main/assets/catalog.json   bundled offline catalog
```

## Data sources

The app never invents values. Missing data is shown as `NO DATA`, and every repeater and talkgroup shows its source.

| Data | Source |
| --- | --- |
| Repeaters, talkgroups | Bundled `catalog.json`. **The bundled repeaters are fictional demo entries marked `DEMO DATA`.** Replace them with a real public source. |
| Bands, digital modes | Bundled general reference (IARU Region 1 overview, official project sites). Always check your national allocation plan. |
| Propagation | NOAA SWPC public JSON (`services.swpc.noaa.gov`): 10.7 cm flux, planetary K and A index, latest X-ray flare. |
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
    "source": "where this entry comes from", "lastUpdated": "2026-10-06"
  }],
  "talkgroups": [], "bands": [], "digitalModes": []
}
```

`rxMHz` is the frequency you listen on (repeater output), `txMHz` the frequency you transmit on (repeater input).
Invalid entries are skipped and reported; entries without a `source` are rejected. A section with no valid entries
never replaces the stored one.

## Author

Yevhen "CUBE" Honchar

## License

GPL-3.0-or-later, see `LICENSE`. Change it before the first public release if you prefer another free license
(also update `fdroid/com.hamdash.chornobyl.yml`).

## Publishing on F-Droid

F-Droid builds the app from source, so nothing is uploaded. Checklist:

1. Make the repository public (the metadata already points to https://github.com/gm3nnnlab/Chernobyl-Ham-clock-; update `fdroid/com.hamdash.chornobyl.yml` if it moves).
2. Replace the bundled demo repeaters in `app/src/main/assets/catalog.json` with real public data.
3. Add real screenshots to `fastlane/metadata/android/<locale>/images/phoneScreenshots/` (1.png, 2.png, ...).
4. Build from a clean clone with `./gradlew assembleRelease` and tag the release (`git tag v0.1.0`).
5. Open a merge request to https://gitlab.com/fdroid/fdroiddata adding `metadata/com.hamdash.chornobyl.yml`
   (start from `fdroid/com.hamdash.chornobyl.yml`), or file a request in https://gitlab.com/fdroid/rfp.

Store texts and the icon live in `fastlane/metadata/android/` (en-US, uk and ru-RU). When you release a new version,
bump `versionCode` and `versionName`, add `changelogs/<versionCode>.txt`, and tag it.

## Privacy

GPS only feeds the map, distance labels and sun times. It is never sent anywhere. Permission is requested only when
you tap something that needs it, and "Show my location" can be turned off in Settings.

## Safety limits

Not implemented on purpose: interception of private communications, decoding of protected systems, key recovery,
searching for military frequencies, detection of military sites, tracking of people, and sharing of the user's
position. Use only public, lawful amateur radio data and transmit only where your license allows it.

## Status

The pure Kotlin logic (domain, catalog parser, NOAA parsers) is covered by JVM unit tests. The Android UI layer has
not been exercised on a device yet.
