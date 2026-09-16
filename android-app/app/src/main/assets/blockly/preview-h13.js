(() => {
  'use strict';

  if (typeof Blockly === 'undefined' || !window.Elekto) return;

  function nextFrame() {
    return new Promise(resolve => requestAnimationFrame(resolve));
  }

  async function settle(block, ws) {
    block.render();
    Blockly.svgResize(ws);
    try {
      const queued = Blockly.renderManagement?.finishQueuedRenders?.();
      if (queued && typeof queued.then === 'function') await queued;
    } catch (_) {}
    await nextFrame();
    await nextFrame();
    block.render();
    Blockly.svgResize(ws);
    await new Promise(resolve => setTimeout(resolve, 50));
  }

  function makeNumber(ws, value, shadow = true) {
    const b = ws.newBlock('math_number');
    b.initSvg();
    b.setFieldValue(String(value), 'NUM');
    b.setShadow(shadow);
    b.render();
    return b;
  }

  function makeBoolean(ws, value) {
    const b = ws.newBlock('logic_boolean');
    b.initSvg();
    b.setFieldValue(value ? 'TRUE' : 'FALSE', 'BOOL');
    b.setShadow(true);
    b.render();
    return b;
  }

  function makeBlock(ws, id) {
    let block;
    switch (id) {
      case 'delay':
        block = ws.newBlock('elekto_delay');
        block.initSvg();
        block.render();
        makeNumber(ws, 1000).outputConnection.connect(block.getInput('TIME').connection);
        break;
      case 'digital_write':
        block = ws.newBlock('elekto_digital_write');
        block.initSvg();
        block.setFieldValue('13', 'PIN');
        block.setFieldValue('HIGH', 'STATE');
        block.render();
        break;
      case 'analog_read':
        block = ws.newBlock('elekto_analog_read');
        block.initSvg();
        block.setFieldValue('A0', 'PIN');
        block.render();
        break;
      case 'number':
        block = ws.newBlock('math_number');
        block.initSvg();
        block.setFieldValue('1000', 'NUM');
        block.render();
        break;
      case 'compare':
        block = ws.newBlock('logic_compare');
        block.initSvg();
        block.render();
        makeNumber(ws, 0).outputConnection.connect(block.getInput('A').connection);
        makeNumber(ws, 500).outputConnection.connect(block.getInput('B').connection);
        break;
      case 'if':
        block = ws.newBlock('elekto_if');
        block.initSvg();
        block.render();
        makeBoolean(ws, true).outputConnection.connect(block.getInput('COND').connection);
        break;
      case 'repeat':
        block = ws.newBlock('elekto_repeat');
        block.initSvg();
        block.render();
        makeNumber(ws, 10).outputConnection.connect(block.getInput('COUNT').connection);
        break;
      default:
        throw new Error('Unbekannte Vorschau: ' + id);
    }
    block.render();
    return block;
  }

  function inlineStyles(source, target) {
    if (source.nodeType !== Node.ELEMENT_NODE || target.nodeType !== Node.ELEMENT_NODE) return;
    const cs = getComputedStyle(source);
    const props = [
      'fill','fill-opacity','stroke','stroke-width','stroke-opacity',
      'font-family','font-size','font-weight','font-style',
      'color','opacity','display','visibility'
    ];
    let inline = target.getAttribute('style') || '';
    for (const prop of props) {
      const value = cs.getPropertyValue(prop);
      if (value) inline += prop + ':' + value + ';';
    }
    inline += 'visibility:visible;opacity:1;';
    target.setAttribute('style', inline);

    const a = source.children;
    const b = target.children;
    for (let i = 0; i < Math.min(a.length, b.length); i++) inlineStyles(a[i], b[i]);
  }

  async function renderPreview(id) {
    const holder = document.createElement('div');
    holder.style.cssText = [
      'position:fixed','left:0','top:0','width:760px','height:480px',
      'pointer-events:none','opacity:0.001','z-index:-1000','overflow:hidden'
    ].join(';') + ';';
    document.body.appendChild(holder);

    let ws;
    try {
      const main = Blockly.getMainWorkspace?.();
      ws = Blockly.inject(holder, {
        toolbox: null,
        theme: main?.getTheme?.() || Blockly.Themes.Zelos || Blockly.Themes.Classic,
        renderer: 'zelos',
        trashcan: false,
        sounds: false,
        move: {scrollbars:false, drag:false, wheel:false},
        zoom: {controls:false, wheel:false, pinch:false, startScale:1}
      });

      const block = makeBlock(ws, id);
      block.moveBy(40, 40);
      await settle(block, ws);

      const sourceSvg = ws.getParentSvg();
      const allBlocks = ws.getAllBlocks(false);
      const blockCanvas = ws.getCanvas?.() || sourceSvg.querySelector('.blocklyBlockCanvas');
      const exportNode = allBlocks.length === 1 ? block.getSvgRoot() : blockCanvas;
      if (!exportNode) throw new Error('Kein SVG-Knoten für ' + id);

      const bbox = exportNode.getBBox();
      if (!(Number.isFinite(bbox.width) && Number.isFinite(bbox.height) && bbox.width > 1 && bbox.height > 1)) {
        throw new Error('Ungültige Größe für ' + id + ': ' + bbox.width + 'x' + bbox.height);
      }

      const pad = 16;
      const width = Math.ceil(bbox.width + pad * 2);
      const height = Math.ceil(bbox.height + pad * 2);
      const ns = 'http://www.w3.org/2000/svg';
      const out = document.createElementNS(ns, 'svg');
      out.setAttribute('xmlns', ns);
      out.setAttribute('width', String(width));
      out.setAttribute('height', String(height));
      out.setAttribute('viewBox', `0 0 ${width} ${height}`);

      const defs = sourceSvg.querySelector('defs');
      if (defs) out.appendChild(defs.cloneNode(true));

      const clone = exportNode.cloneNode(true);
      inlineStyles(exportNode, clone);
      clone.setAttribute('transform', `translate(${pad - bbox.x} ${pad - bbox.y})`);
      out.appendChild(clone);

      const xml = new XMLSerializer().serializeToString(out);
      const url = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(xml);

      return await new Promise((resolve, reject) => {
        const image = new Image();
        image.onload = () => {
          try {
            const scale = 2;
            const canvas = document.createElement('canvas');
            canvas.width = Math.max(1, width * scale);
            canvas.height = Math.max(1, height * scale);
            const ctx = canvas.getContext('2d');
            ctx.scale(scale, scale);
            ctx.clearRect(0, 0, width, height);
            ctx.drawImage(image, 0, 0, width, height);
            resolve(canvas.toDataURL('image/png'));
          } catch (e) { reject(e); }
        };
        image.onerror = () => reject(new Error('Rasterisierung fehlgeschlagen: ' + id));
        image.src = url;
      });
    } finally {
      try { ws?.dispose(); } catch (_) {}
      holder.remove();
    }
  }

  let queue = Promise.resolve();
  const pending = new Set();

  window.Elekto.requestPreview = function requestPreviewH13(id) {
    if (pending.has(id)) return;
    pending.add(id);
    queue = queue
      .then(async () => {
        let data;
        try {
          data = await renderPreview(id);
        } catch (first) {
          await new Promise(resolve => setTimeout(resolve, 100));
          data = await renderPreview(id);
        }
        window.ElektoAndroid?.setPreview?.(id, data);
      })
      .catch(error => console.error('Preview H13 failed', id, error))
      .finally(() => pending.delete(id));
  };
})();
