# -------------------------------------------------------------
# 3. SECTIONS: Messages, Spam Shield, Contacts
# -------------------------------------------------------------

TAB_MESSAGES_HTML = '''
      <!-- ========================================== -->
      <!-- TAB 3: SMART SMS & ANTI-PHISHING INBOX     -->
      <!-- ========================================== -->
      <section id="tab-messages" class="tab-content hidden space-y-6">
        
        <!-- Header Banner -->
        <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-gradient-to-r from-blue-900 via-indigo-900 to-cc-cardDark p-6 rounded-3xl text-white shadow-xl">
          <div>
            <div class="flex items-center gap-2 mb-1">
              <span class="text-xs font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full bg-blue-500/30 text-blue-200 border border-blue-400/30">AI SMS DEFENSE</span>
              <span class="text-xs text-emerald-400 font-semibold flex items-center gap-1">
                <span class="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span> Anti-Phishing Active
              </span>
            </div>
            <h2 class="text-2xl font-extrabold tracking-tight">Smart SMS Organizer</h2>
            <p class="text-xs sm:text-sm text-blue-200 mt-1 max-w-xl">Zero spam, zero fraud. Smart categorization keeps your banking OTPs instant to copy while isolating phishing links and sales bots.</p>
          </div>

          <button id="open-compose-sms-btn" class="px-4 py-2.5 bg-cc-blue hover:bg-cc-darkBlue text-white font-bold text-xs sm:text-sm rounded-xl shadow-md transition-all flex items-center gap-2 self-stretch sm:self-auto justify-center active:scale-95">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
            <span>Compose SMS</span>
          </button>
        </div>

        <!-- SMS Category Filter Tabs -->
        <div class="flex items-center gap-2 overflow-x-auto no-scrollbar py-1">
          <button class="sms-filter-btn px-4 py-2 rounded-xl text-xs font-bold bg-cc-blue text-white shadow-sm transition-all flex items-center gap-1.5" data-category="all">
            <span>All Messages</span>
          </button>
          <button class="sms-filter-btn px-4 py-2 rounded-xl text-xs font-bold bg-slate-200 dark:bg-cc-cardDark text-slate-700 dark:text-slate-300 hover:bg-slate-300 dark:hover:bg-cc-borderDark transition-all flex items-center gap-1.5" data-category="transactions">
            <span>💳 Transactions & OTP</span>
            <span class="px-1.5 py-0.2 rounded bg-blue-100 dark:bg-blue-900/60 text-cc-blue dark:text-blue-300 text-[10px]">1-Tap Copy</span>
          </button>
          <button class="sms-filter-btn px-4 py-2 rounded-xl text-xs font-bold bg-slate-200 dark:bg-cc-cardDark text-slate-700 dark:text-slate-300 hover:bg-slate-300 dark:hover:bg-cc-borderDark transition-all flex items-center gap-1.5" data-category="personal">
            <span>💬 Personal</span>
          </button>
          <button class="sms-filter-btn px-4 py-2 rounded-xl text-xs font-bold bg-slate-200 dark:bg-cc-cardDark text-slate-700 dark:text-slate-300 hover:bg-slate-300 dark:hover:bg-cc-borderDark transition-all flex items-center gap-1.5" data-category="promotions">
            <span>🏷️ Offers & Promos</span>
          </button>
          <button class="sms-filter-btn px-4 py-2 rounded-xl text-xs font-bold bg-slate-200 dark:bg-cc-cardDark text-red-600 dark:text-red-400 hover:bg-slate-300 dark:hover:bg-cc-borderDark transition-all flex items-center gap-1.5" data-category="spam">
            <span>🚨 Quarantined Spam</span>
            <span class="w-2 h-2 rounded-full bg-red-500 animate-ping"></span>
          </button>
        </div>

        <!-- Messages Feed Container -->
        <div id="messages-list-container" class="space-y-3">
          <!-- Populated dynamically by JS renderMessages() -->
        </div>

      </section>
'''

TAB_SPAM_HTML = '''
      <!-- ========================================== -->
      <!-- TAB 4: SPAM SHIELD & THREAT RADAR          -->
      <!-- ========================================== -->
      <section id="tab-spam" class="tab-content hidden space-y-6">
        
        <!-- Spam Stats Overview Banner -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div class="bg-gradient-to-br from-red-600 to-rose-700 rounded-3xl p-6 text-white shadow-lg relative overflow-hidden">
            <div class="absolute -right-6 -bottom-6 w-32 h-32 bg-white/10 rounded-full blur-xl pointer-events-none"></div>
            <div class="flex items-center justify-between">
              <span class="text-xs font-bold uppercase tracking-wider text-red-100">AI Shield Engine</span>
              <span class="px-2 py-0.5 rounded-full bg-white/20 text-xs font-bold">Real-time</span>
            </div>
            <div class="text-3xl sm:text-4xl font-extrabold mt-3 tracking-tight" id="stat-threats-blocked">142</div>
            <p class="text-xs text-red-100 mt-1">Spam & fraud attempts auto-blocked this month</p>
          </div>

          <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 shadow-sm flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between text-xs font-bold text-slate-500 dark:text-slate-400">
                <span>OFFLINE SIGNATURE DB</span>
                <span class="text-emerald-500 font-semibold">● Updated</span>
              </div>
              <div class="text-2xl font-extrabold text-slate-900 dark:text-white mt-3">450,000+</div>
              <p class="text-xs text-slate-500 mt-1">High-risk fraud signatures stored locally</p>
            </div>
            <button id="update-spam-db-btn" class="mt-4 w-full py-2 bg-slate-100 dark:bg-cc-darkBg hover:bg-slate-200 dark:hover:bg-cc-borderDark text-cc-blue dark:text-blue-400 font-bold text-xs rounded-xl transition-all flex items-center justify-center gap-1.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
              <span>Sync Threat Signatures</span>
            </button>
          </div>

          <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 shadow-sm flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between text-xs font-bold text-slate-500 dark:text-slate-400">
                <span>COMMUNITY DEFENSE</span>
                <span class="text-cc-blue font-semibold">Collaborative</span>
              </div>
              <div class="text-2xl font-extrabold text-slate-900 dark:text-white mt-3">3.8M+ Reports</div>
              <p class="text-xs text-slate-500 mt-1">Protecting millions from cyber extortion daily</p>
            </div>
            <button id="open-report-spam-modal-btn" class="mt-4 w-full py-2 bg-cc-spamRed hover:bg-red-600 text-white font-bold text-xs rounded-xl shadow-md shadow-red-500/20 transition-all flex items-center justify-center gap-1.5">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
              <span>Report Spam Number</span>
            </button>
          </div>
        </div>

        <!-- Protection Toggles & Custom Blocklist Grid -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          
          <!-- Advanced Defense Toggles -->
          <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 shadow-sm space-y-5">
            <div class="border-b border-slate-100 dark:border-cc-borderDark pb-3">
              <h3 class="text-base font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
                <svg class="w-5 h-5 text-cc-blue" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/></svg>
                Autonomous Call Interception
              </h3>
              <p class="text-xs text-slate-500 mt-0.5">Customize which incoming calls are silently dropped or screened.</p>
            </div>

            <div class="space-y-4">
              <!-- Toggle 1 -->
              <div class="flex items-center justify-between p-3 rounded-2xl bg-slate-50 dark:bg-cc-darkBg border border-slate-200/60 dark:border-cc-borderDark/60">
                <div>
                  <h4 class="font-bold text-xs sm:text-sm text-slate-900 dark:text-white">Auto-Block Top Spammers</h4>
                  <p class="text-[11px] text-slate-500">Automatically rejects numbers flagged by 50+ community users.</p>
                </div>
                <label class="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" id="toggle-block-spammers" checked class="sr-only peer">
                  <div class="w-11 h-6 bg-slate-300 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-cc-blue"></div>
                </label>
              </div>

              <!-- Toggle 2 -->
              <div class="flex items-center justify-between p-3 rounded-2xl bg-slate-50 dark:bg-cc-darkBg border border-slate-200/60 dark:border-cc-borderDark/60">
                <div>
                  <h4 class="font-bold text-xs sm:text-sm text-slate-900 dark:text-white">Block Foreign / Unknown International</h4>
                  <p class="text-[11px] text-slate-500">Rejects incoming calls with country codes outside your region.</p>
                </div>
                <label class="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" id="toggle-block-foreign" class="sr-only peer">
                  <div class="w-11 h-6 bg-slate-300 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-cc-blue"></div>
                </label>
              </div>

              <!-- Toggle 3 -->
              <div class="flex items-center justify-between p-3 rounded-2xl bg-slate-50 dark:bg-cc-darkBg border border-slate-200/60 dark:border-cc-borderDark/60">
                <div>
                  <h4 class="font-bold text-xs sm:text-sm text-slate-900 dark:text-white">Block Hidden & Private Callers</h4>
                  <p class="text-[11px] text-slate-500">Rejects calls where caller identity/CLI is deliberately obscured.</p>
                </div>
                <label class="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" id="toggle-block-hidden" checked class="sr-only peer">
                  <div class="w-11 h-6 bg-slate-300 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-cc-blue"></div>
                </label>
              </div>

              <!-- Toggle 4 -->
              <div class="flex items-center justify-between p-3 rounded-2xl bg-slate-50 dark:bg-cc-darkBg border border-slate-200/60 dark:border-cc-borderDark/60">
                <div>
                  <h4 class="font-bold text-xs sm:text-sm text-slate-900 dark:text-white">VIP Mode (Allow Contacts Only)</h4>
                  <p class="text-[11px] text-slate-500">Silences all callers who are not in your saved phonebook.</p>
                </div>
                <label class="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" id="toggle-block-nonphonebook" class="sr-only peer">
                  <div class="w-11 h-6 bg-slate-300 peer-focus:outline-none rounded-full peer dark:bg-slate-700 peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-cc-blue"></div>
                </label>
              </div>
            </div>
          </div>

          <!-- Custom Blocklist & Wildcard Manager -->
          <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 shadow-sm flex flex-col justify-between">
            <div>
              <div class="border-b border-slate-100 dark:border-cc-borderDark pb-3">
                <h3 class="text-base font-extrabold text-slate-900 dark:text-white flex items-center gap-2">
                  <svg class="w-5 h-5 text-cc-spamRed" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636"/></svg>
                  Custom Number & Prefix Blocklist
                </h3>
                <p class="text-xs text-slate-500 mt-0.5">Add specific phone numbers or prefixes (e.g. +91 140* or +1 800*).</p>
              </div>

              <!-- Add to blocklist input -->
              <div class="mt-4 flex gap-2">
                <input type="text" id="add-blocklist-input" placeholder="e.g. +91 140* or 9876543210" 
                  class="flex-1 px-3 py-2 rounded-xl text-xs bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white focus:outline-none focus:ring-1 focus:ring-cc-blue">
                <button id="add-blocklist-btn" class="px-4 py-2 bg-cc-spamRed hover:bg-red-600 text-white font-bold text-xs rounded-xl shadow-sm transition-all flex items-center gap-1">
                  <span>Block</span>
                </button>
              </div>

              <!-- Blocklist entries container -->
              <div id="custom-blocklist-items" class="mt-4 space-y-2 max-h-56 overflow-y-auto pr-1">
                <!-- Dynamically rendered -->
              </div>
            </div>
          </div>

        </div>

        <!-- Live Worldwide Scam Attack Radar & Community Feed -->
        <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 shadow-sm">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 dark:border-cc-borderDark pb-4 mb-4">
            <div>
              <div class="flex items-center gap-2">
                <span class="w-2.5 h-2.5 rounded-full bg-red-500 animate-ping"></span>
                <h3 class="text-base font-extrabold text-slate-900 dark:text-white">Live Global Threat Stream</h3>
              </div>
              <p class="text-xs text-slate-500 mt-0.5">Real-time telemarketing & phishing numbers flagged across the Craft community</p>
            </div>
            <span class="text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 dark:bg-cc-darkBg text-slate-600 dark:text-slate-300 w-fit">Auto-Syncing</span>
          </div>

          <div id="live-spam-feed" class="space-y-3">
            <!-- Dynamically populated live scam alerts -->
          </div>
        </div>

      </section>
'''

TAB_CONTACTS_HTML = '''
      <!-- ========================================== -->
      <!-- TAB 5: CONTACTS & CLOUD DIRECTORY          -->
      <!-- ========================================== -->
      <section id="tab-contacts" class="tab-content hidden space-y-6">
        
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white dark:bg-cc-cardDark p-4 sm:p-6 rounded-3xl border border-slate-200 dark:border-cc-borderDark shadow-sm">
          <div>
            <h2 class="text-xl sm:text-2xl font-extrabold text-slate-900 dark:text-white">Smart Phonebook</h2>
            <p class="text-xs sm:text-sm text-slate-500 mt-0.5">Enriched contacts with caller verification badges, telecom carrier details, and export tools.</p>
          </div>

          <div class="flex items-center gap-2 flex-wrap">
            <button id="export-contacts-vcf-btn" class="px-3.5 py-2 rounded-xl text-xs font-bold border border-slate-200 dark:border-cc-borderDark text-slate-700 dark:text-slate-300 hover:border-cc-blue hover:text-cc-blue transition-colors flex items-center gap-1.5 shadow-sm">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
              <span>Export .VCF (vCard)</span>
            </button>
            <button id="add-contact-btn" class="px-4 py-2 rounded-xl text-xs sm:text-sm font-bold bg-cc-blue hover:bg-cc-darkBlue text-white shadow-md shadow-blue-500/20 transition-all flex items-center gap-1.5 active:scale-95">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"/></svg>
              <span>Add Contact</span>
            </button>
          </div>
        </div>

        <!-- Search Contacts bar -->
        <div class="relative">
          <input type="text" id="contacts-search-input" placeholder="Search contacts by name, number, or location..." 
            class="w-full pl-10 pr-4 py-3 rounded-2xl text-xs sm:text-sm bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-cc-blue">
          <div class="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
          </div>
        </div>

        <!-- Contacts Grid Container -->
        <div id="contacts-list-container" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <!-- Populated dynamically by JS renderContacts() -->
        </div>

      </section>
'''

print("Messages, Spam, Contacts HTML prepared.")
