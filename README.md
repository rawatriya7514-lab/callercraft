# 🛡️ CraftCaller — Smart Caller ID, AI Spam Shield & Phone Intelligence

> **The next-generation Truecaller alternative crafted with exquisite UI/UX, AI Call Screening, Smart SMS OTP Copier, T9 Predictive Dialer, Real vCard Export, and Web Audio API tone synthesis.**

---

## 🌟 Highlight Features

### 1. 🔍 Global Caller Search & Reverse Number Lookup
- Instant search by phone number or person/business name across 4.2+ billion records.
- Country code selector (`+91` India, `+1` US/Canada, `+44` UK, `+971` UAE, `+61` Australia, `+49` Germany, `+65` Singapore).
- **Interactive SVG Circular Risk Gauge**: 0% to 100% real-time threat calculation.
- **Smart Verification Badges**:
  - 👑 **CraftCaller Gold VIP**
  - 🏢 **Verified Enterprise Corporate Line**
  - 🚨 **High-Risk Phishing / Severe Scam Alert**
  - 👤 **Verified Personal Contact**
- Direct Actions: **Call**, **SMS**, **WhatsApp direct chat** (`wa.me`), **One-tap Block**, **Save Contact**, **Copy Number**, **Share Profile**.
- Real Community Feedback & Experience Reports with user comments and timestamps.
- **Algorithmic Fallback Intelligence**: Deterministic telecom operator (Jio, Airtel, Vi, BSNL) and geographical circle identification for unlisted numbers.

### 2. 📞 Calls History & Smart T9 Dialer
- **Detailed Call Logs**: Filter by All, Incoming, Outgoing, Missed, Blocked/Spam.
- Real-time search inside call logs.
- **Export Call Logs to CSV**: One-tap download of `.csv` spreadsheet.
- **Tactile 12-Key T9 Dialer**:
  - **Realistic DTMF Audio Synthesizer**: Pure Web Audio API tone synthesis on every touch (standard dual-sine telephone frequencies).
  - **Live T9 Predictive Contact Matching**: Type digits (`22728`) to search both phone numbers and names (`AARAV`) in real time.
  - Voicemail speed dial shortcut on key `1`.
- **Realistic Outgoing Call Simulator**:
  - Standard telephone ringback audio cadence.
  - Automatic connection after 2.5 seconds with harmonic C-major chime.
  - Active in-call timer (`MM:SS`), Mute toggle, Speakerphone toggle, and Disconnect busy tone.
  - **AI Call Recording Visualizer**: Animated soundwave equalizer bars.

### 3. 💬 Smart SMS & Anti-Phishing Inbox
- Truecaller-grade SMS categorization:
  - 💳 **Transactions & Banking**: High-priority alert styling.
  - 📋 **1-Click Copy OTP**: Instant extraction of OTP verification codes with green confirmation toast!
  - 💬 **Personal**: Conversations with saved friends and contacts.
  - 🏷️ **Promotions & Offers**: Discount codes and marketing messages.
  - 🚨 **Quarantined Phishing & Spam**: Automatic quarantine of malicious SMS with highlighted threat alerts.
- **Conversation Thread Modal**: View chat bubbles, timestamps, and send simulated replies.
- **Compose New SMS**: Send simulated text messages to any recipient.

### 4. 🛡️ Spam Shield & Live Threat Radar
- **Live Worldwide Scam Attack Stream**: Real-time ticker showing active phishing numbers flagged across Delhi, Mumbai, Chicago, and global VoIP gateways.
- **Autonomous Protection Toggles**:
  - Auto-block Top Spammers (50+ community complaints)
  - Block Foreign / International Unknowns
  - Block Private & Hidden Caller IDs
  - VIP Mode (Silence all non-phonebook callers)
- **Offline Signature Database**: Instant local signature sync with 450,000+ cached threats.
- **Custom Blocklist & Wildcard Manager**: Add custom phone numbers or prefixes (e.g. `+91 140*`, `+1 800*`).
- **Community Report Modal**: Report scam numbers with category, reason, and notes.

### 5. 👥 Contacts Directory & Genuine .VCF (vCard) Export
- Alphabetical contact list with quick search.
- Contact cards showing phone number, telecom operator, city, and Gold VIP tags.
- **Genuine .VCF vCard 3.0 Export**: Generates and downloads a real `craftcaller_contacts.vcf` file that can be imported directly into Android Contacts app!
- Add, edit, and delete contacts with instant LocalStorage sync.

### 6. 🤖 AI Call Assistant & Voice Screening Studio
- Next-gen Truecaller Assistant simulation.
- **Custom AI Personas**:
  - *Professional Gatekeeper*
  - *Cyber Fraud Hunter*
  - *Friendly Receptionist*
- **Spoken Voice Speech (Web Speech API)**: Speaks aloud through device speakers!
- **Live Call Screening Session**: Live speech transcription bubbles with real-time sentiment & scam risk detection.
- Interactive user actions: *"I am driving"*, *"What is this regarding?"*, *"🚨 Mark Spam & Hang Up"*, *"📞 Take Over Call"*.

### 7. ⚡ Caller ID Simulator Studio
- 7 Built-in Presets:
  1. **SBI KYC Reward Scam** (Financial Fraud • 14,820 reports • Red Radar Pulse)
  2. **Amazon Prime Delivery** (Verified Courier • Blue Radar Pulse)
  3. **HDFC Priority Banking** (Verified Enterprise • Official Line)
  4. **Aarav Sharma** (CraftCaller Gold VIP • Personal Friend • Amber Pulse)
  5. **PolicyBazaar Sales Desk** (Telemarketing Robo • Orange Badge)
  6. **FedEx Customs Impersonator** (Extortion / Digital Arrest Scam)
  7. **Custom Caller Generator**: Input any custom name, phone number, and badge.
- **Full Screen Incoming Call Overlay**: Pulsing radar ripple, ringtone audio synthesis, vibration, accept/decline/block.
- **Floating Android Heads-Up Banner**: Floating pill notification at the top of the screen.

### 8. 📊 Analytics, Privacy & Full Backup
- Audit statistics: Total spam calls blocked, hours saved, phishing SMS quarantined.
- *"Who Looked Up My Number"*: Profile inquiry audit log.
- **Full Data Backup & Restore**: One-click download of `craftcaller_backup.json` and file reader to restore.

---

## 📱 How to Run & Access on Android

### Option 1: Open Directly in Any Browser (Zero Setup!)
The file is saved directly to your phone's storage. You can open it in Chrome, Firefox, Samsung Internet, or any Android browser:
- Path 1: `/storage/emulated/0/Download/CraftCaller - Smart Caller ID, AI Spam Blocker & Phone Lookup.html`
- Path 2: `/storage/emulated/0/Download/craftcaller.html`
- Path 3: `/storage/emulated/0/Download/Truecaller - Smart Caller ID, Spam Blocker & Phone Lookup.html`

Simply open your phone's **Files / Downloads** app and tap the file to open in your browser!

### Option 2: Run Local Web Server via Termux
If you want to run it via local HTTP server:
```bash
cd ~/craftcaller
node server.js 8080
```
Then visit in your phone's browser:
```
http://localhost:8080
```

---

## 🎨 Design & Craftsmanship
- **Color Palette**: Electric Craft Blue (`#0D6EFD`), Imperial Amber (`#F59E0B`), Crimson Shield (`#EF4444`), Cyber Emerald (`#10B981`), Deep Obsidian (`#090D16`).
- **Typography**: Plus Jakarta Sans & JetBrains Mono.
- **Glassmorphism**: High-performance backdrop blur with custom glow keyframes.
- **Web Audio API**: 100% offline synthesis for ringtones, DTMF dial tones, connect chimes, and disconnect beeps.
