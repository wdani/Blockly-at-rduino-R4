# Android alpha signing

Starting with `0.1.0-alpha.4`, Android alpha builds use a stable, reproducible signing identity so later alpha APKs can be installed as updates instead of requiring repeated uninstall/reinstall cycles.

## Alpha package identity

- Application ID: `ch.elekto.blocklyrduino.r4.alpha`
- Build type: debug/alpha
- Signing source: Android Open Source Project public `testkey`
- Pinned source tag: `android-14.0.0_r1`
- The workflow converts the public AOSP test key to a temporary PKCS12 keystore during each CI build.

The AOSP test key is intentionally public and is suitable only for development/testing. It must **never** be used as the production/store signing identity.

## Update behaviour

The earlier alpha builds used per-run debug certificates, so they are not a reliable update base. `alpha.4` starts the stable alpha update channel. Future alpha builds must keep the same application ID and signing source while increasing `versionCode`.

Because the alpha package ID is separate from the future production package, development signing cannot compromise the production app identity. A later production/store release will use a separate private release/upload key.
