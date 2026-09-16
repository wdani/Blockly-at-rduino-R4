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
