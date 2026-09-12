(function () {
    'use strict';

    if (!window.Blockly || !window.PointerEvent) {
        return;
    }

    // Blockly@rduino still ships a legacy Blockly generation that predates the
    // modern Pointer Events API. Current Chromium/Edge versions can expose
    // mouse/touch input in ways that leave the old mousedown/mousemove drag
    // pipeline unreliable. Keep Blockly's public behaviour, but route the
    // legacy mouse bindings through Pointer Events on modern engines.
    var originalBindEventWithChecks = Blockly.bindEventWithChecks_;

    function pointerTypeForLegacyEvent(type) {
        if (type === 'mousedown') return 'pointerdown';
        if (type === 'mousemove') return 'pointermove';
        if (type === 'mouseup') return 'pointerup';
        return null;
    }

    Blockly.bindEventWithChecks_ = function (node, eventName, context, func, optNoCaptureIdentifier) {
        var pointerEventName = pointerTypeForLegacyEvent(eventName);

        if (!pointerEventName) {
            return originalBindEventWithChecks.call(
                Blockly,
                node,
                eventName,
                context,
                func,
                optNoCaptureIdentifier
            );
        }

        var handler = function (event) {
            if (event.isPrimary === false) {
                return;
            }

            // Ignore synthetic/non-primary pointer streams. Mouse, pen and a
            // single touch pointer all use the same coordinates expected by
            // the legacy Blockly drag code.
            if (event.pointerType &&
                event.pointerType !== 'mouse' &&
                event.pointerType !== 'pen' &&
                event.pointerType !== 'touch') {
                return;
            }

            if (pointerEventName === 'pointerdown' &&
                event.currentTarget &&
                typeof event.currentTarget.setPointerCapture === 'function') {
                try {
                    event.currentTarget.setPointerCapture(event.pointerId);
                } catch (ignore) {
                    // Pointer capture is only an optimisation. Dragging still
                    // works through document-level pointermove listeners.
                }
            }

            if (context) {
                func.call(context, event);
            } else {
                func(event);
            }
        };

        node.addEventListener(pointerEventName, handler, false);
        var wrappers = [[node, pointerEventName, handler]];

        if (eventName === 'mouseup') {
            var cancelHandler = function (event) {
                if (event.isPrimary === false) {
                    return;
                }
                if (context) {
                    func.call(context, event);
                } else {
                    func(event);
                }
            };
            node.addEventListener('pointercancel', cancelHandler, false);
            wrappers.push([node, 'pointercancel', cancelHandler]);
        }

        return wrappers;
    };

    // Prevent the browser from interpreting a drag as viewport panning or a
    // native gesture. This is especially important on Windows systems that
    // report touch/pen capability even when a mouse is used.
    var style = document.createElement('style');
    style.textContent = [
        '.blocklySvg,',
        '.blocklyWorkspace,',
        '.blocklyFlyout,',
        '.blocklyDraggable {',
        '  touch-action: none !important;',
        '  -ms-touch-action: none !important;',
        '}'
    ].join('\n');
    document.head.appendChild(style);

    window.__blocklyR4PointerCompat = true;
})();
