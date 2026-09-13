# Blockly@rduino R4 – Android prototype

This branch contains an experimental Android wrapper around the existing Blockly@rduino UI while the mobile architecture is being validated.

## Phase 1 goal

- Run fully offline in an Android app.
- Load the existing Blockly@rduino assets from packaged app resources rather than `file://`.
- Add the Arduino UNO R4 WiFi board profile.
- Make Blockly usable with touch on real phones.
- Generate Arduino code locally.

The Android prototype intentionally does **not** upload firmware to the UNO R4 yet.

## Why upload is separate

The desktop build can bundle Arduino CLI and native tools. Android cannot use the Windows executables directly. For the UNO R4 WiFi the official Renesas core uses the board's SAM-BA/BOSSAC upload path after the 1200-baud reset. A direct Android upload therefore needs a native Android USB implementation/bridge and must be tested separately.

## Test history

### 0.1.0-alpha.1

First real-device test:

- APK installs and opens successfully.
- Blockly content is visible.
- The inherited desktop layout is not usable on a phone in portrait orientation and only becomes partly visible in landscape.
- Touch dragging of blocks is not working reliably.

The drag problem exposed a timing issue: the pointer compatibility layer was loaded only after Blockly had already registered its event handlers.

### 0.1.0-alpha.2

Mobile-focused experiment:

- Android-only full-screen Blockly workspace.
- Desktop header and side control panel hidden.
- Mobile viewport and early pointer compatibility added.

Real-device result:

- App still installs and opens.
- Layout regression: only the workspace/zoom/delete area is visible; the Blockly categories/toolbox are no longer usable.
- Root cause: the mobile CSS forced `.blocklySvg` to `position:absolute; inset:0`, allowing the SVG workspace to cover or displace Blockly's own toolbox geometry.

### 0.1.0-alpha.3

Corrective mobile layout:

- Remove the forced absolute full-screen positioning from `.blocklySvg`.
- Let `Blockly.inject()` control SVG, toolbox and flyout geometry again.
- Keep only the surrounding Android container full-screen.
- Explicitly keep `.blocklyToolboxDiv` visible and above the workspace.
- Repair the toolbox position only if it is clearly off-screen.
- Preserve the early pointer/touch compatibility layer.
- Android versionCode incremented to 3.

Real-device result:

- App installs and opens successfully.
- Blockly categories/toolbox are visible again.
- Categories can be opened and blocks are displayed.
- Blocks can be dragged from the flyout onto the workspace with one finger.
- Placed blocks can be moved around the workspace with touch.
- This confirms the early pointer compatibility layer and the corrected Blockly geometry work on a real Android phone.
- The inherited desktop-style mobile layout is still visually rough and is not considered the final app UI.

### 0.1.0-alpha.4

Dropdown / long-press correction:

- Real-device test exposed another legacy touch issue: while using an editable block field/dropdown, the dropdown can disappear after a short hold and Blockly opens the workspace context menu with actions such as Undo/Redo/Clean up.
- Root cause: the legacy Blockly touch layer registers `touchstart` long-press handlers on both the workspace and every block. After `LONGPRESS` it converts the touch into a synthetic right-click (`button = 2`) and calls the normal context-menu path.
- Android disables this old touch-long-press context-menu gesture while preserving normal pointer dragging and field interaction.
- Browser/WebView context menus are suppressed inside the Blockly editor area.
- Blockly field/dropdown menus are kept above the workspace and receive larger touch targets plus bounded mobile height/scrolling.
- Android versionCode incremented to 4.

Further real-device findings from alpha.4:

- Blockly content can extend into the Android status-bar area. Dragging near the top can therefore pull down the Android notification shade instead of continuing the editor gesture.
- Some legacy blocks display broken/missing image assets.
- Legacy inline numeric/text fields are not reliably editable on the phone; examples include angle values and loop start/end values.
- The toolbox/flyout still consumes too much horizontal space in portrait mode.
- These are no longer treated as isolated bugs to patch one-by-one. Together they confirm that the inherited 2016 desktop/web UI is the wrong long-term Android surface.

### Stable alpha update channel (starts with alpha.4)

The first three APKs were normal CI debug builds and therefore did not provide a dependable long-term signing identity. Starting with alpha.4, the Android alpha channel is deliberately separated from the future production app:

- stable alpha application ID: `ch.elekto.blocklyrduino.r4.alpha`;
- stable public development signing identity generated from the Android Open Source Project `testkey` pinned to tag `android-14.0.0_r1`;
- future alpha builds keep the same application ID/signing identity and only increase `versionCode`;
- a future production/store build will use a separate private release/upload key and will not reuse the public development key.

This means alpha.4 is the baseline for in-place alpha updates. Alpha 5, Alpha 6, etc. should install directly over alpha.4 without uninstalling it first.

## Current milestone

The legacy Android wrapper has completed its purpose as a proof-of-concept:

- offline APK startup works;
- the UNO R4 profile can be loaded;
- Blockly-style categories and blocks render;
- touch drag from the flyout to the workspace works on real Android hardware;
- the stable alpha update channel exists.

The legacy Blockly@rduino mobile UI is now **frozen as a reference/proof-of-concept**. New work should not spend time progressively patching the old desktop surface unless a fix is required to recover data or validate one specific technical assumption.

## Architecture direction after alpha.4

The next Android version should be designed as an app first, not as a website adapted to a phone.

Core principles:

- Keep the stable alpha package/signing identity so the new architecture can still update alpha.4 in place.
- Build the application shell natively with Kotlin/Jetpack Compose.
- Respect Android system insets/status/navigation bars so editor gestures never compete with the notification shade.
- Make the workspace the main surface and avoid permanently occupying large areas with desktop-style panels.
- Use temporary drawers, bottom sheets and dialogs for categories, block settings, board selection, code view, serial monitor, compile/upload status and advanced options.
- Treat block values/settings as structured data, not as fragile inline HTML fields. On phones, editing a number, pin, text value or mode may open a native popup/bottom sheet with large touch targets.
- Store block/program state in an editor-independent model (for example JSON/domain objects). The visual editor and Arduino code generator should consume the same model.
- Keep Arduino code generation separate from rendering so the editor can later change without rewriting the compiler/upload pipeline.
- Package icons/images as local app resources; do not depend on old web-relative asset paths.
- Group functions into clear sections/chapters and allow beginner/advanced levels rather than exposing every possible block at once.
- Preserve portrait usability; landscape/tablet layouts may expose more controls but must not be required.
- Long-press must not secretly trigger destructive/global workspace actions; those belong in explicit menus.

### Editor engine decision

Two approaches remain valid and should be evaluated deliberately:

1. **Modern Blockly engine inside a native Compose app shell** – faster route, mature snapping/connection logic, but the canvas remains HTML/SVG/JavaScript internally.
2. **Custom native block editor** – more work, but complete control over touch, layout, block settings and long-term Android UX.

The next technical prototype should use a very small block set (for example number, delay, digital output, repeat and if/else) and test the custom native editor approach before committing to hundreds of blocks. If the native model/drag/snap prototype proves clean, continue natively; otherwise use modern Blockly only as the editor engine while keeping the rest of the app native.

## Next phases

- Freeze the legacy WebView prototype at alpha.4 as the reference baseline.
- Start the app-first Android architecture while keeping the same alpha package/signing channel.
- Implement a native project/editor shell with correct Android system insets.
- Define the editor-independent program/block data model.
- Prototype a small touch-first native block canvas and compare it against modern Blockly embedded only as an editor engine.
- Add mobile-native block value/settings editing through dialogs/bottom sheets.
- Add local project save/load before relying on more complex UI work.
- Native Android USB device detection.
- UNO R4 WiFi 1200-baud reset / SAM-BA-BOSSAC upload path.
- Serial monitor support where practical.
