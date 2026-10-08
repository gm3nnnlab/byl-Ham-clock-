# Publishing Chornobyl HamDash

What is ready in this repository, and the steps only the owner can do (accounts, identity checks, submissions).

## Ready in the repository

| Item | Where |
| --- | --- |
| Store texts in English, Ukrainian, Russian | `fastlane/metadata/android/<locale>/` (title, short and full description, changelogs) |
| Icon 512x512, feature graphic 1024x500, 5 phone screenshots | `fastlane/metadata/android/en-US/images/` |
| Privacy policy (English and Ukrainian) | `PRIVACY.md`, public URL https://github.com/gm3nnnlab/Chernobyl-Ham-clock-/blob/main/PRIVACY.md |
| F-Droid metadata (passes `fdroid lint`) | `fdroid/com.hamdash.chornobyl.yml` |
| Signed release APK and AAB on every GitHub release | `.github/workflows/build.yml`, once the release key secrets are set (below) |
| Release key certificate SHA-256 | `keystore/release-cert-sha256.txt` |
| Target SDK 36 (Google Play requirement since 31 August 2026) | `app/build.gradle.kts` |

## Step 0: release key secrets (done)

The release key is private and is not in the repository. It lives in four repository secrets at
https://github.com/gm3nnnlab/Chernobyl-Ham-clock-/settings/secrets/actions: `RELEASE_KEYSTORE_BASE64`,
`RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (values in the private `github-secrets.txt` given to
the owner; paste the text inside the file, not the file name). They are set since 0.3.1, so every release carries
`chornobyl-hamdash-vX.apk` (signed release build) and `chornobyl-hamdash-vX.aab` (for Google Play). The release step
checks that the keystore opens and that the APK certificate matches `keystore/release-cert-sha256.txt`, and names the
secret at fault when something is wrong. Keep the `.jks` file and passwords in a password manager and an offline
backup: without them, IzzyOnDroid and direct-APK users cannot get updates.

## Recommended order

1. **IzzyOnDroid** and **Obtainium**: fastest, use the GitHub release APK.
2. **F-Droid**: builds from source, review takes weeks.
3. **Google Play**: paid account and a mandatory 14-day closed test for new personal accounts.
4. **Samsung Galaxy Store**, **Huawei AppGallery**: optional extra reach.

Before the big stores, consider replacing the demo repeaters with real public data: reviewers may question an app
whose main list is labeled "DEMO DATA".

## F-Droid (official repository)

The metadata is ready and passes `fdroid lint`: one build (0.3.1, versionCode 7) pinned to the full hash of the `v0.3.1`
commit, as the F-Droid merge request template requires. The text for the merge request is in
`fdroid/MR_DESCRIPTION.md`.

1. Create an account on https://gitlab.com, fork https://gitlab.com/fdroid/fdroiddata and make the fork public.
2. In the fork, on a new branch (not protected), add `metadata/com.hamdash.chornobyl.yml` with the content of
   `fdroid/com.hamdash.chornobyl.yml` (drop the two comment lines at the top). Add nothing else.
3. Open a merge request to `fdroid/fdroiddata` titled **New app: Chornobyl HamDash** and paste `fdroid/MR_DESCRIPTION.md`
   as the description. Tick the two boxes about the fork and the pipeline yourself.
4. Wait for the pipeline and the reviewers. If it fails, read the Reports tab and fix the metadata. GitLab may ask for a
   phone number or card to run CI: do not give them, leave a note in the MR so the maintainers trigger the pipeline.
5. F-Droid builds the APK itself. Reproducible builds are not enabled for the first inclusion (see the MR text), so the
   F-Droid APK is signed with F-Droid's key and installs only over F-Droid versions.

New versions are picked up from new `v*` tags (`UpdateCheckMode: Tags`, `AutoUpdateMode: Version`). Each new release
must keep `versionCode` increasing and be tagged; the F-Droid bot then proposes the new build with the commit hash.

## IzzyOnDroid (F-Droid compatible repository)

1. The latest GitHub release contains the signed `chornobyl-hamdash-vX.apk` (since 0.3.1).
2. Open an issue at https://gitlab.com/IzzyOnDroid/repo/-/issues using the "app request" template: app name, source
   URL https://github.com/gm3nnnlab/Chernobyl-Ham-clock-, license GPL-3.0-or-later, and the release key SHA-256 from
   `keystore/release-cert-sha256.txt`.
3. Updates follow new GitHub releases within about a day.

## Obtainium (no submission needed)

Users add https://github.com/gm3nnnlab/Chernobyl-Ham-clock- as an app source in Obtainium and get updates straight from
GitHub releases. Tell them to pick the `chornobyl-hamdash-vX.apk` asset (not `-debug`).

## Google Play

1. Create a developer account at https://play.google.com/console (one-time fee, identity verification).
2. Create the app: default language Ukrainian or English, type App, free, category Tools.
3. Personal accounts created after November 2023 must run a closed test with at least 12 testers for 14 days before
   production access. Upload `chornobyl-hamdash-vX.aab` to the closed track and invite testers.
4. Accept Play App Signing. Upload the AAB signed with our release key; it becomes the upload key.
5. Store listing: copy the texts from `fastlane/metadata/android/<locale>/`, upload `icon.png`, `featureGraphic.png` and
   the screenshots.
6. App content forms, suggested answers:
   - Privacy policy URL: https://github.com/gm3nnnlab/Chernobyl-Ham-clock-/blob/main/PRIVACY.md
   - Ads: no. App access: all functions available without login.
   - Target audience: 18 and over (a reference tool for licensed radio amateurs).
   - Content rating (IARC questionnaire): reference/utility, no violence, no user interaction, no shared location.
     The S.T.A.L.K.E.R. notes are text references to a video game, not game content.
   - Data safety: no data shared. Data collected: **Approximate location**, optional, only when the user picks
     "My location" for the weather; purpose App functionality; sent over HTTPS; not stored by the app off the device.
     No other data types.
   - News app: no. Government app: no. Financial features: none. Health: none.
7. After the 14-day test, apply for production and roll out.

## Samsung Galaxy Store

1. Register at https://seller.samsungapps.com (free, seller verification).
2. Add a new Android app, upload the signed release APK, fill the listing from the fastlane texts and images, attach
   the privacy policy URL, choose countries, and submit for review.

## Huawei AppGallery

1. Register at https://developer.huawei.com/consumer/en/appgallery (identity verification required).
2. In AppGallery Connect create the app, upload the signed release APK, fill the listing and privacy policy URL, and
   submit for review.

## Not covered

- Amazon Appstore: closed for Android phones and tablets on 20 August 2025 (it remains only on Amazon's own devices).
- Stores that require proprietary SDKs or ads are not targeted: the app has none, and F-Droid inclusion depends on that.
