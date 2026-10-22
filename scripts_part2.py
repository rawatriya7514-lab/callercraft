# -------------------------------------------------------------
# 7. JAVASCRIPT ENGINE - PART 2: UI LOGIC, DIALER, SMS & SIMULATOR
# -------------------------------------------------------------

SCRIPTS_PART2_JS = '''
    // ==========================================
    // 4. UI NAVIGATION & TAB SWITCHING
    // ==========================================
    const tabs = document.querySelectorAll('.nav-tab');
    const mobileNavBtns = document.querySelectorAll('.mobile-nav-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    function switchTab(targetId) {
      tabContents.forEach(c => {
        if (c.id === targetId) {
          c.classList.remove('hidden');
          c.classList.add('block');
        } else {
          c.classList.add('hidden');
          c.classList.remove('block');
        }
      });

      tabs.forEach(t => {
        if (t.dataset.target === targetId) {
          t.classList.add('border-cc-blue', 'text-cc-blue', 'dark:text-blue-400');
          t.classList.remove('border-transparent', 'text-slate-600', 'dark:text-slate-400');
        } else {
          t.classList.remove('border-cc-blue', 'text-cc-blue', 'dark:text-blue-400');
          t.classList.add('border-transparent', 'text-slate-600', 'dark:text-slate-400');
        }
      });

      mobileNavBtns.forEach(btn => {
        if (btn.dataset.target === targetId) {
          btn.classList.add('text-cc-blue', 'dark:text-blue-400');
          btn.classList.remove('text-slate-500', 'dark:text-slate-400');
        } else {
          btn.classList.remove('text-cc-blue', 'dark:text-blue-400');
          btn.classList.add('text-slate-500', 'dark:text-slate-400');
        }
      });

      window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    tabs.forEach(tab => {
      tab.addEventListener('click', () => switchTab(tab.dataset.target));
    });

    mobileNavBtns.forEach(btn => {
      btn.addEventListener('click', () => switchTab(btn.dataset.target));
    });

    document.getElementById('brand-logo-btn').addEventListener('click', () => {
      switchTab('tab-search');
    });

    document.getElementById('open-call-simulator-btn').addEventListener('click', () => {
      triggerIncomingCall({
        name: 'SBI KYC Reward Scam',
        number: '+91 98765 43210',
        type: 'spam',
        category: 'Financial Fraud',
        reports: 14820,
        location: 'New Delhi, India',
        carrier: 'Vi Telephony'
      });
    });

    // Subtabs in Calls
    const subtabHistoryBtn = document.getElementById('subtab-history-btn');
    const subtabDialerBtn = document.getElementById('subtab-dialer-btn');
    const callHistoryView = document.getElementById('call-history-view');
    const keypadDialerView = document.getElementById('keypad-dialer-view');

    subtabHistoryBtn.addEventListener('click', () => {
      callHistoryView.classList.remove('hidden');
      keypadDialerView.classList.add('hidden');
      subtabHistoryBtn.className = 'flex-1 sm:flex-none px-5 py-2.5 rounded-lg text-xs sm:text-sm font-bold bg-cc-blue text-white shadow-sm transition-all flex items-center justify-center gap-2';
      subtabDialerBtn.className = 'flex-1 sm:flex-none px-5 py-2.5 rounded-lg text-xs sm:text-sm font-bold text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-cc-borderDark transition-all flex items-center justify-center gap-2';
    });

    subtabDialerBtn.addEventListener('click', () => {
      keypadDialerView.classList.remove('hidden');
      callHistoryView.classList.add('hidden');
      subtabDialerBtn.className = 'flex-1 sm:flex-none px-5 py-2.5 rounded-lg text-xs sm:text-sm font-bold bg-cc-blue text-white shadow-sm transition-all flex items-center justify-center gap-2';
      subtabHistoryBtn.className = 'flex-1 sm:flex-none px-5 py-2.5 rounded-lg text-xs sm:text-sm font-bold text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-cc-borderDark transition-all flex items-center justify-center gap-2';
    });

    // Theme & Sound & Mockup Controls
    const themeBtn = document.getElementById('toggle-theme-btn');
    themeBtn.addEventListener('click', () => {
      document.documentElement.classList.toggle('dark');
      try {
        localStorage.setItem('cc_theme', document.documentElement.classList.contains('dark') ? 'dark' : 'light');
      } catch (e) {}
    });

    try {
      if (localStorage.getItem('cc_theme') === 'dark' || (!localStorage.getItem('cc_theme') && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
        document.documentElement.classList.add('dark');
      }
    } catch (e) {}

    const soundBtn = document.getElementById('toggle-sound-btn');
    const soundOnIcon = document.getElementById('sound-on-icon');
    const soundOffIcon = document.getElementById('sound-off-icon');

    soundBtn.addEventListener('click', () => {
      soundEnabled = !soundEnabled;
      if (soundEnabled) {
        soundOnIcon.classList.remove('hidden');
        soundOffIcon.classList.add('hidden');
        showToast('Sound effects enabled (DTMF & Ringtones)', 'info');
        playBeep(800, 0.08);
      } else {
        soundOnIcon.classList.add('hidden');
        soundOffIcon.classList.remove('hidden');
        stopRingtone();
        showToast('Sound muted', 'info');
      }
    });

    // Mobile Viewport Bezel Toggle
    const viewmodeBtn = document.getElementById('toggle-viewmode-btn');
    const appContainer = document.getElementById('app-view-container');
    let isPhoneFrame = false;

    viewmodeBtn.addEventListener('click', () => {
      isPhoneFrame = !isPhoneFrame;
      if (isPhoneFrame) {
        appContainer.classList.add('max-w-md', 'phone-bezel', 'rounded-[44px]', 'p-4', 'bg-white', 'dark:bg-cc-darkBg', 'my-6');
        showToast('Switched to Smartphone Frame Preview', 'info');
      } else {
        appContainer.classList.remove('max-w-md', 'phone-bezel', 'rounded-[44px]', 'p-4', 'bg-white', 'dark:bg-cc-darkBg', 'my-6');
        showToast('Switched to Full Dashboard Mode', 'info');
      }
    });

    // ==========================================
    // 5. SEARCH & REVERSE LOOKUP ENGINE
    // ==========================================
    function sanitizeNum(n) {
      return (n || '').replace(/[^0-9]/g, '');
    }

    function searchDirectory(query) {
      if (!query || !query.trim()) return [];
      const q = query.trim().toLowerCase();
      const qDigits = sanitizeNum(q);

      const matches = db.filter(item => {
        const nameMatch = item.name.toLowerCase().includes(q);
        const catMatch = item.category.toLowerCase().includes(q);
        const numMatch = qDigits.length >= 3 && sanitizeNum(item.number).includes(qDigits);
        return nameMatch || catMatch || numMatch;
      });

      // Deterministic CallerCraft AI Inference for unlisted numbers
      if (matches.length === 0 && qDigits.length >= 6) {
        const isSpamGuess = qDigits.endsWith('0') || qDigits.startsWith('800') || qDigits.includes('999') || qDigits.startsWith('140');
        let operator = 'Jio Telecom';
        let circle = 'New Delhi, India';
        const prefix = qDigits.slice(-10, -8);
        if (prefix === '98' || prefix === '94') { operator = 'Airtel India'; circle = 'Mumbai, Maharashtra'; }
        else if (prefix === '91' || prefix === '80') { operator = 'Vi Cellular'; circle = 'Bangalore, Karnataka'; }
        else if (prefix === '99') { operator = 'BSNL Mobile'; circle = 'Uttar Pradesh (East)'; }

        matches.push({
          id: 'gen-' + qDigits,
          name: isSpamGuess ? 'Suspected Telemarketer / Robocall' : 'Private Caller ID',
          number: query.startsWith('+') ? query : '+91 ' + qDigits.slice(-10),
          cleanNumber: qDigits,
          type: isSpamGuess ? 'spam' : 'safe',
          category: isSpamGuess ? 'Telemarketing / Automated Call' : 'Individual Subscriber',
          reports: isSpamGuess ? 324 : 0,
          carrier: operator,
          location: circle,
          safeScore: isSpamGuess ? 18 : 88,
          verified: false,
          gold: false,
          comments: isSpamGuess ? [{ user: 'Craft Community Flag', text: 'Multiple automated auto-dialer attempts reported.', time: 'Recent' }] : []
        });
      }

      return matches;
    }

    function renderSearchResults(results, query) {
      const container = document.getElementById('search-result-container');
      if (!results || results.length === 0) {
        container.innerHTML = `
          <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-8 text-center">
            <div class="w-16 h-16 mx-auto mb-3 rounded-2xl bg-slate-100 dark:bg-cc-darkBg flex items-center justify-center text-slate-400">
              <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            </div>
            <h3 class="text-lg font-bold text-slate-800 dark:text-slate-200">No match found for "${query}"</h3>
            <p class="text-xs text-slate-500 mt-1">Try entering a 10-digit number like 9876543210 or try clicking one of the trending chips above.</p>
          </div>
        `;
        return;
      }

      container.innerHTML = results.map(item => {
        const isSpam = item.type === 'spam';
        const isVerified = item.verified || item.type === 'verified';
        const isGold = item.gold || item.type === 'gold';
        const riskPercent = isSpam ? (100 - item.safeScore) : (100 - (item.safeScore || 95));

        let badgeHtml = '';
        if (isSpam) {
          badgeHtml = `<span class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-red-100 dark:bg-red-950/80 text-red-600 dark:text-red-400 flex items-center gap-1 border border-red-200 dark:border-red-900">
            <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"></path></svg>
            🚨 High Risk Fraud (${item.reports.toLocaleString()} reports)
          </span>`;
        } else if (isGold) {
          badgeHtml = `<span class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-100 dark:bg-amber-950/80 text-amber-600 dark:text-amber-400 flex items-center gap-1 border border-amber-300 dark:border-amber-800">
            👑 CallerCraft Gold VIP
          </span>`;
        } else if (isVerified) {
          badgeHtml = `<span class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-100 dark:bg-blue-950/80 text-cc-blue dark:text-blue-400 flex items-center gap-1 border border-blue-200 dark:border-blue-900">
            <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
            Verified Enterprise
          </span>`;
        } else {
          badgeHtml = `<span class="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
            Safe Caller
          </span>`;
        }

        const avatarBg = isSpam ? 'bg-red-500 text-white' : (isGold ? 'bg-gradient-to-tr from-amber-400 to-yellow-600 text-white' : 'bg-cc-blue text-white');
        const initial = isSpam ? '!' : (item.name.charAt(0) || '?');

        // Circular SVG Risk Gauge
        const strokeDash = 2 * Math.PI * 22;
        const offset = strokeDash - (strokeDash * (riskPercent / 100));
        const meterColor = isSpam ? '#EF4444' : (isGold ? '#F59E0B' : '#10B981');

        return `
          <div class="bg-white dark:bg-cc-cardDark border ${isSpam ? 'border-red-300 dark:border-red-900/60 shadow-red-500/5' : 'border-slate-200 dark:border-cc-borderDark'} rounded-3xl p-5 sm:p-6 shadow-md transition-all">
            <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
              <div class="flex items-start gap-4">
                <div class="w-14 h-14 rounded-2xl ${avatarBg} flex items-center justify-center font-extrabold text-2xl shadow-sm flex-shrink-0">
                  ${initial}
                </div>
                <div>
                  <div class="flex flex-wrap items-center gap-2">
                    <h3 class="text-xl font-extrabold text-slate-900 dark:text-white tracking-tight">${item.name}</h3>
                    ${badgeHtml}
                  </div>
                  <div class="text-base font-bold text-slate-700 dark:text-slate-300 font-mono mt-0.5">${item.number}</div>
                  <div class="text-xs text-slate-500 flex flex-wrap items-center gap-2 mt-1">
                    <span class="font-semibold text-cc-blue dark:text-blue-400">${item.carrier}</span>
                    <span>•</span>
                    <span>${item.location}</span>
                    <span>•</span>
                    <span class="font-medium text-slate-700 dark:text-slate-300">${item.category}</span>
                  </div>
                </div>
              </div>

              <!-- Risk Score Meter & Actions -->
              <div class="flex items-center gap-4 flex-wrap">
                <div class="flex items-center gap-2.5 bg-slate-50 dark:bg-cc-darkBg px-3 py-2 rounded-2xl border border-slate-200/60 dark:border-cc-borderDark/60">
                  <svg class="w-12 h-12 transform -rotate-90">
                    <circle cx="24" cy="24" r="20" stroke="currentColor" stroke-width="4" fill="transparent" class="text-slate-200 dark:text-slate-800"/>
                    <circle cx="24" cy="24" r="20" stroke="${meterColor}" stroke-width="4" stroke-linecap="round" fill="transparent" stroke-dasharray="125.6" stroke-dashoffset="${125.6 - (125.6 * (riskPercent / 100))}"/>
                  </svg>
                  <div>
                    <div class="text-sm font-extrabold text-slate-900 dark:text-white">${riskPercent}% Risk</div>
                    <div class="text-[10px] text-slate-400 font-medium">${isSpam ? 'Threat Level High' : 'Trusted & Safe'}</div>
                  </div>
                </div>

                <div class="flex items-center gap-1.5 flex-wrap">
                  <button class="call-action-btn px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-sm flex items-center gap-1.5 transition-all"
                    data-name="${item.name}" data-number="${item.number}" data-type="${item.type}">
                    <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path d="M2 3a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 4V3z"></path></svg>
                    Call
                  </button>

                  <a href="https://wa.me/${sanitizeNum(item.number)}" target="_blank" class="px-3.5 py-2 bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold rounded-xl shadow-sm flex items-center gap-1.5 transition-all">
                    WhatsApp
                  </a>

                  <button class="sms-action-btn px-3.5 py-2 bg-slate-100 dark:bg-cc-darkBg hover:bg-slate-200 dark:hover:bg-cc-borderDark text-slate-700 dark:text-slate-300 text-xs font-bold rounded-xl transition-all"
                    data-name="${item.name}" data-number="${item.number}">
                    SMS
                  </button>

                  <button class="block-toggle-btn px-3.5 py-2 ${isSpam ? 'bg-slate-100 dark:bg-cc-darkBg text-slate-700 dark:text-slate-300' : 'bg-red-50 dark:bg-red-950/60 text-red-600 dark:text-red-400'} text-xs font-bold rounded-xl transition-all"
                    data-number="${item.number}">
                    ${isSpam ? 'Unblock' : 'Block Number'}
                  </button>

                  <button class="simulate-card-call-btn px-3.5 py-2 bg-cc-blue hover:bg-cc-darkBlue text-white text-xs font-bold rounded-xl shadow-sm transition-all"
                    data-name="${item.name}" data-number="${item.number}" data-type="${item.type}" data-carrier="${item.carrier}" data-location="${item.location}" data-category="${item.category}" data-reports="${item.reports}">
                    Simulate Call
                  </button>
                </div>
              </div>
            </div>

            <!-- Community Comments Section -->
            <div class="mt-5 pt-4 border-t border-slate-100 dark:border-cc-borderDark">
              <h4 class="text-xs font-bold text-slate-700 dark:text-slate-300 flex items-center gap-1.5 mb-2.5">
                <svg class="w-4 h-4 text-cc-blue" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/></svg>
                Community Experience Reports (${(item.comments || []).length})
              </h4>
              <div class="space-y-2">
                ${(item.comments && item.comments.length > 0) ? item.comments.map(c => `
                  <div class="p-3 rounded-2xl bg-slate-50 dark:bg-cc-darkBg border border-slate-200/60 dark:border-cc-borderDark/60 text-xs">
                    <div class="flex items-center justify-between font-bold text-slate-800 dark:text-slate-200 mb-1">
                      <span>${c.user}</span>
                      <span class="text-[10px] text-slate-400 font-normal">${c.time}</span>
                    </div>
                    <p class="text-slate-600 dark:text-slate-400">${c.text}</p>
                  </div>
                `).join('') : '<p class="text-xs text-slate-400 italic">No community complaints filed for this number. Looks safe!</p>'}
              </div>
            </div>
          </div>
        `;
      }).join('');

      // Wire up card action buttons
      container.querySelectorAll('.call-action-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          makeOutgoingCall(btn.dataset.name, btn.dataset.number, btn.dataset.type);
        });
      });

      container.querySelectorAll('.sms-action-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          openComposeSMS(btn.dataset.number);
        });
      });

      container.querySelectorAll('.simulate-card-call-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          triggerIncomingCall({
            name: btn.dataset.name,
            number: btn.dataset.number,
            type: btn.dataset.type,
            carrier: btn.dataset.carrier,
            location: btn.dataset.location,
            category: btn.dataset.category,
            reports: btn.dataset.reports
          });
        });
      });

      container.querySelectorAll('.block-toggle-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          toggleBlockNumber(btn.dataset.number);
        });
      });
    }

    function toggleBlockNumber(num) {
      if (!num) return;
      const idx = customBlocklist.indexOf(num);
      if (idx > -1) {
        customBlocklist.splice(idx, 1);
        showToast(`Number ${num} removed from blocklist`, 'info');
      } else {
        customBlocklist.push(num);
        showToast(`Number ${num} added to blocklist`, 'spam');
      }
      saveBlocklist();
      renderBlocklist();
    }

    function executeSearch(query) {
      if (!query || !query.trim()) return;
      const cc = document.getElementById('search-country-code').value;
      let clean = query.trim();
      if (!clean.startsWith('+') && !isNaN(clean.charAt(0))) {
        clean = cc + ' ' + clean;
      }
      const results = searchDirectory(clean);
      renderSearchResults(results, clean);
      switchTab('tab-search');
      window.scrollTo({ top: 380, behavior: 'smooth' });
    }

    document.getElementById('main-search-btn').addEventListener('click', () => {
      executeSearch(document.getElementById('main-search-input').value);
    });

    document.getElementById('main-search-input').addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        executeSearch(e.target.value);
      }
    });

    document.getElementById('header-search-btn').addEventListener('click', () => {
      executeSearch(document.getElementById('header-search-input').value);
    });

    document.getElementById('header-search-input').addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        executeSearch(e.target.value);
      }
    });

    document.querySelectorAll('.quick-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        executeSearch(chip.dataset.query);
      });
    });

    // ==========================================
    // 6. CALL HISTORY & T9 SMART DIALER
    // ==========================================
    let currentCallFilter = 'all';

    function renderCallLogs() {
      const container = document.getElementById('call-logs-list');
      const searchQ = (document.getElementById('call-logs-search').value || '').toLowerCase();

      const filtered = callLogs.filter(log => {
        if (searchQ && !log.name.toLowerCase().includes(searchQ) && !log.number.includes(searchQ)) {
          return false;
        }
        if (currentCallFilter === 'all') return true;
        if (currentCallFilter === 'incoming') return log.callType === 'incoming';
        if (currentCallFilter === 'outgoing') return log.callType === 'outgoing';
        if (currentCallFilter === 'missed') return log.callType === 'missed';
        if (currentCallFilter === 'spam') return log.type === 'spam' || log.callType === 'blocked';
        return true;
      });

      const missedCount = callLogs.filter(l => l.callType === 'missed').length;
      const missedBadge = document.getElementById('badge-missed-count');
      missedBadge.textContent = missedCount;
      if (missedCount === 0) missedBadge.classList.add('hidden');
      else missedBadge.classList.remove('hidden');

      if (filtered.length === 0) {
        container.innerHTML = `
          <div class="p-8 text-center text-slate-400">
            <svg class="w-10 h-10 mx-auto mb-2 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"/></svg>
            <p class="font-bold text-xs sm:text-sm">No calls matching filter</p>
          </div>
        `;
        return;
      }

      container.innerHTML = filtered.map(log => {
        const isSpam = log.type === 'spam' || log.callType === 'blocked';
        let callIcon = '';
        if (log.callType === 'incoming') {
          callIcon = '<svg class="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3"/></svg>';
        } else if (log.callType === 'outgoing') {
          callIcon = '<svg class="w-4 h-4 text-cc-blue" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18"/></svg>';
        } else if (log.callType === 'missed') {
          callIcon = '<svg class="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 8l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2M5 3a2 2 0 00-2 2v1c0 8.284 6.716 15 15 15h1a2 2 0 002-2v-3.28a1 1 0 00-.684-.948l-4.493-1.498a1 1 0 00-1.21.502l-1.13 2.257a11.042 11.042 0 01-5.516-5.517l2.257-1.128a1 1 0 00.502-1.21L9.228 3.683A1 1 0 008.279 3H5z"/></svg>';
        } else {
          callIcon = '<svg class="w-4 h-4 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/></svg>';
        }

        const avatarInitial = isSpam ? '!' : log.name.charAt(0);
        const avatarBg = isSpam ? 'bg-red-500 text-white' : 'bg-slate-200 dark:bg-cc-darkBg text-slate-800 dark:text-slate-200';

        return `
          <div class="p-3.5 sm:p-4 hover:bg-slate-50 dark:hover:bg-slate-800/40 flex items-center justify-between gap-3 transition-colors">
            <div class="flex items-center gap-3 min-w-0">
              <div class="w-10 h-10 rounded-2xl ${avatarBg} font-extrabold flex items-center justify-center flex-shrink-0 text-sm">
                ${avatarInitial}
              </div>
              <div class="min-w-0">
                <div class="flex items-center gap-1.5 truncate">
                  <span class="font-bold text-xs sm:text-sm text-slate-900 dark:text-white truncate">${log.name}</span>
                  ${isSpam ? '<span class="text-[9px] font-bold px-1.5 py-0.2 rounded bg-red-100 dark:bg-red-950 text-red-600 dark:text-red-400">SPAM</span>' : ''}
                </div>
                <div class="flex items-center gap-1.5 text-[11px] text-slate-500 mt-0.5">
                  <span class="flex-shrink-0">${callIcon}</span>
                  <span class="font-mono text-slate-600 dark:text-slate-400">${log.number}</span>
                  <span>•</span>
                  <span>${log.time}</span>
                  <span>(${log.duration})</span>
                </div>
              </div>
            </div>

            <div class="flex items-center gap-1.5 flex-shrink-0">
              <button class="log-call-back-btn p-2 rounded-xl bg-slate-100 dark:bg-cc-darkBg text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-950/60 transition-colors"
                data-name="${log.name}" data-number="${log.number}" data-type="${log.type}" title="Call Back">
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path d="M2 3a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 4V3z"></path></svg>
              </button>
              <button class="log-info-btn p-2 rounded-xl bg-slate-100 dark:bg-cc-darkBg text-cc-blue hover:bg-blue-50 dark:hover:bg-blue-950/60 transition-colors"
                data-number="${log.number}" title="Lookup Caller ID">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
              </button>
            </div>
          </div>
        `;
      }).join('');

      container.querySelectorAll('.log-call-back-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          makeOutgoingCall(btn.dataset.name, btn.dataset.number, btn.dataset.type);
        });
      });

      container.querySelectorAll('.log-info-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          executeSearch(btn.dataset.number);
        });
      });
    }

    document.querySelectorAll('.call-filter-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        document.querySelectorAll('.call-filter-chip').forEach(c => {
          c.classList.remove('bg-cc-blue', 'text-white');
          c.classList.add('bg-slate-200', 'dark:bg-cc-cardDark', 'text-slate-700', 'dark:text-slate-300');
        });
        chip.classList.add('bg-cc-blue', 'text-white');
        chip.classList.remove('bg-slate-200', 'dark:bg-cc-cardDark', 'text-slate-700', 'dark:text-slate-300');
        currentCallFilter = chip.dataset.filter;
        renderCallLogs();
      });
    });

    document.getElementById('call-logs-search').addEventListener('input', renderCallLogs);

    document.getElementById('clear-call-logs-btn').addEventListener('click', () => {
      callLogs = [];
      saveLogs();
      renderCallLogs();
      showToast('Call history cleared', 'info');
    });

    // Export Call Logs CSV
    document.getElementById('export-call-logs-btn').addEventListener('click', () => {
      if (callLogs.length === 0) {
        showToast('No call logs to export', 'error');
        return;
      }
      let csv = 'ID,Name,Phone Number,Type,Category,Call Direction,Time,Duration\\n';
      callLogs.forEach(l => {
        csv += `"${l.id}","${l.name}","${l.number}","${l.type}","${l.category || ''}","${l.callType}","${l.time}","${l.duration}"\\n`;
      });
      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `callercraft_call_logs_${Date.now()}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      showToast('Exported call logs to CSV file!', 'success');
    });

    // T9 Keypad Dialer Logic
    const dialerDisplay = document.getElementById('dialer-display');
    const dialerMatched = document.getElementById('dialer-matched-contact');
    let dialerBuffer = '';

    const T9_MAP = {
      '2': 'abc', '3': 'def', '4': 'ghi', '5': 'jkl',
      '6': 'mno', '7': 'pqrs', '8': 'tuv', '9': 'wxyz'
    };

    function updateDialerMatch() {
      if (!dialerBuffer) {
        dialerMatched.innerHTML = '<span class="text-slate-400 font-normal">Type digits to search contacts (T9)</span>';
        return;
      }

      // Check number match or T9 letter match
      const cleanBuf = dialerBuffer;
      const numMatch = db.find(d => sanitizeNum(d.number).includes(cleanBuf));
      
      // Match by name
      const nameMatch = db.find(d => {
        const cleanName = d.name.toLowerCase().replace(/[^a-z]/g, '');
        // translate name characters to T9
        let t9Str = '';
        for (let char of cleanName) {
          for (let key in T9_MAP) {
            if (T9_MAP[key].includes(char)) {
              t9Str += key;
              break;
            }
          }
        }
        return t9Str.includes(cleanBuf);
      });

      const matchedContact = numMatch || nameMatch;

      if (matchedContact) {
        dialerMatched.innerHTML = `
          <div class="flex items-center justify-between w-full">
            <span class="truncate font-bold">${matchedContact.name}</span>
            <span class="font-mono text-[10px] text-slate-400">${matchedContact.number}</span>
          </div>
        `;
      } else {
        dialerMatched.innerHTML = `<span class="text-slate-400">CallerCraft Smart ID Ready</span>`;
      }
    }

    document.querySelectorAll('.keypad-key').forEach(key => {
      key.addEventListener('click', () => {
        const digit = key.dataset.digit;
        playTone(digit);
        dialerBuffer += digit;
        dialerDisplay.value = dialerBuffer;
        updateDialerMatch();
      });
    });

    document.getElementById('dialer-backspace-btn').addEventListener('click', () => {
      playBeep(300, 0.05);
      dialerBuffer = dialerBuffer.slice(0, -1);
      dialerDisplay.value = dialerBuffer;
      updateDialerMatch();
    });

    document.getElementById('dialer-call-action-btn').addEventListener('click', () => {
      if (!dialerBuffer) {
        showToast('Please dial a number first', 'error');
        return;
      }
      makeOutgoingCall('Dialed Number', dialerBuffer, 'safe');
    });
'''

print("Scripts Part 2 prepared.")
