# Elekto Block Language

Status: Alpha 7 foundation

This document defines the visual grammar of the native Elekto Blocks editor. Blockly was analysed as a reference for proven concepts, but the Android renderer and geometry are implemented independently in Kotlin/Jetpack Compose.

## Core rule

The visual language must explain itself before a child reads every label:

- **Shape = grammar / data type**
- **Colour = functional family**
- **Text and icon = concrete action**

Colour alone must never decide whether two blocks fit together.

## Connection families

### Statement connections

Commands and control blocks participate in a top-to-bottom statement stack.

- `previous`: inward socket on the top edge
- `next`: matching outward tab on the bottom edge
- compatible blocks share the same geometry and snap position

Examples: `Warten`, `Digitaler Ausgang`, `PWM-Ausgang`, `Wiederholen`, `Wenn`.

### Statement inputs

Control blocks expose a C-shaped statement body. The first nested command connects to a matching statement tab directly below the header. The container height is calculated from its children; the body is not a decorative frame with arbitrary empty space.

Examples: body of `Wiederholen` and `Wenn`.

### Value connections

Value blocks do not join statement stacks. They plug into typed value sockets.

Initial Alpha 7 type:

- `NUMBER`: rounded/capsule shape

Planned grammar:

- `BOOLEAN`: hexagonal shape
- `TEXT`: a distinct value shape while remaining visually compatible with text inputs

The shape and the connection checker must describe the same type rule.

## Alpha 7 typed inputs

| Owner block | Input key | Type | Fallback |
|---|---|---|---|
| Warten | `duration` | NUMBER | stored milliseconds |
| Wiederholen | `count` | NUMBER | stored repetition count |

`Analogwert A0–A5` is the first real NUMBER output block. It can therefore be dragged into these NUMBER sockets. When connected, generated Arduino code uses the value expression directly, e.g. `delay(analogRead(A0));` or a loop bound based on `analogRead(A0)`.

The fallback remains visible as a shadow-style value when no value block is connected. Later a dedicated editable number block will replace more of these fallback-only values.

## Geometry constants

The renderer uses a shared geometry vocabulary instead of per-block decorative shapes:

- statement height: 56 dp
- connector overlap: 8 dp
- command width: 248 dp
- number value size: 112 × 42 dp
- control width: 304 dp
- control header: 54 dp
- control footer: 18 dp
- statement indentation: 34 dp

These values may be tuned after device testing, but connector alignment must remain mathematically consistent.

## Snapping rules

1. Statement blocks may snap only to statement connections or statement inputs.
2. Value blocks may snap only to value inputs whose declared type matches the block output type.
3. One value socket may contain exactly one value block.
4. Replacing a value must detach the old block rather than delete it.
5. Moving a connected owner moves its nested statements and connected values with it.
6. Invalid connections never become part of the saved program model.

## Mobile interaction rules

- Workspace controls and the `Blöcke` action must occupy different screen zones.
- Zoom must not change drag distance; pointer movement is converted back into workspace coordinates.
- Editing values happens in touch-friendly sheets/popups, not tiny desktop-style controls.
- The workspace must remain usable in portrait orientation.
- Later: pinch zoom, connection highlight, insertion preview and haptic feedback on successful snap.

## Data model direction

A `ProgramBlock` can participate in one of three structural relations:

- `previousId`: statement sequence
- `parentId`: statement inside a control block
- `valueOwnerId` + `valueInputKey`: typed value inside an input socket

These relations are persisted locally and are independent from the visual renderer. Code generation reads the structure, not screen coordinates.

## Planned next grammar extensions

1. Dedicated NUMBER literal block.
2. BOOLEAN outputs and hexagonal sockets.
3. Digital input as a BOOLEAN value block.
4. Comparison block: NUMBER × operator × NUMBER → BOOLEAN.
5. `Wenn` refactor from fixed D-pin state settings to a BOOLEAN value input.
6. Variables with explicit data types.
7. Text values and serial output.
8. Multiple statement inputs (`wenn / sonst`).

The editor should grow from this grammar rather than adding block-specific one-off shapes.
