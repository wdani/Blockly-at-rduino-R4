# Android alpha signing key

`elekto-alpha-dev.keystore` is the fixed signing key for the public Android alpha/debug builds of Blockly@rduino R4.

Purpose:
- keep the Android `applicationId` stable;
- allow a newly built alpha APK to be installed as an update over the previous alpha APK;
- avoid GitHub Actions generating a different temporary debug certificate on each runner.

This key is intentionally a **development/alpha key only**. It is stored in the public repository so the CI build is reproducible; therefore it must never be used to sign a production/store release.

Current alpha signing identity:
- alias: `elektoAlpha`
- keystore SHA-256: `be94dee89a39b4886a6bbde8dc08254d647940fae13662361f553282f7a05d4e`

A future production release must use a separate private release/upload key that is not committed to this repository.
