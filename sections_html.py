# -------------------------------------------------------------
# 2. SECTIONS 1 to 4: Search, Calls, Messages, Spam Shield
# -------------------------------------------------------------

TAB_SEARCH_HTML = '''
      <!-- ========================================== -->
      <!-- TAB 1: CALLER SEARCH / NUMBER LOOKUP       -->
      <!-- ========================================== -->
      <section id="tab-search" class="tab-content block space-y-6">
        
        <!-- Search Hero Banner -->
        <div class="relative overflow-hidden rounded-3xl bg-gradient-to-br from-blue-700 via-cc-blue to-indigo-800 text-white p-6 sm:p-8 shadow-xl">
          <div class="absolute -right-10 -bottom-10 w-72 h-72 bg-white/10 rounded-full blur-3xl pointer-events-none"></div>
          <div class="relative z-10 max-w-3xl">
            <div class="flex items-center gap-2 mb-3">
              <span class="inline-flex items-center gap-1.5 text-xs font-bold px-3 py-1 rounded-full bg-white/20 text-blue-100 backdrop-blur-md">
                <svg class="w-3.5 h-3.5 text-emerald-300" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path></svg>
                Verified Global Directory • 4.2 Billion Numbers
              </span>
              <span class="text-xs bg-amber-400 text-slate-900 font-extrabold px-2 py-0.5 rounded-full shadow-sm">v2026.10</span>
            </div>
            <h1 class="text-2xl sm:text-4xl font-extrabold tracking-tight">Who is calling you?</h1>
            <p class="text-blue-100 text-sm sm:text-base mt-2 mb-6">Identify unknown numbers, expose financial fraud before answering, view community scam reports, and verify couriers or banks.</p>

            <!-- Search Form with Country Selector -->
            <div class="bg-white dark:bg-cc-cardDark p-2 sm:p-2.5 rounded-2xl shadow-2xl flex flex-col sm:flex-row items-stretch gap-2 border border-white/20">
              <div class="flex items-center bg-slate-100 dark:bg-cc-darkBg rounded-xl px-3 py-2 sm:py-0 border border-slate-200 dark:border-cc-borderDark">
                <span class="text-base sm:text-lg mr-1.5">🌐</span>
                <select id="search-country-code" class="bg-transparent text-xs sm:text-sm font-bold text-slate-800 dark:text-slate-200 focus:outline-none cursor-pointer">
                  <option value="+91">🇮🇳 +91 (IN)</option>
                  <option value="+1">🇺🇸 +1 (US/CA)</option>
                  <option value="+44">🇬🇧 +44 (UK)</option>
                  <option value="+971">🇦🇪 +971 (UAE)</option>
                  <option value="+61">🇦🇺 +61 (AU)</option>
                  <option value="+49">🇩🇪 +49 (DE)</option>
                  <option value="+65">🇸🇬 +65 (SG)</option>
                </select>
              </div>

              <div class="relative flex-1">
                <input type="text" id="main-search-input" placeholder="Enter phone number (e.g. 9876543210) or person name..." 
                  class="w-full px-4 py-3 rounded-xl text-sm sm:text-base bg-slate-50 dark:bg-cc-darkBg text-slate-900 dark:text-white placeholder-slate-400 border border-slate-200 dark:border-cc-borderDark focus:outline-none focus:ring-2 focus:ring-cc-blue focus:border-transparent font-medium">
                <button id="main-search-clear-btn" class="hidden absolute right-3 inset-y-0 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 my-auto h-6 w-6">
                  ✕
                </button>
              </div>

              <button id="main-search-btn" class="px-6 py-3 bg-cc-blue hover:bg-cc-darkBlue text-white font-bold rounded-xl transition-all shadow-md shadow-blue-500/30 flex items-center justify-center gap-2 active:scale-95">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
                <span>Identify</span>
              </button>
            </div>

            <!-- Quick Suggestion Chips -->
            <div class="mt-4 flex flex-wrap items-center gap-1.5 sm:gap-2">
              <span class="text-xs font-semibold text-blue-200 mr-1">Trending:</span>
              <button class="quick-chip text-xs px-2.5 py-1 rounded-full bg-white/15 hover:bg-white/25 text-white transition-colors" data-query="+91 98765 43210">🚨 SBI KYC Scam</button>
              <button class="quick-chip text-xs px-2.5 py-1 rounded-full bg-white/15 hover:bg-white/25 text-white transition-colors" data-query="+91 80000 99888">📦 FedEx Narcotics Fraud</button>
              <button class="quick-chip text-xs px-2.5 py-1 rounded-full bg-white/15 hover:bg-white/25 text-white transition-colors" data-query="+91 98877 66554">🚚 Amazon Courier</button>
              <button class="quick-chip text-xs px-2.5 py-1 rounded-full bg-white/15 hover:bg-white/25 text-white transition-colors" data-query="+91 1800 266 7100">🏦 HDFC Official</button>
              <button class="quick-chip text-xs px-2.5 py-1 rounded-full bg-white/15 hover:bg-white/25 text-white transition-colors" data-query="+91 98200 12345">⭐ Aarav (Gold VIP)</button>
            </div>
          </div>
        </div>

        <!-- Search Results Showcase Area -->
        <div id="search-result-container" class="space-y-4">
          <!-- Dynamically populated by JS renderSearchResults -->
        </div>

      </section>
'''

TAB_CALLS_HTML = '''
      <!-- ========================================== -->
      <!-- TAB 2: CALLS HISTORY & SMART T9 DIALER     -->
      <!-- ========================================== -->
      <section id="tab-calls" class="tab-content hidden space-y-6">
        
        <!-- Sub-navigation: Call History vs T9 Dialer -->
        <div class="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white dark:bg-cc-cardDark p-3 rounded-2xl border border-slate-200 dark:border-cc-borderDark shadow-sm">
          <div class="flex items-center p-1 bg-slate-100 dark:bg-cc-darkBg rounded-xl w-full sm:w-auto">
            <button id="subtab-history-btn" class="flex-1 sm:flex-none px-5 py-2.5 rounded-lg text-xs sm:text-sm font-bold bg-cc-blue text-white shadow-sm transition-all flex items-center justify-center gap-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
              <span>Call History</span>
            </button>
            <button id="subtab-dialer-btn" class="flex-1 sm:flex-none px-5 py-2.5 rounded-lg text-xs sm:text-sm font-bold text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-cc-borderDark transition-all flex items-center justify-center gap-2">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"/></svg>
              <span>Smart T9 Keypad</span>
            </button>
          </div>

          <div class="flex items-center gap-2 w-full sm:w-auto justify-end">
            <button id="export-call-logs-btn" class="text-xs font-bold text-slate-700 dark:text-slate-300 hover:text-cc-blue px-3 py-2 rounded-xl border border-slate-200 dark:border-cc-borderDark hover:border-cc-blue transition-colors flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
              <span>Export CSV</span>
            </button>
            <button id="clear-call-logs-btn" class="text-xs font-bold text-slate-500 hover:text-red-500 px-3 py-2 rounded-xl border border-slate-200 dark:border-cc-borderDark hover:border-red-400 transition-colors flex items-center gap-1.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
              <span>Clear</span>
            </button>
          </div>
        </div>

        <!-- VIEW 1: CALL HISTORY -->
        <div id="call-history-view" class="space-y-4">
          <!-- Filter chips & Search -->
          <div class="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
            <div class="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-1">
              <button class="call-filter-chip px-3 py-1.5 rounded-xl text-xs font-bold bg-cc-blue text-white shadow-sm transition-all" data-filter="all">All Calls</button>
              <button class="call-filter-chip px-3 py-1.5 rounded-xl text-xs font-bold bg-slate-200 dark:bg-cc-cardDark text-slate-700 dark:text-slate-300 hover:bg-slate-300 dark:hover:bg-cc-borderDark transition-all" data-filter="incoming">Incoming</button>
              <button class="call-filter-chip px-3 py-1.5 rounded-xl text-xs font-bold bg-slate-200 dark:bg-cc-cardDark text-slate-700 dark:text-slate-300 hover:bg-slate-300 dark:hover:bg-cc-borderDark transition-all" data-filter="outgoing">Outgoing</button>
              <button class="call-filter-chip px-3 py-1.5 rounded-xl text-xs font-bold bg-slate-200 dark:bg-cc-cardDark text-slate-700 dark:text-slate-300 hover:bg-slate-300 dark:hover:bg-cc-borderDark transition-all" data-filter="missed">Missed</button>
              <button class="call-filter-chip px-3 py-1.5 rounded-xl text-xs font-bold bg-slate-200 dark:bg-cc-cardDark text-slate-700 dark:text-slate-300 hover:bg-slate-300 dark:hover:bg-cc-borderDark transition-all text-red-600 dark:text-red-400" data-filter="spam">🚨 Blocked / Spam</button>
            </div>

            <div class="relative w-full sm:w-64">
              <input type="text" id="call-logs-search" placeholder="Search call logs..." 
                class="w-full pl-8 pr-3 py-1.5 rounded-xl text-xs bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-cc-blue">
              <svg class="w-3.5 h-3.5 text-slate-400 absolute left-2.5 inset-y-0 my-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
            </div>
          </div>

          <!-- Logs List Container -->
          <div id="call-logs-list" class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-2xl divide-y divide-slate-100 dark:divide-cc-borderDark shadow-sm overflow-hidden">
            <!-- Populated dynamically -->
          </div>
        </div>

        <!-- VIEW 2: T9 KEYPAD DIALER -->
        <div id="keypad-dialer-view" class="hidden max-w-sm mx-auto bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 shadow-2xl space-y-4">
          
          <!-- Live T9 Predictive Match Header -->
          <div id="dialer-matched-contact" class="min-h-[2.5rem] p-2 rounded-xl bg-slate-50 dark:bg-cc-darkBg text-xs font-bold text-cc-blue dark:text-blue-400 flex items-center justify-between border border-slate-200 dark:border-cc-borderDark">
            <span class="text-slate-400 font-normal">Type digits to search contacts (T9)</span>
          </div>

          <!-- Dial Number Display -->
          <div class="relative flex items-center justify-center border-b border-slate-200 dark:border-cc-borderDark pb-2">
            <input type="text" id="dialer-display" readonly 
              placeholder="" 
              class="w-full text-center text-3xl font-extrabold font-mono tracking-wider bg-transparent text-slate-900 dark:text-white focus:outline-none">
            <button id="dialer-backspace-btn" class="absolute right-0 p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors" title="Backspace">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2M3 12l6.414-6.414a2 2 0 011.414-.586H19a2 2 0 012 2v10a2 2 0 01-2 2h-9.172a2 2 0 01-1.414-.586L3 12z"/></svg>
            </button>
          </div>

          <!-- 12-Key Tactile Dialpad Grid -->
          <div class="grid grid-cols-3 gap-3">
            <button class="keypad-key group flex flex-col items-center justify-center h-16 rounded-2xl bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark active:scale-95 transition-all shadow-sm border border-slate-200/60 dark:border-cc-borderDark/60" data-digit="1">
              <span class="text-2xl font-extrabold text-slate-900 dark:text-white">1</span>
              <span class="text-[9px] font-bold text-slate-400 tracking-widest flex items-center gap-0.5">
                <svg class="w-2.5 h-2.5" fill="currentColor" viewBox="0 0 20 20"><path d="M2 3a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 4V3z"/></svg>
                VM
              </span>
            </button>
            <button class="keypad-key flex flex-col items-center justify-center h-16 rounded-2xl bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark active:scale-95 transition-all shadow-sm border border-slate-200/60 dark:border-cc-borderDark/60" data-digit="2">
              <span class="text-2xl font-extrabold text-slate-900 dark:text-white">2</span>
              <span class="text-[9px] font-bold text-slate-400 tracking-widest">ABC</span>
            </button>
            <button class="keypad-key flex flex-col items-center justify-center h-16 rounded-2xl bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark active:scale-95 transition-all shadow-sm border border-slate-200/60 dark:border-cc-borderDark/60" data-digit="3">
              <span class="text-2xl font-extrabold text-slate-900 dark:text-white">3</span>
              <span class="text-[9px] font-bold text-slate-400 tracking-widest">DEF</span>
            </button>
            <button class="keypad-key flex flex-col items-center justify-center h-16 rounded-2xl bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark active:scale-95 transition-all shadow-sm border border-slate-200/60 dark:border-cc-borderDark/60" data-digit="4">
              <span class="text-2xl font-extrabold text-slate-900 dark:text-white">4</span>
              <span class="text-[9px] font-bold text-slate-400 tracking-widest">GHI</span>
            </button>
            <button class="keypad-key flex flex-col items-center justify-center h-16 rounded-2xl bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark active:scale-95 transition-all shadow-sm border border-slate-200/60 dark:border-cc-borderDark/60" data-digit="5">
              <span class="text-2xl font-extrabold text-slate-900 dark:text-white">5</span>
              <span class="text-[9px] font-bold text-slate-400 tracking-widest">JKL</span>
            </button>
            <button class="keypad-key flex flex-col items-center justify-center h-16 rounded-2xl bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark active:scale-95 transition-all shadow-sm border border-slate-200/60 dark:border-cc-borderDark/60" data-digit="6">
              <span class="text-2xl font-extrabold text-slate-900 dark:text-white">6</span>
              <span class="text-[9px] font-bold text-slate-400 tracking-widest">MNO</span>
            </button>
            <button class="keypad-key flex flex-col items-center justify-center h-16 rounded-2xl bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark active:scale-95 transition-all shadow-sm border border-slate-200/60 dark:border-cc-borderDark/60" data-digit="7">
              <span class="text-2xl font-extrabold text-slate-900 dark:text-white">7</span>
              <span class="text-[9px] font-bold text-slate-400 tracking-widest">PQRS</span>
            </button>
            <button class="keypad-key flex flex-col items-center justify-center h-16 rounded-2xl bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark active:scale-95 transition-all shadow-sm border border-slate-200/60 dark:border-cc-borderDark/60" data-digit="8">
              <span class="text-2xl font-extrabold text-slate-900 dark:text-white">8</span>
              <span class="text-[9px] font-bold text-slate-400 tracking-widest">TUV</span>
            </button>
            <button class="keypad-key flex flex-col items-center justify-center h-16 rounded-2xl bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark active:scale-95 transition-all shadow-sm border border-slate-200/60 dark:border-cc-borderDark/60" data-digit="9">
              <span class="text-2xl font-extrabold text-slate-900 dark:text-white">9</span>
              <span class="text-[9px] font-bold text-slate-400 tracking-widest">WXYZ</span>
            </button>
            <button class="keypad-key flex flex-col items-center justify-center h-16 rounded-2xl bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark active:scale-95 transition-all shadow-sm border border-slate-200/60 dark:border-cc-borderDark/60" data-digit="*">
              <span class="text-2xl font-extrabold text-slate-900 dark:text-white">*</span>
              <span class="text-[9px] font-bold text-slate-400 tracking-widest">Tone</span>
            </button>
            <button class="keypad-key flex flex-col items-center justify-center h-16 rounded-2xl bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark active:scale-95 transition-all shadow-sm border border-slate-200/60 dark:border-cc-borderDark/60" data-digit="0">
              <span class="text-2xl font-extrabold text-slate-900 dark:text-white">0</span>
              <span class="text-[9px] font-bold text-slate-400 tracking-widest">+</span>
            </button>
            <button class="keypad-key flex flex-col items-center justify-center h-16 rounded-2xl bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark active:scale-95 transition-all shadow-sm border border-slate-200/60 dark:border-cc-borderDark/60" data-digit="#">
              <span class="text-2xl font-extrabold text-slate-900 dark:text-white">#</span>
              <span class="text-[9px] font-bold text-slate-400 tracking-widest">Wait</span>
            </button>
          </div>

          <!-- Direct Call Action Button -->
          <div class="flex items-center justify-center pt-2">
            <button id="dialer-call-action-btn" class="w-16 h-16 rounded-full bg-emerald-500 hover:bg-emerald-600 text-white flex items-center justify-center shadow-xl shadow-emerald-500/30 active:scale-90 transition-all">
              <svg class="w-7 h-7" fill="currentColor" viewBox="0 0 20 20"><path d="M2 3a1 1 0 011-1h2.153a1 1 0 01.986.836l.74 4.435a1 1 0 01-.54 1.06l-1.548.773a11.037 11.037 0 006.105 6.105l.774-1.548a1 1 0 011.059-.54l4.435.74a1 1 0 01.836.986V17a1 1 0 01-1 1h-2C7.82 18 2 12.18 2 4V3z"></path></svg>
            </button>
          </div>

        </div>

      </section>
'''

print("Search & Calls sections defined.")
