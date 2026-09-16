(function (root, factory) {
  'use strict';
  const api = factory(root.Blockly);
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  root.ElektoBlueprintEngine = api;
})(typeof globalThis !== 'undefined' ? globalThis : window, function (Blockly) {
  'use strict';

  if (!Blockly) throw new Error('Blockly ist für ElektoBlueprintEngine erforderlich.');

  function resolveRoot(workspace, blockId) {
    let block = workspace?.getBlockById?.(blockId) || null;
    if (!block) return null;
    if (block.isShadow?.()) block = block.getParent?.() || block;
    return block.getRootBlock?.() || block;
  }

  function createSelection(workspace, initialBlockId) {
    const selected = new Set();
    const first = resolveRoot(workspace, initialBlockId);
    if (first) selected.add(first.id);

    return {
      toggleByBlockId(blockId) {
        const groupRoot = resolveRoot(workspace, blockId);
        if (!groupRoot) {
          return { selectedRootIds: [...selected], groupCount: selected.size };
        }
        if (selected.has(groupRoot.id)) selected.delete(groupRoot.id);
        else selected.add(groupRoot.id);
        return { selectedRootIds: [...selected], groupCount: selected.size };
      },
      getSelectedRootIds() {
        return [...selected];
      }
    };
  }

  return { resolveRoot, createSelection };
});
