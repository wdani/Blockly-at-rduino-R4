'use strict';

const { app, BrowserWindow, ipcMain } = require('electron');
const { execFile } = require('child_process');
const fs = require('fs');
const path = require('path');

const FQBN = 'arduino:renesas_uno:unor4wifi';
const CORE = 'arduino:renesas_uno';
const DEFAULT_TIMEOUT = 120000;

let mainWindow = null;

function ensureDir(dir) {
    fs.mkdirSync(dir, { recursive: true });
    return dir;
}

function appPath() {
    return app.getAppPath();
}

function cliPath() {
    if (app.isPackaged) {
        return path.join(process.resourcesPath, 'arduino-cli', 'arduino-cli.exe');
    }

    const bundledDev = path.join(appPath(), 'desktop', 'vendor', 'arduino-cli.exe');
    if (fs.existsSync(bundledDev)) {
        return bundledDev;
    }

    return 'arduino-cli.exe';
}

function arduinoPaths() {
    const root = ensureDir(path.join(app.getPath('userData'), 'arduino'));
    const data = ensureDir(path.join(root, 'data'));
    const downloads = ensureDir(path.join(root, 'downloads'));
    const user = ensureDir(path.join(root, 'user'));
    const config = path.join(root, 'arduino-cli.yaml');

    const yaml = [
        'directories:',
        '  data: "' + data.replace(/\\/g, '/') + '"',
        '  downloads: "' + downloads.replace(/\\/g, '/') + '"',
        '  user: "' + user.replace(/\\/g, '/') + '"',
        'updater:',
        '  enable_notification: false',
        ''
    ].join('\n');

    if (!fs.existsSync(config) || fs.readFileSync(config, 'utf8') !== yaml) {
        fs.writeFileSync(config, yaml, 'utf8');
    }

    return { root, data, downloads, user, config };
}

function runCli(args, timeout) {
    const paths = arduinoPaths();
    const executable = cliPath();
    const finalArgs = ['--config-file', paths.config].concat(args);

    return new Promise((resolve, reject) => {
        execFile(executable, finalArgs, {
            windowsHide: true,
            timeout: timeout || DEFAULT_TIMEOUT,
            maxBuffer: 16 * 1024 * 1024
        }, (error, stdout, stderr) => {
            const result = {
                command: [executable].concat(finalArgs).join(' '),
                stdout: stdout || '',
                stderr: stderr || '',
                code: error && typeof error.code !== 'undefined' ? error.code : 0
            };

            if (error) {
                const message = (stderr || stdout || error.message || 'Arduino CLI error').trim();
                const wrapped = new Error(message);
                wrapped.result = result;
                reject(wrapped);
                return;
            }

            resolve(result);
        });
    });
}

function parseJson(text) {
    try {
        return JSON.parse(text);
    } catch (error) {
        return null;
    }
}

async function getEnvironment() {
    let version = null;
    let coreInstalled = false;
    let cliAvailable = true;
    let cliError = null;

    try {
        const versionResult = await runCli(['version', '--format', 'json'], 15000);
        version = parseJson(versionResult.stdout) || versionResult.stdout.trim();
    } catch (error) {
        cliAvailable = false;
        cliError = error.message;
        return {
            cliAvailable,
            cliError,
            cliPath: cliPath(),
            coreInstalled: false,
            fqbn: FQBN
        };
    }

    try {
        const coreResult = await runCli(['core', 'list', '--format', 'json'], 30000);
        const json = parseJson(coreResult.stdout);
        const platforms = Array.isArray(json) ? json : (json && (json.platforms || json.installed_platforms)) || [];
        coreInstalled = platforms.some((item) => {
            const id = item.id || item.ID || item.platform || '';
            return id === CORE || String(id).indexOf(CORE) !== -1;
        });
    } catch (error) {
        coreInstalled = false;
    }

    return {
        cliAvailable,
        cliError,
        cliPath: cliPath(),
        cliVersion: version,
        coreInstalled,
        fqbn: FQBN
    };
}

async function installCore() {
    await runCli(['core', 'update-index'], 180000);
    const install = await runCli(['core', 'install', CORE], 600000);
    return {
        ok: true,
        stdout: install.stdout,
        stderr: install.stderr
    };
}

async function listPorts() {
    const result = await runCli(['board', 'list', '--format', 'json'], 30000);
    const json = parseJson(result.stdout);
    if (!json) {
        return [];
    }

    const detected = Array.isArray(json) ? json : (json.detected_ports || json.ports || []);
    return detected.map((entry) => {
        const port = entry.port || entry;
        const boards = entry.matching_boards || entry.boards || [];
        return {
            address: port.address || port.port || port.name || '',
            label: port.label || port.address || port.port || port.name || '',
            protocol: port.protocol || '',
            matchingBoards: boards.map((board) => ({
                name: board.name || '',
                fqbn: board.fqbn || ''
            }))
        };
    }).filter((item) => item.address);
}

function writeSketch(code) {
    if (typeof code !== 'string' || !code.trim()) {
        throw new Error('Es wurde kein Arduino-Code erzeugt.');
    }

    const sketchDir = ensureDir(path.join(app.getPath('userData'), 'sketches', 'blockly_r4'));
    const inoPath = path.join(sketchDir, 'blockly_r4.ino');
    fs.writeFileSync(inoPath, code, 'utf8');
    return sketchDir;
}

async function compileSketch(code) {
    const sketchDir = writeSketch(code);
    const result = await runCli(['compile', '--fqbn', FQBN, sketchDir], 300000);
    return {
        ok: true,
        stdout: result.stdout,
        stderr: result.stderr
    };
}

async function uploadSketch(code, port) {
    if (!port || typeof port !== 'string') {
        throw new Error('Kein COM-Port ausgewählt.');
    }

    const sketchDir = writeSketch(code);
    const compileResult = await runCli(['compile', '--fqbn', FQBN, sketchDir], 300000);
    const uploadResult = await runCli(['upload', '--port', port, '--fqbn', FQBN, sketchDir], 180000);

    return {
        ok: true,
        compileStdout: compileResult.stdout,
        compileStderr: compileResult.stderr,
        uploadStdout: uploadResult.stdout,
        uploadStderr: uploadResult.stderr
    };
}

function injectRendererSupport(win) {
    const profilePath = path.join(appPath(), 'r4', 'uno-r4-profiles.js');
    const bridgePath = path.join(appPath(), 'r4', 'desktop-bridge.js');

    const profileCode = fs.readFileSync(profilePath, 'utf8') + '\n//# sourceURL=uno-r4-profiles.js';
    const bridgeCode = fs.readFileSync(bridgePath, 'utf8') + '\n//# sourceURL=desktop-bridge.js';

    return win.webContents.executeJavaScript(profileCode)
        .then(() => win.webContents.executeJavaScript(`
            if (window.BlocklyDuino && typeof BlocklyDuino.setArduinoBoard === 'function') {
                BlocklyDuino.setArduinoBoard();
            }
        `))
        .then(() => win.webContents.executeJavaScript(bridgeCode));
}

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1440,
        height: 900,
        minWidth: 1024,
        minHeight: 700,
        title: 'Blockly@rduino R4',
        backgroundColor: '#ffffff',
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false,
            sandbox: false
        }
    });

    mainWindow.removeMenu();

    mainWindow.loadFile(path.join(appPath(), 'index_electron.html'), {
        query: {
            board: 'arduino_uno_r4_wifi',
            lang: 'de'
        }
    });

    mainWindow.webContents.on('did-finish-load', () => {
        injectRendererSupport(mainWindow).catch((error) => {
            console.error('R4 renderer injection failed:', error);
        });
    });

    mainWindow.on('closed', () => {
        mainWindow = null;
    });
}

ipcMain.handle('r4:get-environment', async () => getEnvironment());
ipcMain.handle('r4:install-core', async () => installCore());
ipcMain.handle('r4:list-ports', async () => listPorts());
ipcMain.handle('r4:compile', async (_event, code) => compileSketch(code));
ipcMain.handle('r4:upload', async (_event, payload) => uploadSketch(payload.code, payload.port));

app.whenReady().then(() => {
    createWindow();

    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) {
            createWindow();
        }
    });
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});
