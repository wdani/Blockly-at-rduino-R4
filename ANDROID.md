# Blockly@rduino R4 – Android prototype

This branch contains an experimental Android wrapper around the existing Blockly@rduino web UI.

## Phase 1 goal

- Run fully offline in an Android WebView.
- Load the existing Blockly@rduino UI from packaged app assets rather than `file://`.
- Add the Arduino UNO R4 WiFi board profile.
- Reuse the legacy Blockly pointer compatibility layer for touch/pointer input.
- Generate Arduino code locally.

The first APK intentionally does **not** upload firmware to the UNO R4 yet.

## Why upload is separate

The desktop build can bundle `arduino-cli.exe`, but Android cannot use that Windows executable. The official Arduino Renesas core uploads UNO R4 firmware through its native upload tools, including `dfu-util`. Direct Android upload therefore needs a native Android USB/DFU implementation or bridge and must be tested separately.

## Test checklist

1. Install the debug APK.
2. Open the app with no network connection.
3. Confirm the Blockly categories and blocks appear.
4. Drag a block from the flyout into the workspace using touch.
5. Move an existing block on the workspace.
6. Confirm `Arduino UNO R4 WiFi` appears as the selected board.
7. Build a simple Blink program and verify that Arduino code is generated.

## Next phases

- Native Android USB device detection.
- UNO R4 bootloader / DFU transition handling.
- Native DFU upload bridge.
- Serial monitor support where practical.
- Modern mobile-oriented UI after the functional baseline is stable.
