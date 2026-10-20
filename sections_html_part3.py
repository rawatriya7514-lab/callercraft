# -------------------------------------------------------------
# 4. SECTIONS: AI Assistant, Simulator Studio, Analytics & Modals
# -------------------------------------------------------------

TAB_ASSISTANT_HTML = '''
      <!-- ========================================== -->
      <!-- TAB 6: AI CALL ASSISTANT & SCREENING       -->
      <!-- ========================================== -->
      <section id="tab-assistant" class="tab-content hidden space-y-6">
        
        <!-- Assistant Hero -->
        <div class="relative overflow-hidden rounded-3xl bg-gradient-to-br from-purple-900 via-indigo-950 to-cc-cardDark text-white p-6 sm:p-8 shadow-xl border border-purple-800/40">
          <div class="absolute -right-8 -bottom-8 w-64 h-64 bg-purple-500/10 rounded-full blur-3xl pointer-events-none"></div>
          <div class="relative z-10 max-w-2xl">
            <span class="inline-flex items-center gap-1.5 text-xs font-bold px-3 py-1 rounded-full bg-purple-500/20 text-purple-200 border border-purple-400/30 mb-3">
              <span class="w-2 h-2 rounded-full bg-purple-400 animate-ping"></span>
              Autonomous AI Call Screening
            </span>
            <h2 class="text-2xl sm:text-3xl font-extrabold tracking-tight">Never answer another scam call</h2>
            <p class="text-purple-200 text-xs sm:text-sm mt-1.5 mb-6">When an unknown number rings, CraftCaller AI Assistant answers on your behalf, questions the caller, and transcribes their voice live on your screen.</p>

            <div class="flex flex-wrap items-center gap-3">
              <button id="start-assistant-demo-btn" class="px-5 py-2.5 bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white font-bold text-xs sm:text-sm rounded-xl shadow-lg shadow-purple-600/30 transition-all flex items-center gap-2 active:scale-95">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                <span>Launch Assistant Simulation</span>
              </button>

              <div class="flex items-center gap-2 bg-purple-950/60 border border-purple-800/60 px-3 py-2 rounded-xl text-xs text-purple-200">
                <span>Voice Speech Audio:</span>
                <button id="toggle-assistant-voice-btn" class="font-bold text-emerald-400 hover:underline">
                  Enabled (Spoken)
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Assistant Interactive Studio -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          <!-- Column 1: Persona Configuration -->
          <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 shadow-sm space-y-4">
            <h3 class="font-bold text-sm text-slate-900 dark:text-white flex items-center gap-2">
              <svg class="w-4 h-4 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/></svg>
              Assistant Persona
            </h3>
            
            <div class="space-y-2">
              <label class="assistant-persona-card block p-3 rounded-2xl border-2 border-purple-500 bg-purple-50/50 dark:bg-purple-950/30 cursor-pointer transition-all" data-persona="professional">
                <div class="flex items-center justify-between">
                  <span class="font-bold text-xs text-slate-900 dark:text-white">Professional Gatekeeper</span>
                  <span class="w-2.5 h-2.5 rounded-full bg-purple-500"></span>
                </div>
                <p class="text-[11px] text-slate-500 mt-1">Formal & concise. "Hello, I am CraftCaller Assistant. Who is calling and what is this regarding?"</p>
              </label>

              <label class="assistant-persona-card block p-3 rounded-2xl border-2 border-transparent bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark cursor-pointer transition-all" data-persona="strict">
                <div class="flex items-center justify-between">
                  <span class="font-bold text-xs text-slate-900 dark:text-white">Cyber Fraud Hunter</span>
                  <span class="w-2.5 h-2.5 rounded-full bg-slate-400"></span>
                </div>
                <p class="text-[11px] text-slate-500 mt-1">Strict & assertive. "Warning: Calls to this line are recorded. State your registered business ID."</p>
              </label>

              <label class="assistant-persona-card block p-3 rounded-2xl border-2 border-transparent bg-slate-50 dark:bg-cc-darkBg hover:bg-slate-100 dark:hover:bg-cc-borderDark cursor-pointer transition-all" data-persona="friendly">
                <div class="flex items-center justify-between">
                  <span class="font-bold text-xs text-slate-900 dark:text-white">Friendly Assistant</span>
                  <span class="w-2.5 h-2.5 rounded-full bg-slate-400"></span>
                </div>
                <p class="text-[11px] text-slate-500 mt-1">Warm & helpful. "Hi there! The recipient is currently away. How can I assist you today?"</p>
              </label>
            </div>
          </div>

          <!-- Column 2 & 3: Live Screening Dialogue Console -->
          <div class="lg:col-span-2 bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 shadow-sm flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between border-b border-slate-100 dark:border-cc-borderDark pb-3 mb-4">
                <div class="flex items-center gap-2">
                  <div class="w-8 h-8 rounded-xl bg-purple-600 text-white flex items-center justify-center font-bold text-xs">AI</div>
                  <div>
                    <h4 class="font-bold text-xs sm:text-sm text-slate-900 dark:text-white">Live Call Screening Session</h4>
                    <p class="text-[10px] text-slate-400" id="assistant-session-caller">Ready • Waiting for call</p>
                  </div>
                </div>
                <span id="assistant-risk-indicator" class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 dark:bg-cc-darkBg text-slate-500">Standby</span>
              </div>

              <!-- Chat Transcript Stream -->
              <div id="assistant-transcript-box" class="space-y-3 min-h-[160px] max-h-72 overflow-y-auto pr-1">
                <div class="text-center text-xs text-slate-400 py-10">
                  Click <span class="font-bold text-purple-500">"Launch Assistant Simulation"</span> to see the AI screen a call in real time!
                </div>
              </div>
            </div>

            <!-- Real-Time User Controls during Call Screening -->
            <div id="assistant-action-controls" class="pt-4 border-t border-slate-100 dark:border-cc-borderDark grid grid-cols-2 sm:grid-cols-4 gap-2">
              <button class="assistant-prompt-btn p-2 rounded-xl bg-slate-100 dark:bg-cc-darkBg hover:bg-slate-200 dark:hover:bg-cc-borderDark text-[11px] font-bold text-slate-700 dark:text-slate-300 transition-colors" data-reply="busy">
                "I am busy right now"
              </button>
              <button class="assistant-prompt-btn p-2 rounded-xl bg-slate-100 dark:bg-cc-darkBg hover:bg-slate-200 dark:hover:bg-cc-borderDark text-[11px] font-bold text-slate-700 dark:text-slate-300 transition-colors" data-reply="details">
                "What is this regarding?"
              </button>
              <button class="assistant-prompt-btn p-2 rounded-xl bg-red-100 dark:bg-red-950/60 hover:bg-red-200 dark:hover:bg-red-900/60 text-[11px] font-bold text-red-600 dark:text-red-300 transition-colors" data-reply="block">
                🚨 Mark Spam & Hang Up
              </button>
              <button class="assistant-prompt-btn p-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-[11px] font-bold text-white transition-colors" data-reply="takeover">
                📞 Take Over Call
              </button>
            </div>
          </div>

        </div>

      </section>
'''

TAB_SIMULATOR_HTML = '''
      <!-- ========================================== -->
      <!-- TAB 7: CALLER ID SIMULATOR STUDIO          -->
      <!-- ========================================== -->
      <section id="tab-simulator" class="tab-content hidden space-y-6">
        
        <div class="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-3xl p-6 sm:p-8 text-white shadow-xl border border-indigo-900/40">
          <div class="flex items-center gap-2 mb-2">
            <span class="text-xs font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full bg-purple-500/30 text-purple-200">INTERACTIVE LAB</span>
            <span class="text-xs text-emerald-400 font-semibold">Sound Synthesizer Active</span>
          </div>
          <h2 class="text-2xl sm:text-3xl font-extrabold flex items-center gap-2.5">
            <svg class="w-7 h-7 text-cc-blue" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            Caller ID Simulator Studio
          </h2>
          <p class="text-slate-300 text-xs sm:text-sm mt-1 max-w-2xl">Experience how CraftCaller identifies incoming calls in real-time. Test scam alerts, verified couriers, banking agents, and personal contacts with synthetic dual-tone ringtones and responsive overlays.</p>
        </div>

        <!-- Presets Grid (7 Real Scenarios) -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          
          <!-- Preset 1: High Risk Scam -->
          <div class="bg-white dark:bg-cc-cardDark border border-red-200 dark:border-red-900/60 rounded-3xl p-5 shadow-sm hover:shadow-md transition-all flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between mb-3">
                <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-red-100 dark:bg-red-950/80 text-red-600 dark:text-red-400">99% Spam Probability</span>
                <span class="text-[10px] text-slate-400">14,820 Reports</span>
              </div>
              <div class="flex items-center gap-3">
                <div class="w-12 h-12 rounded-2xl bg-red-500 text-white flex items-center justify-center font-bold text-xl shadow-md">!</div>
                <div>
                  <h4 class="font-bold text-sm text-slate-900 dark:text-white">SBI KYC Reward Scam</h4>
                  <p class="text-xs text-slate-500 font-mono">+91 98765 43210</p>
                </div>
              </div>
              <p class="text-xs text-slate-600 dark:text-slate-400 mt-3">Fraudster pretending to be bank staff asking for card CVV & OTP under guise of KYC update.</p>
            </div>
            <button class="simulate-preset-btn mt-4 w-full py-2.5 bg-cc-spamRed hover:bg-red-600 text-white rounded-xl font-bold text-xs transition-all shadow-sm flex items-center justify-center gap-1.5 active:scale-95"
              data-name="SBI KYC Reward Scam" data-number="+91 98765 43210" data-type="spam" data-category="Financial Fraud" data-reports="14820" data-location="New Delhi, India" data-carrier="Vi Telephony">
              Simulate Scam Call
            </button>
          </div>

          <!-- Preset 2: Verified Delivery -->
          <div class="bg-white dark:bg-cc-cardDark border border-blue-200 dark:border-blue-900/60 rounded-3xl p-5 shadow-sm hover:shadow-md transition-all flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between mb-3">
                <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-100 dark:bg-blue-950/80 text-cc-blue dark:text-blue-400">Verified Courier</span>
                <span class="text-[10px] text-emerald-500 font-semibold">Priority Delivery</span>
              </div>
              <div class="flex items-center gap-3">
                <div class="w-12 h-12 rounded-2xl bg-cc-blue text-white flex items-center justify-center font-bold text-lg shadow-md">AZ</div>
                <div>
                  <h4 class="font-bold text-sm text-slate-900 dark:text-white">Amazon Prime Delivery</h4>
                  <p class="text-xs text-slate-500 font-mono">+91 98877 66554</p>
                </div>
              </div>
              <p class="text-xs text-slate-600 dark:text-slate-400 mt-3">Verified delivery partner delivering your scheduled package today.</p>
            </div>
            <button class="simulate-preset-btn mt-4 w-full py-2.5 bg-cc-blue hover:bg-cc-darkBlue text-white rounded-xl font-bold text-xs transition-all shadow-sm flex items-center justify-center gap-1.5 active:scale-95"
              data-name="Amazon Prime Delivery" data-number="+91 98877 66554" data-type="verified" data-category="Delivery & Logistics" data-reports="0" data-location="Lucknow, UP" data-carrier="Vodafone Idea">
              Simulate Delivery Call
            </button>
          </div>

          <!-- Preset 3: Verified Banking Priority -->
          <div class="bg-white dark:bg-cc-cardDark border border-indigo-200 dark:border-indigo-900/60 rounded-3xl p-5 shadow-sm hover:shadow-md transition-all flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between mb-3">
                <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-100 dark:bg-indigo-950/80 text-indigo-600 dark:text-indigo-400">Verified Enterprise</span>
                <span class="text-[10px] text-indigo-500 font-semibold">Official Line</span>
              </div>
              <div class="flex items-center gap-3">
                <div class="w-12 h-12 rounded-2xl bg-indigo-600 text-white flex items-center justify-center font-bold text-sm shadow-md">HDFC</div>
                <div>
                  <h4 class="font-bold text-sm text-slate-900 dark:text-white">HDFC Priority Banking</h4>
                  <p class="text-xs text-slate-500 font-mono">+91 1800 266 7100</p>
                </div>
              </div>
              <p class="text-xs text-slate-600 dark:text-slate-400 mt-3">Certified official toll-free bank line. Caller badge verified by CraftCaller Enterprise.</p>
            </div>
            <button class="simulate-preset-btn mt-4 w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold text-xs transition-all shadow-sm flex items-center justify-center gap-1.5 active:scale-95"
              data-name="HDFC Priority Banking" data-number="+91 1800 266 7100" data-type="verified" data-category="Banking & Finance" data-reports="0" data-location="All India Toll-Free" data-carrier="Tata Teleservices">
              Simulate Bank Call
            </button>
          </div>

          <!-- Preset 4: Personal Friend & Gold VIP -->
          <div class="bg-white dark:bg-cc-cardDark border border-amber-200 dark:border-amber-900/60 rounded-3xl p-5 shadow-sm hover:shadow-md transition-all flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between mb-3">
                <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 dark:bg-amber-950/80 text-amber-700 dark:text-amber-400">CraftCaller Gold VIP</span>
                <span class="text-[10px] text-slate-400">100% Safe</span>
              </div>
              <div class="flex items-center gap-3">
                <div class="w-12 h-12 rounded-2xl bg-gradient-to-tr from-amber-400 to-yellow-600 text-white flex items-center justify-center font-extrabold text-lg shadow-md">AS</div>
                <div>
                  <h4 class="font-bold text-sm text-slate-900 dark:text-white">Aarav Sharma</h4>
                  <p class="text-xs text-slate-500 font-mono">+91 98200 12345</p>
                </div>
              </div>
              <p class="text-xs text-slate-600 dark:text-slate-400 mt-3">Personal contact and verified CraftCaller Gold VIP profile holder.</p>
            </div>
            <button class="simulate-preset-btn mt-4 w-full py-2.5 bg-amber-500 hover:bg-amber-600 text-white rounded-xl font-bold text-xs transition-all shadow-sm flex items-center justify-center gap-1.5 active:scale-95"
              data-name="Aarav Sharma" data-number="+91 98200 12345" data-type="gold" data-category="Personal Contact" data-reports="0" data-location="Mumbai, Maharashtra" data-carrier="Jio 5G">
              Simulate VIP Friend Call
            </button>
          </div>

          <!-- Preset 5: Telemarketer Robo -->
          <div class="bg-white dark:bg-cc-cardDark border border-orange-200 dark:border-orange-900/60 rounded-3xl p-5 shadow-sm hover:shadow-md transition-all flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between mb-3">
                <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-orange-100 dark:bg-orange-950/80 text-orange-700 dark:text-orange-400">Telemarketing / Sales</span>
                <span class="text-[10px] text-orange-500 font-semibold">5,120 Reports</span>
              </div>
              <div class="flex items-center gap-3">
                <div class="w-12 h-12 rounded-2xl bg-orange-500 text-white flex items-center justify-center font-bold text-xl shadow-md">☎</div>
                <div>
                  <h4 class="font-bold text-sm text-slate-900 dark:text-white">PolicyBazaar Sales Desk</h4>
                  <p class="text-xs text-slate-500 font-mono">+91 98990 01122</p>
                </div>
              </div>
              <p class="text-xs text-slate-600 dark:text-slate-400 mt-3">Automated auto-dialer offering term life insurance and loan top-ups.</p>
            </div>
            <button class="simulate-preset-btn mt-4 w-full py-2.5 bg-orange-600 hover:bg-orange-700 text-white rounded-xl font-bold text-xs transition-all shadow-sm flex items-center justify-center gap-1.5 active:scale-95"
              data-name="PolicyBazaar Sales Desk" data-number="+91 98990 01122" data-type="spam" data-category="Telemarketing" data-reports="5120" data-location="Gurgaon, Haryana" data-carrier="Airtel">
              Simulate Telemarketer
            </button>
          </div>

          <!-- Preset 6: Customs Narcotics Extortion Scam -->
          <div class="bg-white dark:bg-cc-cardDark border border-red-200 dark:border-red-900/60 rounded-3xl p-5 shadow-sm hover:shadow-md transition-all flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between mb-3">
                <span class="text-[10px] font-bold px-2 py-0.5 rounded-full bg-red-100 dark:bg-red-950/80 text-red-600 dark:text-red-400">Severe Extortion Fraud</span>
                <span class="text-[10px] text-red-500 font-semibold">22,410 Reports</span>
              </div>
              <div class="flex items-center gap-3">
                <div class="w-12 h-12 rounded-2xl bg-red-600 text-white flex items-center justify-center font-bold text-xl shadow-md">⚠️</div>
                <div>
                  <h4 class="font-bold text-sm text-slate-900 dark:text-white">FedEx Customs Impersonator</h4>
                  <p class="text-xs text-slate-500 font-mono">+91 80000 99888</p>
                </div>
              </div>
              <p class="text-xs text-slate-600 dark:text-slate-400 mt-3">Threatens digital arrest alleging illegal courier parcel found with fake passports.</p>
            </div>
            <button class="simulate-preset-btn mt-4 w-full py-2.5 bg-red-700 hover:bg-red-800 text-white rounded-xl font-bold text-xs transition-all shadow-sm flex items-center justify-center gap-1.5 active:scale-95"
              data-name="FedEx Customs Impersonator" data-number="+91 80000 99888" data-type="spam" data-category="Courier Extortion / Digital Arrest" data-reports="22410" data-location="VoIP International" data-carrier="Gateway">
              Simulate Extortion Scam
            </button>
          </div>

        </div>

        <!-- Custom Call Simulator Form -->
        <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 shadow-sm">
          <h3 class="font-extrabold text-base text-slate-900 dark:text-white mb-1">Custom Caller Simulator Studio</h3>
          <p class="text-xs text-slate-500 mb-4">Input any custom phone identity to test how CraftCaller renders it.</p>
          
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
            <div>
              <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Caller Name</label>
              <input type="text" id="custom-caller-name" value="Bank Fraud Investigator" class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white">
            </div>
            <div>
              <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Phone Number</label>
              <input type="text" id="custom-caller-number" value="+91 99999 88888" class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white">
            </div>
            <div>
              <label class="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Classification Badge</label>
              <select id="custom-caller-type" class="w-full px-3 py-2 rounded-xl text-xs sm:text-sm bg-slate-50 dark:bg-cc-darkBg border border-slate-200 dark:border-cc-borderDark text-slate-900 dark:text-white">
                <option value="spam">🚨 Severe Scam / Fraud</option>
                <option value="verified">✅ Verified Business</option>
                <option value="gold">⭐ CraftCaller Gold VIP</option>
                <option value="safe">👤 Safe Contact / Personal</option>
              </select>
            </div>
            <div class="flex items-end">
              <button id="trigger-custom-call-btn" class="w-full py-2.5 bg-cc-blue hover:bg-cc-darkBlue text-white font-bold text-xs sm:text-sm rounded-xl transition-all shadow-md active:scale-95">
                Simulate Call
              </button>
            </div>
          </div>
        </div>

      </section>
'''

TAB_ANALYTICS_HTML = '''
      <!-- ========================================== -->
      <!-- TAB 8: ANALYTICS, PRIVACY & BACKUP         -->
      <!-- ========================================== -->
      <section id="tab-analytics" class="tab-content hidden space-y-6">
        
        <!-- Stats Banner -->
        <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 shadow-sm">
          <h2 class="text-xl font-extrabold text-slate-900 dark:text-white">Privacy & Security Analytics</h2>
          <p class="text-xs text-slate-500 mt-0.5">Comprehensive audit of blocked spam, time saved, and data backups.</p>

          <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-6">
            <div class="p-4 rounded-2xl bg-slate-50 dark:bg-cc-darkBg border border-slate-200/60 dark:border-cc-borderDark/60">
              <span class="text-xs text-slate-500">Spam Blocked</span>
              <div class="text-2xl font-extrabold text-red-500 mt-1">142</div>
              <span class="text-[10px] text-emerald-500">+18% this month</span>
            </div>
            <div class="p-4 rounded-2xl bg-slate-50 dark:bg-cc-darkBg border border-slate-200/60 dark:border-cc-borderDark/60">
              <span class="text-xs text-slate-500">Hours Saved</span>
              <div class="text-2xl font-extrabold text-cc-blue mt-1">4.2 hrs</div>
              <span class="text-[10px] text-slate-400">Zero interrupted meetings</span>
            </div>
            <div class="p-4 rounded-2xl bg-slate-50 dark:bg-cc-darkBg border border-slate-200/60 dark:border-cc-borderDark/60">
              <span class="text-xs text-slate-500">Phishing Quarantined</span>
              <div class="text-2xl font-extrabold text-amber-500 mt-1">29 SMS</div>
              <span class="text-[10px] text-emerald-500">All credentials safe</span>
            </div>
            <div class="p-4 rounded-2xl bg-slate-50 dark:bg-cc-darkBg border border-slate-200/60 dark:border-cc-borderDark/60">
              <span class="text-xs text-slate-500">Profile Lookups</span>
              <div class="text-2xl font-extrabold text-purple-500 mt-1">14</div>
              <span class="text-[10px] text-purple-400">Viewed your profile</span>
            </div>
          </div>
        </div>

        <!-- Privacy & Profile Lookup Insights -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 shadow-sm">
            <h3 class="font-extrabold text-sm text-slate-900 dark:text-white flex items-center gap-2">
              <svg class="w-4 h-4 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/></svg>
              Who Viewed My Profile (Last 7 Days)
            </h3>
            <p class="text-xs text-slate-500 mt-0.5">Contacts who searched for your phone number recently.</p>

            <div class="mt-4 space-y-3">
              <div class="flex items-center justify-between p-3 rounded-2xl bg-slate-50 dark:bg-cc-darkBg">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-xl bg-cc-blue text-white flex items-center justify-center font-bold text-xs">PK</div>
                  <div>
                    <h4 class="font-bold text-xs text-slate-900 dark:text-white">Priya Kapoor</h4>
                    <p class="text-[10px] text-slate-500">+91 98111 22334 • Architect</p>
                  </div>
                </div>
                <span class="text-[10px] text-slate-400">2 hrs ago</span>
              </div>

              <div class="flex items-center justify-between p-3 rounded-2xl bg-slate-50 dark:bg-cc-darkBg">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-xl bg-emerald-600 text-white flex items-center justify-center font-bold text-xs">AS</div>
                  <div>
                    <h4 class="font-bold text-xs text-slate-900 dark:text-white">Aarav Sharma</h4>
                    <p class="text-[10px] text-slate-500">+91 98200 12345 • Tech Lead</p>
                  </div>
                </div>
                <span class="text-[10px] text-slate-400">Yesterday</span>
              </div>
            </div>
          </div>

          <!-- Backup & Data Portability -->
          <div class="bg-white dark:bg-cc-cardDark border border-slate-200 dark:border-cc-borderDark rounded-3xl p-6 shadow-sm flex flex-col justify-between">
            <div>
              <h3 class="font-extrabold text-sm text-slate-900 dark:text-white flex items-center gap-2">
                <svg class="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"/></svg>
                Backup & Data Portability
              </h3>
              <p class="text-xs text-slate-500 mt-0.5">Export full CraftCaller contacts, database, and logs to JSON.</p>
            </div>

            <div class="mt-6 flex flex-col gap-2.5">
              <button id="export-full-backup-btn" class="w-full py-2.5 bg-cc-blue hover:bg-cc-darkBlue text-white font-bold text-xs rounded-xl shadow-md transition-all flex items-center justify-center gap-2">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
                <span>Export Complete Backup (JSON)</span>
              </button>

              <label class="w-full py-2.5 bg-slate-100 dark:bg-cc-darkBg hover:bg-slate-200 dark:hover:bg-cc-borderDark text-slate-700 dark:text-slate-300 font-bold text-xs rounded-xl transition-all flex items-center justify-center gap-2 cursor-pointer border border-slate-200 dark:border-cc-borderDark">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l4-4m0 0l4 4m-4-4v12"/></svg>
                <span>Restore Backup File</span>
                <input type="file" id="import-backup-input" accept=".json" class="hidden">
              </label>

              <button id="reset-factory-db-btn" class="text-[11px] text-red-500 hover:text-red-700 underline text-center mt-1">
                Reset App Database to Default
              </button>
            </div>
          </div>
        </div>

      </section>
'''

print("Assistant, Simulator, Analytics HTML ready.")
