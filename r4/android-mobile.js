(function () {
    'use strict';

    if (window.__blocklyR4AndroidMobileBootstrapInstalled) {
        return;
    }
    window.__blocklyR4AndroidMobileBootstrapInstalled = true;

    function ensureViewport() {
        if (!document.head) {
            return;
        }
        var viewport = document.querySelector('meta[name="viewport"]');
        if (!viewport) {
            viewport = document.createElement('meta');
            viewport.name = 'viewport';
            document.head.appendChild(viewport);
        }
        viewport.content = 'width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover';
    }

    function installMobileStyles() {
        if (!document.documentElement || document.getElementById('r4-android-mobile-style')) {
            return;
        }

        document.documentElement.classList.add('r4-android-mobile');
        if (document.body) {
            document.body.classList.add('r4-android-mobile-body');
        }

        var style = document.createElement('style');
        style.id = 'r4-android-mobile-style';
        style.textContent = [
            'html.r4-android-mobile, html.r4-android-mobile body {',
            '  width: 100% !important;',
            '  height: 100% !important;',
            '  margin: 0 !important;',
            '  padding: 0 !important;',
            '  overflow: hidden !important;',
            '  overscroll-behavior: none !important;',
            '}',
            'html.r4-android-mobile body { position: fixed !important; inset: 0 !important; }',
            '',
            'html.r4-android-mobile #header,',
            'html.r4-android-mobile #menuPanel { display: none !important; }',
            '',
            'html.r4-android-mobile #divBody {',
            '  position: fixed !important;',
            '  inset: 0 !important;',
            '  width: 100% !important;',
            '  height: 100% !important;',
            '  margin: 0 !important;',
            '  padding: 0 !important;',
            '}',
            'html.r4-android-mobile #divTabpanel,',
            'html.r4-android-mobile .divTabpanel-ver,',
            'html.r4-android-mobile .divTabpanel-hor {',
            '  position: absolute !important;',
            '  inset: 0 !important;',
            '  width: 100% !important;',
            '  height: 100% !important;',
            '  margin: 0 !important;',
            '  padding: 0 !important;',
            '}',
            'html.r4-android-mobile #content_area,',
            'html.r4-android-mobile #content_blocks {',
            '  width: 100% !important;',
            '  height: 100% !important;',
            '  margin: 0 !important;',
            '  padding: 0 !important;',
            '  overflow: hidden !important;',
            '}',
            '',
            'html.r4-android-mobile .blocklySvg,',
            'html.r4-android-mobile .blocklyWorkspace,',
            'html.r4-android-mobile .blocklyFlyout,',
            'html.r4-android-mobile .blocklyDraggable {',
            '  touch-action: none !important;',
            '  -ms-touch-action: none !important;',
            '}',
            '',
            'html.r4-android-mobile .blocklyToolboxDiv {',
            '  display: block !important;',
            '  visibility: visible !important;',
            '  z-index: 80 !important;',
            '  max-width: 46vw !important;',
            '  overflow-x: hidden !important;',
            '  overflow-y: auto !important;',
            '  -webkit-overflow-scrolling: touch;',
            '}',
            'html.r4-android-mobile .blocklyToolboxDiv .blocklyTreeRoot [role="treeitem"] .blocklyTreeRow {',
            '  min-height: 38px !important;',
            '  height: 38px !important;',
            '  line-height: 30px !important;',
            '  padding: 4px 8px 4px 10px !important;',
            '}',
            'html.r4-android-mobile .blocklyTreeLabel {',
            '  font-size: 12px !important;',
            '  white-space: nowrap !important;',
            '}',
            '',
            'html.r4-android-mobile .blocklyWidgetDiv {',
            '  z-index: 300 !important;',
            '  max-width: calc(100vw - 16px) !important;',
            '}',
            'html.r4-android-mobile .blocklyWidgetDiv .goog-menu {',
            '  max-width: calc(100vw - 24px) !important;',
            '  max-height: 60dvh !important;',
            '  overflow-x: hidden !important;',
            '  overflow-y: auto !important;',
            '  -webkit-overflow-scrolling: touch;',
            '  touch-action: pan-y !important;',
            '}',
            'html.r4-android-mobile .blocklyWidgetDiv .goog-menuitem {',
            '  min-height: 44px !important;',
            '  line-height: 44px !important;',
            '  font-size: 16px !important;',
            '  padding-left: 14px !important;',
            '  padding-right: 14px !important;',
            '}',
            '',
            'html.r4-android-mobile #btn_delete {',
            '  right: 10px !important;',
            '  bottom: 10px !important;',
            '  padding: 8px 10px !important;',
            '  min-width: 44px !important;',
            '  min-height: 44px !important;',
            '  z-index: 90 !important;',
            '}',
            'html.r4-android-mobile #btn_delete #span_delete { display: none !important; }',
            'html.r4-android-mobile .modal-dialog { width: auto !important; margin: 12px !important; }'
        ].join('\n');

        (document.head || document.documentElement).appendChild(style);
    }

    function findWorkspace() {
        if (!window.Blockly) {
            return null;
        }
        if (Blockly.mainWorkspace) {
            return Blockly.mainWorkspace;
        }
        if (window.BlocklyDuino) {
            return BlocklyDuino.workspace || BlocklyDuino.workspaceBlockly || null;
        }
        return null;
    }

    function keepToolboxVisible() {
        var toolbox = document.querySelector('.blocklyToolboxDiv');
        if (!toolbox) {
            return;
        }

        toolbox.style.display = 'block';
        toolbox.style.visibility = 'visible';
        toolbox.style.zIndex = '80';

        var rect = toolbox.getBoundingClientRect();
        if (rect.right <= 0 || rect.left >= window.innerWidth) {
            toolbox.style.left = '0px';
            toolbox.style.right = 'auto';
        }
        if (rect.bottom <= 0 || rect.top >= window.innerHeight) {
            toolbox.style.top = '0px';
        }
    }

    function installContextMenuGuard() {
        if (window.__blocklyR4AndroidContextGuardInstalled) {
            return;
        }
        window.__blocklyR4AndroidContextGuardInstalled = true;

        document.addEventListener('contextmenu', function (event) {
            var target = event.target;
            if (!target || !target.closest) {
                return;
            }
            if (target.closest('.blocklySvg') ||
                target.closest('.blocklyWidgetDiv') ||
                target.closest('.blocklyToolboxDiv')) {
                event.preventDefault();
            }
        }, true);
    }

    function resizeBlockly() {
        try {
            var workspace = findWorkspace();
            if (workspace && window.Blockly && typeof Blockly.svgResize === 'function') {
                Blockly.svgResize(workspace);
            }
            keepToolboxVisible();
        } catch (error) {
            console.warn('Android mobile Blockly resize failed', error);
        }
    }

    function applyMobileMode() {
        ensureViewport();
        installMobileStyles();
        installContextMenuGuard();
        window.setTimeout(resizeBlockly, 0);
        window.setTimeout(resizeBlockly, 100);
        window.setTimeout(resizeBlockly, 300);
        window.setTimeout(resizeBlockly, 800);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', applyMobileMode, false);
    } else {
        applyMobileMode();
    }

    window.addEventListener('load', applyMobileMode, false);
    window.addEventListener('resize', function () {
        window.setTimeout(resizeBlockly, 50);
    }, false);
    window.addEventListener('orientationchange', function () {
        window.setTimeout(resizeBlockly, 150);
        window.setTimeout(resizeBlockly, 450);
    }, false);
})();
