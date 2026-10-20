# -------------------------------------------------------------
# 5. MODALS & CALL OVERLAYS
# -------------------------------------------------------------

MODALS_HTML = '''
    </div>
  </main>

  <!-- ======================================================== -->
  <!-- LIVE CRAFTCALLER INCOMING CALL OVERLAY (FULL SCREEN)     -->
  <!-- ======================================================== -->
  <div id="incoming-call-modal" class="fixed inset-0 z-50 hidden bg-slate-950/85 backdrop-blur-md flex items-center justify-center p-3 sm:p-6 transition-all duration-300">
    <div id="incoming-call-card" class="relative w-full max-w-sm rounded-[36px] overflow-hidden shadow-2xl transition-all transform scale-95 duration-300 border-4 border-slate-800 bg-slate-900 text-white">
      
      <!-- Top CraftCaller Brand Header -->
      <div class="pt-5 px-6 pb-2 flex items-center justify-between text-xs text-slate-400 border-b border-white/10">
        <div class="flex items-center gap-1.5">
          <span class="w-2.5 h-2.5 rounded-full bg-cc-blue animate-pulse"></span>
          <span class="font-extrabold tracking-wide text-white">CraftCaller</span>
          <span class="text-[9px] px-1.5 py-0.2 rounded bg-blue-500/20 text-blue-300 font-bold">SMART ID</span>
        </div>
        <div class="flex items-center gap-2">
          <span id="call-sim-timer" class="text-emerald-400 font-mono font-bold hidden">00:00</span>
          <button id="close-modal-x-btn" class="text-slate-400 hover:text-white transition-colors" title="Dismiss">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
      </div>

      <!-- Caller ID Body -->
      <div class="p-6 text-center space-y-4">
        
        <!-- Spam Badge Banner (shown if spam) -->
        <div id="incoming-spam-banner" class="hidden py-1.5 px-3 rounded-full bg-red-600/30 border border-red-500/50 text-red-300 text-xs font-bold flex items-center justify-center gap-1.5">
          <svg class="w-4 h-4 text-red-400" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"></path></svg>
          <span id="incoming-spam-tag-text">WARNING: SUSPECTED SPAM / FRAUD</span>
        </div>

        <!-- Verified Badge Banner (shown if verified) -->
        <div id="incoming-verified-banner" class="hidden py-1.5 px-3 rounded-full bg-blue-600/30 border border-blue-400/50 text-blue-300 text-xs font-bold flex items-center justify-center gap-1.5">
          <svg class="w-4 h-4 text-blue-400" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M6.267 3.455a3.066 3.066 0 001.745-.723 3.066 3.066 0 013.976 0 3.066 3.066 0 001.745.723 3.066 3.066 0 012.812 2.812c.051.643.304 1.254.723 1.745a3.066 3.066 0 010 3.976 3.066 3.066 0 00-.723 1.745 3.066 3.066 0 01-2.812 2.812 3.066 3.066 0 00-1.745.723 3.066 3.066 0 01-3.976 0 3.066 3.066 0 00-1.745-.723 3.066 3.066 0 01-2.812-2.812zm7.44 5.252a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path></svg>
          <span id="incoming-verified-tag-text">VERIFIED BY CRAFTCALLER</span>
        </div>

        <!-- Gold VIP Banner -->
        <div id="incoming-gold-banner" class="hidden py-1.5 px-3 rounded-full bg-amber-500/20 border border-amber-400/40 text-amber-300 text-xs font-bold flex items-center justify-center gap-1.5">
          <span>👑 CRAFTCALLER GOLD VIP MEMBER</span>
        </div>

        <!-- Caller Avatar with Pulsing Radar Ripple -->
        <div class="relative inline-block my-2">
          <div id="incoming-radar-pulse" class="w-24 h-24 rounded-full flex items-center justify-center text-3xl font-extrabold shadow-lg transition-all radar-pulse-spam">
            <span id="incoming-caller-avatar-char">!</span>
          </div>
          <div id="incoming-verified-shield" class="absolute -bottom-1 -right-1 w-8 h-8 rounded-full bg-cc-blue text-white flex items-center justify-center border-2 border-slate-900 shadow-md">
            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path></svg>
          </div>
        </div>

        <!-- Caller Identity -->
        <div>
          <h3 id="incoming-caller-name" class="text-xl sm:text-2xl font-extrabold tracking-tight text-white">SBI KYC Reward Scam</h3>
          <p id="incoming-caller-number" class="text-sm font-semibold text-slate-300 mt-0.5 tracking-wider font-mono">+91 98765 43210</p>
          <div class="mt-2 flex items-center justify-center gap-2 text-xs text-slate-400">
            <span id="incoming-caller-carrier">Vi Telephony</span>
            <span>•</span>
            <span id="incoming-caller-location">New Delhi, India</span>
          </div>
        </div>

        <!-- Reports Counter Box -->
        <div id="incoming-reports-box" class="bg-red-950/50 border border-red-900/60 rounded-xl p-2.5 text-xs text-red-300">
          <span class="font-bold text-red-200" id="incoming-reports-count">14,820 people</span> reported this number as fraud
        </div>

        <!-- ACTIVE CALL CONTROLS (Shown when answered) -->
        <div id="in-call-active-controls" class="hidden pt-4 border-t border-slate-800 space-y-4">
          
          <!-- Waveform Visualizer for AI Call Recording -->
          <div id="in-call-recorder-wave" class="hidden flex items-center justify-center gap-1.5 h-8 bg-red-950/40 border border-red-800/40 rounded-xl px-3">
            <span class="w-2 h-2 rounded-full bg-red-500 animate-ping mr-1"></span>
            <span class="text-[10px] font-bold text-red-300 uppercase tracking-widest mr-2">Recording Call</span>
            <div class="w-1 bg-red-400 rounded-full h-3 bar-1"></div>
            <div class="w-1 bg-red-400 rounded-full h-5 bar-2"></div>
            <div class="w-1 bg-red-400 rounded-full h-4 bar-3"></div>
            <div class="w-1 bg-red-400 rounded-full h-6 bar-4"></div>
            <div class="w-1 bg-red-400 rounded-full h-3 bar-5"></div>
          </div>

          <div class="grid grid-cols-4 gap-2">
            <button id="in-call-mute-btn" class="flex flex-col items-center gap-1 p-2.5 rounded-2xl bg-slate-800 hover:bg-slate-700 transition-colors">
              <svg class="w-5 h-5 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"/></svg>
              <span class="text-[9px] text-slate-400">Mute</span>
            </button>
            <button id="in-call-speaker-btn" class="flex flex-col items-center gap-1 p-2.5 rounded-2xl bg-slate-800 hover:bg-slate-700 transition-colors">
              <svg class="w-5 h-5 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/></svg>
              <span class="text-[9px] text-slate-400">Speaker</span>
            </button>
            <button id="in-call-record-btn" class="flex flex-col items-center gap-1 p-2.5 rounded-2xl bg-slate-800 hover:bg-slate-700 transition-colors">
              <svg class="w-5 h-5 text-slate-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"/></svg>
              <span class="text-[9px] text-slate-400">Record</span>
            </button>
            <button id="in-call-block-report-btn" class="flex flex-col items-center gap-1 p-2.5 rounded-2xl bg-red-950/70 hover:bg-red-900/80 text-red-300 transition-colors">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/></svg>
              <span class="text-[9px]">Block</span>
            </button>
          </div>
        </div>

        <!-- RINGING INCOMING CALL ACTIONS (Decline vs Answer) -->
        <div id="incoming-call-actions" class="pt-4 flex items-center justify-around gap-2">
          
          <!-- Decline Button -->
          <div class="flex flex-col items-center gap-1.5">
            <button id="decline-call-btn" class="w-14 h-14 rounded-full bg-red-600 hover:bg-red-700 text-white flex items-center justify-center shadow-lg shadow-red-600/40 active:scale-90 transition-all">
              <svg class="w-7 h-7 rotate-[135deg]" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20.01 15.38c-1.23 0-2.42-.2-3.53-.56a.977.977 0 00-1.01.24l-1.57 1.97c-2.83-1.44-5.15-3.75-6.59-6.59l1.97-1.57c.28-.28.36-.67.25-1.02A11.36 11.36 0 018.96 4.3c0-.55-.45-1-1-1H4.3c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.62c0-.55-.45-1-.99-1z"/>
              </svg>
            </button>
            <span class="text-[11px] text-slate-400 font-semibold">Decline</span>
          </div>

          <!-- Quick Block & Decline (for scam callers) -->
          <div id="block-and-decline-wrapper" class="flex flex-col items-center gap-1.5">
            <button id="block-and-decline-btn" class="w-12 h-12 rounded-full bg-slate-800 hover:bg-red-950 text-red-400 border border-red-500/30 flex items-center justify-center active:scale-90 transition-all" title="Block & Reject">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/></svg>
            </button>
            <span class="text-[10px] text-red-400 font-semibold">Block</span>
          </div>

          <!-- AI Assistant Screening Action -->
          <div class="flex flex-col items-center gap-1.5">
            <button id="screen-call-ai-btn" class="w-12 h-12 rounded-full bg-purple-900 hover:bg-purple-800 text-purple-200 border border-purple-500/40 flex items-center justify-center active:scale-90 transition-all" title="Let AI Assistant Answer">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z"/></svg>
            </button>
            <span class="text-[10px] text-purple-300 font-semibold">AI Screen</span>
          </div>

          <!-- Answer Button -->
          <div class="flex flex-col items-center gap-1.5">
            <button id="answer-call-btn" class="w-14 h-14 rounded-full bg-emerald-500 hover:bg-emerald-600 text-white flex items-center justify-center shadow-lg shadow-emerald-500/40 active:scale-90 transition-all animate-bounce-subtle">
              <svg class="w-7 h-7" viewBox="0 0 24 24" fill="currentColor">
                <path d="M20.01 15.38c-1.23 0-2.42-.2-3.53-.56a.977.977 0 00-1.01.24l-1.57 1.97c-2.83-1.44-5.15-3.75-6.59-6.59l1.97-1.57c.28-.28.36-.67.25-1.02A11.36 11.36 0 018.96 4.3c0-.55-.45-1-1-1H4.3c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.62c0-.55-.45-1-.99-1z"/>
              </svg>
            </button>
            <span class="text-[11px] text-emerald-400 font-semibold">Answer</span>
          </div>

        </div>

        <!-- Hang Up Button (Active In-Call state) -->
        <div id="hang-up-active-wrapper" class="hidden pt-4 flex justify-center">
          <button id="hang-up-active-btn" class="w-16 h-16 rounded-full bg-red-600 hover:bg-red-700 text-white flex items-center justify-center shadow-lg shadow-red-600/40 active:scale-90 transition-all">
            <svg class="w-8 h-8 rotate-[135deg]" viewBox="0 0 24 24" fill="currentColor">
              <path d="M20.01 15.38c-1.23 0-2.42-.2-3.53-.56a.977.977 0 00-1.01.24l-1.57 1.97c-2.83-1.44-5.15-3.75-6.59-6.59l1.97-1.57c.28-.28.36-.67.25-1.02A11.36 11.36 0 018.96 4.3c0-.55-.45-1-1-1H4.3c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.62c0-.55-.45-1-.99-1z"/>
            </svg>
          </button>
        </div>

      </div>

    </div>
  </div>

  <!-- ======================================================== -->
  <!-- FLOATING HEADS-UP CALLER ID BANNER (ANDROID HEADS UP)     -->
  <!-- ======================================================== -->
  <div id="floating-caller-banner" class="fixed top-4 inset-x-4 sm:max-w-md sm:mx-auto z-50 hidden transition-all duration-300 transform -translate-y-24">
    <div class="bg-slate-900/95 text-white p-4 rounded-3xl shadow-2xl border border-slate-700 backdrop-blur-md flex items-center justify-between gap-3">
      <div class="flex items-center gap-3 min-w-0">
        <div id="floating-avatar" class="w-11 h-11 rounded-2xl bg-red-600 text-white flex items-center justify-center font-bold text-lg flex-shrink-0">
          !
        </div>
        <div class="min-w-0">
          <div class="flex items-center gap-1.5">
            <h4 id="floating-caller-name" class="font-bold text-sm truncate">SBI KYC Reward Scam</h4>
            <span id="floating-risk-tag" class="text-[9px] px-1.5 py-0.2 rounded font-bold bg-red-950 text-red-300 border border-red-800">SPAM</span>
          </div>
          <p id="floating-caller-number" class="text-xs text-slate-400 font-mono mt-0.5">+91 98765 43210</p>
        </div>
      </div>

      <div class="flex items-center gap-2 flex-shrink-0">
        <button id="floating-decline-btn" class="w-9 h-9 rounded-full bg-red-600 hover:bg-red-700 text-white flex items-center justify-center">
          <svg class="w-4 h-4 rotate-[135deg]" viewBox="0 0 24 24" fill="currentColor"><path d="M20.01 15.38c-1.23 0-2.42-.2-3.53-.56a.977.977 0 00-1.01.24l-1.57 1.97c-2.83-1.44-5.15-3.75-6.59-6.59l1.97-1.57c.28-.28.36-.67.25-1.02A11.36 11.36 0 018.96 4.3c0-.55-.45-1-1-1H4.3c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.62c0-.55-.45-1-.99-1z"/></svg>
        </button>
        <button id="floating-answer-btn" class="w-9 h-9 rounded-full bg-emerald-500 hover:bg-emerald-600 text-white flex items-center justify-center">
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor"><path d="M20.01 15.38c-1.23 0-2.42-.2-3.53-.56a.977.977 0 00-1.01.24l-1.57 1.97c-2.83-1.44-5.15-3.75-6.59-6.59l1.97-1.57c.28-.28.36-.67.25-1.02A11.36 11.36 0 018.96 4.3c0-.55-.45-1-1-1H4.3c-.55 0-1 .45-1 1 0 9.39 7.61 17 17 17 .55 0 1-.45 1-1v-3.62c0-.55-.45-1-.99-1z"/></svg>
        </button>
      </div>
    </div>
  </div>

  <!-- ======================================================== -->
  <!-- MODAL: SMS CONVERSATION THREAD VIEWER                    -->
  <!-- ======================================================== -->
  <div id="sms-thread-modal" class="fixed inset-0 z-50 hidden bg-slate-950/70 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4">
    <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl max-w-lg w-full shadow-2xl flex flex-col h-[85vh] max-h-[600px] overflow-hidden">
      
      <!-- Thread Header -->
      <div class="p-4 border-b border-slate-100 dark:border-cc-borderDark flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div id="sms-thread-avatar" class="w-10 h-10 rounded-2xl bg-cc-blue text-white font-bold flex items-center justify-center text-sm">
            HDFC
          </div>
          <div>
            <h3 id="sms-thread-sender" class="font-extrabold text-sm text-slate-900 dark:text-white">HDFC Bank Alert</h3>
            <p id="sms-thread-status" class="text-[10px] text-emerald-500 font-semibold">Verified Bank SMS Gateway</p>
          </div>
        </div>
        <button id="close-sms-thread-btn" class="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
        </button>
      </div>

      <!-- Messages Stream -->
      <div id="sms-thread-messages" class="flex-1 p-4 overflow-y-auto space-y-3 bg-slate-50 dark:bg-cc-darkBg">
        <!-- Message bubbles -->
      </div>

      <!-- Reply Composer -->
      <div class="p-3 border-t border-slate-100 dark:border-cc-borderDark flex items-center gap-2 bg-white dark:bg-cc-cardDark">
        <input type="text" id="sms-reply-input" placeholder="Type a message..." 
          class="flex-1 px-4 py-2.5 rounded-xl text-xs bg-slate-100 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white focus:outline-none focus:ring-1 focus:ring-cc-blue">
        <button id="sms-reply-send-btn" class="px-4 py-2.5 bg-cc-blue hover:bg-cc-darkBlue text-white font-bold text-xs rounded-xl shadow-sm transition-all flex items-center gap-1">
          <span>Send</span>
        </button>
      </div>

    </div>
  </div>

  <!-- ======================================================== -->
  <!-- MODAL: COMPOSE NEW SMS                                   -->
  <!-- ======================================================== -->
  <div id="compose-sms-modal" class="fixed inset-0 z-50 hidden bg-slate-950/70 backdrop-blur-sm flex items-center justify-center p-4">
    <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 max-w-md w-full shadow-2xl space-y-4">
      <div class="flex items-center justify-between border-b border-slate-100 dark:border-cc-borderDark pb-3">
        <h3 class="font-extrabold text-base text-slate-900 dark:text-white">Compose New Message</h3>
        <button id="close-compose-sms-btn" class="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
        </button>
      </div>

      <div class="space-y-3">
        <div>
          <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Recipient Number or Contact</label>
          <input type="text" id="compose-sms-recipient" placeholder="+91 98200 12345 or Aarav" class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white">
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Message Text</label>
          <textarea id="compose-sms-body" rows="3" placeholder="Type your message here..." class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white"></textarea>
        </div>
      </div>

      <div class="flex gap-2 pt-2">
        <button id="cancel-compose-sms-btn" class="flex-1 py-2.5 rounded-xl border border-slate-200 dark:border-cc-borderDark font-semibold text-xs text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-cc-darkBg">
          Cancel
        </button>
        <button id="send-compose-sms-btn" class="flex-1 py-2.5 bg-cc-blue hover:bg-cc-darkBlue text-white font-bold text-xs rounded-xl shadow-md transition-all">
          Send SMS
        </button>
      </div>
    </div>
  </div>

  <!-- ======================================================== -->
  <!-- MODAL: REPORT SPAM & PHISHING                            -->
  <!-- ======================================================== -->
  <div id="report-spam-modal" class="fixed inset-0 z-50 hidden bg-slate-950/70 backdrop-blur-sm flex items-center justify-center p-4">
    <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 max-w-md w-full shadow-2xl space-y-4">
      <div class="flex items-center justify-between border-b border-slate-100 dark:border-cc-borderDark pb-3">
        <div class="flex items-center gap-2">
          <span class="w-3 h-3 rounded-full bg-cc-spamRed"></span>
          <h3 class="font-extrabold text-base text-slate-900 dark:text-white">Report Number to Community</h3>
        </div>
        <button id="close-report-modal-btn" class="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
        </button>
      </div>

      <div class="space-y-3">
        <div>
          <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Phone Number *</label>
          <input type="text" id="report-input-number" placeholder="+91 98765 00000" class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white font-mono">
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Caller / Business Name (Optional)</label>
          <input type="text" id="report-input-name" placeholder="e.g. Loan Agent, Fake Electricity SMS" class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white">
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Fraud Classification *</label>
          <select id="report-input-category" class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white">
            <option value="Financial Fraud">💳 Banking, Credit Card & OTP Scam</option>
            <option value="Courier Extortion / Digital Arrest">⚠️ Courier Impersonation / Digital Arrest</option>
            <option value="Utility Disconnection Phishing">⚡ Electricity / Gas Disconnection Threat</option>
            <option value="Telemarketing">☎ Telemarketing & Unsolicited Sales</option>
            <option value="Lottery & Job Scam">🎁 Fake Job Offer or Lottery Prize</option>
            <option value="Harassment / Robocall">🤖 Automated Auto-dialer / Robocall</option>
          </select>
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">What happened? (Help other users)</label>
          <textarea id="report-input-comment" rows="2" placeholder="e.g. Asked me for card CVV or electricity unpaid bill link..." class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white"></textarea>
        </div>
      </div>

      <div class="flex gap-2 pt-2">
        <button id="cancel-report-btn" class="flex-1 py-2.5 rounded-xl border border-slate-200 dark:border-cc-borderDark font-semibold text-xs text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-cc-darkBg">
          Cancel
        </button>
        <button id="submit-report-btn" class="flex-1 py-2.5 bg-cc-spamRed hover:bg-red-600 text-white font-bold text-xs rounded-xl shadow-md transition-all">
          Submit Report & Block
        </button>
      </div>
    </div>
  </div>

  <!-- ======================================================== -->
  <!-- MODAL: ADD / EDIT CONTACT                                -->
  <!-- ======================================================== -->
  <div id="add-contact-modal" class="fixed inset-0 z-50 hidden bg-slate-950/70 backdrop-blur-sm flex items-center justify-center p-4">
    <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 max-w-md w-full shadow-2xl space-y-4">
      <div class="flex items-center justify-between border-b border-slate-100 dark:border-cc-borderDark pb-3">
        <h3 id="contact-modal-title" class="font-extrabold text-base text-slate-900 dark:text-white">Save Contact</h3>
        <button id="close-add-contact-btn" class="p-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
        </button>
      </div>

      <div class="space-y-3">
        <div>
          <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Full Name *</label>
          <input type="text" id="contact-input-name" placeholder="John Doe" class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white">
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Phone Number *</label>
          <input type="text" id="contact-input-phone" placeholder="+91 98765 43210" class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white font-mono">
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Telecom Carrier / Label</label>
          <input type="text" id="contact-input-carrier" placeholder="Jio 5G / Airtel / Work" class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white">
        </div>
        <div>
          <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Location / Circle</label>
          <input type="text" id="contact-input-location" placeholder="Mumbai, Maharashtra" class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white">
        </div>
        <div class="flex items-center gap-2 pt-1">
          <input type="checkbox" id="contact-input-gold" class="w-4 h-4 rounded text-amber-500 focus:ring-amber-400">
          <label for="contact-input-gold" class="text-xs font-bold text-slate-700 dark:text-slate-300 cursor-pointer">
            Mark as CraftCaller Gold VIP Member ⭐
          </label>
        </div>
      </div>

      <div class="flex gap-2 pt-2">
        <button id="cancel-add-contact-btn" class="flex-1 py-2.5 rounded-xl border border-slate-200 dark:border-cc-borderDark font-semibold text-xs text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-cc-darkBg">
          Cancel
        </button>
        <button id="save-new-contact-btn" class="flex-1 py-2.5 bg-cc-blue hover:bg-cc-darkBlue text-white font-bold text-xs rounded-xl shadow-md transition-all">
          Save Contact
        </button>
      </div>
    </div>
  </div>

  <!-- TOAST NOTIFICATION CONTAINER -->
  <div id="toast-container" class="fixed bottom-6 right-6 z-50 flex flex-col gap-2 pointer-events-none"></div>
'''

print("Modals HTML ready.")
