(() => {
  'use strict';

  if (typeof Blockly === 'undefined' || !window.ElektoBlueprintEngine || !window.Elekto) return;

  const workspace = Blockly.getMainWorkspace?.();
  if (!workspace) return;

  const MOVE_TOLERANCE = 14;
  let selection = null;
  let pointerDown = null;

  function active() {
    return !!selection;
  }

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

  function clearHighlight() {
    document
      .querySelectorAll('.elekto-blueprint-selected')
      .forEach(node => node.classList.remove('elekto-blueprint-selected'));
  }

  function refreshHighlight() {
    clearHighlight();
    if (!selection) return;
    for (const blockId of selection.getSelectedBlockIds()) {
      const block = workspace.getBlockById(blockId);
      block?.getSvgRoot?.()?.classList.add('elekto-blueprint-selected');
    }
  }

  function reportSelection() {
    const count = selection?.getSelectedRootIds?.().length || 0;
    window.ElektoAndroid?.setBlueprintSelection?.(!!selection, count);
  }

  function finishSelection() {
    pointerDown = null;
    clearHighlight();
    selection = null;
    window.ElektoAndroid?.setBlueprintSelection?.(false, 0);
  }

  function intercept(event) {
    event.preventDefault();
    event.stopPropagation();
    event.stopImmediatePropagation();
  }

  document.addEventListener('pointerdown', event => {
    if (!active()) return;
    if (event.button != null && event.button !== 0) return;
    const block = findBlockFromTarget(event.target);
    if (!block) return;

    workspace.cancelCurrentGesture?.();
    pointerDown = {
      pointerId: event.pointerId,
      blockId: block.id,
      x: event.clientX,
      y: event.clientY,
      moved: false
    };
    intercept(event);
  }, true);

  document.addEventListener('pointermove', event => {
    if (!active() || !pointerDown || event.pointerId !== pointerDown.pointerId) return;
    const dx = event.clientX - pointerDown.x;
    const dy = event.clientY - pointerDown.y;
    if (Math.hypot(dx, dy) > MOVE_TOLERANCE) pointerDown.moved = true;
    intercept(event);
  }, true);

  document.addEventListener('pointerup', event => {
    if (!active() || !pointerDown || event.pointerId !== pointerDown.pointerId) return;
    const pending = pointerDown;
    pointerDown = null;
    intercept(event);

    if (!pending.moved) {
      selection.toggleByBlockId(pending.blockId);
      refreshHighlight();
      reportSelection();
    }
  }, true);

  document.addEventListener('pointercancel', event => {
    if (!active() || !pointerDown || event.pointerId !== pointerDown.pointerId) return;
    pointerDown = null;
    intercept(event);
  }, true);

  window.Elekto.startBlueprintSelection = function startBlueprintSelection(blockId) {
    const next = window.ElektoBlueprintEngine.createSelection(workspace, blockId);
    if (next.getSelectedRootIds().length === 0) return false;
    workspace.cancelCurrentGesture?.();
    selection = next;
    pointerDown = null;
    refreshHighlight();
    reportSelection();
    return true;
  };

  window.Elekto.requestBlueprintPayload = function requestBlueprintPayload() {
    if (!selection) return false;
    const payload = selection.serialize();
    if (payload.groupCount < 1) return false;
    window.ElektoAndroid?.blueprintPayloadReady?.(JSON.stringify(payload));
    return true;
  };

  window.Elekto.cancelBlueprintSelection = function cancelBlueprintSelection() {
    if (!selection) return false;
    finishSelection();
    return true;
  };

  window.Elekto.commitBlueprintSelection = function commitBlueprintSelection() {
    if (!selection) return false;
    finishSelection();
    return true;
  };

  window.Elekto.isBlueprintSelectionActive = active;

  window.Elekto.insertBlueprint = function insertBlueprint(payloadJson) {
    const payload = typeof payloadJson === 'string' ? JSON.parse(payloadJson) : payloadJson;
    const metrics = workspace.getMetrics();
    const scale = workspace.scale || 1;
    const anchorX = ((metrics.viewLeft || 0) + (metrics.viewWidth || 320) * 0.32) / scale;
    const anchorY = ((metrics.viewTop || 0) + (metrics.viewHeight || 520) * 0.24) / scale;

    Blockly.Events.setGroup(true);
    try {
      const result = window.ElektoBlueprintEngine.insert(workspace, payload, anchorX, anchorY);
      if (result.rootIds.length) workspace.getBlockById(result.rootIds[0])?.select?.();
      Blockly.svgResize(workspace);
      workspace.resizeContents?.();
      return result;
    } catch (error) {
      console.error('Blueprint insertion failed', error);
      throw error;
    } finally {
      Blockly.Events.setGroup(false);
    }
  };
})();
