(function (root, factory) {
  'use strict';
  const api = factory(root.Blockly);
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  root.ElektoBlueprintEngine = api;
})(typeof globalThis !== 'undefined' ? globalThis : window, function (Blockly) {
  'use strict';

  if (!Blockly) throw new Error('Blockly ist für ElektoBlueprintEngine erforderlich.');

  const SCHEMA_VERSION = 1;

  function deepClone(value) {
    return JSON.parse(JSON.stringify(value));
  }

  function stripIds(value) {
    if (Array.isArray(value)) return value.map(stripIds);
    if (!value || typeof value !== 'object') return value;
    const out = {};
    for (const [key, child] of Object.entries(value)) {
      if (key !== 'id') out[key] = stripIds(child);
    }
    return out;
  }

  function resolveRoot(workspace, blockId) {
    let block = workspace?.getBlockById?.(blockId) || null;
    if (!block) return null;
    if (block.isShadow?.()) block = block.getParent?.() || block;
    return block.getRootBlock?.() || block;
  }

  function countDescendants(root) {
    if (!root) return 0;
    if (typeof root.getDescendants === 'function') return root.getDescendants(false).length;
    return 1;
  }

  function rootPoint(root) {
    if (typeof root.getRelativeToSurfaceXY === 'function') {
      const point = root.getRelativeToSurfaceXY();
      return { x: Number(point.x) || 0, y: Number(point.y) || 0 };
    }
    return { x: 0, y: 0 };
  }

  function moveRootTo(root, x, y) {
    if (typeof root.moveBy !== 'function') return;
    const current = rootPoint(root);
    root.moveBy(x - current.x, y - current.y);
  }

  function createSelection(workspace, initialBlockId) {
    const selected = new Set();
    const first = resolveRoot(workspace, initialBlockId);
    if (first) selected.add(first.id);

    function prune() {
      for (const id of [...selected]) {
        const block = workspace?.getBlockById?.(id) || null;
        if (!block) {
          selected.delete(id);
          continue;
        }
        const root = block.getRootBlock?.() || block;
        if (!root || root.id !== id) selected.delete(id);
      }
    }

    function selectedRoots() {
      prune();
      return [...selected]
        .map(id => workspace.getBlockById(id))
        .filter(Boolean);
    }

    return {
      toggleByBlockId(blockId) {
        prune();
        const groupRoot = resolveRoot(workspace, blockId);
        if (!groupRoot) {
          return { selectedRootIds: [...selected], groupCount: selected.size };
        }
        if (selected.has(groupRoot.id)) selected.delete(groupRoot.id);
        else selected.add(groupRoot.id);
        return { selectedRootIds: [...selected], groupCount: selected.size };
      },

      getSelectedRootIds() {
        prune();
        return [...selected];
      },

      getSelectedBlockIds() {
        const ids = new Set();
        for (const root of selectedRoots()) {
          const descendants = typeof root.getDescendants === 'function'
            ? root.getDescendants(false)
            : [root];
          for (const block of descendants) ids.add(block.id);
        }
        return [...ids];
      },

      serialize() {
        const roots = selectedRoots();
        if (!roots.length) {
          return {
            schemaVersion: SCHEMA_VERSION,
            groupCount: 0,
            blockCount: 0,
            groups: []
          };
        }

        const points = roots.map(rootPoint);
        const minX = Math.min(...points.map(point => point.x));
        const minY = Math.min(...points.map(point => point.y));
        let blockCount = 0;

        const groups = roots.map((root, index) => {
          const rawState = Blockly.serialization.blocks.save(root);
          const blockState = stripIds(deepClone(rawState));
          if (blockState && typeof blockState === 'object') {
            delete blockState.x;
            delete blockState.y;
          }
          blockCount += countDescendants(root);
          return {
            x: points[index].x - minX,
            y: points[index].y - minY,
            blockState
          };
        });

        return {
          schemaVersion: SCHEMA_VERSION,
          groupCount: groups.length,
          blockCount,
          groups
        };
      }
    };
  }

  function findNewRoot(workspace, beforeGroupIds) {
    const created = workspace.getAllBlocks(false).filter(block => !beforeGroupIds.has(block.id));
    if (!created.length) throw new Error('Blueprint-Gruppe konnte nicht eingefügt werden.');
    const createdIds = new Set(created.map(block => block.id));
    return created.find(block => {
      const parent = block.getParent?.() || null;
      return !parent || !createdIds.has(parent.id);
    }) || created[0];
  }

  function rollbackNewBlocks(workspace, beforeIds) {
    const created = workspace.getAllBlocks(false).filter(block => !beforeIds.has(block.id));
    const createdIds = new Set(created.map(block => block.id));
    const roots = created.filter(block => {
      const parent = block.getParent?.() || null;
      return !parent || !createdIds.has(parent.id);
    });
    for (const root of roots) {
      if (workspace.getBlockById(root.id)) root.dispose(false);
    }
  }

  function insert(workspace, payload, anchorX, anchorY) {
    if (!workspace) throw new Error('Workspace fehlt.');
    if (!payload || payload.schemaVersion !== SCHEMA_VERSION || !Array.isArray(payload.groups)) {
      throw new Error('Ungültiger Blueprint-Payload.');
    }

    const beforeIds = new Set(workspace.getAllBlocks(false).map(block => block.id));
    const rootIds = [];

    try {
      for (const group of payload.groups) {
        if (!group || !group.blockState || typeof group.blockState !== 'object') {
          throw new Error('Ungültige Blueprint-Gruppe.');
        }
        const beforeGroupIds = new Set(workspace.getAllBlocks(false).map(block => block.id));
        Blockly.serialization.blocks.append(
          deepClone(group.blockState),
          workspace,
          { recordUndo: true }
        );
        const root = findNewRoot(workspace, beforeGroupIds);
        moveRootTo(
          root,
          Number(anchorX || 0) + Number(group.x || 0),
          Number(anchorY || 0) + Number(group.y || 0)
        );
        rootIds.push(root.id);
      }
      return { rootIds };
    } catch (error) {
      rollbackNewBlocks(workspace, beforeIds);
      throw error;
    }
  }

  return {
    SCHEMA_VERSION,
    resolveRoot,
    createSelection,
    insert
  };
});
