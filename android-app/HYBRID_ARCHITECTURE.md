# Elekto Hybrid Architecture

Status: Hybrid prototype 1

## Goal

Elekto should remain a child-friendly electronics and programming environment that can support multiple microcontroller families over time. The Arduino UNO R4 WiFi is the first fully tested target, not a permanent limitation.

Blockly is used as an editor engine, not as the product UI.

## Layers

1. Native Android UI
   - learning content
   - projects
   - block catalogue
   - examples and explanations
   - board selection
   - code view
   - upload / serial monitor later
   - simulation / coding playground later

2. Elekto editor adapter
   - small stable API between Android and the editor engine
   - addBlock(id)
   - undo / redo
   - load project / save project
   - request generated code
   - later: selection, context actions, diagnostics

3. Blockly engine
   - connections
   - stack behaviour
   - insertion preview
   - shadow blocks
   - undo / redo
   - serialization
   - variables / procedures / mutators
   - plugin support

4. Elekto block definitions
   - visual programming grammar
   - child-friendly text
   - data types and connection checks
   - board-independent block IDs where possible

5. Target / generator layer
   - UNO R4 WiFi first
   - later other Arduino boards, ESP32, micro:bit or other targets
   - a block should not need to know which board is active when its meaning is generic

## Rule for flexibility

The native app must never manipulate Blockly SVG paths or connection objects directly.

The Android side addresses semantic Elekto IDs such as:
- delay
- digital_write
- analog_read
- compare
- repeat
- if

The adapter translates those IDs into the currently used editor engine.

This makes it possible to:
- upgrade Blockly
- change renderer
- install Blockly plugins
- replace the toolbox
- change block visuals
- add another target board
without rewriting the whole Android app.

## UI principle

Blockly's default toolbox is not the final user interface.

Elekto provides its own mobile catalogue with:
- category
- real/representative block preview
- short explanation
- simple example
- later help / learning link

Blockly remains responsible for editor mechanics.

## Interaction direction

Keep Blockly's strong insertion behaviour:
- existing blocks move aside during insertion preview
- connected descendants move with a grabbed stack

Add Elekto-specific improvements:
- dragged block becomes partly transparent near a valid target
- clearer target highlight
- mobile bottom-sheet context menu
- larger touch targets
- child-friendly explanations

## Licensing boundary

Blockly is an external open-source dependency and must keep its required notices/license.
Blockoli is only a behavioural/technical reference. Do not copy proprietary code, graphics or app assets from it.
