var cacheName = 'hello-pwa';
var filesToCache = [
  '/',
  '/index.html',
  '/pages/serialnotactive.html',
  '/images/notconnectedgif.gif',
  '/images/upload.gif',
  '/images/noSerial.png',
  '/css/serialisactive.css',
  '/css/serialnotactive.css',
  '/js/serialactive.js'
];

/* Start the service worker and cache all of the app's content */
self.addEventListener('install', function(e) {
  e.waitUntil(
    caches.open(cacheName).then(function(cache) {
      return cache.addAll(filesToCache);
    })
  );
});

/* Serve cached content when offline */
self.addEventListener('fetch', function(e) {
  e.respondWith(
    caches.match(e.request).then(function(response) {
      return response || fetch(e.request);
    })
  );

  e.waitUntil(update(e.request).then(refresh))
});

function update(request) {
  return caches.open(cacheName).then(function (cache) {
    return fetch(request).then(function (response) {
      return cache.put(request, response.clone()).then(function () {
        return response;
      });
    });
  });
}

function refresh(response) {
  return self.clients.matchAll().then(function (clients) {
    clients.forEach(function (client) {
	var message = {
        	type: 'refresh',
        	url: response.url,
		eTag: response.headers.get('ETag')
        }; 
        client.postMessage(JSON.stringify(message));
    });
  });
}