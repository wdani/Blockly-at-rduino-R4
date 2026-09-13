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

## Current test checklist

1. Install the newest debug APK.
2. Open the app in portrait orientation.
3. Confirm the Blockly categories/toolbox are visible.
4. Open a category and confirm its blocks appear.
5. Drag one block from the flyout into the workspace with one finger.
6. Move the placed block around the workspace with one finger.
7. Rotate portrait -> landscape -> portrait and confirm the workspace remains usable.

Only after toolbox visibility and touch dragging work reliably will the Android UI be redesigned into a proper mobile-native app structure.

## Next phases

- Replace the inherited desktop chrome with a deliberate mobile app shell rather than progressively hiding desktop elements.
- Decide whether the long-term editor remains modern Blockly embedded in a native Kotlin/Compose app or becomes a custom native block editor.
- Native Android USB device detection.
- UNO R4 WiFi 1200-baud reset / SAM-BA-BOSSAC upload path.
- Serial monitor support where practical.
