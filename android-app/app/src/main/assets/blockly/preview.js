(() => {
  const params = new URLSearchParams(location.search);
  const id = params.get('id') || 'delay';
  const dark = params.get('theme') === 'dark';

  function defineElektoBlocks() {
    Blockly.defineBlocksWithJsonArray([
      {
        type:'elekto_delay', message0:'warte %1 ms',
        args0:[{type:'input_value',name:'TIME',check:'Number'}],
        previousStatement:null,nextStatement:null, colour:'#6C55C7'
      },
      {
        type:'elekto_digital_write', message0:'setze Pin %1 auf %2',
        args0:[
          {type:'field_dropdown',name:'PIN',options:[['D13','13'],['D2','2']]},
          {type:'field_dropdown',name:'STATE',options:[['HIGH','HIGH'],['LOW','LOW']]}
        ],
        previousStatement:null,nextStatement:null, colour:'#16865C'
      },
      {
        type:'elekto_analog_read', message0:'Analogwert %1',
        args0:[{type:'field_dropdown',name:'PIN',options:[['A0','A0'],['A1','A1']]}],
        output:'Number', colour:'#2D6BC4'
      },
      {
        type:'elekto_repeat', message0:'wiederhole %1 mal',
        args0:[{type:'input_value',name:'COUNT',check:'Number'}],
        message1:'%1',args1:[{type:'input_statement',name:'DO'}],
        previousStatement:null,nextStatement:null, colour:'#D89A00'
      },
      {
        type:'elekto_if', message0:'wenn %1',
        args0:[{type:'input_value',name:'COND',check:'Boolean'}],
        message1:'%1',args1:[{type:'input_statement',name:'DO'}],
        previousStatement:null,nextStatement:null, colour:'#C25235'
      }
    ]);
  }

  function start() {
    const host = document.getElementById('preview');
    const w = Math.max(220, host.clientWidth || window.innerWidth || 320);
    const h = Math.max(90, host.clientHeight || window.innerHeight || 116);
    host.style.width = w + 'px';
    host.style.height = h + 'px';

    defineElektoBlocks();

    const theme = Blockly.Theme.defineTheme(dark ? 'preview-dark' : 'preview-light', {
      base: Blockly.Themes.Zelos || Blockly.Themes.Classic,
      componentStyles: {
        workspaceBackgroundColour: dark ? '#1B1B22' : '#F9FAFD'
      },
      fontStyle: { family:'system-ui, sans-serif', weight:'600', size:12 }
    });

    const ws = Blockly.inject('preview', {
      toolbox:null,
      theme,
      renderer:'zelos',
      trashcan:false,
      sounds:false,
      move:{scrollbars:false,drag:false,wheel:false},
      zoom:{controls:false,wheel:false,pinch:false,startScale:0.78,minScale:0.18,maxScale:1.0},
      grid:{spacing:0,length:0,colour:'transparent',snap:false}
    });

    function number(v) {
      const b = ws.newBlock('math_number');
      b.setFieldValue(String(v),'NUM');
      b.setShadow(true);
      b.initSvg();
      b.render();
      return b;
    }

    let b;
    switch(id) {
      case 'delay':
        b=ws.newBlock('elekto_delay'); b.initSvg(); b.render();
        number(1000).outputConnection.connect(b.getInput('TIME').connection);
        break;
      case 'digital_write':
        b=ws.newBlock('elekto_digital_write');
        break;
      case 'analog_read':
        b=ws.newBlock('elekto_analog_read');
        break;
      case 'number':
        b=ws.newBlock('math_number'); b.setFieldValue('1000','NUM');
        break;
      case 'compare':
        b=ws.newBlock('logic_compare'); b.initSvg(); b.render();
        number(0).outputConnection.connect(b.getInput('A').connection);
        number(500).outputConnection.connect(b.getInput('B').connection);
        break;
      case 'if':
        b=ws.newBlock('elekto_if');
        break;
      case 'repeat':
        b=ws.newBlock('elekto_repeat'); b.initSvg(); b.render();
        number(10).outputConnection.connect(b.getInput('COUNT').connection);
        break;
      default:
        b=ws.newBlock('math_number'); b.setFieldValue('0','NUM');
    }

    if (!b.getSvgRoot?.()) {
      b.initSvg();
      b.render();
    }

    // Fit the complete Blockly block into the preview card only after both
    // Android WebView layout and Blockly rendering have settled.
    function fitPreview() {
      Blockly.svgResize(ws);
      try {
        ws.zoomToFit();
        ws.centerOnBlock(b.id);
      } catch (_) {}
      Blockly.svgResize(ws);
    }

    requestAnimationFrame(() => requestAnimationFrame(fitPreview));
    setTimeout(fitPreview, 120);
    setTimeout(fitPreview, 350);
    window.addEventListener('resize', fitPreview);
  }

  function waitForLayout(tries = 0) {
    const host = document.getElementById('preview');
    if ((host.clientWidth > 40 && host.clientHeight > 40) || tries > 30) {
      start();
    } else {
      setTimeout(() => waitForLayout(tries + 1), 30);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => waitForLayout());
  } else {
    waitForLayout();
  }
})();