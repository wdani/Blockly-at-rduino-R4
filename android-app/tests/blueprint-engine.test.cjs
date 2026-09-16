const test = require('node:test');
const assert = require('node:assert/strict');
const Blockly = require('../blockly-package/node_modules/blockly');

globalThis.Blockly = Blockly;

let blueprints = null;
try {
  blueprints = require('../app/src/main/assets/blockly/blueprint-engine.js');
} catch (_) {}

Blockly.defineBlocksWithJsonArray([
  {
    type: 'bp_stmt',
    message0: 'stmt',
    previousStatement: null,
    nextStatement: null,
    colour: 210
  },
  {
    type: 'bp_container',
    message0: 'container',
    message1: '%1',
    args1: [{ type: 'input_statement', name: 'DO' }],
    previousStatement: null,
    nextStatement: null,
    colour: 160
  },
  {
    type: 'bp_value_owner',
    message0: 'owner %1',
    args0: [{ type: 'input_value', name: 'VALUE', check: 'Number' }],
    previousStatement: null,
    nextStatement: null,
    colour: 120
  }
]);

function number(workspace, value, shadow = false) {
  const block = workspace.newBlock('math_number');
  block.setFieldValue(String(value), 'NUM');
  block.setShadow(shadow);
  return block;
}

function makeConnectedTree(workspace) {
  const root = workspace.newBlock('bp_container');
  const child = workspace.newBlock('bp_value_owner');
  const tail = workspace.newBlock('bp_stmt');
  const shadow = number(workspace, 42, true);
  root.getInput('DO').connection.connect(child.previousConnection);
  child.getInput('VALUE').connection.connect(shadow.outputConnection);
  child.nextConnection.connect(tail.previousConnection);
  return { root, child, tail, shadow };
}

test('module exists', () => {
  assert.ok(blueprints, 'blueprint-engine.js muss implementiert sein');
});

test('any block in a connected tree resolves to the same root group', () => {
  assert.ok(blueprints);
  const ws = new Blockly.Workspace();
  const { root, child, tail, shadow } = makeConnectedTree(ws);
  for (const block of [root, child, tail, shadow]) {
    assert.equal(blueprints.resolveRoot(ws, block.id).id, root.id);
  }
  ws.dispose();
});

test('selection starts with the initial connected group and toggles a second group', () => {
  assert.ok(blueprints);
  const ws = new Blockly.Workspace();
  const first = makeConnectedTree(ws);
  const second = ws.newBlock('bp_stmt');
  const selection = blueprints.createSelection(ws, first.child.id);

  assert.deepEqual(selection.getSelectedRootIds(), [first.root.id]);
  selection.toggleByBlockId(second.id);
  assert.deepEqual(new Set(selection.getSelectedRootIds()), new Set([first.root.id, second.id]));
  selection.toggleByBlockId(first.tail.id);
  assert.deepEqual(selection.getSelectedRootIds(), [second.id]);
  ws.dispose();
});

test('selectedBlockIds includes the whole connected group including shadows', () => {
  assert.ok(blueprints);
  const ws = new Blockly.Workspace();
  const tree = makeConnectedTree(ws);
  const selection = blueprints.createSelection(ws, tree.tail.id);
  const ids = new Set(selection.getSelectedBlockIds());
  for (const block of [tree.root, tree.child, tree.tail, tree.shadow]) {
    assert.equal(ids.has(block.id), true);
  }
  ws.dispose();
});

test('serialize keeps relative group layout, counts descendants, and strips ids', () => {
  assert.ok(blueprints);
  const ws = new Blockly.Workspace();
  const first = makeConnectedTree(ws);
  const second = ws.newBlock('bp_stmt');
  first.root.moveBy(100, 80);
  second.moveBy(340, 200);
  const originalIds = new Set(ws.getAllBlocks(false).map(block => block.id));

  const selection = blueprints.createSelection(ws, first.root.id);
  selection.toggleByBlockId(second.id);
  const payload = selection.serialize();

  assert.equal(payload.schemaVersion, 1);
  assert.equal(payload.groupCount, 2);
  assert.equal(payload.blockCount, 5);
  assert.deepEqual(payload.groups.map(group => [group.x, group.y]), [[0, 0], [240, 120]]);
  const raw = JSON.stringify(payload);
  for (const id of originalIds) assert.equal(raw.includes(id), false);
  ws.dispose();
});

test('insert recreates all groups with fresh ids and preserved relative positions', () => {
  assert.ok(blueprints);
  const source = new Blockly.Workspace();
  const first = makeConnectedTree(source);
  const second = source.newBlock('bp_stmt');
  first.root.moveBy(20, 30);
  second.moveBy(220, 130);
  const oldIds = new Set(source.getAllBlocks(false).map(block => block.id));
  const selection = blueprints.createSelection(source, first.root.id);
  selection.toggleByBlockId(second.id);
  const payload = selection.serialize();
  source.dispose();

  const target = new Blockly.Workspace();
  const result = blueprints.insert(target, payload, 500, 300);
  assert.equal(result.rootIds.length, 2);
  const roots = result.rootIds.map(id => target.getBlockById(id));
  const points = roots.map(block => block.getRelativeToSurfaceXY());
  assert.deepEqual(points.map(point => [point.x, point.y]), [[500, 300], [700, 400]]);
  for (const block of target.getAllBlocks(false)) assert.equal(oldIds.has(block.id), false);
  target.dispose();
});

test('insert rolls back all newly created blocks when a later group is invalid', () => {
  assert.ok(blueprints);
  const source = new Blockly.Workspace();
  const root = source.newBlock('bp_stmt');
  const payload = blueprints.createSelection(source, root.id).serialize();
  source.dispose();
  payload.groupCount = 2;
  payload.groups.push({ x: 120, y: 0, blockState: { type: 'bp_missing_type' } });

  const target = new Blockly.Workspace();
  assert.throws(() => blueprints.insert(target, payload, 10, 10));
  assert.equal(target.getAllBlocks(false).length, 0);
  target.dispose();
});

test('serialized payload remains usable after original blocks are deleted', () => {
  assert.ok(blueprints);
  const ws = new Blockly.Workspace();
  const tree = makeConnectedTree(ws);
  const originalIds = new Set(ws.getAllBlocks(false).map(block => block.id));
  const payload = blueprints.createSelection(ws, tree.root.id).serialize();
  tree.root.dispose(false);
  assert.equal(ws.getAllBlocks(false).length, 0);

  const result = blueprints.insert(ws, payload, 100, 100);
  assert.equal(result.rootIds.length, 1);
  assert.ok(ws.getAllBlocks(false).length > 0);
  for (const block of ws.getAllBlocks(false)) assert.equal(originalIds.has(block.id), false);
  ws.dispose();
});

test('removed selected roots are pruned instead of crashing serialization', () => {
  assert.ok(blueprints);
  const ws = new Blockly.Workspace();
  const root = ws.newBlock('bp_stmt');
  const selection = blueprints.createSelection(ws, root.id);
  root.dispose(false);

  assert.deepEqual(selection.getSelectedRootIds(), []);
  const payload = selection.serialize();
  assert.equal(payload.groupCount, 0);
  assert.equal(payload.blockCount, 0);
  assert.deepEqual(payload.groups, []);
  ws.dispose();
});

test('unknown initial block id creates an empty selection', () => {
  assert.ok(blueprints);
  const ws = new Blockly.Workspace();
  const selection = blueprints.createSelection(ws, 'missing-id');
  assert.deepEqual(selection.getSelectedRootIds(), []);
  assert.deepEqual(selection.getSelectedBlockIds(), []);
  ws.dispose();
});
