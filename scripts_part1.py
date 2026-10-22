# -------------------------------------------------------------
# 6. JAVASCRIPT ENGINE - PART 1: AUDIO, SPEECH, DATA & STATE
# -------------------------------------------------------------

SCRIPTS_PART1_JS = '''
  <script>
    // ==========================================
    // 1. AUDIO SYNTHESIZER & SPEECH ENGINE
    // ==========================================
    let audioCtx = null;
    let isRinging = false;
    let isRingback = false;
    let soundEnabled = true;
    let assistantVoiceEnabled = true;

    function initAudio() {
      if (!audioCtx) {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (AudioContext) {
          audioCtx = new AudioContext();
        }
      }
      if (audioCtx && audioCtx.state === 'suspended') {
        audioCtx.resume();
      }
    }

    // Standard DTMF Dual-Tone Frequencies
    const DTMF_FREQS = {
      '1': [697, 1209], '2': [697, 1336], '3': [697, 1477],
      '4': [770, 1209], '5': [770, 1336], '6': [770, 1477],
      '7': [852, 1209], '8': [852, 1336], '9': [852, 1477],
      '*': [941, 1209], '0': [941, 1336], '#': [941, 1477]
    };

    function playTone(digit) {
      if (!soundEnabled) return;
      try {
        initAudio();
        if (!audioCtx) return;
        const freqs = DTMF_FREQS[digit] || [440, 880];
        const osc1 = audioCtx.createOscillator();
        const osc2 = audioCtx.createOscillator();
        const gain = audioCtx.createGain();

        osc1.type = 'sine';
        osc2.type = 'sine';
        osc1.frequency.value = freqs[0];
        osc2.frequency.value = freqs[1];

        const now = audioCtx.currentTime;
        gain.gain.setValueAtTime(0.12, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 0.18);

        osc1.connect(gain);
        osc2.connect(gain);
        gain.connect(audioCtx.destination);

        osc1.start(now);
        osc2.start(now);
        osc1.stop(now + 0.18);
        osc2.stop(now + 0.18);

        if (navigator.vibrate) {
          navigator.vibrate(25);
        }
      } catch (e) {}
    }

    // Realistic Telephone Ringtone (440Hz + 480Hz modulated cadence)
    function startRingtone() {
      if (!soundEnabled) return;
      try {
        initAudio();
        if (!audioCtx) return;
        stopRingtone();
        isRinging = true;

        const pulseRing = () => {
          if (!isRinging) return;
          try {
            const now = audioCtx.currentTime;
            const oscA = audioCtx.createOscillator();
            const oscB = audioCtx.createOscillator();
            const gain = audioCtx.createGain();

            oscA.frequency.value = 440;
            oscB.frequency.value = 480;
            oscA.type = 'sine';
            oscB.type = 'sine';

            gain.gain.setValueAtTime(0, now);
            gain.gain.linearRampToValueAtTime(0.14, now + 0.05);
            gain.gain.setValueAtTime(0.14, now + 1.4);
            gain.gain.linearRampToValueAtTime(0, now + 1.45);

            oscA.connect(gain);
            oscB.connect(gain);
            gain.connect(audioCtx.destination);

            oscA.start(now);
            oscB.start(now);
            oscA.stop(now + 1.45);
            oscB.stop(now + 1.45);

            if (navigator.vibrate) {
              navigator.vibrate([400, 200, 400]);
            }

            if (isRinging) {
              window.ringInterval = setTimeout(pulseRing, 3500);
            }
          } catch (err) {}
        };

        pulseRing();
      } catch (e) {}
    }

    function stopRingtone() {
      isRinging = false;
      if (window.ringInterval) {
        clearTimeout(window.ringInterval);
        window.ringInterval = null;
      }
    }

    // Outgoing Ringback Tone
    function playRingbackTone() {
      if (!soundEnabled) return;
      try {
        initAudio();
        if (!audioCtx) return;
        const now = audioCtx.currentTime;
        const oscA = audioCtx.createOscillator();
        const oscB = audioCtx.createOscillator();
        const gain = audioCtx.createGain();

        oscA.frequency.value = 440;
        oscB.frequency.value = 480;
        gain.gain.setValueAtTime(0.08, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 1.2);

        oscA.connect(gain);
        oscB.connect(gain);
        gain.connect(audioCtx.destination);
        oscA.start(now);
        oscB.start(now);
        oscA.stop(now + 1.2);
        oscB.stop(now + 1.2);
      } catch (e) {}
    }

    // Connected Chime (Upbeat Harmonic Triad)
    function playConnectedChime() {
      if (!soundEnabled) return;
      try {
        initAudio();
        if (!audioCtx) return;
        const freqs = [523.25, 659.25, 783.99]; // C5, E5, G5
        freqs.forEach((freq, idx) => {
          const osc = audioCtx.createOscillator();
          const gain = audioCtx.createGain();
          const start = audioCtx.currentTime + (idx * 0.08);
          osc.frequency.value = freq;
          gain.gain.setValueAtTime(0.08, start);
          gain.gain.exponentialRampToValueAtTime(0.001, start + 0.2);
          osc.connect(gain);
          gain.connect(audioCtx.destination);
          osc.start(start);
          osc.stop(start + 0.2);
        });
      } catch (e) {}
    }

    // Disconnect Busy Tone
    function playHangupTone() {
      if (!soundEnabled) return;
      try {
        initAudio();
        if (!audioCtx) return;
        [0, 0.35, 0.7].forEach(delay => {
          const osc = audioCtx.createOscillator();
          const gain = audioCtx.createGain();
          const start = audioCtx.currentTime + delay;
          osc.frequency.value = 480;
          gain.gain.setValueAtTime(0.09, start);
          gain.gain.exponentialRampToValueAtTime(0.001, start + 0.22);
          osc.connect(gain);
          gain.connect(audioCtx.destination);
          osc.start(start);
          osc.stop(start + 0.22);
        });
      } catch (e) {}
    }

    function playBeep(freq, duration) {
      freq = freq || 600;
      duration = duration || 0.1;
      if (!soundEnabled) return;
      try {
        initAudio();
        if (!audioCtx) return;
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.08, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + duration);
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.start();
        osc.stop(audioCtx.currentTime + duration);
      } catch (e) {}
    }

    // Speech Synthesis for AI Assistant
    function speakAssistantText(text) {
      if (!assistantVoiceEnabled || !('speechSynthesis' in window)) return;
      try {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.rate = 1.05;
        utterance.pitch = 1.0;
        const voices = window.speechSynthesis.getVoices();
        const englishVoice = voices.find(v => v.lang.startsWith('en') && (v.name.includes('Google') || v.name.includes('Natural') || v.name.includes('Samantha')));
        if (englishVoice) utterance.voice = englishVoice;
        window.speechSynthesis.speak(utterance);
      } catch (e) {}
    }

    // ==========================================
    // 2. TOAST NOTIFICATION SYSTEM
    // ==========================================
    function showToast(message, type) {
      type = type || 'info';
      const container = document.getElementById('toast-container');
      const toast = document.createElement('div');
      toast.className = 'pointer-events-auto flex items-center gap-2.5 px-4 py-3 rounded-2xl shadow-2xl border text-xs sm:text-sm font-bold transition-all transform duration-300 translate-y-4 opacity-0 max-w-sm';
      
      let bg = 'bg-slate-900 text-white border-slate-700';
      let icon = '<svg class="w-5 h-5 text-cc-blue flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>';

      if (type === 'success') {
        bg = 'bg-emerald-950 text-emerald-100 border-emerald-700';
        icon = '<svg class="w-5 h-5 text-emerald-400 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path></svg>';
      } else if (type === 'error' || type === 'spam') {
        bg = 'bg-red-950 text-red-100 border-red-700';
        icon = '<svg class="w-5 h-5 text-red-400 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"></path></svg>';
      } else if (type === 'gold') {
        bg = 'bg-amber-950 text-amber-100 border-amber-700';
        icon = '<span class="text-base">👑</span>';
      }

      toast.className += ' ' + bg;
      toast.innerHTML = icon + '<span class="flex-1">' + message + '</span>';
      container.appendChild(toast);

      requestAnimationFrame(() => {
        toast.classList.remove('translate-y-4', 'opacity-0');
      });

      setTimeout(() => {
        toast.classList.add('translate-y-4', 'opacity-0');
        setTimeout(() => {
          if (toast.parentNode) toast.parentNode.removeChild(toast);
        }, 350);
      }, 3500);
    }

    // ==========================================
    // 3. INITIAL MOCK DATA (CRAFT DIRECTORY)
    // ==========================================
    const DEFAULT_DATABASE = [
      {
        id: 'cc-1',
        name: 'SBI Credit Card Loan Scam',
        number: '+91 98765 43210',
        cleanNumber: '919876543210',
        type: 'spam',
        category: 'Financial Fraud',
        reports: 14820,
        carrier: 'Vi Telephony',
        location: 'New Delhi, India',
        safeScore: 2,
        verified: false,
        gold: false,
        comments: [
          { user: 'Vikram M.', text: 'Claimed my SBI card was blocked and asked for card expiry date and CVV!', time: '12 mins ago' },
          { user: 'Neha P.', text: 'Dangerous robocall pretending to be bank manager.', time: '1 hour ago' }
        ]
      },
      {
        id: 'cc-2',
        name: 'Aarav Sharma',
        number: '+91 98200 12345',
        cleanNumber: '919820012345',
        type: 'gold',
        category: 'Personal / VP Engineering @ TechNova',
        reports: 0,
        carrier: 'Jio 5G',
        location: 'Mumbai, Maharashtra',
        safeScore: 100,
        verified: true,
        gold: true,
        comments: []
      },
      {
        id: 'cc-3',
        name: 'FedEx Customs Impersonator',
        number: '+91 80000 99888',
        cleanNumber: '918000099888',
        type: 'spam',
        category: 'Courier Extortion / Digital Arrest',
        reports: 22410,
        carrier: 'International VoIP',
        location: 'Virtual Gateway',
        safeScore: 1,
        verified: false,
        gold: false,
        comments: [
          { user: 'Karan S.', text: 'Threatened digital arrest for Taiwan package with illegal contraband. Complete extortion!', time: '35 mins ago' }
        ]
      },
      {
        id: 'cc-4',
        name: 'HDFC Priority Banking',
        number: '+91 1800 266 7100',
        cleanNumber: '9118002667100',
        type: 'verified',
        category: 'Banking & Financial Services',
        reports: 12,
        carrier: 'Tata Teleservices',
        location: 'All India Toll-Free',
        safeScore: 99,
        verified: true,
        gold: false,
        comments: [
          { user: 'Craft Enterprise Bot', text: 'Verified official corporate phone line for HDFC Bank priority desk.', time: 'Verified' }
        ]
      },
      {
        id: 'cc-5',
        name: 'Dr. Sunita Patel',
        number: '+91 91234 56789',
        cleanNumber: '919123456789',
        type: 'safe',
        category: 'Healthcare & Cardiology Clinic',
        reports: 0,
        carrier: 'Airtel 5G',
        location: 'Bangalore, Karnataka',
        safeScore: 99,
        verified: true,
        gold: false,
        comments: []
      },
      {
        id: 'cc-6',
        name: 'PolicyBazaar Sales Desk',
        number: '+91 98990 01122',
        cleanNumber: '919899001122',
        type: 'spam',
        category: 'Telemarketing',
        reports: 5120,
        carrier: 'Airtel Telephony',
        location: 'Gurgaon, Haryana',
        safeScore: 22,
        verified: false,
        gold: false,
        comments: [
          { user: 'Rahul D.', text: 'Calls 4 times every week offering term life insurance policy renewal.', time: 'Yesterday' }
        ]
      },
      {
        id: 'cc-7',
        name: 'Amazon Prime Delivery',
        number: '+91 98877 66554',
        cleanNumber: '919887766554',
        type: 'verified',
        category: 'Logistics & Courier Delivery',
        reports: 2,
        carrier: 'Vodafone Idea',
        location: 'Lucknow, UP',
        safeScore: 98,
        verified: true,
        gold: false,
        comments: [
          { user: 'Amazon Logistics', text: 'Certified delivery partner line. Verification badge issued.', time: 'Verified' }
        ]
      },
      {
        id: 'cc-8',
        name: 'Electricity Disconnection Scam',
        number: '+91 99999 00000',
        cleanNumber: '919999900000',
        type: 'spam',
        category: 'Utility Disconnection Phishing',
        reports: 18940,
        carrier: 'BSNL Mobile',
        location: 'Uttar Pradesh (East)',
        safeScore: 1,
        verified: false,
        gold: false,
        comments: [
          { user: 'Mohit G.', text: 'Sends fake SMS: Power disconnected tonight at 9:30 PM, call this number!', time: '2 hours ago' }
        ]
      },
      {
        id: 'cc-9',
        name: 'IRS Tax Audit Robocall',
        number: '+1 800 555 0199',
        cleanNumber: '18005550199',
        type: 'spam',
        category: 'Government Impersonation',
        reports: 9840,
        carrier: 'US VoIP Gateway',
        location: 'United States',
        safeScore: 2,
        verified: false,
        gold: false,
        comments: [
          { user: 'Dave K.', text: 'Automated voice threatening arrest for unpaid federal taxes.', time: 'Today' }
        ]
      },
      {
        id: 'cc-10',
        name: 'Priya Kapoor',
        number: '+91 98111 22334',
        cleanNumber: '919811122334',
        type: 'gold',
        category: 'Principal Architect & Urbanist',
        reports: 0,
        carrier: 'Airtel 5G',
        location: 'New Delhi, India',
        safeScore: 100,
        verified: true,
        gold: true,
        comments: []
      }
    ];

    let db = [];
    try {
      const stored = localStorage.getItem('callercraft_db');
      db = stored ? JSON.parse(stored) : DEFAULT_DATABASE;
    } catch (e) {
      db = DEFAULT_DATABASE;
    }

    function saveDB() {
      try {
        localStorage.setItem('callercraft_db', JSON.stringify(db));
      } catch (e) {}
    }

    // Call Logs Data
    let callLogs = [
      {
        id: 'log-1',
        name: 'SBI Credit Card Loan Scam',
        number: '+91 98765 43210',
        type: 'spam',
        category: 'Financial Fraud',
        callType: 'blocked',
        time: 'Today, 11:20 AM',
        duration: '0s (Auto-blocked)'
      },
      {
        id: 'log-2',
        name: 'Amazon Prime Delivery',
        number: '+91 98877 66554',
        type: 'verified',
        category: 'Logistics',
        callType: 'incoming',
        time: 'Today, 10:45 AM',
        duration: '42s'
      },
      {
        id: 'log-3',
        name: 'Aarav Sharma',
        number: '+91 98200 12345',
        type: 'gold',
        category: 'Personal',
        callType: 'outgoing',
        time: 'Yesterday, 6:15 PM',
        duration: '3m 12s'
      },
      {
        id: 'log-4',
        name: 'PolicyBazaar Sales Desk',
        number: '+91 98990 01122',
        type: 'spam',
        category: 'Telemarketing',
        callType: 'missed',
        time: 'Yesterday, 3:30 PM',
        duration: 'Missed Call'
      },
      {
        id: 'log-5',
        name: 'Dr. Sunita Patel',
        number: '+91 91234 56789',
        type: 'safe',
        category: 'Healthcare',
        callType: 'incoming',
        time: '18 Oct, 2:10 PM',
        duration: '1m 45s'
      }
    ];

    try {
      const storedLogs = localStorage.getItem('callercraft_logs');
      if (storedLogs) callLogs = JSON.parse(storedLogs);
    } catch (e) {}

    function saveLogs() {
      try {
        localStorage.setItem('callercraft_logs', JSON.stringify(callLogs));
      } catch (e) {}
    }

    // Smart SMS Messages Data
    const DEFAULT_MESSAGES = [
      {
        id: 'msg-1',
        sender: 'HDFC Bank',
        senderNumber: '+91 1800 266 7100',
        category: 'transactions',
        isVerified: true,
        body: 'Your OTP for transaction of Rs. 4,250 at Amazon India is 749201. Valid for 10 mins. DO NOT share this OTP with anyone.',
        otp: '749201',
        time: '10 mins ago',
        isSpam: false,
        thread: [
          { sender: 'HDFC Bank', text: 'Alert: Rs 12,000 credited to a/c **4910 via IMPS.', time: 'Yesterday' },
          { sender: 'HDFC Bank', text: 'Your OTP for transaction of Rs. 4,250 at Amazon India is 749201. Valid for 10 mins. DO NOT share this OTP with anyone.', time: '10 mins ago' }
        ]
      },
      {
        id: 'msg-2',
        sender: 'State Bank Alert',
        senderNumber: '+91 98765 43210',
        category: 'spam',
        isVerified: false,
        body: 'URGENT: Your SBI YONO Account suspended due to pending KYC update. Click here to verify immediately: http://bit.ly/sbi-kyc-verify-492',
        otp: null,
        time: '25 mins ago',
        isSpam: true,
        threatReason: 'Phishing: Deceptive URL mimicking State Bank of India login portal.',
        thread: [
          { sender: 'State Bank Alert', text: 'URGENT: Your SBI YONO Account suspended due to pending KYC update. Click here to verify immediately: http://bit.ly/sbi-kyc-verify-492', time: '25 mins ago' }
        ]
      },
      {
        id: 'msg-3',
        sender: 'Aarav Sharma',
        senderNumber: '+91 98200 12345',
        category: 'personal',
        isVerified: true,
        body: 'Hey! Are we still catching up for coffee today at 5 PM at Starbucks?',
        otp: null,
        time: '1 hour ago',
        isSpam: false,
        thread: [
          { sender: 'Aarav Sharma', text: 'Hey! Are we still catching up for coffee today at 5 PM at Starbucks?', time: '1 hour ago' }
        ]
      },
      {
        id: 'msg-4',
        sender: 'Swiggy Gourmet',
        senderNumber: 'SWIGGY',
        category: 'promotions',
        isVerified: true,
        body: 'Craving Biryani? Get 50% OFF up to Rs 120 on your favorite restaurants. Use code CRAFT50 today!',
        otp: null,
        time: '3 hours ago',
        isSpam: false,
        thread: [
          { sender: 'Swiggy Gourmet', text: 'Craving Biryani? Get 50% OFF up to Rs 120 on your favorite restaurants. Use code CRAFT50 today!', time: '3 hours ago' }
        ]
      },
      {
        id: 'msg-5',
        sender: 'Zomato Delivery',
        senderNumber: 'ZOMATO',
        category: 'transactions',
        isVerified: true,
        body: 'Your food is arriving! Give PIN 8391 to delivery partner Ramesh to verify delivery.',
        otp: '8391',
        time: 'Yesterday, 8:40 PM',
        isSpam: false,
        thread: [
          { sender: 'Zomato Delivery', text: 'Your food is arriving! Give PIN 8391 to delivery partner Ramesh to verify delivery.', time: 'Yesterday, 8:40 PM' }
        ]
      }
    ];

    let messages = [];
    try {
      const storedMsgs = localStorage.getItem('callercraft_messages');
      messages = storedMsgs ? JSON.parse(storedMsgs) : DEFAULT_MESSAGES;
    } catch (e) {
      messages = DEFAULT_MESSAGES;
    }

    function saveMessages() {
      try {
        localStorage.setItem('callercraft_messages', JSON.stringify(messages));
      } catch (e) {}
    }

    // Custom Blocklist
    let customBlocklist = [
      '+91 98765 43210',
      '+91 80000 99888',
      '+91 99999 00000',
      '+1 800 555 0199',
      '+91 140*'
    ];

    try {
      const storedBlocklist = localStorage.getItem('callercraft_blocklist');
      if (storedBlocklist) customBlocklist = JSON.parse(storedBlocklist);
    } catch (e) {}

    function saveBlocklist() {
      try {
        localStorage.setItem('callercraft_blocklist', JSON.stringify(customBlocklist));
      } catch (e) {}
    }
'''

print("Scripts Part 1 prepared.")
