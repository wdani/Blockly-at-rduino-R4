(function () {
    'use strict';

    if (window.__blocklyR4PointerCompatBootstrapInstalled) {
        return;
    }
    window.__blocklyR4PointerCompatBootstrapInstalled = true;

    function installPointerCompatibility() {
        if (window.__blocklyR4PointerCompat) {
            return true;
        }
        if (!window.Blockly || !window.PointerEvent ||
            typeof Blockly.bindEventWithChecks_ !== 'function') {
            return false;
        }

        // Blockly@rduino ships an older Blockly generation whose drag bindings
        // were created for mouse/touch events before Pointer Events became the
        // normal input path in current Chromium/WebView builds.
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
                        // Dragging still works through document-level listeners.
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

        var style = document.createElement('style');
        style.id = 'r4-pointer-compat-style';
        style.textContent = [
            '.blocklySvg,',
            '.blocklyWorkspace,',
            '.blocklyFlyout,',
            '.blocklyDraggable {',
            '  touch-action: none !important;',
            '  -ms-touch-action: none !important;',
            '}'
        ].join('\n');
        (document.head || document.documentElement).appendChild(style);

        window.__blocklyR4PointerCompat = true;
        return true;
    }

    // When injected at document start Blockly itself does not exist yet. Patch
    // it before BlocklyDuino.init() runs from the body's onload handler.
    if (!installPointerCompatibility()) {
        var attempts = 0;
        var timer = window.setInterval(function () {
            attempts += 1;
            if (installPointerCompatibility() || attempts >= 400) {
                window.clearInterval(timer);
            }
        }, 5);

        document.addEventListener('DOMContentLoaded', installPointerCompatibility, false);
        window.addEventListener('load', installPointerCompatibility, false);
    }
})();
