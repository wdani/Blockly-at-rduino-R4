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
- Android now disables this old touch-long-press context-menu gesture while preserving normal pointer dragging and field interaction.
- Browser/WebView context menus are suppressed inside the Blockly editor area.
- Blockly field/dropdown menus are kept above the workspace and receive larger touch targets plus bounded mobile height/scrolling.
- Android versionCode incremented to 4.

### Stable alpha update channel (starts with alpha.4)

The first three APKs were normal CI debug builds and therefore did not provide a dependable long-term signing identity. Starting with alpha.4, the Android alpha channel is deliberately separated from the future production app:

- stable alpha application ID: `ch.elekto.blocklyrduino.r4.alpha`;
- stable public development signing identity generated from the Android Open Source Project `testkey` pinned to tag `android-14.0.0_r1`;
- future alpha builds keep the same application ID/signing identity and only increase `versionCode`;
- a future production/store build will use a separate private release/upload key and will not reuse the public development key.

This means alpha.4 is the baseline for in-place alpha updates. Alpha 5, Alpha 6, etc. should install directly over alpha.4 without uninstalling it first.

## Current milestone

The core mobile interaction proof-of-concept is successful: Blockly loads offline in the Android app and touch dragging works on real hardware. Alpha 4 focuses on making editable block fields/dropdowns stable on touch devices and establishes the stable alpha update channel.

## Mobile UI direction

The final Android app should not copy the desktop Blockly@rduino layout one-to-one. The working editor behaviour should stay stable while the app shell is redesigned specifically for phones and tablets.

Principles for the mobile layout:

- Keep the workspace as the main surface; avoid permanently occupying large areas with desktop-style panels.
- Use a compact top app bar for project title, board/connection status and a small number of primary actions.
- Open block categories in a temporary drawer, bottom sheet or popup rather than reserving nearly half the screen permanently.
- Use bottom sheets / dialogs for block-specific settings, board selection, code view, serial monitor, compile/upload status and advanced options.
- Make popups large enough for touch, scrollable when needed, and easy to dismiss without losing the current workspace.
- Group larger functions into clear sections/chapters instead of exposing everything at once.
- Keep beginner and advanced functions separated so the interface can grow without becoming crowded.
- Preserve portrait usability; landscape/tablet layouts may expose more controls but must not be required for normal use.
- Long-press should not secretly trigger destructive or global workspace actions. Such actions belong in an explicit overflow/menu surface.

## Next phases

- Verify Alpha 4: dropdowns remain open and selectable without the workspace context menu appearing.
- Verify Alpha 4 as the stable update baseline before moving to Alpha 5.
- Replace the inherited desktop chrome with a deliberate mobile app shell rather than progressively hiding desktop elements.
- Design a compact mobile project/editor flow with only the controls needed on a phone.
- Decide whether the long-term editor remains modern Blockly embedded in a native Kotlin/Compose app or becomes a custom native block editor.
- Preserve the now-working touch/drag behaviour while the UI is redesigned.
- Native Android USB device detection.
- UNO R4 WiFi 1200-baud reset / SAM-BA-BOSSAC upload path.
- Serial monitor support where practical.
