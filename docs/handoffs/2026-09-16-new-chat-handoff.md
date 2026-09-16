# Elekto Blocks – New Chat Handoff

**Date:** 2026-09-16
**Repository:** `wdani/Blockly-at-rduino-R4`
**Development branch:** `android/blockly-modern-poc`
**Current HEAD:** `57472b5564743bcc117c95a5c6b091fa3baf2fea`
**Current build:** **Hybrid 15** (`0.16.0-hybrid.15`, versionCode 21)

## IMPORTANT FOR THE NEXT CHAT

Continue from this state. **Do not restart the architecture/design discussion and do not rebuild Hybrid 13/14 work.** The Blueprint system has already been implemented on the branch and CI builds successfully. The immediate next step is **device acceptance testing of Hybrid 15**, followed by targeted fixes only if the user reports problems.

The user prefers a direct, practical workflow. When producing an Android test build: make the change → wait for GitHub Actions in the same task → verify success → download/extract the artifact → provide the **direct APK**, not just a ZIP. Do not create hourly build-monitor automations.

## Stable baseline before Blueprints

Hybrid 13 fixed the Blockly preview system. Hybrid 14 added a native long-press block menu with:

- Duplizieren
- Einklappen / Ausklappen
- Deaktivieren / Aktivieren
- Löschen

The user tested Hybrid 14 and confirmed **everything worked**. Do not regress these behaviors.

## Blueprint UX agreed with the user

Blueprints are reusable local templates containing one or more Blockly groups.

Creation flow:

1. Long-press any block.
2. Existing native block menu contains **„Als Blueprint speichern“**.
3. The complete connected stack/group containing that block becomes selected automatically, regardless of which block in the stack was long-pressed.
4. The normal menu closes and the editor enters **Blueprint selection mode**.
5. Tapping any block in another connected group toggles that entire group selected/unselected.
6. Disconnected groups may be selected together.
7. Selected groups receive a visual highlight.
8. A native selection bar provides **Abbrechen** and **Blueprint erstellen**.
9. Blueprint erstellen asks for a name.
10. Blueprints are stored locally/offline.

Browser UX:

- Existing **Blöcke auswählen** browser gains two top-level areas: **Blöcke | Blueprints**.
- Blueprints are NOT added as another horizontal category chip.
- Blueprint cards support insert, rename, delete.
- Tapping the Blueprint itself inserts a copy.
- Relative positions between disconnected saved groups are preserved.
- Inserted blocks get fresh Blockly IDs; Blueprints are templates, not live references.

## Future block-browser navigation decision

The user explicitly raised that the current horizontally scrolling category chips will become bad once many more blocks/categories are added.

Agreed direction for a later separate step:

- Top level remains `Blöcke | Blueprints`.
- Under Blöcke, replace the ever-growing horizontal chip row with a compact category selector/menu.
- Allow at most **two category levels** (e.g. `Ein-/Ausgänge → Digital | Analog | PWM`).
- Add search later as a helper, not as the only navigation.
- Do **not** implement this redesign together with Blueprint bug fixing unless specifically requested.

## Blueprint implementation documents

Read these if details are needed:

- Design/spec: `docs/superpowers/specs/2026-09-16-blueprints-design.md`
- Implementation plan: `docs/superpowers/plans/2026-09-16-blueprints-implementation.md`

The design was explicitly approved by the user with “weiter”.

## Hybrid 15 implementation now present

Compared with the pre-Blueprint plan commit, the branch contains the full implementation across 19 commits.

Key new files:

- `android-app/app/src/main/assets/blockly/blueprint-engine.js`
- `android-app/app/src/main/assets/blockly/blueprint-controller.js`
- `android-app/tests/blueprint-engine.test.cjs`
- `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/model/Blueprint.kt`
- `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/data/BlueprintStore.kt`
- `android-app/app/src/test/java/ch/elekto/blocklyrduino/r4/data/BlueprintStoreTest.kt`
- `android-app/app/src/main/java/ch/elekto/blocklyrduino/r4/ui/BlueprintUi.kt`

Modified integration files include:

- `MainActivity.kt`
- `block-context.js`
- `index.html`
- `styles.css`
- `build.gradle`
- `.github/workflows/build-android.yml`

Blueprint responsibilities remain intentionally separated:

- Blockly/JS: graph resolution, selection, serialization, insertion, visual marking.
- Android/Kotlin: native UI, naming, list/management, persistent local Blueprint storage.

## CI status

Final Hybrid 15 build commit:

`57472b5564743bcc117c95a5c6b091fa3baf2fea` — `chore(ci): publish Hybrid 15 blueprint build`

GitHub Actions **Build Android APK** run:

- Run ID: `35098806101`
- Conclusion: **success**
- Completed: 2026-09-16 13:00 UTC

The separate `forgeedu-sync` workflow failed on the same commit, but that is unrelated to the Android build. The Android build workflow is the relevant gate and succeeded.

## NEXT EXACT STEP

Obtain/use the Hybrid 15 APK and have the user test on the Android device.

Focus acceptance testing on:

1. Long press → **Als Blueprint speichern**.
2. Whole connected group is selected automatically.
3. Tap another disconnected stack → entire second stack selected.
4. Tap it again → deselected.
5. Save Blueprint with a name.
6. Open Blöcke → Blueprints and find it after closing/reopening app.
7. Insert Blueprint and verify all groups/connections plus relative layout.
8. Rename Blueprint.
9. Delete Blueprint.
10. Confirm normal Hybrid 14 actions still work outside selection mode: duplicate, collapse, disable, delete, drag-to-delete, previews, undo/redo.

If anything fails, fix only the reported behavior, run the existing Blueprint/Block tests, wait for CI, and provide a new direct APK.

## User/project preferences worth preserving

- German, normal standard German, no gender-star/colon/Binnen-I.
- Child-friendly but technically correct electronics/programming app.
- Final app local/offline, no user account/cloud requirement.
- GitHub is the central versioned project archive.
- Mobile UX should feel like a real app, not a web wrapper.
- Blockly is the engine, not the visible product UI.
- Do not modify default/release branches casually; current work stays on `android/blockly-modern-poc`.
- User dislikes being asked to manually inspect CI/download ZIPs. Handle CI and APK extraction directly when possible.
