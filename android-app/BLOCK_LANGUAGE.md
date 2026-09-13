# Elekto Block Language

Status: Alpha 8 grammar + renderer foundation

This document defines the visual grammar of the native Elekto Blocks editor. Blockly was analysed as a reference for proven concepts, but the Android renderer, geometry and connection model are implemented independently in Kotlin/Jetpack Compose.

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

Control blocks expose a C-shaped statement body. The first nested command connects directly below the header. The container height is calculated from its children; the body is not a decorative frame with arbitrary empty space.

Alpha 8 also allows insertion between existing children: the preview shows the insertion position before the finger is released.

### Value connections

Value blocks do not join statement stacks. They plug into typed value sockets.

Implemented in Alpha 8:

- `NUMBER`: rounded/capsule shape
- `BOOLEAN`: hexagonal shape

Planned:

- `TEXT`: a distinct value shape while remaining visually compatible with text inputs

The visible shape and the connection checker describe the same type rule.

## Alpha 8 typed inputs and outputs

| Block | Input / Output | Type | Fallback |
|---|---|---|---|
| Warten | `duration` | NUMBER input | stored milliseconds |
| Wiederholen | `count` | NUMBER input | stored repetition count |
| Wenn | `condition` | BOOLEAN input | stored digital-pin HIGH/LOW test |
| Analogwert | output | NUMBER | A0–A5 |
| Zahl | output | NUMBER | editable literal |
| Digitaler Zustand | output | BOOLEAN | digital pin HIGH/LOW |
| Zahlen vergleichen | `left`, `right` | NUMBER inputs | two stored numbers |
| Zahlen vergleichen | output | BOOLEAN | result of comparison |

Examples that are now structurally possible:

- `Warten [1000]`
- `Warten [Analog A0]`
- `Wiederhole [5] mal`
- `Wenn [D2 = HIGH]`
- `Wenn [[Analog A0] > [500]]`

The Arduino generator reads the connection tree, not screen coordinates.

## Shadow-style fallbacks

Typed sockets show a default value while they are empty. This keeps beginner blocks immediately usable without forcing a child to fetch a separate value block for every simple constant.

When a compatible value block is inserted, it replaces the visual fallback. Removing the value restores the fallback; the value block itself is detached rather than silently deleted.

## Geometry and layout

Alpha 8 moves away from fixed per-block rectangles toward a shared layout vocabulary:

- statement height: 56 dp
- value height: 42 dp
- connector overlap: 8 dp
- control header: 54 dp
- control footer: 18 dp
- statement indentation: 34 dp
- minimum command width: 208 dp
- minimum control width: 286 dp
- NUMBER socket minimum: 92 dp
- BOOLEAN socket minimum: 152 dp

Block width is calculated from content and connected value width. Socket positions are derived from the same layout rules instead of hard-coded screen coordinates. A wide Boolean comparison inserted into `Wenn`, for example, can make the owner header wider without changing the connection meaning.

## Snapping rules

1. Statement blocks may snap only to statement connections or statement inputs.
2. Value blocks may snap only to value inputs whose declared type matches the block output type.
3. One value socket contains exactly one value block.
4. Replacing a value detaches the old value rather than deleting it.
5. Moving a connected owner moves its nested statements and connected values with it.
6. Invalid connections never become part of the saved program model.
7. Statement insertion inside a control body preserves the surrounding child order.
8. A visible ghost preview appears at the exact snap location before drop.

## Mobile interaction rules

- Workspace controls and the `Blöcke` action occupy different screen zones.
- Zoom does not change drag distance; pointer movement is converted back into workspace coordinates.
- Editing values uses touch-friendly sheets/popups, not tiny desktop-style controls.
- The workspace remains usable in portrait orientation.
- Connection previews give feedback before drop.
- Planned: pinch zoom and haptic feedback on successful snap.

## Data model

A `ProgramBlock` can participate in one of three structural relations:

- `previousId`: statement sequence
- `parentId` + `childOrder`: statement inside a control block
- `valueOwnerId` + `valueInputKey`: typed value inside an input socket

Additional semantic data is stored independently from drawing geometry, including numeric fallback values, HIGH/LOW flags and comparison operator.

## Current design direction

The editor must grow from the grammar, not from one-off block artwork. New blocks should first declare:

1. role: command, value or container
2. input/output data types
3. semantic parameters
4. generated Arduino meaning
5. then visual content using the shared renderer

## Planned grammar extensions

1. Text values and serial output.
2. Variables with explicit data types.
3. Multiple statement inputs (`wenn / sonst`).
4. Boolean operations (`UND`, `ODER`, `NICHT`).
5. Arithmetic NUMBER blocks.
6. Dedicated Arduino `Beim Start` / `Immer wieder` structural blocks if user testing confirms this is clearer for children.
7. Haptic snap confirmation and stronger connection highlighting.
