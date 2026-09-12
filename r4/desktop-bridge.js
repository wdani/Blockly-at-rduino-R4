(function () {
    'use strict';

    if (!window.r4Desktop) {
        return;
    }

    var FQBN = 'arduino:renesas_uno:unor4wifi';
    var busy = false;

    function statusElement() {
        var existing = document.getElementById('r4_desktop_status');
        if (existing) {
            return existing;
        }

        var host = document.getElementById('barre') || document.body;
        var span = document.createElement('span');
        span.id = 'r4_desktop_status';
        span.style.marginLeft = '12px';
        span.style.color = 'white';
        span.style.fontWeight = 'bold';
        span.textContent = 'UNO R4 Desktop wird vorbereitet…';
        host.appendChild(span);
        return span;
    }

    function setStatus(text) {
        statusElement().textContent = text;
    }

    function errorText(error) {
        if (!error) return 'Unbekannter Fehler';
        return error.message || String(error);
    }

    function generatedCode() {
        if (!window.Blockly || !Blockly.Arduino || !window.BlocklyDuino || !BlocklyDuino.workspace) {
            throw new Error('Blockly ist noch nicht vollständig geladen.');
        }
        return Blockly.Arduino.workspaceToCode(BlocklyDuino.workspace);
    }

    async function ensureCore() {
        var env = await window.r4Desktop.getEnvironment();
        if (!env.cliAvailable) {
            throw new Error('Arduino CLI wurde nicht gefunden. Bitte den Desktop-Build verwenden.');
        }

        if (env.coreInstalled) {
            return true;
        }

        var install = window.confirm(
            'Der Arduino Renesas UNO Core für den UNO R4 ist noch nicht installiert.\n\n' +
            'Jetzt herunterladen und installieren? Dafür wird einmalig eine Internetverbindung benötigt.'
        );

        if (!install) {
            return false;
        }

        setStatus('Renesas UNO Core wird installiert…');
        await window.r4Desktop.installCore();
        setStatus('Renesas UNO Core ist installiert.');
        return true;
    }

    async function refreshPorts() {
        var select = document.getElementById('serialport_ide');
        if (!select || busy) {
            return;
        }

        var previous = select.value;
        var ports;
        try {
            ports = await window.r4Desktop.listPorts();
        } catch (error) {
            setStatus('Portsuche fehlgeschlagen: ' + errorText(error));
            return;
        }

        select.innerHTML = '';

        if (!ports.length) {
            var none = document.createElement('option');
            none.value = '';
            none.textContent = 'Kein Arduino gefunden';
            select.appendChild(none);
            return;
        }

        var preferred = '';
        ports.forEach(function (port) {
            var option = document.createElement('option');
            option.value = port.address;
            var match = (port.matchingBoards || []).find(function (board) {
                return board.fqbn === FQBN;
            });
            option.textContent = (match ? match.name + ' – ' : '') + port.address;
            select.appendChild(option);
            if (match && !preferred) {
                preferred = port.address;
            }
        });

        if (previous && ports.some(function (port) { return port.address === previous; })) {
            select.value = previous;
        } else if (preferred) {
            select.value = preferred;
        }
    }

    async function compile() {
        if (busy) return;
        busy = true;
        try {
            if (!(await ensureCore())) {
                setStatus('Kompilierung abgebrochen.');
                return;
            }
            setStatus('Kompiliere für UNO R4 WiFi…');
            await window.r4Desktop.compile(generatedCode());
            setStatus('✓ Kompilierung erfolgreich');
        } catch (error) {
            setStatus('✗ Kompilierung fehlgeschlagen');
            window.alert('Kompilierung fehlgeschlagen:\n\n' + errorText(error));
        } finally {
            busy = false;
        }
    }

    async function upload() {
        if (busy) return;
        busy = true;
        try {
            if (!(await ensureCore())) {
                setStatus('Upload abgebrochen.');
                return;
            }

            await refreshPorts();
            var select = document.getElementById('serialport_ide');
            var port = select ? select.value : '';
            if (!port) {
                throw new Error('Kein Arduino UNO R4 gefunden. Board per USB anschliessen und erneut versuchen.');
            }

            setStatus('Kompiliere und lade auf ' + port + '…');
            await window.r4Desktop.upload(generatedCode(), port);
            setStatus('✓ Upload auf ' + port + ' erfolgreich');
        } catch (error) {
            setStatus('✗ Upload fehlgeschlagen');
            window.alert('Upload fehlgeschlagen:\n\n' + errorText(error));
        } finally {
            busy = false;
        }
    }

    async function init() {
        var verify = document.getElementById('btn_verify_local');
        var flash = document.getElementById('btn_flash_local');
        var select = document.getElementById('serialport_ide');

        if (!verify || !flash || !select) {
            setStatus('Desktop-Steuerung konnte nicht geladen werden.');
            return;
        }

        verify.disabled = false;
        flash.disabled = false;
        select.disabled = false;

        verify.title = 'Für Arduino UNO R4 WiFi kompilieren';
        flash.title = 'Direkt auf Arduino UNO R4 WiFi hochladen';
        select.title = 'USB-/COM-Port des Arduino UNO R4 WiFi';

        verify.addEventListener('click', compile);
        flash.addEventListener('click', upload);

        try {
            var env = await window.r4Desktop.getEnvironment();
            if (!env.cliAvailable) {
                setStatus('Arduino CLI fehlt im Desktop-Paket.');
            } else if (!env.coreInstalled) {
                setStatus('UNO R4 bereit – Renesas Core wird beim ersten Kompilieren installiert.');
            } else {
                setStatus('UNO R4 Desktop bereit');
            }
        } catch (error) {
            setStatus('Desktop-Initialisierung fehlgeschlagen: ' + errorText(error));
        }

        await refreshPorts();
        window.setInterval(refreshPorts, 4000);
    }

    init();
})();
