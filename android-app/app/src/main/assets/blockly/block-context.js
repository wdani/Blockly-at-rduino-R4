(() => {
  'use strict';

  if (typeof Blockly === 'undefined' || !window.ElektoBlockActions || !window.Elekto) return;

  const workspace = Blockly.getMainWorkspace?.();
  if (!workspace) return;

  const LONG_PRESS_MS = 520;
  const MOVE_TOLERANCE = 14;
  let pendingPress = null;

  function findBlockFromTarget(target) {
    let node = target;
    while (node && node !== document.body) {
      const id = node.getAttribute?.('data-id');
      if (id) {
        let block = workspace.getBlockById(id);
        if (block?.isShadow?.()) block = block.getParent?.() || block;
        if (block) return block;
      }
      node = node.parentNode;
    }
    return null;
  }

  function clearPendingPress() {
    if (!pendingPress) return;
    clearTimeout(pendingPress.timer);
    pendingPress = null;
  }

  function openNativeBlockMenu(block) {
    workspace.cancelCurrentGesture?.();
    block.select?.();
    const state = window.ElektoBlockActions.getState(workspace, block.id);
    window.ElektoAndroid?.showBlockMenu?.(
      state.id,
      state.type,
      !!state.collapsed,
      !!state.enabled
    );
  }

  document.addEventListener('pointerdown', event => {
    if (event.button != null && event.button !== 0) return;
    const block = findBlockFromTarget(event.target);
    if (!block) return;

    clearPendingPress();
    const startX = event.clientX;
    const startY = event.clientY;
    const blockId = block.id;

    const timer = setTimeout(() => {
      const current = workspace.getBlockById(blockId);
      if (current) openNativeBlockMenu(current);
      pendingPress = null;
    }, LONG_PRESS_MS);

    pendingPress = { timer, startX, startY, pointerId: event.pointerId };
  }, true);

  document.addEventListener('pointermove', event => {
    if (!pendingPress || event.pointerId !== pendingPress.pointerId) return;
    const dx = event.clientX - pendingPress.startX;
    const dy = event.clientY - pendingPress.startY;
    if (Math.hypot(dx, dy) > MOVE_TOLERANCE) clearPendingPress();
  }, true);

  document.addEventListener('pointerup', clearPendingPress, true);
  document.addEventListener('pointercancel', clearPendingPress, true);
  document.addEventListener('contextmenu', event => {
    if (findBlockFromTarget(event.target)) event.preventDefault();
  }, true);

  window.Elekto.blockAction = function blockAction(blockId, action) {
    Blockly.Events.setGroup(true);
    try {
      const result = window.ElektoBlockActions.perform(workspace, blockId, action);
      if (result?.blockId) workspace.getBlockById(result.blockId)?.select?.();
      Blockly.svgResize(workspace);
      workspace.resizeContents?.();
      return result || null;
    } finally {
      Blockly.Events.setGroup(false);
    }
  };
})();
