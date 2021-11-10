var fs = require('fs');
var http = require('http');

var express = require('express');
var app = express();


app.get('/firmwares/firmware.bin', async (req, res) => {
    const axios = require('axios');

    const response = await axios.get( await getLatestLink('firmware.bin'), {responseType: 'arraybuffer', headers: {'accept': 'application/octet-stream'}});
    console.log(response.data);
    res.setHeader('content-type', 'application/octet-stream');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.send(response.data);
    
})

app.get('/firmwares/bootloader_dio_40m.bin', async (req, res) => {
    const axios = require('axios');

    const response = await axios.get( await getLatestLink('bootloader_dio_40m.bin'), {responseType: 'arraybuffer', headers: {'accept': 'application/octet-stream'}});
    console.log(response.data);
    res.setHeader('content-type', 'application/octet-stream');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.send(response.data);
    
})

app.get('/firmwares/partitions.bin', async (req, res) => {
    const axios = require('axios');

    const response = await axios.get( await getLatestLink('partitions.bin'), {responseType: 'arraybuffer', headers: {'accept': 'application/octet-stream'}});
    console.log(response.data);
    res.setHeader('content-type', 'application/octet-stream');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.send(response.data);
    
})

app.get('/firmwares/boot_app0.bin', async (req, res) => {
    const axios = require('axios');

    const response = await axios.get( await getLatestLink('boot_app0.bin'), {responseType: 'arraybuffer', headers: {'accept': 'application/octet-stream'}});
    console.log(response.data);
    res.setHeader('content-type', 'application/octet-stream');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.send(response.data);
    
})

app.get('/hashes/hashes.json', async (req, res) => {
    const axios = require('axios');

    const response = await axios.get( await getLatestLink('hashes.json'), {responseType: 'arraybuffer', headers: {'accept': 'application/octet-stream'}});
    res.setHeader('content-type', 'application/octet-stream');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.send(response.data);
    
})

async function getLatestLink(fileName) {
    const axios = require('axios');

    const response = await axios.get('https://api.github.com/repos/sekassel/CoronaTrackerEsp32/releases/latest', {responseType: 'json', headers: {'accept': 'application/vnd.github.v3+json'}});
    const data = response['data'];
    console.log(data);
    const assets = data['assets'];
    for(var i = 0; i < assets.length; i++){
        const asset = assets[i];
        console.log(asset);
        if (asset['name'] == fileName){
            return asset['url'];
        }
    }
    throw new Error('Latest Firmware Asset Link Not Found');
}

app.get('/css/serialnotactive.css', (req, res) => {
    res.sendFile('./css/serialnotactive.css', { root: '.' });
})

app.get('/css/serialisactive.css', (req, res) => {
    res.sendFile('./css/serialisactive.css', { root: '.' });
})

app.get('/images/notconnectedgif.gif', (req, res) => {
    res.sendFile('./images/notconnectedgif.gif', { root: '.' });
})

app.get('/images/flashComplete.png', (req, res) => {
    res.sendFile('./images/flashComplete.png', { root: '.' });
})

app.get('/images/failed.png', (req, res) => {
    res.sendFile('./images/failed.png', { root: '.' });
})

app.get('/images/upload.gif', (req, res) => {
    res.sendFile('./images/upload.gif', { root: '.' });
})

app.get('/images/noSerial.png', (req, res) => {
    res.sendFile('./images/noSerial.png', { root: '.' });
})

app.get('/images/android-chrome-192x192.png', (req, res) => {
    res.sendFile('./images/android-chrome-192x192.png', { root: '.' });
})

app.get('/images/android-chrome-512x512.png', (req, res) => {
    res.sendFile('./images/android-chrome-512x512.png', { root: '.' });
})

app.get('/js/serialactive.js', (req, res) => {
    res.sendFile('./js/serialactive.js', { root: '.' });
})

app.get('/pages/serialnotactive.html', (req, res) => {
    res.sendFile('./pages/serialnotactive.html', { root: '.' });
})

app.get('/manifest.json', (req, res) => {
    res.sendFile('./manifest.json', { root: '.' });
})

app.get('/sw.js', (req, res) => {
    res.sendFile('./sw.js', { root: '.' });
})

app.get('/', (req, res) => {
    res.sendFile('./index.html', { root: '.' });
})
app.get('/index.html', (req, res) => {
    res.sendFile('./index.html', { root: '.' });
})

app.get('/favicon.ico', (req, res) => {
    res.sendFile('./favicon.ico', { root: '.' });
})

//app.listen(80, () => {console.log('Started')})
var httpServer = http.createServer(app);

httpServer.listen(80, () => { console.log('Started http Server') });