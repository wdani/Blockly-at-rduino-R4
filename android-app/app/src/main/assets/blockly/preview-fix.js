(() => {
  'use strict';

  if (typeof Blockly === 'undefined' || !window.Elekto) return;

  function makePreviewBlock(previewWorkspace, id) {
    function number(value) {
      const b = previewWorkspace.newBlock('math_number');
      b.setFieldValue(String(value), 'NUM');
      b.setShadow(true);
      b.initSvg();
      b.render();
      return b;
    }

    function boolean(value) {
      const b = previewWorkspace.newBlock('logic_boolean');
      b.setFieldValue(value ? 'TRUE' : 'FALSE', 'BOOL');
      b.setShadow(true);
      b.initSvg();
      b.render();
      return b;
    }

    let block;
    switch (id) {
      case 'delay':
        block = previewWorkspace.newBlock('elekto_delay');
        block.initSvg();
        block.render();
        number(1000).outputConnection.connect(block.getInput('TIME').connection);
        break;
      case 'digital_write':
        block = previewWorkspace.newBlock('elekto_digital_write');
        break;
      case 'analog_read':
        block = previewWorkspace.newBlock('elekto_analog_read');
        break;
      case 'number':
        block = previewWorkspace.newBlock('math_number');
        block.setFieldValue('1000', 'NUM');
        break;
      case 'compare':
        block = previewWorkspace.newBlock('logic_compare');
        block.initSvg();
        block.render();
        number(0).outputConnection.connect(block.getInput('A').connection);
        number(500).outputConnection.connect(block.getInput('B').connection);
        break;
      case 'if':
        block = previewWorkspace.newBlock('elekto_if');
        block.initSvg();
        block.render();
        boolean(true).outputConnection.connect(block.getInput('COND').connection);
        break;
      case 'repeat':
        block = previewWorkspace.newBlock('elekto_repeat');
        block.initSvg();
        block.render();
        number(10).outputConnection.connect(block.getInput('COUNT').connection);
        break;
      default:
        block = previewWorkspace.newBlock('math_number');
        block.setFieldValue('0', 'NUM');
    }

    // Some Blockly block types already own an SVG root after newBlock(),
    // while others do not. Always ensure that every preview block is fully
    // rendered before measuring/exporting it.
    if (!block.getSvgRoot?.()) block.initSvg();
    block.render();

    // Re-render after connecting shadows so the parent geometry and all
    // connected children have their final positions.
    block.render();
    return block;
  }

  function inlineComputedStyles(source, target) {
    if (source.nodeType !== Node.ELEMENT_NODE || target.nodeType !== Node.ELEMENT_NODE) return;

    const computed = getComputedStyle(source);
    const props = [
      'fill', 'fill-opacity', 'stroke', 'stroke-width', 'stroke-opacity',
      'font-family', 'font-size', 'font-weight', 'font-style',
      'color', 'opacity', 'display', 'visibility'
    ];

    let inline = target.getAttribute('style') || '';
    for (const prop of props) {
      const value = computed.getPropertyValue(prop);
      if (value) inline += prop + ':' + value + ';';
    }
    inline += 'visibility:visible;';
    target.setAttribute('style', inline);

    const sourceChildren = source.children;
    const targetChildren = target.children;
    for (let i = 0; i < Math.min(sourceChildren.length, targetChildren.length); i++) {
      inlineComputedStyles(sourceChildren[i], targetChildren[i]);
    }
  }

  async function renderPreviewPngFixed(id) {
    const holder = document.createElement('div');
    holder.style.cssText =
      'position:fixed;left:-12000px;top:0;width:760px;height:480px;pointer-events:none;';
    document.body.appendChild(holder);

    let previewWorkspace = null;
    try {
      const mainWorkspace = Blockly.getMainWorkspace?.();
      const activeTheme = mainWorkspace?.getTheme?.() || Blockly.Themes.Zelos || Blockly.Themes.Classic;

      previewWorkspace = Blockly.inject(holder, {
        toolbox: null,
        theme: activeTheme,
        renderer: 'zelos',
        trashcan: false,
        sounds: false,
        move: { scrollbars: false, drag: false, wheel: false },
        zoom: { controls: false, wheel: false, pinch: false, startScale: 1 }
      });

      const block = makePreviewBlock(previewWorkspace, id);
      block.moveBy(36, 36);
      Blockly.svgResize(previewWorkspace);

      // Give Blockly two frames to settle connection/shadow geometry.
      await new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)));
      block.render();
      Blockly.svgResize(previewWorkspace);

      const sourceSvg = previewWorkspace.getParentSvg();
      const blockCanvas = previewWorkspace.getCanvas?.() || sourceSvg.querySelector('.blocklyBlockCanvas');
      if (!blockCanvas) throw new Error('Blockly block canvas fehlt.');

      // The block canvas contains the parent block and every connected shadow
      // as separate SVG groups. Exporting the full canvas therefore preserves
      // their correct relative positions, unlike cloning only the parent root.
      const bbox = blockCanvas.getBBox();
      if (!(bbox.width > 0 && bbox.height > 0)) {
        throw new Error('Ungültige Blockgröße: ' + bbox.width + 'x' + bbox.height);
      }

      const pad = 16;
      const width = Math.ceil(bbox.width + pad * 2);
      const height = Math.ceil(bbox.height + pad * 2);
      const svgNS = 'http://www.w3.org/2000/svg';
      const standalone = document.createElementNS(svgNS, 'svg');
      standalone.setAttribute('xmlns', svgNS);
      standalone.setAttribute('width', String(width));
      standalone.setAttribute('height', String(height));
      standalone.setAttribute('viewBox', '0 0 ' + width + ' ' + height);

      const defs = sourceSvg.querySelector('defs');
      if (defs) standalone.appendChild(defs.cloneNode(true));

      const clone = blockCanvas.cloneNode(true);
      inlineComputedStyles(blockCanvas, clone);
      clone.setAttribute(
        'transform',
        'translate(' + (pad - bbox.x) + ' ' + (pad - bbox.y) + ')'
      );
      standalone.appendChild(clone);

      const xml = new XMLSerializer().serializeToString(standalone);
      const svgUrl = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(xml);

      return await new Promise((resolve, reject) => {
        const image = new Image();
        image.onload = () => {
          try {
            const scale = 2;
            const canvas = document.createElement('canvas');
            canvas.width = Math.max(1, width * scale);
            canvas.height = Math.max(1, height * scale);
            const context = canvas.getContext('2d');
            context.scale(scale, scale);
            context.clearRect(0, 0, width, height);
            context.drawImage(image, 0, 0, width, height);
            resolve(canvas.toDataURL('image/png'));
          } catch (error) {
            reject(error);
          }
        };
        image.onerror = () => reject(new Error('Blockvorschau konnte nicht gerastert werden.'));
        image.src = svgUrl;
      });
    } finally {
      try { previewWorkspace?.dispose(); } catch (_) {}
      holder.remove();
    }
  }

  window.Elekto.requestPreview = async function requestPreviewFixed(id) {
    try {
      const data = await renderPreviewPngFixed(id);
      window.ElektoAndroid?.setPreview?.(id, data);
    } catch (error) {
      console.error('Preview render failed (Hybrid 11)', id, error);
    }
  };
})();
