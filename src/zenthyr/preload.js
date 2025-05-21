const { contextBridge, ipcRenderer } = require('electron');

const zenthyr = {
  emit: (message) => {
    ipcRenderer.send('emit', message);
  },
  invoke: (message) => {
    try {
      return ipcRenderer.invoke('invoke', message);
    } catch(e) {
      // Handler registered already
    }
  }
}

contextBridge.exposeInMainWorld('zenthyr', zenthyr);

contextBridge.exposeInMainWorld('asyncTest', async () => {
  const result = await zenthyr.invoke({
    type: 'counter',
    action: 'increment',
    data: {}
  });
  console.log('Got synchronous response:', result);
  return null;
});

ipcRenderer.on('from-backend', (event, message) => {
  window.dispatchEvent(new CustomEvent('backend-message', { 
    detail: typeof message === 'string' ? JSON.parse(message) : message 
  }));
});