# Blockly@rduino R4 – Windows Desktop

This branch replaces the browser-only R4 test path with a real Windows desktop application.

## Goal

The user workflow should be:

1. Start Blockly@rduino R4 as a Windows app.
2. Build the program with Blockly blocks.
3. Select the detected Arduino UNO R4 WiFi USB/COM port.
4. Press the check button to compile.
5. Press the upload arrow to compile and upload directly to the UNO R4 WiFi.

No browser, Python web server, copy/paste into Arduino IDE, or legacy Codebender plugin should be required.

## Desktop stack

- Electron 44.3.0
- Arduino CLI 1.5.1
- Board target: `arduino:renesas_uno:unor4wifi`
- Arduino core: `arduino:renesas_uno`

The Electron renderer runs the existing `index_electron.html` Blockly UI. A secure preload bridge exposes only the required Arduino operations to the renderer; Node.js is not exposed to Blockly content.

## First-run behaviour

The Windows build bundles `arduino-cli.exe`.

The Renesas UNO board core is stored in the app's user-data directory. If it is missing, the first compile asks the user for permission to download/install it. Once installed, normal compilation and upload can work without Arduino IDE.

## Existing toolbar mapping

- Check button (`btn_verify_local`): compile the generated sketch for UNO R4 WiFi.
- Port selector (`serialport_ide`): lists USB/COM ports discovered by Arduino CLI and prefers a detected UNO R4 WiFi.
- Arrow button (`btn_flash_local`): compile and upload directly to the selected port.

## Security / isolation

Electron runs with:

- `contextIsolation: true`
- `nodeIntegration: false`
- a small preload API (`window.r4Desktop`) for environment checks, core installation, port discovery, compile and upload.

This avoids exposing `child_process` or the local filesystem directly to the Blockly renderer.

## Current prototype limitations

- Windows x64 only.
- Serial monitor is not implemented yet.
- The Renesas core is downloaded on first use instead of being embedded in the installer.
- Dedicated UNO R4 board artwork is still pending.
- Hardware upload has not yet been verified on the physical UNO R4 WiFi.

## Build

The GitHub Actions workflow `.github/workflows/build-windows-desktop.yml` downloads Arduino CLI 1.5.1, builds the Electron/NSIS application and uploads the installer as a workflow artifact named `Blockly-rduino-R4-Windows`.

The desktop work remains isolated on `desktop/uno-r4-windows` until the generated installer has passed real-hardware tests.
