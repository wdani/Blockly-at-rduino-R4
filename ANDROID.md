# Blockly@rduino R4 – Android prototype

This branch contains an experimental Android wrapper around the existing Blockly@rduino web UI.

## Phase 1 goal

- Run fully offline in an Android WebView.
- Load the existing Blockly@rduino UI from packaged app assets rather than `file://`.
- Add the Arduino UNO R4 WiFi board profile.
- Reuse the legacy Blockly pointer compatibility layer for touch/pointer input.
- Generate Arduino code locally.

The Android prototype intentionally does **not** upload firmware to the UNO R4 yet.

## Why upload is separate

The desktop build can bundle `arduino-cli.exe`, but Android cannot use that Windows executable. The official Arduino Renesas core uploads UNO R4 firmware through its native upload tools, including `dfu-util`. Direct Android upload therefore needs a native Android USB/DFU implementation or bridge and must be tested separately.

## Test history

### 0.1.0-alpha.1

First real-device test:

- APK installs and opens successfully.
- Blockly content is visible.
- The inherited desktop layout is not usable on a phone in portrait orientation and only becomes partly visible in landscape.
- Touch dragging of blocks is not working reliably.

The drag problem exposed a timing issue: the pointer compatibility layer was loaded only after Blockly had already registered its event handlers.

### 0.1.0-alpha.2

Mobile-focused changes:

- Android-only responsive layout; desktop/web UI remains unchanged.
- Desktop header and side control panel are hidden in the Android workspace so Blockly can use the full phone screen.
- Mobile viewport configuration and orientation/resize handling were added.
- Blockly toolbox rows receive touch-sized targets while remaining compact enough for portrait screens.
- Pointer compatibility is injected at document start, before `BlocklyDuino.init()` binds the workspace drag handlers.
- Android app versionCode incremented to 2.

## Current test checklist

1. Install the newest debug APK.
2. Open the app in portrait orientation.
3. Confirm the Blockly workspace fills the available screen.
4. Confirm the Blockly categories are usable without needing landscape orientation.
5. Drag a block from the flyout into the workspace using one finger.
6. Move an existing block around the workspace using one finger.
7. Rotate portrait -> landscape -> portrait and confirm the workspace resizes correctly.
8. Confirm `Arduino UNO R4 WiFi` appears as the selected board.
9. Build a simple Blink program and verify that Arduino code is generated.

## Next phases

- Refine the Android mobile toolbar after touch/workspace behaviour is stable.
- Native Android USB device detection.
- UNO R4 bootloader / DFU transition handling.
- Native DFU upload bridge.
- Serial monitor support where practical.
