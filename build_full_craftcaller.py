import os
import sys
import json

print("Starting full CraftCaller build...")

# -------------------------------------------------------------
# 1. HEADER & TOP BAR
# -------------------------------------------------------------
HEADER_HTML = '''
  <!-- TOP APP BAR -->
  <header class="sticky top-0 z-40 bg-white/90 dark:bg-cc-cardDark/90 glassmorphism border-b border-slate-200 dark:border-cc-borderDark transition-colors">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-2 sm:gap-4">
      
      <!-- Brand Logo -->
      <div class="flex items-center gap-3 cursor-pointer select-none" id="brand-logo-btn">
        <div class="relative w-10 h-10 rounded-2xl bg-gradient-to-tr from-cc-blue via-blue-600 to-indigo-600 flex items-center justify-center text-white shadow-lg shadow-blue-500/25 ring-2 ring-blue-400/20">
          <svg class="w-6 h-6 transform -rotate-12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            <path d="M9 10a3 3 0 0 0 6 0v-1a3 3 0 0 0-6 0v1z"/>
            <path d="M12 13v3"/>
            <path d="M10 16h4"/>
          </svg>
          <span class="absolute -top-1 -right-1 w-3 h-3 bg-emerald-400 rounded-full border-2 border-white dark:border-cc-darkBg animate-pulse"></span>
        </div>
        <div>
          <div class="flex items-center gap-1.5">
            <span class="font-extrabold text-xl tracking-tight bg-gradient-to-r from-cc-blue via-blue-500 to-indigo-500 bg-clip-text text-transparent">CraftCaller</span>
            <span class="text-[9px] font-black uppercase tracking-wider px-1.5 py-0.5 rounded-full bg-gradient-to-r from-amber-500 to-amber-600 text-white shadow-sm">AI SHIELD</span>
          </div>
          <p class="text-[11px] text-slate-500 dark:text-slate-400 font-medium hidden sm:block">Intelligent Caller ID & Cyber Spam Defense</p>
        </div>
      </div>

      <!-- Quick Search Bar (Desktop / Tablet) -->
      <div class="hidden md:flex flex-1 max-w-md mx-4">
        <div class="relative w-full">
          <input type="text" id="header-search-input" placeholder="Search number or name (e.g. +91 98765...)" 
            class="w-full pl-9 pr-16 py-2 text-xs sm:text-sm rounded-xl bg-slate-100 dark:bg-cc-darkBg border border-slate-300 dark:border-cc-borderDark focus:outline-none focus:ring-2 focus:ring-cc-blue focus:border-transparent text-slate-900 dark:text-white placeholder-slate-400 transition-all">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
          </div>
          <button id="header-search-btn" class="absolute inset-y-0 right-0 pr-2 flex items-center" title="Lookup Number">
            <span class="text-xs font-bold bg-cc-blue text-white px-2.5 py-1 rounded-lg hover:bg-cc-darkBlue shadow-sm transition-all">Lookup</span>
          </button>
        </div>
      </div>

      <!-- Header Action Controls -->
      <div class="flex items-center gap-1 sm:gap-2">
        
        <!-- Simulate Incoming Call CTA -->
        <button id="open-call-simulator-btn" class="relative group flex items-center gap-1.5 px-3 py-2 bg-gradient-to-r from-cc-blue to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white rounded-xl font-bold text-xs sm:text-sm shadow-md shadow-blue-500/25 transition-all transform active:scale-95" title="Simulate Live Incoming Call">
          <span class="w-2 h-2 rounded-full bg-emerald-300 animate-ping"></span>
          <svg class="w-4 h-4 animate-bounce-subtle" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/>
          </svg>
          <span class="hidden sm:inline">Simulate Call</span>
        </button>

        <!-- Sound FX Switch -->
        <button id="toggle-sound-btn" class="p-2.5 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-cc-borderDark transition-colors" title="Toggle Sound Effects (DTMF / Ringtone)">
          <svg id="sound-on-icon" class="w-5 h-5 text-cc-blue" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"/>
          </svg>
          <svg id="sound-off-icon" class="w-5 h-5 hidden text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15zM17 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2"/>
          </svg>
        </button>

        <!-- Smartphone Frame Preview Switch -->
        <button id="toggle-viewmode-btn" class="p-2.5 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-cc-borderDark transition-colors" title="Toggle Phone Mockup / Full Dashboard">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"/>
          </svg>
        </button>

        <!-- Dark/Light Theme Switch -->
        <button id="toggle-theme-btn" class="p-2.5 rounded-xl text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-cc-borderDark transition-colors" title="Toggle Dark/Light Mode">
          <svg id="theme-moon-icon" class="w-5 h-5 hidden dark:block text-yellow-400" fill="currentColor" viewBox="0 0 20 20">
            <path d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z"></path>
          </svg>
          <svg id="theme-sun-icon" class="w-5 h-5 block dark:hidden text-amber-500" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z" clip-rule="evenodd"></path>
          </svg>
        </button>

      </div>
    </div>

    <!-- Desktop Navigation Tabs (Horizontal Scrollbar-free) -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex overflow-x-auto no-scrollbar space-x-1 sm:space-x-3 border-t border-slate-200/60 dark:border-cc-borderDark/60 text-xs sm:text-sm font-semibold">
      
      <button class="nav-tab active-tab py-3 px-3.5 border-b-2 border-cc-blue text-cc-blue dark:text-blue-400 flex items-center gap-2 whitespace-nowrap transition-colors" data-target="tab-search">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
        <span>Caller Search</span>
      </button>

      <button class="nav-tab py-3 px-3.5 border-b-2 border-transparent text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 flex items-center gap-2 whitespace-nowrap transition-colors" data-target="tab-calls">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/></svg>
        <span>Calls & T9 Dialer</span>
        <span id="badge-missed-count" class="bg-red-500 text-white text-[10px] px-1.5 py-0.2 rounded-full font-bold">2</span>
      </button>

      <button class="nav-tab py-3 px-3.5 border-b-2 border-transparent text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 flex items-center gap-2 whitespace-nowrap transition-colors" data-target="tab-messages">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
        <span>SMS & Anti-Phishing</span>
        <span class="bg-blue-100 dark:bg-blue-900/60 text-cc-blue dark:text-blue-300 text-[10px] px-1.5 py-0.2 rounded-full font-bold">Smart OTP</span>
      </button>

      <button class="nav-tab py-3 px-3.5 border-b-2 border-transparent text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 flex items-center gap-2 whitespace-nowrap transition-colors" data-target="tab-spam">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
        <span>Spam Shield</span>
        <span class="bg-cc-spamRed/15 text-cc-spamRed dark:text-red-400 text-[10px] px-1.5 py-0.2 rounded-full font-bold flex items-center gap-1">
          <span class="w-1.5 h-1.5 rounded-full bg-cc-spamRed animate-ping"></span> Live
        </span>
      </button>

      <button class="nav-tab py-3 px-3.5 border-b-2 border-transparent text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 flex items-center gap-2 whitespace-nowrap transition-colors" data-target="tab-contacts">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/></svg>
        <span>Contacts</span>
      </button>

      <button class="nav-tab py-3 px-3.5 border-b-2 border-transparent text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 flex items-center gap-2 whitespace-nowrap transition-colors" data-target="tab-assistant">
        <svg class="w-4 h-4 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z"/></svg>
        <span>AI Assistant</span>
        <span class="bg-purple-100 dark:bg-purple-950 text-purple-600 dark:text-purple-300 text-[10px] px-1.5 py-0.2 rounded-full font-bold">Voice Screen</span>
      </button>

      <button class="nav-tab py-3 px-3.5 border-b-2 border-transparent text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 flex items-center gap-2 whitespace-nowrap transition-colors" data-target="tab-simulator">
        <svg class="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
        <span>Call Simulator</span>
      </button>

      <button class="nav-tab py-3 px-3.5 border-b-2 border-transparent text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 flex items-center gap-2 whitespace-nowrap transition-colors" data-target="tab-analytics">
        <svg class="w-4 h-4 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/></svg>
        <span>Analytics & Backup</span>
      </button>

    </div>
  </header>

  <!-- MOBILE BOTTOM NAVIGATION BAR -->
  <nav class="md:hidden fixed bottom-0 inset-x-0 z-40 bg-white/95 dark:bg-cc-cardDark/95 glassmorphism border-t border-slate-200 dark:border-cc-borderDark flex items-center justify-around py-2 px-1">
    <button class="mobile-nav-btn flex flex-col items-center gap-1 text-cc-blue dark:text-blue-400 flex-1 py-1" data-target="tab-search">
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
      <span class="text-[10px] font-bold">Search</span>
    </button>
    <button class="mobile-nav-btn flex flex-col items-center gap-1 text-slate-500 dark:text-slate-400 flex-1 py-1" data-target="tab-calls">
      <div class="relative">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"/></svg>
        <span class="absolute -top-1 -right-1 w-2 h-2 bg-red-500 rounded-full"></span>
      </div>
      <span class="text-[10px] font-bold">Calls</span>
    </button>
    <button class="mobile-nav-btn flex flex-col items-center gap-1 text-slate-500 dark:text-slate-400 flex-1 py-1" data-target="tab-messages">
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/></svg>
      <span class="text-[10px] font-bold">SMS</span>
    </button>
    <button class="mobile-nav-btn flex flex-col items-center gap-1 text-slate-500 dark:text-slate-400 flex-1 py-1" data-target="tab-spam">
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
      <span class="text-[10px] font-bold">Shield</span>
    </button>
    <button class="mobile-nav-btn flex flex-col items-center gap-1 text-slate-500 dark:text-slate-400 flex-1 py-1" data-target="tab-simulator">
      <svg class="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
      <span class="text-[10px] font-bold">Studio</span>
    </button>
  </nav>

  <!-- MAIN CONTAINER (Supports Phone Mockup & Dashboard view) -->
  <main class="flex-1 max-w-7xl w-full mx-auto p-3 sm:p-6 transition-all duration-300">
    
    <!-- Phone Bezel Wrapper (toggled by #toggle-viewmode-btn) -->
    <div id="app-view-container" class="w-full mx-auto transition-all duration-300">
'''

print("Header & Navigation configured.")
