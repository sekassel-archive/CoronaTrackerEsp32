var fs = require('fs');
var http = require('http');
var https = require('https');
var privateKey = fs.readFileSync('keys/key.pem');
var certificate = fs.readFileSync('keys/cert.pem');

var credentials = { key: privateKey, cert: certificate };
var express = require('express');
var app = express();

app.get('/', (req, res) => {
    res.sendFile('./index.html', { root: '.' });
})

app.get('/pages/serialnotactive.html', (req, res) => {
    res.sendFile('./pages/serialnotactive.html', { root: '.' });
})

app.get('/firmwares/firmware.bin', async (req, res) => {
    const axios = require('axios');

    const response = await axios.get( await getLatestFirmwareLink(), {responseType: 'arraybuffer', headers: {'accept': 'application/octet-stream'}});
    console.log(response.data);
    res.setHeader('content-type', 'application/octet-stream');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.send(response.data);
    
})

app.get('/hashes/firmware.hash', async (req, res) => {
    /*const axios = require('axios');
    -------TODO-------
    const response = await axios.get('https://api.github.com/repos/drinkbuddy/trackerTest/releases/assets/29689661', {responseType: 'arraybuffer', headers: {'accept': 'application/octet-stream'}});
    console.log(response.data);
    res.setHeader('content-type', 'application/octet-stream');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.send(response.data);*/
    
})

async function getLatestFirmwareLink() {
    const axios = require('axios');

    const response = await axios.get('https://api.github.com/repos/drinkbuddy/trackerTest/releases/latest', {responseType: 'json', headers: {'accept': 'application/vnd.github.v3+json'}});
    const data = response['data'];
    console.log(data);
    const assets = data['assets'];
    for(var i = 0; i < assets.length; i++){
        const asset = assets[i];
        console.log(asset);
        if (asset['name'] == 'firmware.bin'){
            return asset['url'];
        }
    }
    throw new Error('Latest Firmware Asset Link Not Found');
}

app.get('/css/serialnotactive.css', (req, res) => {
    res.sendFile('./css/serialnotactive.css', { root: '.' });
})

app.get('/images/noSerial.png', (req, res) => {
    res.sendFile('./images/noSerial.png', { root: '.' });
})

app.get('/images/hello-icon-152.png', (req, res) => {
    res.sendFile('./images/hello-icon-152.png', { root: '.' });
})

app.get('/favicon.ico', (req, res) => {
    res.sendFile('./favicon.ico', { root: '.' });
})

app.get('/css/serialisactive.css', (req, res) => {
    res.sendFile('./css/serialisactive.css', { root: '.' });
})

app.get('/js/serialactive.js', (req, res) => {
    res.sendFile('./js/serialactive.js', { root: '.' });
})

app.get('/images/notconnectedgif.gif', (req, res) => {
    res.sendFile('./images/notconnectedgif.gif', { root: '.' });
})

app.get('/manifest.json', (req, res) => {
    res.sendFile('./manifest.json', { root: '.' });
})

app.get('/images/hello-icon-144.png', (req, res) => {
    res.sendFile('./images/hello-icon-144.png', { root: '.' });
})

app.get('/sw.js', (req, res) => {
    res.sendFile('./sw.js', { root: '.' });
})

//app.listen(80, () => {console.log('Started')})
var httpServer = http.createServer(app);
var httpsServer = https.createServer(credentials, app);

httpServer.listen(80, () => { console.log('Started http Server') });
httpsServer.listen(443, () => { console.log('Started https Server') });