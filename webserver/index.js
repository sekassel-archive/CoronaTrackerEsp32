//var fs = require('fs');
//var http = require('http');
//var https = require('https');
//var privateKey  = fs.readFileSync('sslcert/server.key', 'utf8');
//var certificate = fs.readFileSync('sslcert/server.crt', 'utf8');

//var credentials = {key: privateKey, cert: certificate};
var express = require('express');
var app = express();

app.get('/', (req, res) => {
    res.sendFile('./index.html', { root: '.'});
  })

app.get('/pages/serialnotactive.html', (req, res) => {
    res.sendFile('./pages/serialnotactive.html', { root: '.'});
})

app.get('/css/serialnotactive.css', (req, res) => {
    res.sendFile('./css/serialnotactive.css', { root: '.'});
})

app.get('/images/noSerial.png', (req, res) => {
    res.sendFile('./images/noSerial.png', { root: '.'});
})

app.get('/images/hello-icon-152.png', (req, res) => {
    res.sendFile('./images/hello-icon-152.png', { root: '.'});
})

app.get('/favicon.ico', (req, res) => {
    res.sendFile('./favicon.ico', { root: '.'});
})

app.get('/css/serialisactive.css', (req, res) => {
    res.sendFile('./css/serialisactive.css', { root: '.'});
})

app.get('/js/serialactive.js', (req, res) => {
    res.sendFile('./js/serialactive.js', { root: '.'});
})

app.get('/images/notconnectedgif.gif', (req, res) => {
    res.sendFile('./images/notconnectedgif.gif', { root: '.'});
})

app.get('/manifest.json', (req, res) => {
    res.sendFile('./manifest.json', { root: '.'});
})

app.get('/images/hello-icon-144.png', (req, res) => {
    res.sendFile('./images/hello-icon-144.png', { root: '.'});
})

app.listen(80, () => {console.log('Started')})
//var httpServer = http.createServer(app);
//var httpsServer = https.createServer(credentials, app);

//httpServer.listen(8080);
//httpsServer.listen(8443);