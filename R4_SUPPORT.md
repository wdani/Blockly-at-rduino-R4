# UNO R4 support status

This fork is adding first-class support for the Arduino UNO R4 family while keeping the original Blockly@rduino project structure intact.

## Verified board targets

- Arduino UNO R4 WiFi: `arduino:renesas_uno:unor4wifi`
- Arduino UNO R4 Minima: `arduino:renesas_uno:minima`

Official Arduino core: https://github.com/arduino/ArduinoCore-renesas

## Shared UNO R4 pin profile

The first implementation deliberately exposes the standard UNO headers that Blockly@rduino already understands well:

- Digital: D0-D13
- Analog: A0-A5
- PWM: D3, D5, D6, D9, D10, D11
- UART: D0/RX, D1/TX
- I2C: A4/SDA, A5/SCL
- SPI: D10/SS, D11/MOSI, D12/MISO, D13/SCK
- DAC: A0
- RTC: available

UNO R4 WiFi additionally provides the ESP32-S3 connectivity processor, 12x8 LED matrix and Qwiic connector.

## Current bootstrap

`r4.html` is a temporary compatibility entry point. It loads the upstream `index.html`, injects the UNO R4 profiles from `r4/uno-r4-profiles.js`, and defaults to UNO R4 WiFi.

This lets us validate Blockly code generation and the existing UI before making invasive edits to the original large HTML/board-profile files.

### Current limitations

- The board artwork still falls back to the classic UNO image.
- Direct `file://` use of `r4.html` is not supported by the bootstrap because browsers normally block `fetch()` from local files.
- Upload/compile still depends on the Blockly@rduino local upload backend and therefore also requires that backend to understand/install the Arduino Renesas UNO core.
- R4-specific blocks (LED matrix, RTC, DAC controls, WiFi/Bluetooth, Qwiic/Wire1, CAN) are not implemented yet.
- Interrupt blocks are currently exposed conservatively as D2/D3 until behaviour has been checked on physical hardware.

## Integration plan

1. Verify standard Blockly blocks generate valid UNO R4 sketches.
2. Verify compilation with Arduino CLI and `arduino:renesas_uno:unor4wifi`.
3. Verify USB upload and serial console with physical UNO R4 WiFi hardware.
4. Integrate the R4 profiles natively into `blockly@rduino_boards.js` and all relevant board selectors.
5. Add dedicated UNO R4 board artwork.
6. Add R4-specific blocks in separate, testable groups.
7. Restore full local/offline use without the temporary bootstrap loader.

## Compatibility principle

Existing Arduino/Blockly behaviour should not be changed unless required for the Renesas architecture. R4 support should be additive so that upstream changes remain easy to compare and merge.
