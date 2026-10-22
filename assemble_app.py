import os
import shutil
import json

print("Assembling CallerCraft Application...")

# Import modules
from generate_app import HEAD_CONTENT
from build_full_callercraft import HEADER_HTML
from sections_html import TAB_SEARCH_HTML, TAB_CALLS_HTML
from sections_html_part2 import TAB_MESSAGES_HTML, TAB_SPAM_HTML, TAB_CONTACTS_HTML
from sections_html_part3 import TAB_ASSISTANT_HTML, TAB_SIMULATOR_HTML, TAB_ANALYTICS_HTML
from modals_html import MODALS_HTML
from scripts_part1 import SCRIPTS_PART1_JS
from scripts_part2 import SCRIPTS_PART2_JS
from scripts_part3 import SCRIPTS_PART3_JS

full_html = (
    HEAD_CONTENT +
    HEADER_HTML +
    TAB_SEARCH_HTML +
    TAB_CALLS_HTML +
    TAB_MESSAGES_HTML +
    TAB_SPAM_HTML +
    TAB_CONTACTS_HTML +
    TAB_ASSISTANT_HTML +
    TAB_SIMULATOR_HTML +
    TAB_ANALYTICS_HTML +
    MODALS_HTML +
    SCRIPTS_PART1_JS +
    SCRIPTS_PART2_JS +
    SCRIPTS_PART3_JS
)

# 1. Save to ~/callercraft/index.html
dest_local = "/data/data/com.termux/files/home/callercraft/index.html"
with open(dest_local, "w", encoding="utf-8") as f:
    f.write(full_html)
print(f"Generated {dest_local} ({len(full_html)} chars, {len(full_html.splitlines())} lines)")

# 2. Save to /storage/emulated/0/Download/CallerCraft - Smart Caller ID, AI Spam Blocker & Phone Lookup.html
dest_dl_1 = "/storage/emulated/0/Download/CallerCraft - Smart Caller ID, AI Spam Blocker & Phone Lookup.html"
with open(dest_dl_1, "w", encoding="utf-8") as f:
    f.write(full_html)
print(f"Generated {dest_dl_1}")

# 3. Save to /storage/emulated/0/Download/callercraft.html
dest_dl_2 = "/storage/emulated/0/Download/callercraft.html"
with open(dest_dl_2, "w", encoding="utf-8") as f:
    f.write(full_html)
print(f"Generated {dest_dl_2}")

# 4. Update the user's original file /storage/emulated/0/Download/Truecaller - Smart Caller ID, Spam Blocker & Phone Lookup.html
dest_dl_orig = "/storage/emulated/0/Download/Truecaller - Smart Caller ID, Spam Blocker & Phone Lookup.html"
with open(dest_dl_orig, "w", encoding="utf-8") as f:
    f.write(full_html)
print(f"Updated original file {dest_dl_orig}")

# 5. Create PWA manifest.json
import json
manifest_json = {
    "name": "CallerCraft - Smart Caller ID & AI Spam Shield",
    "short_name": "CallerCraft",
    "description": "Intelligent Caller ID, AI Spam Defense, Call Recorder, Smart SMS & Phonebook",
    "start_url": "index.html",
    "display": "standalone",
    "background_color": "#090D16",
    "theme_color": "#0D6EFD",
    "orientation": "portrait-primary",
    "icons": [
        {
            "src": "icon-192.png",
            "sizes": "192x192",
            "type": "image/png",
            "purpose": "any maskable"
        },
        {
            "src": "icon-512.png",
            "sizes": "512x512",
            "type": "image/png",
            "purpose": "any maskable"
        }
    ]
}

manifest_path = "/data/data/com.termux/files/home/callercraft/manifest.json"
with open(manifest_path, "w", encoding="utf-8") as f:
    json.dump(manifest_json, f, indent=2)
print(f"Generated {manifest_path}")

# 6. Create Service Worker sw.js for offline usage
sw_content = '''const CACHE_NAME = 'callercraft-v2';
const ASSETS_TO_CACHE = [
  './',
  './index.html',
  './manifest.json',
  'https://cdn.tailwindcss.com/3.4.17',
  'https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;600;700&display=swap'
];

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(ASSETS_TO_CACHE).catch(() => {});
    })
  );
  self.skipWaiting();
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((k) => {
          if (k !== CACHE_NAME) return caches.delete(k);
        })
      );
    })
  );
  self.clients.claim();
});

self.addEventListener('fetch', (e) => {
  e.respondWith(
    caches.match(e.request).then((res) => {
      return res || fetch(e.request).catch(() => caches.match('./index.html'));
    })
  );
});
'''
sw_path = "/data/data/com.termux/files/home/callercraft/sw.js"
with open(sw_path, "w", encoding="utf-8") as f:
    f.write(sw_content)
print(f"Generated {sw_path}")

# 7. Create Node.js static server script server.js
server_content = '''const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 8080;
const MIME_TYPES = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml'
};

const server = http.createServer((req, res) => {
  let reqPath = req.url.split('?')[0];
  if (reqPath === '/' || reqPath === '') reqPath = '/index.html';
  const filePath = path.join(__dirname, reqPath);

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('404 Not Found');
      return;
    }
    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    res.writeHead(200, { 'Content-Type': contentType });
    fs.createReadStream(filePath).pipe(res);
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 CallerCraft Web Server running at: http://localhost:${PORT}`);
  console.log(`📱 Access on Android Browser: http://localhost:${PORT} or open index.html directly!`);
});
'''
server_path = "/data/data/com.termux/files/home/callercraft/server.js"
with open(server_path, "w", encoding="utf-8") as f:
    f.write(server_content)
print(f"Generated {server_path}")

print("All components assembled successfully!")
