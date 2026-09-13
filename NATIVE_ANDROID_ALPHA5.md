# Native Android editor – Alpha 5

Alpha 5 is the architecture cut from the legacy Blockly@rduino WebView prototype to an app-first Android implementation.

## Why the cut happens here

Alpha 1–4 proved that the old editor can run offline in an APK and that touch dragging can be made to work. Real-device testing also exposed structural mobile problems: desktop-sized toolbox geometry, legacy long-press/context-menu behavior, HTML field editing issues, missing web assets, and workspace content extending into Android system gesture/status areas.

Those findings are kept as requirements, not as reasons to continue patching the legacy UI.

## Alpha 5 architecture

- Native Android activity written in Kotlin.
- Jetpack Compose UI; no WebView is used for the new editor.
- Material 3 app shell with Android safe-area/system-bar handling.
- Large scrollable workspace with a local grid.
- Native draggable program blocks with 8 dp snap-to-grid.
- Block palette is a temporary bottom sheet instead of a permanent wide desktop toolbox.
- Block values are edited in large native bottom sheets with numeric keyboard, +/- buttons and quick values.
- Project state is stored locally with SharedPreferences/JSON and survives app restarts and in-place alpha updates.
- Deterministic UNO R4 validation runs locally without AI.
- Arduino C++ is generated locally from the internal block model.
- The existing stable alpha package/signing identity remains in use so later alpha APKs can update Alpha 5 in place.

## First native block set

- Warten
- Digitalausgang HIGH/LOW
- PWM-Ausgang
- Analogwert lesen
- Wiederholen
- Wenn Eingang HIGH/LOW

UNO R4 validation currently checks digital pin ranges, PWM-capable pins D3/D5/D6/D9/D10/D11, analog inputs A0–A5, value ranges, and conflicting input/output use of the same digital pin.

## Included workflow

A fresh install starts with a Blink example:

1. D13 HIGH
2. wait 1000 ms
3. D13 LOW
4. wait 1000 ms

The Code action renders the generated Arduino sketch and allows copying it locally.

## Deliberate Alpha 5 limitation

REPEAT and IF blocks are represented and generate valid C++ containers, but nested child-block placement is not implemented yet. The block itself states this instead of pretending the feature already works.

This is the next editor-engine milestone because it requires a real connection/tree model rather than a visual-only indentation trick.

## Next implementation milestones

1. Block connection graph and snap points, including nesting into REPEAT/IF containers.
2. Undo/redo command history and multi-block stack dragging.
3. Project screen: create, duplicate, rename and switch projects.
4. Better block geometry and connection affordances while keeping touch targets large.
5. More UNO R4 blocks: digital input, variables, math, serial, tone, servo and selected Starter Kit components.
6. Compile service architecture and native USB upload path for UNO R4 WiFi.
7. Optional local small-code-model assistant only as an explanation layer on top of deterministic validation/compiler diagnostics.

## Mobile UI principles

The workspace stays primary. Categories, block settings, board selection, code, compile/upload state and advanced tools use drawers/sheets/dialogs instead of permanently occupying phone screen area. Portrait is a first-class layout; landscape/tablet may expose more controls but is not required.
