<!--
Ready-to-paste text for the merge request at https://gitlab.com/fdroid/fdroiddata (title: "New app: Chornobyl HamDash").
It follows the repository's "App inclusion" template. Steps are in PUBLISHING.md. Tick the boxes yourself after checking them;
the notes below say what was checked in this repository and what only you can confirm.
-->

## Abstract

Chornobyl HamDash is an offline-first dashboard for licensed radio amateurs working in the Chornobyl region, in the spirit
of HamClock. It collects public amateur radio and reference information in one place:

* repeater list (DMR, FM, D-STAR, C4FM) with an on-air status for every entry, DMR parameters and talkgroups, band and
  digital mode references;
* UTC and local clocks with sunrise and sunset, space weather (NOAA SWPC: solar flux, K and A index, X-ray flares);
* a map (OpenStreetMap through osmdroid, no API key) with an optional radiation background layer built from Safecast
  open data (volunteer readings grouped into 2 km cells, no safety verdict);
* a weather forecast (Open-Meteo) with a drone flight weather rating and an FPV reference (5.8 GHz channels, control
  links, checklist, legal notice);
* the history and public civilian landmarks of the Exclusion Zone, with S.T.A.L.K.E.R. game references clearly labeled
  as fiction.

Everything that was downloaded once works offline. Missing data is shown as `NO DATA`, never invented, and every
repeater and talkgroup shows its source. The interface is available in English and Ukrainian.

Screenshots (also in `fastlane/metadata/android/en-US/images/phoneScreenshots/`):

![Dashboard](https://raw.githubusercontent.com/gm3nnnlab/Chernobyl-Ham-clock-/main/docs/screenshots/1.png)
![Map](https://raw.githubusercontent.com/gm3nnnlab/Chernobyl-Ham-clock-/main/docs/screenshots/2.png)
![Repeaters](https://raw.githubusercontent.com/gm3nnnlab/Chernobyl-Ham-clock-/main/docs/screenshots/3.png)

### Why the app exists and what is unique

<!-- Replace the first paragraph with your own motivation (why you wrote it, how it differs from apps you know in F-Droid). -->

HamClock-style dashboards exist for desktops and for the web, and there are repeater directories, but this app puts a
repeater list with status, space weather, a map, a flight weather rating for drones and local Zone history in one
free, ad-free, offline-capable Android app. It is also explicit about the legal situation: amateur transmitting is
banned in Ukraine under martial law (Presidential Decrees 133/2022, 259/2022, 341/2022), so every repeater list
carries that notice and no repeater is shown as on air unless its source says so.

Safety limits are part of the design: no interception of communications, no decoding of protected systems, no search
for military frequencies or sites, no tracking of people. The app is not a navigator and not intended for military use.
GPS is used only to show the user's own position and to run the map; it is sent anywhere only if the user picks "My
location" as the weather place, and then rounded to about 10 km. The radiation request always uses the fixed
coordinates of the power plant, never the user's position.

Privacy policy: https://github.com/gm3nnnlab/Chernobyl-Ham-clock-/blob/main/PRIVACY.md (no accounts, no ads, no analytics).

## Checklist

### Policy

* [x] The app complies with the [inclusion criteria](https://f-droid.org/docs/Inclusion_Policy).
  GPL-3.0-or-later; dependencies are AndroidX, Jetpack Compose, Room, DataStore, Retrofit, OkHttp, kotlinx and osmdroid
  (all open source); no Google Play services, no ads, no trackers, no proprietary binaries. Network services used are
  public and open: NOAA SWPC (US government data), Open-Meteo (open source API, data CC BY 4.0), Safecast (data CC0)
  and OpenStreetMap tiles.
* [x] The original app author has been notified. The submitter is the author.
* [x] The upstream repository has Fastlane metadata (`fastlane/metadata/android/`: `en-US`, `uk`, `ru-RU`) with title,
  short and full description, changelogs, icon, feature graphic and phone screenshots.

### Docs

* [x] I read the [contributing guide](https://gitlab.com/fdroid/fdroiddata/-/blob/master/CONTRIBUTING.md).
* [x] The metadata follows the templates.
* [x] I read the Build Metadata Reference; `fdroid lint` passes on the file.
* [x] I read the Quick Start Guide.

### Merge Request Setup

* [x] The title is "New app: Chornobyl HamDash".
* [ ] My fdroiddata fork is public and the branch is not protected. (Tick after creating the fork.)
* [x] No rebase was needed.
* [x] There is no related fdroiddata or RFP issue yet. (Add the link here if an RFP issue is opened.)
* [x] One app in this MR.

### Metadata

* [x] Metadata is in `metadata/com.hamdash.chornobyl.yml`, valid YAML, LF line endings.
* [x] Nothing that belongs upstream (summary, description, changelog, images) is added in this MR.
* [x] Releases are tagged (`v0.3.1`) and auto update is enabled (`AutoUpdateMode: Version`, `UpdateCheckMode: Tags`).
* [x] Issue tracker and author contact: https://github.com/gm3nnnlab/Chernobyl-Ham-clock-/issues, `AuthorName` set.
* [x] No external repos, no submodules, no srclibs.
* [ ] Reproducible builds are **not enabled** for the first inclusion. Reason: the project's own release signing key was
  only just created and no store-signed release APK has been published yet, so I cannot yet verify that the F-Droid
  build matches a published signed APK byte for byte. I would like to enable `Binaries` and `AllowedAPKSigningKeys`
  later; if that must be decided now, I am happy to hold the MR until a signed, verified release exists (release key
  SHA-256 is in `keystore/release-cert-sha256.txt`).
* [x] ABI split is not needed: the APK has no native libraries.
* [x] Only the latest version (0.3.1, versionCode 7) is in the metadata, no disabled versions.
* [x] The `commit` field is the full hash (`341236df9438a09c7af72f8860c887aa5717cd44`, the commit tagged `v0.3.1`).

### Pipeline

* [ ] All pipelines pass. (Check after pushing; answer anything in the Reports tab.)
* [ ] Warnings and errors in the Reports tab are fixed or explained.
* [ ] I will leave a note if GitLab asks for a phone number or a credit card.
