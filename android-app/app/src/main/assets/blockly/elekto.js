(() => {
  'use strict';
  window.__ELEKTO_STARTED = true;

  function showFatal(error) {
    const box = document.createElement('div');
    box.style.cssText = 'position:fixed;left:12px;right:12px;top:70px;z-index:99999;background:#fff0f0;color:#8b1d1d;border:2px solid #e57373;border-radius:12px;padding:12px;font:14px system-ui;white-space:pre-wrap;box-shadow:0 4px 18px rgba(0,0,0,.16)';
    box.textContent = 'Blockly konnte nicht gestartet werden:\n' + (error?.stack || error?.message || String(error));
    document.body.appendChild(box);
  }

  window.addEventListener('error', e => showFatal(e.error || e.message));
  window.addEventListener('unhandledrejection', e => showFatal(e.reason));

  try {
  if (typeof Blockly === 'undefined') {
    document.body.innerHTML = '<p style="padding:20px">Blockly konnte nicht geladen werden.</p>';
    return;
  }

  const lightComponents = {
      workspaceBackgroundColour: '#FAFAFD',
      toolboxBackgroundColour: '#FFFFFF',
      toolboxForegroundColour: '#33333D',
      flyoutBackgroundColour: '#F0F1F6',
      flyoutForegroundColour: '#30303A',
      flyoutOpacity: 1,
      scrollbarColour: '#AEB1BC',
      insertionMarkerColour: '#55A9FF',
      insertionMarkerOpacity: 0.45,
      cursorColour: '#2E87E8'
  };
  const darkComponents = {
      workspaceBackgroundColour: '#121217',
      toolboxBackgroundColour: '#1B1B22',
      toolboxForegroundColour: '#EEEEF5',
      flyoutBackgroundColour: '#202028',
      flyoutForegroundColour: '#F1F1F6',
      flyoutOpacity: 1,
      scrollbarColour: '#565663',
      insertionMarkerColour: '#65B5FF',
      insertionMarkerOpacity: 0.52,
      cursorColour: '#65B5FF'
  };

  function makeTheme(name, components) {
    return Blockly.Theme.defineTheme(name, {

    base: Blockly.Themes.Zelos || Blockly.Themes.Classic,
    blockStyles: {
      time_blocks: { colourPrimary: '#6C55C7', colourSecondary: '#5A46AA', colourTertiary: '#48368D' },
      io_blocks: { colourPrimary: '#16865C', colourSecondary: '#11714D', colourTertiary: '#0D5B3E' },
      sensor_blocks: { colourPrimary: '#2D6BC4', colourSecondary: '#2459A4', colourTertiary: '#1C4783' },
      logic_blocks: { colourPrimary: '#C25235', colourSecondary: '#A4442C', colourTertiary: '#853624' },
      loop_blocks: { colourPrimary: '#D89A00', colourSecondary: '#B57F00', colourTertiary: '#916600' }
    },
    categoryStyles: {
      basics_category: { colour: '#6C55C7' },
      io_category: { colour: '#16865C' },
      values_category: { colour: '#2D6BC4' },
      logic_category: { colour: '#C25235' },
      loops_category: { colour: '#D89A00' }
    },
    componentStyles: components,
    fontStyle: {
      family: 'system-ui, sans-serif',
      weight: '600',
      size: 12
    },
    startHats: true
    });
  }

  const elektoLightTheme = makeTheme('elekto-light', lightComponents);
  const elektoDarkTheme = makeTheme('elekto-dark', darkComponents);
  let currentThemeName = 'light';

  Blockly.defineBlocksWithJsonArray([
    {
      type: 'elekto_delay',
      message0: 'warte %1 ms',
      args0: [{ type: 'input_value', name: 'TIME', check: 'Number' }],
      previousStatement: null,
      nextStatement: null,
      style: 'time_blocks',
      tooltip: 'Pausiert das Programm für die angegebene Zeit.'
    },
    {
      type: 'elekto_digital_write',
      message0: 'setze Pin %1 auf %2',
      args0: [
        { type: 'field_dropdown', name: 'PIN', options: [['D13','13'],['D12','12'],['D11','11'],['D10','10'],['D9','9'],['D8','8'],['D7','7'],['D6','6'],['D5','5'],['D4','4'],['D3','3'],['D2','2']] },
        { type: 'field_dropdown', name: 'STATE', options: [['HIGH','HIGH'],['LOW','LOW']] }
      ],
      previousStatement: null,
      nextStatement: null,
      style: 'io_blocks',
      tooltip: 'Schaltet einen digitalen Ausgang ein oder aus.'
    },
    {
      type: 'elekto_analog_read',
      message0: 'Analogwert %1',
      args0: [{ type: 'field_dropdown', name: 'PIN', options: [['A0','A0'],['A1','A1'],['A2','A2'],['A3','A3'],['A4','A4'],['A5','A5']] }],
      output: 'Number',
      style: 'sensor_blocks',
      tooltip: 'Liest einen analogen Eingang und liefert eine Zahl.'
    },
    {
      type: 'elekto_repeat',
      message0: 'wiederhole %1 mal',
      args0: [{ type: 'input_value', name: 'COUNT', check: 'Number' }],
      message1: '%1',
      args1: [{ type: 'input_statement', name: 'DO' }],
      previousStatement: null,
      nextStatement: null,
      style: 'loop_blocks',
      tooltip: 'Führt die Befehle im Inneren mehrfach aus.'
    },
    {
      type: 'elekto_if',
      message0: 'wenn %1',
      args0: [{ type: 'input_value', name: 'COND', check: 'Boolean' }],
      message1: '%1',
      args1: [{ type: 'input_statement', name: 'DO' }],
      previousStatement: null,
      nextStatement: null,
      style: 'logic_blocks',
      tooltip: 'Führt die Befehle nur aus, wenn die Bedingung wahr ist.'
    }
  ]);

  const toolbox = {
    kind: 'categoryToolbox',
    contents: [
      {
        kind: 'category', name: 'Grundlagen', categorystyle: 'basics_category',
        contents: [
          {
            kind: 'block', type: 'elekto_delay',
            inputs: { TIME: { shadow: { type: 'math_number', fields: { NUM: 1000 } } } }
          }
        ]
      },
      {
        kind: 'category', name: 'Ein-/Ausgänge', categorystyle: 'io_category',
        contents: [
          { kind: 'block', type: 'elekto_digital_write' }
        ]
      },
      {
        kind: 'category', name: 'Werte & Sensoren', categorystyle: 'values_category',
        contents: [
          { kind: 'block', type: 'math_number', fields: { NUM: 1000 } },
          { kind: 'block', type: 'elekto_analog_read' }
        ]
      },
      {
        kind: 'category', name: 'Logik', categorystyle: 'logic_category',
        contents: [
          {
            kind: 'block', type: 'logic_compare',
            inputs: {
              A: { shadow: { type: 'math_number', fields: { NUM: 0 } } },
              B: { shadow: { type: 'math_number', fields: { NUM: 500 } } }
            }
          },
          {
            kind: 'block', type: 'elekto_if',
            inputs: {
              COND: { shadow: { type: 'logic_boolean', fields: { BOOL: 'TRUE' } } }
            }
          }
        ]
      },
      {
        kind: 'category', name: 'Schleifen', categorystyle: 'loops_category',
        contents: [
          {
            kind: 'block', type: 'elekto_repeat',
            inputs: { COUNT: { shadow: { type: 'math_number', fields: { NUM: 10 } } } }
          }
        ]
      }
    ]
  };


  const workspaceHost = document.getElementById('workspace');
  function sizeWorkspaceHost() {
    const w = Math.max(1, window.innerWidth || document.documentElement.clientWidth || 360);
    const h = Math.max(1, window.innerHeight || document.documentElement.clientHeight || 640);
    workspaceHost.style.width = w + 'px';
    workspaceHost.style.height = h + 'px';
  }
  sizeWorkspaceHost();

  const workspace = Blockly.inject('workspace', {
    toolbox: null,
    theme: elektoLightTheme,
    renderer: 'zelos',
    trashcan: true,
    sounds: false,
    move: { scrollbars: false, drag: true, wheel: false },
    zoom: {
      controls: false,
      wheel: false,
      startScale: 0.85,
      maxScale: 1.6,
      minScale: 0.45,
      scaleSpeed: 1.15,
      pinch: true
    },
    grid: { spacing: 24, length: 2, colour: '#D9DAE2', snap: false }
  });

  Blockly.svgResize(workspace);

  function valueCode(block, inputName, fallback) {
    const child = block.getInputTargetBlock(inputName);
    return child ? expressionCode(child) : fallback;
  }

  function expressionCode(block) {
    if (!block || block.isInsertionMarker?.()) return '0';
    switch (block.type) {
      case 'math_number':
        return String(block.getFieldValue('NUM') ?? 0);
      case 'elekto_analog_read':
        return 'analogRead(' + block.getFieldValue('PIN') + ')';
      case 'logic_compare': {
        const ops = { EQ: '==', NEQ: '!=', LT: '<', LTE: '<=', GT: '>', GTE: '>=' };
        const a = valueCode(block, 'A', '0');
        const b = valueCode(block, 'B', '0');
        return '(' + a + ' ' + (ops[block.getFieldValue('OP')] || '==') + ' ' + b + ')';
      }
      case 'logic_boolean':
        return block.getFieldValue('BOOL') === 'TRUE' ? 'true' : 'false';
      default:
        return '0';
    }
  }

  function statementChain(first, indent) {
    let out = '';
    let block = first;
    const pad = '  '.repeat(indent);
    while (block) {
      if (!block.isEnabled || block.isEnabled()) {
        switch (block.type) {
          case 'elekto_delay':
            out += pad + 'delay(' + valueCode(block, 'TIME', '1000') + ');\n';
            break;
          case 'elekto_digital_write':
            out += pad + 'digitalWrite(' + block.getFieldValue('PIN') + ', ' + block.getFieldValue('STATE') + ');\n';
            break;
          case 'elekto_repeat': {
            const count = valueCode(block, 'COUNT', '10');
            out += pad + 'for (int i = 0; i < ' + count + '; i++) {\n';
            out += statementChain(block.getInputTargetBlock('DO'), indent + 1);
            out += pad + '}\n';
            break;
          }
          case 'elekto_if': {
            const cond = valueCode(block, 'COND', 'false');
            out += pad + 'if (' + cond + ') {\n';
            out += statementChain(block.getInputTargetBlock('DO'), indent + 1);
            out += pad + '}\n';
            break;
          }
        }
      }
      block = block.getNextBlock();
    }
    return out;
  }

  function collectOutputs() {
    const pins = new Set();
    for (const block of workspace.getAllBlocks(false)) {
      if (block.type === 'elekto_digital_write') pins.add(block.getFieldValue('PIN'));
    }
    return [...pins];
  }

  function generateArduino() {
    const pins = collectOutputs();
    const setup = pins.map(p => '  pinMode(' + p + ', OUTPUT);').join('\n');
    const tops = workspace.getTopBlocks(true).filter(b => b.previousConnection || b.nextConnection);
    let body = '';
    for (const top of tops) body += statementChain(top, 1);

    return [
      '// Elekto Blocks – Blockly POC',
      '// Zielplattform im POC: Arduino UNO R4 WiFi',
      '',
      'void setup() {',
      setup || '  // keine Ausgänge konfiguriert',
      '}',
      '',
      'void loop() {',
      body || '  // Ziehe Blöcke auf die Arbeitsfläche.',
      '}',
      ''
    ].join('\n');
  }

  let saveTimer = null;
  workspace.addChangeListener(event => {
    if (event.isUiEvent) return;
    clearTimeout(saveTimer);
    saveTimer = setTimeout(() => {
      try {
        const state = Blockly.serialization.workspaces.save(workspace);
        localStorage.setItem('elekto-blockly-poc-workspace', JSON.stringify(state));
      } catch (_) {}
    }, 180);
  });

  function restore() {
    try {
      const raw = localStorage.getItem('elekto-blockly-poc-workspace');
      if (!raw) return false;
      Blockly.serialization.workspaces.load(JSON.parse(raw), workspace);
      return workspace.getAllBlocks(false).length > 0;
    } catch (_) {
      return false;
    }
  }

  function addNumber(value) {
    const n = workspace.newBlock('math_number');
    n.setFieldValue(String(value), 'NUM');
    n.initSvg();
    n.render();
    return n;
  }

  function loadDemo() {
    Blockly.Events.disable();
    try {
      workspace.clear();
      const hi = workspace.newBlock('elekto_digital_write');
      hi.setFieldValue('13', 'PIN');
      hi.setFieldValue('HIGH', 'STATE');
      hi.initSvg(); hi.render();

      const wait1 = workspace.newBlock('elekto_delay');
      wait1.initSvg(); wait1.render();
      addNumber(1000).outputConnection.connect(wait1.getInput('TIME').connection);

      const lo = workspace.newBlock('elekto_digital_write');
      lo.setFieldValue('13', 'PIN');
      lo.setFieldValue('LOW', 'STATE');
      lo.initSvg(); lo.render();

      const wait2 = workspace.newBlock('elekto_delay');
      wait2.initSvg(); wait2.render();
      addNumber(1000).outputConnection.connect(wait2.getInput('TIME').connection);

      hi.nextConnection.connect(wait1.previousConnection);
      wait1.nextConnection.connect(lo.previousConnection);
      lo.nextConnection.connect(wait2.previousConnection);
      hi.moveBy(48, 64);
    } finally {
      Blockly.Events.enable();
    }
    Blockly.svgResize(workspace);
    const state = Blockly.serialization.workspaces.save(workspace);
    localStorage.setItem('elekto-blockly-poc-workspace', JSON.stringify(state));
  }

  function connectShadowNumber(block, inputName, value) {
    const shadow = workspace.newBlock('math_number');
    shadow.setShadow(true);
    shadow.setFieldValue(String(value), 'NUM');
    shadow.initSvg();
    shadow.render();
    const input = block.getInput(inputName);
    if (input?.connection && shadow.outputConnection) {
      input.connection.connect(shadow.outputConnection);
    }
  }

  function connectShadowBoolean(block, inputName, value) {
    const shadow = workspace.newBlock('logic_boolean');
    shadow.setShadow(true);
    shadow.setFieldValue(value ? 'TRUE' : 'FALSE', 'BOOL');
    shadow.initSvg();
    shadow.render();
    const input = block.getInput(inputName);
    if (input?.connection && shadow.outputConnection) {
      input.connection.connect(shadow.outputConnection);
    }
  }

  function placeNewBlock(block) {
    if (!block.getSvgRoot?.()) {
      block.initSvg();
      block.render();
    }

    const metrics = workspace.getMetrics();
    const scale = workspace.scale || 1;
    const x = ((metrics.viewLeft || 0) + (metrics.viewWidth || 320) * 0.42) / scale;
    const y = ((metrics.viewTop || 0) + (metrics.viewHeight || 520) * 0.28) / scale;
    block.moveBy(Math.max(24, x), Math.max(24, y));
    block.select?.();
    Blockly.svgResize(workspace);
    return block;
  }

  function createCatalogBlock(id) {
    Blockly.Events.setGroup(true);
    try {
      let block;
      switch (id) {
        case 'delay':
          block = workspace.newBlock('elekto_delay');
          block.initSvg(); block.render();
          connectShadowNumber(block, 'TIME', 1000);
          break;
        case 'digital_write':
          block = workspace.newBlock('elekto_digital_write');
          break;
        case 'analog_read':
          block = workspace.newBlock('elekto_analog_read');
          break;
        case 'number':
          block = workspace.newBlock('math_number');
          block.setFieldValue('1000', 'NUM');
          break;
        case 'compare':
          block = workspace.newBlock('logic_compare');
          block.initSvg(); block.render();
          connectShadowNumber(block, 'A', 0);
          connectShadowNumber(block, 'B', 500);
          break;
        case 'if':
          block = workspace.newBlock('elekto_if');
          block.initSvg(); block.render();
          connectShadowBoolean(block, 'COND', true);
          break;
        case 'repeat':
          block = workspace.newBlock('elekto_repeat');
          block.initSvg(); block.render();
          connectShadowNumber(block, 'COUNT', 10);
          break;
        default:
          throw new Error('Unbekannter Elekto-Block: ' + id);
      }
      return placeNewBlock(block);
    } finally {
      Blockly.Events.setGroup(false);
    }
  }

  function setDraggedVisual(blockId, active) {
    const block = workspace.getBlockById(blockId);
    const root = block?.getSvgRoot?.();
    if (!root) return;
    root.style.opacity = active ? '0.58' : '';
    root.style.filter = active ? 'drop-shadow(0 3px 7px rgba(35,120,230,.28))' : '';
  }

  workspace.addChangeListener(event => {
    if (event.type === Blockly.Events.BLOCK_DRAG && event.blockId) {
      setDraggedVisual(event.blockId, !!event.isStart);
    }
  });

  function applyTheme(name) {
    currentThemeName = name === 'dark' ? 'dark' : 'light';
    workspace.setTheme(currentThemeName === 'dark' ? elektoDarkTheme : elektoLightTheme);
    document.body.dataset.theme = currentThemeName;
  }

  window.Elekto = {
    setTheme(name) {
      applyTheme(name);
    },
    zoomIn() {
      workspace.zoomCenter(1);
    },
    zoomOut() {
      workspace.zoomCenter(-1);
    },
    resetZoom() {
      const metrics = workspace.getMetrics();
      if (workspace.setScale) workspace.setScale(0.85);
      else workspace.zoomToFit();
      workspace.scrollCenter?.();
    },
    addBlock(id) {
      createCatalogBlock(id);
    },
    undo() {
      workspace.undo(false);
    },
    redo() {
      workspace.undo(true);
    },
    requestCode() {
      const code = generateArduino();
      if (window.ElektoAndroid?.showCode) {
        window.ElektoAndroid.showCode(code);
      }
      return code;
    },
    loadDemo,
    clear() {
      workspace.clear();
      localStorage.removeItem('elekto-blockly-poc-workspace');
    }
  };

  if (!restore()) loadDemo();
  window.__ELEKTO_READY = true;
  window.addEventListener('resize', () => {
    sizeWorkspaceHost();
    Blockly.svgResize(workspace);
    workspace.resizeContents?.();
  });
  } catch (error) {
    console.error(error);
    showFatal(error);
  }
})();
