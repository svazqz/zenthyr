const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const WebSocket = require('ws');

const ipcPort = process.argv[2];
const vitePort = process.argv[3];

let ws = null;
let mainWindow = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 800,
    height: 600,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js')
    }
  });

  // Create WebSocket connection
  ws = new WebSocket('ws://localhost:' + ipcPort);

  ws.onerror = (error) => {
    console.error('WebSocket error:', error);
  };

  // Add a map to store pending requests
  const pendingRequests = new Map();
  let requestId = 0;

  ws.onmessage = (message) => {
    if (mainWindow) {
      const data = JSON.parse(message.data);
      if (data.requestId && pendingRequests.has(data.requestId)) {
        const { resolve } = pendingRequests.get(data.requestId);
        pendingRequests.delete(data.requestId);
        resolve(data);
      } else {
        // Regular message handling
        mainWindow.webContents.send('from-backend', message.data);
      }
    }
  };

  ipcMain.handle('invoke', async (event, message) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      const currentRequestId = ++requestId;
      const messageWithId = { ...message, requestId: currentRequestId };
      
      return new Promise((resolve, reject) => {
        // Store the promise callbacks
        pendingRequests.set(currentRequestId, { resolve, reject });
        
        // Send the message
        ws.send(JSON.stringify(messageWithId));
        
        // Set a timeout for the request
        setTimeout(() => {
          if (pendingRequests.has(currentRequestId)) {
            pendingRequests.delete(currentRequestId);
            reject(new Error('WebSocket request timeout'));
          }
        }, 5000); // 5 second timeout
      });
    }
    throw new Error('WebSocket is not connected');
  });

  ws.onopen = () => {
    console.log('WebSocket connection established');
  };

  mainWindow.loadURL('http://localhost:' + vitePort);
  mainWindow.webContents.openDevTools();
}

ipcMain.on('emit', (event, message) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(message));
  } else {
    console.error('WebSocket is not connected');
  }
});

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  ipcMain.removeHandler("invoke")
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});