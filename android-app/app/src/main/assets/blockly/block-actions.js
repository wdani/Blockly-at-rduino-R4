(function (root, factory) {
  'use strict';
  const api = factory(root.Blockly);
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  root.ElektoBlockActions = api;
})(typeof globalThis !== 'undefined' ? globalThis : window, function (Blockly) {
  'use strict';

  if (!Blockly) throw new Error('Blockly ist für ElektoBlockActions erforderlich.');

  function requireBlock(workspace, blockId) {
    const block = workspace?.getBlockById?.(blockId);
    if (!block) throw new Error('Block nicht gefunden: ' + blockId);
    return block;
  }

  function getState(workspace, blockId) {
    const block = requireBlock(workspace, blockId);
    return {
      id: block.id,
      type: block.type,
      collapsed: typeof block.isCollapsed === 'function' ? !!block.isCollapsed() : false,
      enabled: typeof block.isEnabled === 'function' ? !!block.isEnabled() : true
    };
  }

  function duplicateBlock(workspace, block) {
    const before = new Set(workspace.getAllBlocks(false).map(item => item.id));
    const state = Blockly.serialization.blocks.save(block);
    Blockly.serialization.blocks.append(state, workspace, { recordUndo: true });

    const created = workspace.getAllBlocks(false).filter(item => !before.has(item.id));
    if (!created.length) throw new Error('Block konnte nicht dupliziert werden.');

    const createdIds = new Set(created.map(item => item.id));
    const duplicateRoot = created.find(item => {
      const parent = item.getParent?.();
      return !parent || !createdIds.has(parent.id);
    }) || created[0];

    if (
      typeof block.getRelativeToSurfaceXY === 'function' &&
      typeof duplicateRoot.getRelativeToSurfaceXY === 'function' &&
      typeof duplicateRoot.moveBy === 'function'
    ) {
      const original = block.getRelativeToSurfaceXY();
      const copy = duplicateRoot.getRelativeToSurfaceXY();
      duplicateRoot.moveBy((original.x + 28) - copy.x, (original.y + 28) - copy.y);
    }

    duplicateRoot.select?.();
    return duplicateRoot;
  }

  function toggleEnabled(block) {
    const nextEnabled = !(typeof block.isEnabled === 'function' ? block.isEnabled() : true);
    if (typeof block.setEnabled === 'function') {
      block.setEnabled(nextEnabled);
    } else if (typeof block.setDisabledReason === 'function') {
      block.setDisabledReason(!nextEnabled, 'ELEKTO_USER');
    } else {
      throw new Error('Dieser Blockly-Build unterstützt Aktivieren/Deaktivieren nicht.');
    }
  }

  function perform(workspace, blockId, action) {
    const block = requireBlock(workspace, blockId);

    switch (action) {
      case 'duplicate': {
        const duplicate = duplicateBlock(workspace, block);
        return { blockId: duplicate.id };
      }
      case 'toggleCollapsed':
        if (typeof block.setCollapsed !== 'function' || typeof block.isCollapsed !== 'function') {
          throw new Error('Dieser Block kann nicht ein- oder ausgeklappt werden.');
        }
        block.setCollapsed(!block.isCollapsed());
        return getState(workspace, blockId);
      case 'toggleEnabled':
        toggleEnabled(block);
        return getState(workspace, blockId);
      case 'delete':
        block.dispose(false);
        return { deleted: true };
      default:
        throw new Error('Unbekannte Block-Aktion: ' + action);
    }
  }

  return { getState, perform };
});
