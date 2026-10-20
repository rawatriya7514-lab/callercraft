# -------------------------------------------------------------
# 8. JAVASCRIPT ENGINE - PART 3: SMS, SPAM, AI ASSISTANT, SIMULATOR & EXPORTS
# -------------------------------------------------------------

SCRIPTS_PART3_JS = '''
    // ==========================================
    // 7. OUTGOING CALL & ACTIVE CALL OVERLAY
    // ==========================================
    let activeCallDuration = 0;
    let activeCallInterval = null;
    let currentCallData = null;
    let isCallActive = false;
    let isCallMuted = false;
    let isCallSpeaker = false;
    let isCallRecording = false;

    function makeOutgoingCall(name, number, type) {
      currentCallData = {
        name: name || 'Outgoing Call',
        number: number,
        type: type || 'safe',
        callType: 'outgoing',
        carrier: 'Outgoing Line',
        location: 'Local Network'
      };

      // Open Modal in Outgoing State
      const callModal = document.getElementById('incoming-call-modal');
      const callerNameEl = document.getElementById('incoming-caller-name');
      const callerNumberEl = document.getElementById('incoming-caller-number');
      const callerCarrierEl = document.getElementById('incoming-caller-carrier');
      const callerLocationEl = document.getElementById('incoming-caller-location');
      const avatarChar = document.getElementById('incoming-caller-avatar-char');
      const radarPulse = document.getElementById('incoming-radar-pulse');
      const incomingActions = document.getElementById('incoming-call-actions');
      const inCallControls = document.getElementById('in-call-active-controls');
      const hangUpActiveWrapper = document.getElementById('hang-up-active-wrapper');
      const callTimerEl = document.getElementById('call-sim-timer');
      const spamBanner = document.getElementById('incoming-spam-banner');
      const verifiedBanner = document.getElementById('incoming-verified-banner');
      const goldBanner = document.getElementById('incoming-gold-banner');
      const reportsBox = document.getElementById('incoming-reports-box');

      callerNameEl.textContent = currentCallData.name;
      callerNumberEl.textContent = currentCallData.number;
      callerCarrierEl.textContent = 'Craft Cellular Network';
      callerLocationEl.textContent = 'Dialing...';
      avatarChar.textContent = currentCallData.name.charAt(0) || '📞';
      radarPulse.className = 'w-24 h-24 rounded-full flex items-center justify-center text-3xl font-extrabold shadow-lg transition-all radar-pulse-safe bg-cc-blue text-white';

      spamBanner.classList.add('hidden');
      verifiedBanner.classList.add('hidden');
      goldBanner.classList.add('hidden');
      reportsBox.classList.add('hidden');

      incomingActions.classList.add('hidden');
      inCallControls.classList.add('hidden');
      hangUpActiveWrapper.classList.remove('hidden');
      callTimerEl.classList.remove('hidden');
      callTimerEl.textContent = 'Calling...';

      callModal.classList.remove('hidden');

      // Start Outgoing Ringback Audio
      playRingbackTone();
      window.ringbackTimer = setInterval(playRingbackTone, 3000);

      // Simulate Answer after 2.5 seconds
      window.outgoingConnectTimer = setTimeout(() => {
        clearInterval(window.ringbackTimer);
        connectActiveCall();
      }, 2600);
    }

    function connectActiveCall() {
      isCallActive = true;
      activeCallDuration = 0;
      playConnectedChime();

      const callTimerEl = document.getElementById('call-sim-timer');
      const inCallControls = document.getElementById('in-call-active-controls');
      const callerLocationEl = document.getElementById('incoming-caller-location');

      callerLocationEl.textContent = 'Call Connected';
      inCallControls.classList.remove('hidden');
      callTimerEl.textContent = '00:00';

      activeCallInterval = setInterval(() => {
        activeCallDuration++;
        const mins = String(Math.floor(activeCallDuration / 60)).padStart(2, '0');
        const secs = String(activeCallDuration % 60).padStart(2, '0');
        callTimerEl.textContent = `${mins}:${secs}`;
      }, 1000);

      showToast('Call connected', 'success');
    }

    function hangUpCall() {
      stopRingtone();
      clearInterval(window.ringbackTimer);
      clearTimeout(window.outgoingConnectTimer);
      clearInterval(activeCallInterval);
      playHangupTone();

      // Log the completed call
      if (currentCallData) {
        const mins = Math.floor(activeCallDuration / 60);
        const secs = activeCallDuration % 60;
        const durStr = activeCallDuration > 0 ? (mins > 0 ? `${mins}m ${secs}s` : `${secs}s`) : '0s';

        callLogs.unshift({
          id: 'log-' + Date.now(),
          name: currentCallData.name,
          number: currentCallData.number,
          type: currentCallData.type || 'safe',
          category: currentCallData.category || 'General',
          callType: isCallActive ? (currentCallData.callType || 'outgoing') : 'missed',
          time: 'Just now',
          duration: durStr
        });
        saveLogs();
        renderCallLogs();
      }

      isCallActive = false;
      isCallRecording = false;
      document.getElementById('in-call-recorder-wave').classList.add('hidden');
      document.getElementById('incoming-call-modal').classList.add('hidden');
      document.getElementById('floating-caller-banner').classList.add('-translate-y-24');
      document.getElementById('floating-caller-banner').classList.add('hidden');
    }

    document.getElementById('hang-up-active-btn').addEventListener('click', hangUpCall);
    document.getElementById('close-modal-x-btn').addEventListener('click', hangUpCall);

    // In-Call Toggle Buttons
    document.getElementById('in-call-mute-btn').addEventListener('click', () => {
      isCallMuted = !isCallMuted;
      showToast(isCallMuted ? 'Microphone muted' : 'Microphone unmuted', 'info');
      document.getElementById('in-call-mute-btn').classList.toggle('bg-red-800', isCallMuted);
    });

    document.getElementById('in-call-speaker-btn').addEventListener('click', () => {
      isCallSpeaker = !isCallSpeaker;
      showToast(isCallSpeaker ? 'Speakerphone turned on' : 'Speakerphone off', 'info');
      document.getElementById('in-call-speaker-btn').classList.toggle('bg-cc-blue', isCallSpeaker);
    });

    document.getElementById('in-call-record-btn').addEventListener('click', () => {
      isCallRecording = !isCallRecording;
      const wave = document.getElementById('in-call-recorder-wave');
      if (isCallRecording) {
        wave.classList.remove('hidden');
        showToast('Craft AI Call Recording & Transcription Active', 'spam');
      } else {
        wave.classList.add('hidden');
        showToast('Call recording stopped & saved', 'info');
      }
    });

    document.getElementById('in-call-block-report-btn').addEventListener('click', () => {
      if (currentCallData) {
        toggleBlockNumber(currentCallData.number);
        hangUpCall();
      }
    });

    // ==========================================
    // 8. SMART SMS & ANTI-PHISHING ENGINE
    // ==========================================
    let currentSmsFilter = 'all';

    function renderMessages() {
      const container = document.getElementById('messages-list-container');
      const filtered = messages.filter(m => {
        if (currentSmsFilter === 'all') return true;
        return m.category === currentSmsFilter;
      });

      if (filtered.length === 0) {
        container.innerHTML = `
          <div class="p-8 text-center bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl text-slate-400">
            <p class="font-bold text-xs sm:text-sm">No messages in this folder</p>
          </div>
        `;
        return;
      }

      container.innerHTML = filtered.map(msg => {
        const isSpam = msg.isSpam;
        const hasOtp = Boolean(msg.otp);
        const avatarBg = isSpam ? 'bg-red-500 text-white' : (msg.category === 'transactions' ? 'bg-indigo-600 text-white' : 'bg-cc-blue text-white');

        return `
          <div class="bg-white dark:bg-cc-cardDark border ${isSpam ? 'border-red-300 dark:border-red-900/60 shadow-red-500/5' : 'border-slate-200 dark:border-cc-borderDark'} rounded-3xl p-4 sm:p-5 shadow-sm hover:shadow-md transition-all">
            <div class="flex items-start justify-between gap-3">
              <div class="flex items-start gap-3 min-w-0">
                <div class="w-10 h-10 rounded-2xl ${avatarBg} font-extrabold text-sm flex items-center justify-center flex-shrink-0 shadow-sm">
                  ${isSpam ? '!' : msg.sender.charAt(0)}
                </div>
                <div class="min-w-0">
                  <div class="flex items-center gap-2">
                    <h4 class="font-extrabold text-xs sm:text-sm text-slate-900 dark:text-white truncate">${msg.sender}</h4>
                    ${msg.isVerified ? '<span class="text-[9px] font-bold px-1.5 py-0.2 rounded bg-blue-100 dark:bg-blue-950 text-cc-blue dark:text-blue-300">VERIFIED</span>' : ''}
                    ${isSpam ? '<span class="text-[9px] font-bold px-1.5 py-0.2 rounded bg-red-100 dark:bg-red-950 text-red-600 dark:text-red-400">🚨 PHISHING</span>' : ''}
                  </div>
                  <p class="text-xs text-slate-600 dark:text-slate-300 mt-1 line-clamp-2">${msg.body}</p>
                  
                  ${isSpam ? `
                    <div class="mt-2 p-2 rounded-xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-900 text-[11px] text-red-700 dark:text-red-300 font-semibold flex items-center gap-1.5">
                      <svg class="w-4 h-4 flex-shrink-0 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"></path></svg>
                      <span>${msg.threatReason || 'Phishing danger'}</span>
                    </div>
                  ` : ''}
                </div>
              </div>

              <span class="text-[10px] text-slate-400 whitespace-nowrap flex-shrink-0">${msg.time}</span>
            </div>

            <!-- Action Controls on SMS card -->
            <div class="mt-3 pt-3 border-t border-slate-100 dark:border-cc-borderDark flex items-center justify-between gap-2 flex-wrap">
              <button class="view-thread-btn text-xs font-bold text-cc-blue dark:text-blue-400 hover:underline flex items-center gap-1" data-id="${msg.id}">
                View Conversation
              </button>

              <div class="flex items-center gap-1.5">
                ${hasOtp ? `
                  <button class="copy-otp-btn px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-extrabold shadow-sm flex items-center gap-1.5 transition-all active:scale-95" data-otp="${msg.otp}">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/></svg>
                    <span>Copy OTP: ${msg.otp}</span>
                  </button>
                ` : ''}

                ${isSpam ? `
                  <button class="delete-spam-sms-btn px-3 py-1.5 bg-red-100 dark:bg-red-950 text-red-600 dark:text-red-400 rounded-xl text-xs font-bold hover:bg-red-200 transition-all" data-id="${msg.id}">
                    Delete Threat
                  </button>
                ` : ''}
              </div>
            </div>
          </div>
        `;
      }).join('');

      // Wire up OTP copy buttons
      container.querySelectorAll('.copy-otp-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const otp = btn.dataset.otp;
          navigator.clipboard.writeText(otp).then(() => {
            showToast(`OTP ${otp} copied to clipboard!`, 'success');
          }).catch(() => {
            showToast(`OTP ${otp} copied!`, 'success');
          });
        });
      });

      // View Thread Buttons
      container.querySelectorAll('.view-thread-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          openSmsThread(btn.dataset.id);
        });
      });

      // Delete Spam Buttons
      container.querySelectorAll('.delete-spam-sms-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          messages = messages.filter(m => m.id !== btn.dataset.id);
          saveMessages();
          renderMessages();
          showToast('Spam message deleted', 'info');
        });
      });
    }

    // Filter Chips Event Listeners
    document.querySelectorAll('.sms-filter-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.sms-filter-btn').forEach(b => {
          b.classList.remove('bg-cc-blue', 'text-white');
          b.classList.add('bg-slate-200', 'dark:bg-cc-cardDark', 'text-slate-700', 'dark:text-slate-300');
        });
        btn.classList.add('bg-cc-blue', 'text-white');
        btn.classList.remove('bg-slate-200', 'dark:bg-cc-cardDark', 'text-slate-700', 'dark:text-slate-300');
        currentSmsFilter = btn.dataset.category;
        renderMessages();
      });
    });

    // SMS Thread Modal Logic
    let activeThreadMsg = null;

    function openSmsThread(msgId) {
      activeThreadMsg = messages.find(m => m.id === msgId);
      if (!activeThreadMsg) return;

      const modal = document.getElementById('sms-thread-modal');
      const senderEl = document.getElementById('sms-thread-sender');
      const statusEl = document.getElementById('sms-thread-status');
      const avatarEl = document.getElementById('sms-thread-avatar');
      const messagesEl = document.getElementById('sms-thread-messages');

      senderEl.textContent = activeThreadMsg.sender;
      statusEl.textContent = activeThreadMsg.isSpam ? '🚨 Quarantined Fraud' : (activeThreadMsg.isVerified ? 'Verified Official Sender' : 'Personal SMS');
      statusEl.className = activeThreadMsg.isSpam ? 'text-[10px] text-red-500 font-bold' : 'text-[10px] text-emerald-500 font-semibold';
      avatarEl.textContent = activeThreadMsg.sender.charAt(0);

      messagesEl.innerHTML = (activeThreadMsg.thread || [{ sender: activeThreadMsg.sender, text: activeThreadMsg.body, time: activeThreadMsg.time }]).map(m => {
        const isMe = m.sender === 'You';
        return `
          <div class="flex flex-col ${isMe ? 'items-end' : 'items-start'}">
            <div class="max-w-[80%] p-3 rounded-2xl text-xs ${isMe ? 'bg-cc-blue text-white rounded-br-none' : 'bg-white dark:bg-cc-cardDark text-slate-800 dark:text-slate-200 border border-slate-200 dark:border-cc-borderDark rounded-bl-none shadow-sm'}">
              <p>${m.text}</p>
            </div>
            <span class="text-[9px] text-slate-400 mt-1 px-1">${m.time}</span>
          </div>
        `;
      }).join('');

      modal.classList.remove('hidden');
    }

    document.getElementById('close-sms-thread-btn').addEventListener('click', () => {
      document.getElementById('sms-thread-modal').classList.add('hidden');
    });

    document.getElementById('sms-reply-send-btn').addEventListener('click', () => {
      const input = document.getElementById('sms-reply-input');
      const text = input.value.trim();
      if (!text || !activeThreadMsg) return;

      if (!activeThreadMsg.thread) {
        activeThreadMsg.thread = [{ sender: activeThreadMsg.sender, text: activeThreadMsg.body, time: activeThreadMsg.time }];
      }
      activeThreadMsg.thread.push({ sender: 'You', text: text, time: 'Just now' });
      saveMessages();
      openSmsThread(activeThreadMsg.id);
      input.value = '';
      showToast('SMS reply sent', 'success');
    });

    // Compose SMS Modal Logic
    const composeModal = document.getElementById('compose-sms-modal');
    document.getElementById('open-compose-sms-btn').addEventListener('click', () => {
      document.getElementById('compose-sms-recipient').value = '';
      document.getElementById('compose-sms-body').value = '';
      composeModal.classList.remove('hidden');
    });

    function openComposeSMS(recipient) {
      document.getElementById('compose-sms-recipient').value = recipient || '';
      document.getElementById('compose-sms-body').value = '';
      composeModal.classList.remove('hidden');
    }

    document.getElementById('close-compose-sms-btn').addEventListener('click', () => composeModal.classList.add('hidden'));
    document.getElementById('cancel-compose-sms-btn').addEventListener('click', () => composeModal.classList.add('hidden'));

    document.getElementById('send-compose-sms-btn').addEventListener('click', () => {
      const rec = document.getElementById('compose-sms-recipient').value.trim();
      const body = document.getElementById('compose-sms-body').value.trim();
      if (!rec || !body) {
        showToast('Please enter recipient and message', 'error');
        return;
      }
      messages.unshift({
        id: 'msg-' + Date.now(),
        sender: rec,
        senderNumber: rec,
        category: 'personal',
        isVerified: false,
        body: body,
        otp: null,
        time: 'Just now',
        isSpam: false,
        thread: [{ sender: 'You', text: body, time: 'Just now' }]
      });
      saveMessages();
      renderMessages();
      composeModal.classList.add('hidden');
      showToast(`SMS sent to ${rec}`, 'success');
    });

    // ==========================================
    // 9. SPAM SHIELD & THREAT RADAR
    // ==========================================
    function renderBlocklist() {
      const container = document.getElementById('custom-blocklist-items');
      if (customBlocklist.length === 0) {
        container.innerHTML = '<p class="text-xs text-slate-400 italic">No custom blocked numbers.</p>';
        return;
      }
      container.innerHTML = customBlocklist.map(num => `
        <div class="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 dark:bg-cc-darkBg border border-slate-200/60 dark:border-cc-borderDark/60">
          <span class="text-xs font-mono font-bold text-slate-800 dark:text-slate-200">${num}</span>
          <button class="remove-block-btn text-xs text-red-500 hover:text-red-700 font-bold px-2 py-0.5 rounded-lg" data-num="${num}">
            Unblock
          </button>
        </div>
      `).join('');

      container.querySelectorAll('.remove-block-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          toggleBlockNumber(btn.dataset.num);
        });
      });
    }

    document.getElementById('add-blocklist-btn').addEventListener('click', () => {
      const input = document.getElementById('add-blocklist-input');
      const val = input.value.trim();
      if (!val) return;
      if (!customBlocklist.includes(val)) {
        customBlocklist.push(val);
        saveBlocklist();
        renderBlocklist();
        showToast(`Added ${val} to blocklist`, 'spam');
      }
      input.value = '';
    });

    // Offline Threat DB Update Button
    document.getElementById('update-spam-db-btn').addEventListener('click', () => {
      showToast('Downloading latest threat signatures...', 'info');
      setTimeout(() => {
        showToast('Threat DB v2026.10 updated: 450,000 Signatures active offline!', 'success');
        playBeep(900, 0.15);
      }, 1200);
    });

    // Live Spam Threat Stream Generator
    const LIVE_FEED_TEMPLATES = [
      { name: 'SBI Credit Card Phishing', number: '+91 98765 43210', location: 'New Delhi', carrier: 'Vi', reports: 14820 },
      { name: 'FedEx Customs Narcotics Threat', number: '+91 80000 99888', location: 'VoIP Gateway', carrier: 'VoIP', reports: 22410 },
      { name: 'Electricity Bill Disconnect Scam', number: '+91 99999 00000', location: 'Uttar Pradesh', carrier: 'BSNL', reports: 18940 },
      { name: 'IRS Tax Lawsuit Robocall', number: '+1 800 555 0199', location: 'Chicago, USA', carrier: 'US VoIP', reports: 9840 },
      { name: 'Digital Arrest Cyber Cell Fake', number: '+91 91234 44556', location: 'Kolkata', carrier: 'Airtel', reports: 8410 },
      { name: 'Work From Home Telegram Task Scam', number: '+91 98990 88776', location: 'Bangalore', carrier: 'Jio', reports: 6310 }
    ];

    function renderSpamFeed() {
      const container = document.getElementById('live-spam-feed');
      container.innerHTML = LIVE_FEED_TEMPLATES.map((item, idx) => `
        <div class="p-3 sm:p-4 rounded-2xl bg-red-50/50 dark:bg-red-950/20 border border-red-200 dark:border-red-900/40 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-2xl bg-red-600 text-white font-extrabold flex items-center justify-center text-sm shadow-sm flex-shrink-0">
              !
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h4 class="font-bold text-xs sm:text-sm text-slate-900 dark:text-white">${item.name}</h4>
                <span class="text-[9px] font-bold px-1.5 py-0.2 rounded bg-red-100 dark:bg-red-950 text-red-600 dark:text-red-400">HIGH RISK</span>
              </div>
              <div class="text-[11px] text-slate-500 font-mono mt-0.5">${item.number} • ${item.location} • ${item.reports.toLocaleString()} reports</div>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <button class="feed-sim-btn px-3 py-1.5 bg-cc-spamRed hover:bg-red-600 text-white rounded-xl text-xs font-bold transition-all shadow-sm"
              data-name="${item.name}" data-number="${item.number}" data-carrier="${item.carrier}" data-location="${item.location}">
              Simulate Scam Call
            </button>
            <button class="feed-block-btn p-1.5 rounded-xl bg-slate-200 dark:bg-cc-darkBg text-slate-700 dark:text-slate-300 hover:text-red-500" data-number="${item.number}" title="Block">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/></svg>
            </button>
          </div>
        </div>
      `).join('');

      container.querySelectorAll('.feed-sim-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          triggerIncomingCall({
            name: btn.dataset.name,
            number: btn.dataset.number,
            type: 'spam',
            carrier: btn.dataset.carrier,
            location: btn.dataset.location,
            category: 'Financial Fraud',
            reports: 14820
          });
        });
      });

      container.querySelectorAll('.feed-block-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          toggleBlockNumber(btn.dataset.number);
        });
      });
    }

    // Report Spam Modal Logic
    const reportModal = document.getElementById('report-spam-modal');
    document.getElementById('open-report-spam-modal-btn').addEventListener('click', () => {
      document.getElementById('report-input-number').value = '';
      document.getElementById('report-input-name').value = '';
      document.getElementById('report-input-comment').value = '';
      reportModal.classList.remove('hidden');
    });

    document.getElementById('close-report-modal-btn').addEventListener('click', () => reportModal.classList.add('hidden'));
    document.getElementById('cancel-report-btn').addEventListener('click', () => reportModal.classList.add('hidden'));

    document.getElementById('submit-report-btn').addEventListener('click', () => {
      const num = document.getElementById('report-input-number').value.trim();
      const name = document.getElementById('report-input-name').value.trim() || 'Reported Scam Caller';
      const cat = document.getElementById('report-input-category').value;
      const comment = document.getElementById('report-input-comment').value.trim();

      if (!num) {
        showToast('Please provide a phone number', 'error');
        return;
      }

      db.unshift({
        id: 'rep-' + Date.now(),
        name: name,
        number: num,
        cleanNumber: sanitizeNum(num),
        type: 'spam',
        category: cat,
        reports: 1,
        carrier: 'Reported Gateway',
        location: 'Community Flagged',
        safeScore: 2,
        verified: false,
        gold: false,
        comments: comment ? [{ user: 'You (Craft Community)', text: comment, time: 'Just now' }] : []
      });

      customBlocklist.push(num);
      saveDB();
      saveBlocklist();
      renderBlocklist();
      reportModal.classList.add('hidden');
      showToast(`Number ${num} reported & added to auto-blocklist!`, 'spam');
    });

    // ==========================================
    // 10. CONTACTS & CLOUD DIRECTORY
    // ==========================================
    function renderContacts(filterText) {
      filterText = filterText || '';
      const container = document.getElementById('contacts-list-container');
      const contacts = db.filter(d => d.type !== 'spam');
      const filtered = contacts.filter(c => {
        if (!filterText) return true;
        const q = filterText.toLowerCase();
        return c.name.toLowerCase().includes(q) || c.number.includes(q) || (c.location || '').toLowerCase().includes(q);
      });

      if (filtered.length === 0) {
        container.innerHTML = `
          <div class="col-span-full p-8 text-center text-slate-400">
            <p class="font-bold text-xs sm:text-sm">No contacts found</p>
          </div>
        `;
        return;
      }

      container.innerHTML = filtered.map(c => {
        const isGold = c.gold || c.type === 'gold';
        return `
          <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-5 shadow-sm hover:shadow-md transition-all flex flex-col justify-between">
            <div class="flex items-start gap-3">
              <div class="w-12 h-12 rounded-2xl ${isGold ? 'bg-gradient-to-tr from-amber-400 to-yellow-600' : 'bg-cc-blue'} text-white font-extrabold text-lg flex items-center justify-center flex-shrink-0 shadow-sm">
                ${c.name.charAt(0)}
              </div>
              <div class="min-w-0">
                <div class="flex items-center gap-1.5">
                  <h4 class="font-bold text-sm text-slate-900 dark:text-white truncate">${c.name}</h4>
                  ${isGold ? '<span class="text-[9px] font-bold px-1.5 py-0.2 rounded bg-amber-100 dark:bg-amber-950 text-amber-600 dark:text-amber-400">GOLD VIP</span>' : ''}
                </div>
                <div class="text-xs font-mono font-semibold text-slate-600 dark:text-slate-400 mt-0.5">${c.number}</div>
                <div class="text-[11px] text-slate-500 mt-1 truncate">${c.category} • ${c.location}</div>
              </div>
            </div>

            <div class="mt-4 pt-3 border-t border-slate-100 dark:border-cc-borderDark flex items-center justify-between gap-1.5">
              <button class="contact-call-btn flex-1 py-1.5 px-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs flex items-center justify-center gap-1 shadow-sm transition-all"
                data-name="${c.name}" data-number="${c.number}">
                <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path d="M2 3a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 4V3z"></path></svg>
                Call
              </button>

              <button class="contact-sms-btn py-1.5 px-3 rounded-xl bg-blue-50 dark:bg-cc-darkBg text-cc-blue dark:text-blue-400 hover:bg-blue-100 transition-all text-xs font-bold"
                data-name="${c.name}" data-number="${c.number}">
                SMS
              </button>

              <button class="contact-sim-btn py-1.5 px-3 rounded-xl bg-slate-100 dark:bg-cc-darkBg text-slate-700 dark:text-slate-300 hover:bg-slate-200 transition-all text-xs font-bold"
                data-name="${c.name}" data-number="${c.number}" data-carrier="${c.carrier}" data-location="${c.location}" data-gold="${isGold}">
                Simulate
              </button>
            </div>
          </div>
        `;
      }).join('');

      container.querySelectorAll('.contact-call-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          makeOutgoingCall(btn.dataset.name, btn.dataset.number, 'safe');
        });
      });

      container.querySelectorAll('.contact-sms-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          openComposeSMS(btn.dataset.number);
        });
      });

      container.querySelectorAll('.contact-sim-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          triggerIncomingCall({
            name: btn.dataset.name,
            number: btn.dataset.number,
            type: btn.dataset.gold === 'true' ? 'gold' : 'safe',
            category: 'Personal Contact',
            reports: 0,
            carrier: btn.dataset.carrier,
            location: btn.dataset.location
          });
        });
      });
    }

    document.getElementById('contacts-search-input').addEventListener('input', (e) => {
      renderContacts(e.target.value);
    });

    // Real vCard (.vcf) Export Feature
    document.getElementById('export-contacts-vcf-btn').addEventListener('click', () => {
      const contacts = db.filter(d => d.type !== 'spam');
      if (contacts.length === 0) {
        showToast('No contacts to export', 'error');
        return;
      }

      let vcf = '';
      contacts.forEach(c => {
        vcf += 'BEGIN:VCARD\\r\\n';
        vcf += 'VERSION:3.0\\r\\n';
        vcf += `FN:${c.name}\\r\\n`;
        vcf += `TEL;TYPE=CELL:${c.number}\\r\\n`;
        if (c.carrier) vcf += `ORG:${c.carrier}\\r\\n`;
        if (c.location) vcf += `NOTE:Location: ${c.location} | Category: ${c.category}\\r\\n`;
        vcf += 'END:VCARD\\r\\n';
      });

      const blob = new Blob([vcf], { type: 'text/vcard;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `craftcaller_contacts_${Date.now()}.vcf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      showToast('Exported contacts as .VCF (vCard 3.0)! Importable to Android.', 'success');
    });

    // Add Contact Modal Logic
    const addContactModal = document.getElementById('add-contact-modal');
    document.getElementById('add-contact-btn').addEventListener('click', () => {
      document.getElementById('contact-input-name').value = '';
      document.getElementById('contact-input-phone').value = '';
      document.getElementById('contact-input-carrier').value = '';
      document.getElementById('contact-input-location').value = '';
      document.getElementById('contact-input-gold').checked = false;
      addContactModal.classList.remove('hidden');
    });

    document.getElementById('close-add-contact-btn').addEventListener('click', () => addContactModal.classList.add('hidden'));
    document.getElementById('cancel-add-contact-btn').addEventListener('click', () => addContactModal.classList.add('hidden'));

    document.getElementById('save-new-contact-btn').addEventListener('click', () => {
      const name = document.getElementById('contact-input-name').value.trim();
      const phone = document.getElementById('contact-input-phone').value.trim();
      const carrier = document.getElementById('contact-input-carrier').value.trim() || 'Mobile Operator';
      const loc = document.getElementById('contact-input-location').value.trim() || 'India';
      const isGold = document.getElementById('contact-input-gold').checked;

      if (!name || !phone) {
        showToast('Please enter both name and phone number', 'error');
        return;
      }

      db.unshift({
        id: 'user-' + Date.now(),
        name: name,
        number: phone,
        cleanNumber: sanitizeNum(phone),
        type: isGold ? 'gold' : 'safe',
        category: 'Personal Phonebook',
        reports: 0,
        carrier: carrier,
        location: loc,
        safeScore: 100,
        verified: true,
        gold: isGold,
        comments: []
      });

      saveDB();
      renderContacts();
      addContactModal.classList.add('hidden');
      showToast(`Contact "${name}" saved to Craft Directory!`, 'success');
    });

    // ==========================================
    // 11. AI CALL ASSISTANT SCREENING STUDIO
    // ==========================================
    let assistantPersona = 'professional';
    let isAssistantScreening = false;

    document.querySelectorAll('.assistant-persona-card').forEach(card => {
      card.addEventListener('click', () => {
        document.querySelectorAll('.assistant-persona-card').forEach(c => {
          c.classList.remove('border-purple-500', 'bg-purple-50/50', 'dark:bg-purple-950/30');
          c.classList.add('border-transparent', 'bg-slate-50', 'dark:bg-cc-darkBg');
        });
        card.classList.add('border-purple-500', 'bg-purple-50/50', 'dark:bg-purple-950/30');
        card.classList.remove('border-transparent', 'bg-slate-50', 'dark:bg-cc-darkBg');
        assistantPersona = card.dataset.persona;
        showToast(`AI Assistant persona set to: ${assistantPersona.toUpperCase()}`, 'info');
      });
    });

    const toggleAssistantVoiceBtn = document.getElementById('toggle-assistant-voice-btn');
    toggleAssistantVoiceBtn.addEventListener('click', () => {
      assistantVoiceEnabled = !assistantVoiceEnabled;
      toggleAssistantVoiceBtn.textContent = assistantVoiceEnabled ? 'Enabled (Spoken)' : 'Muted (Text only)';
      toggleAssistantVoiceBtn.className = assistantVoiceEnabled ? 'font-bold text-emerald-400 hover:underline' : 'font-bold text-slate-400 hover:underline';
      showToast(`AI Assistant voice speech: ${assistantVoiceEnabled ? 'ON' : 'OFF'}`, 'info');
    });

    document.getElementById('start-assistant-demo-btn').addEventListener('click', () => {
      launchAssistantScreening({
        name: 'SBI KYC Reward Scam',
        number: '+91 98765 43210'
      });
    });

    function launchAssistantScreening(caller) {
      isAssistantScreening = true;
      switchTab('tab-assistant');
      const box = document.getElementById('assistant-transcript-box');
      const sessionCaller = document.getElementById('assistant-session-caller');
      const riskInd = document.getElementById('assistant-risk-indicator');

      sessionCaller.textContent = `Screening: ${caller.name} (${caller.number})`;
      riskInd.textContent = 'Analyzing Voice...';
      riskInd.className = 'text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 dark:bg-amber-950 text-amber-600 dark:text-amber-400 animate-pulse';

      let promptText = '';
      if (assistantPersona === 'strict') {
        promptText = 'Warning: This call is recorded by CraftCaller AI. State your registered business credentials and purpose.';
      } else if (assistantPersona === 'friendly') {
        promptText = 'Hi! The recipient is away right now. I am CraftCaller Assistant. How may I help you?';
      } else {
        promptText = 'Hello. I am CraftCaller AI Assistant. Who is calling and what is this regarding?';
      }

      box.innerHTML = `
        <div class="flex items-start gap-2">
          <div class="w-7 h-7 rounded-lg bg-purple-600 text-white flex items-center justify-center text-[10px] font-bold flex-shrink-0">AI</div>
          <div class="p-3 rounded-2xl bg-purple-50 dark:bg-purple-950/60 text-xs text-purple-900 dark:text-purple-200 border border-purple-200 dark:border-purple-800">
            <span class="font-bold">Craft Assistant:</span> ${promptText}
          </div>
        </div>
      `;

      speakAssistantText(promptText);

      // Caller responds after 2.5 seconds
      setTimeout(() => {
        const callerResponse = 'Sir, I am calling from State Bank card department. Your card reward points of Rs. 9,450 will expire tonight unless you share card verification OTP right now.';
        box.innerHTML += `
          <div class="flex items-start gap-2 justify-end">
            <div class="p-3 rounded-2xl bg-red-50 dark:bg-red-950/60 text-xs text-red-900 dark:text-red-200 border border-red-200 dark:border-red-800 text-right">
              <span class="font-bold text-red-600 dark:text-red-400">Caller (Transcribed):</span> "${callerResponse}"
            </div>
            <div class="w-7 h-7 rounded-lg bg-red-600 text-white flex items-center justify-center text-[10px] font-bold flex-shrink-0">!</div>
          </div>
        `;
        riskInd.textContent = '🚨 FRAUD DETECTED (99%)';
        riskInd.className = 'text-[10px] font-bold px-2 py-0.5 rounded-full bg-red-100 dark:bg-red-950 text-red-600 dark:text-red-400';
        box.scrollTop = box.scrollHeight;
        showToast('AI Detected Financial OTP Phishing Scam!', 'spam');
      }, 2600);
    }

    document.querySelectorAll('.assistant-prompt-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const reply = btn.dataset.reply;
        const box = document.getElementById('assistant-transcript-box');
        if (reply === 'block') {
          box.innerHTML += `<div class="p-2 rounded-xl bg-red-600 text-white text-xs font-bold text-center">🚨 Call Blocked & Number Added to Community Blacklist</div>`;
          showToast('Call disconnected and number blocked', 'spam');
          playHangupTone();
        } else if (reply === 'takeover') {
          showToast('Taking over live call...', 'success');
          makeOutgoingCall('SBI KYC Scam', '+91 98765 43210', 'spam');
        } else {
          const t = reply === 'busy' ? 'I am driving right now. Please message on WhatsApp.' : 'What registered company are you calling from?';
          box.innerHTML += `
            <div class="flex items-start gap-2">
              <div class="w-7 h-7 rounded-lg bg-purple-600 text-white flex items-center justify-center text-[10px] font-bold flex-shrink-0">AI</div>
              <div class="p-3 rounded-2xl bg-purple-50 dark:bg-purple-950/60 text-xs text-purple-900 dark:text-purple-200 border border-purple-200 dark:border-purple-800">
                <span class="font-bold">Craft Assistant:</span> "${t}"
              </div>
            </div>
          `;
          speakAssistantText(t);
        }
        box.scrollTop = box.scrollHeight;
      });
    });

    // ==========================================
    // 12. CALL SIMULATOR STUDIO
    // ==========================================
    function triggerIncomingCall(data) {
      currentCallData = data;
      isCallActive = false;
      activeCallDuration = 0;
      clearInterval(activeCallInterval);

      const callModal = document.getElementById('incoming-call-modal');
      const callerNameEl = document.getElementById('incoming-caller-name');
      const callerNumberEl = document.getElementById('incoming-caller-number');
      const callerCarrierEl = document.getElementById('incoming-caller-carrier');
      const callerLocationEl = document.getElementById('incoming-caller-location');
      const avatarChar = document.getElementById('incoming-caller-avatar-char');
      const radarPulse = document.getElementById('incoming-radar-pulse');
      const incomingActions = document.getElementById('incoming-call-actions');
      const inCallControls = document.getElementById('in-call-active-controls');
      const hangUpActiveWrapper = document.getElementById('hang-up-active-wrapper');
      const callTimerEl = document.getElementById('call-sim-timer');
      const spamBanner = document.getElementById('incoming-spam-banner');
      const verifiedBanner = document.getElementById('incoming-verified-banner');
      const goldBanner = document.getElementById('incoming-gold-banner');
      const reportsBox = document.getElementById('incoming-reports-box');
      const reportsCountEl = document.getElementById('incoming-reports-count');
      const blockDeclineWrap = document.getElementById('block-and-decline-wrapper');

      callerNameEl.textContent = data.name;
      callerNumberEl.textContent = data.number;
      callerCarrierEl.textContent = data.carrier || 'Cellular Operator';
      callerLocationEl.textContent = data.location || 'India';

      const isSpam = data.type === 'spam';
      const isVerified = data.type === 'verified';
      const isGold = data.type === 'gold';

      if (isSpam) {
        spamBanner.classList.remove('hidden');
        verifiedBanner.classList.add('hidden');
        goldBanner.classList.add('hidden');
        reportsBox.classList.remove('hidden');
        reportsCountEl.textContent = `${Number(data.reports || 14820).toLocaleString()} people`;
        blockDeclineWrap.classList.remove('hidden');
        radarPulse.className = 'w-24 h-24 rounded-full flex items-center justify-center text-3xl font-extrabold shadow-lg transition-all radar-pulse-spam bg-red-600 text-white';
        avatarChar.textContent = '!';
      } else if (isGold) {
        spamBanner.classList.add('hidden');
        verifiedBanner.classList.add('hidden');
        goldBanner.classList.remove('hidden');
        reportsBox.classList.add('hidden');
        blockDeclineWrap.classList.add('hidden');
        radarPulse.className = 'w-24 h-24 rounded-full flex items-center justify-center text-3xl font-extrabold shadow-lg transition-all radar-pulse-gold bg-gradient-to-tr from-amber-400 to-yellow-600 text-white';
        avatarChar.textContent = data.name.charAt(0);
      } else if (isVerified) {
        spamBanner.classList.add('hidden');
        verifiedBanner.classList.remove('hidden');
        goldBanner.classList.add('hidden');
        reportsBox.classList.add('hidden');
        blockDeclineWrap.classList.add('hidden');
        radarPulse.className = 'w-24 h-24 rounded-full flex items-center justify-center text-3xl font-extrabold shadow-lg transition-all radar-pulse-safe bg-cc-blue text-white';
        avatarChar.textContent = data.name.charAt(0);
      } else {
        spamBanner.classList.add('hidden');
        verifiedBanner.classList.add('hidden');
        goldBanner.classList.add('hidden');
        reportsBox.classList.add('hidden');
        blockDeclineWrap.classList.add('hidden');
        radarPulse.className = 'w-24 h-24 rounded-full flex items-center justify-center text-3xl font-extrabold shadow-lg transition-all radar-pulse-green bg-emerald-600 text-white';
        avatarChar.textContent = data.name.charAt(0);
      }

      incomingActions.classList.remove('hidden');
      inCallControls.classList.add('hidden');
      hangUpActiveWrapper.classList.add('hidden');
      callTimerEl.classList.add('hidden');

      callModal.classList.remove('hidden');
      startRingtone();
    }

    // Preset Buttons
    document.querySelectorAll('.simulate-preset-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        triggerIncomingCall({
          name: btn.dataset.name,
          number: btn.dataset.number,
          type: btn.dataset.type,
          category: btn.dataset.category,
          reports: btn.dataset.reports,
          location: btn.dataset.location,
          carrier: btn.dataset.carrier
        });
      });
    });

    // Custom Caller Form
    document.getElementById('trigger-custom-call-btn').addEventListener('click', () => {
      const name = document.getElementById('custom-caller-name').value.trim() || 'Custom Caller';
      const number = document.getElementById('custom-caller-number').value.trim() || '+91 99999 88888';
      const type = document.getElementById('custom-caller-type').value;

      triggerIncomingCall({
        name: name,
        number: number,
        type: type,
        category: 'Custom Simulation',
        reports: type === 'spam' ? 5400 : 0,
        location: 'Local Region',
        carrier: 'Test Carrier'
      });
    });

    // Accept / Decline / Block Call Actions in Modal
    document.getElementById('answer-call-btn').addEventListener('click', () => {
      stopRingtone();
      connectActiveCall();
      document.getElementById('incoming-call-actions').classList.add('hidden');
      document.getElementById('hang-up-active-wrapper').classList.remove('hidden');
      document.getElementById('call-sim-timer').classList.remove('hidden');
    });

    document.getElementById('decline-call-btn').addEventListener('click', () => {
      stopRingtone();
      hangUpCall();
      showToast('Call declined', 'info');
    });

    document.getElementById('block-and-decline-btn').addEventListener('click', () => {
      stopRingtone();
      if (currentCallData) {
        toggleBlockNumber(currentCallData.number);
      }
      hangUpCall();
    });

    document.getElementById('screen-call-ai-btn').addEventListener('click', () => {
      stopRingtone();
      document.getElementById('incoming-call-modal').classList.add('hidden');
      if (currentCallData) {
        launchAssistantScreening(currentCallData);
      }
    });

    // ==========================================
    // 13. FULL DATA BACKUP & RESTORE
    // ==========================================
    document.getElementById('export-full-backup-btn').addEventListener('click', () => {
      const backupData = {
        app: 'CraftCaller',
        version: '2.0',
        exportedAt: new Date().toISOString(),
        database: db,
        callLogs: callLogs,
        messages: messages,
        blocklist: customBlocklist
      };

      const blob = new Blob([JSON.stringify(backupData, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `craftcaller_backup_${Date.now()}.json`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      showToast('Full backup exported to JSON!', 'success');
    });

    document.getElementById('import-backup-input').addEventListener('change', (e) => {
      const file = e.target.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = (event) => {
        try {
          const parsed = JSON.parse(event.target.result);
          if (parsed.database) db = parsed.database;
          if (parsed.callLogs) callLogs = parsed.callLogs;
          if (parsed.messages) messages = parsed.messages;
          if (parsed.blocklist) customBlocklist = parsed.blocklist;
          saveDB();
          saveLogs();
          saveMessages();
          saveBlocklist();
          renderCallLogs();
          renderMessages();
          renderBlocklist();
          renderContacts();
          showToast('CraftCaller data successfully restored from backup!', 'success');
        } catch (err) {
          showToast('Invalid backup JSON file', 'error');
        }
      };
      reader.readAsText(file);
    });

    document.getElementById('reset-factory-db-btn').addEventListener('click', () => {
      if (confirm('Reset CraftCaller to initial factory settings and database?')) {
        localStorage.removeItem('craftcaller_db');
        localStorage.removeItem('craftcaller_logs');
        localStorage.removeItem('craftcaller_messages');
        localStorage.removeItem('craftcaller_blocklist');
        location.reload();
      }
    });

    // Keyboard Shortcuts (Esc to close modals)
    window.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        document.querySelectorAll('.fixed.inset-0:not(.hidden)').forEach(modal => {
          modal.classList.add('hidden');
        });
        stopRingtone();
      }
    });

    // ==========================================
    // 14. APPLICATION INITIALIZATION
    // ==========================================
    window.addEventListener('DOMContentLoaded', () => {
      renderCallLogs();
      renderMessages();
      renderBlocklist();
      renderSpamFeed();
      renderContacts();

      // Initial sample lookup to showcase caller card
      const initialResults = searchDirectory('+91 98765 43210');
      renderSearchResults(initialResults, '+91 98765 43210');
    });
  </script>
</body>
</html>
'''

print("Scripts Part 3 prepared.")
