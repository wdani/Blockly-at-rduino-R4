const test = require('node:test');
const assert = require('node:assert/strict');
const Blockly = require('../blockly-package/node_modules/blockly');

globalThis.Blockly = Blockly;

let actions = null;
try {
  actions = require('../app/src/main/assets/blockly/block-actions.js');
} catch (_) {
  // RED phase: the production module does not exist yet.
}

Blockly.defineBlocksWithJsonArray([
  {
    type: 'elekto_test_statement',
    message0: 'test',
    previousStatement: null,
    nextStatement: null,
    colour: 230
  },
  {
    type: 'elekto_test_value',
    message0: 'wert',
    output: 'Number',
    colour: 210
  }
]);

function makeStatementStack(workspace) {
  const a = workspace.newBlock('elekto_test_statement');
  const b = workspace.newBlock('elekto_test_statement');
  a.nextConnection.connect(b.previousConnection);
  return { a, b };
}

test('block action module exists', () => {
  assert.ok(actions, 'block-actions.js muss implementiert sein');
});

test('duplicate copies a connected statement stack', () => {
  assert.ok(actions, 'block-actions.js muss implementiert sein');
  const workspace = new Blockly.Workspace();
  const { a } = makeStatementStack(workspace);

  const result = actions.perform(workspace, a.id, 'duplicate');

  assert.ok(result?.blockId, 'Duplizieren soll die neue Root-Block-ID liefern');
  const duplicate = workspace.getBlockById(result.blockId);
  assert.ok(duplicate, 'Duplikat muss im Workspace existieren');
  assert.ok(duplicate.getNextBlock(), 'verbundener Folgeblock muss mit dupliziert werden');
  assert.equal(workspace.getTopBlocks(false).length, 2);
  workspace.dispose();
});

test('toggleCollapsed flips the collapsed state', () => {
  assert.ok(actions, 'block-actions.js muss implementiert sein');
  const workspace = new Blockly.Workspace();
  const block = workspace.newBlock('elekto_test_statement');

  actions.perform(workspace, block.id, 'toggleCollapsed');
  assert.equal(block.isCollapsed(), true);
  actions.perform(workspace, block.id, 'toggleCollapsed');
  assert.equal(block.isCollapsed(), false);
  workspace.dispose();
});

test('toggleEnabled flips enabled state', () => {
  assert.ok(actions, 'block-actions.js muss implementiert sein');
  const workspace = new Blockly.Workspace();
  const block = workspace.newBlock('elekto_test_statement');

  actions.perform(workspace, block.id, 'toggleEnabled');
  assert.equal(block.isEnabled(), false);
  actions.perform(workspace, block.id, 'toggleEnabled');
  assert.equal(block.isEnabled(), true);
  workspace.dispose();
});

test('delete removes the selected statement and its connected tail', () => {
  assert.ok(actions, 'block-actions.js muss implementiert sein');
  const workspace = new Blockly.Workspace();
  const { a, b } = makeStatementStack(workspace);

  actions.perform(workspace, a.id, 'delete');

  assert.equal(workspace.getBlockById(a.id), null);
  assert.equal(workspace.getBlockById(b.id), null);
  workspace.dispose();
});

test('state reports labels needed by the native menu', () => {
  assert.ok(actions, 'block-actions.js muss implementiert sein');
  const workspace = new Blockly.Workspace();
  const block = workspace.newBlock('elekto_test_statement');

  const state = actions.getState(workspace, block.id);

  assert.equal(state.id, block.id);
  assert.equal(state.type, 'elekto_test_statement');
  assert.equal(state.collapsed, false);
  assert.equal(state.enabled, true);
  workspace.dispose();
});
