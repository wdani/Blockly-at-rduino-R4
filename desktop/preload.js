'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('r4Desktop', {
    getEnvironment: function () {
        return ipcRenderer.invoke('r4:get-environment');
    },
    installCore: function () {
        return ipcRenderer.invoke('r4:install-core');
    },
    listPorts: function () {
        return ipcRenderer.invoke('r4:list-ports');
    },
    compile: function (code) {
        return ipcRenderer.invoke('r4:compile', code);
    },
    upload: function (code, port) {
        return ipcRenderer.invoke('r4:upload', { code: code, port: port });
    }
});
