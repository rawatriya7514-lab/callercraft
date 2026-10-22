import os
import sys
import json

print("Crafting CallerCraft Application...")

# -------------------------------------------------------------
# Part 1: HTML Header, Styles & Tailwind Configuration
# -------------------------------------------------------------
HEAD_CONTENT = '''<!DOCTYPE html>
<html lang="en" class="h-full">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
  <title>CallerCraft - Smart Caller ID, AI Spam Shield & Phone Intelligence</title>
  
  <!-- Web App Metadata & Favicon -->
  <meta name="theme-color" content="#0D6EFD">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
  <meta name="apple-mobile-web-app-title" content="CallerCraft">
  <link rel="manifest" href="manifest.json">
  
  <!-- Fonts & Tailwind CSS CDN -->
  <script src="https://cdn.tailwindcss.com/3.4.17"></script>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&family=JetBrains+Mono:wght@400;600;700&display=swap" rel="stylesheet">
  
  <script>
    tailwind.config = {
      darkMode: 'class',
      theme: {
        extend: {
          fontFamily: {
            sans: ['"Plus Jakarta Sans"', 'system-ui', '-apple-system', 'sans-serif'],
            mono: ['"JetBrains Mono"', 'monospace'],
          },
          colors: {
            cc: {
              blue: '#0D6EFD',
              darkBlue: '#0A58CA',
              lightBlue: '#E7F1FF',
              darkBg: '#090D16',
              cardDark: '#121A29',
              borderDark: '#1E2B42',
              spamRed: '#EF4444',
              spamDark: '#7F1D1D',
              spamBg: '#FEE2E2',
              safeGreen: '#10B981',
              safeBg: '#D1FAE5',
              gold: '#F59E0B',
              goldBg: '#FEF3C7',
              goldDark: '#78350F',
              purple: '#8B5CF6',
              cyan: '#06B6D4'
            }
          },
          animation: {
            'pulse-fast': 'pulse 1s cubic-bezier(0.4, 0, 0.6, 1) infinite',
            'bounce-subtle': 'bounceSubtle 2s infinite',
            'radar-pulse': 'radarPulse 1.8s infinite',
            'shimmer': 'shimmer 2.5s infinite linear',
            'wave-bar': 'waveBar 1.2s ease-in-out infinite alternate',
          },
          keyframes: {
            bounceSubtle: {
              '0%, 100%': { transform: 'translateY(0)' },
              '50%': { transform: 'translateY(-4px)' },
            },
            radarPulse: {
              '0%': { transform: 'scale(0.96)', opacity: '1' },
              '70%': { transform: 'scale(1.16)', opacity: '0.1' },
              '100%': { transform: 'scale(1.22)', opacity: '0' },
            },
            shimmer: {
              '0%': { backgroundPosition: '-200% 0' },
              '100%': { backgroundPosition: '200% 0' },
            },
            waveBar: {
              '0%': { height: '15%' },
              '100%': { height: '100%' },
            }
          }
        }
      }
    }
  </script>
  
  <style>
    /* Custom Scrollbars */
    ::-webkit-scrollbar { width: 6px; height: 6px; }
    ::-webkit-scrollbar-track { background: transparent; }
    ::-webkit-scrollbar-thumb { background: rgba(148, 163, 184, 0.4); border-radius: 9999px; }
    ::-webkit-scrollbar-thumb:hover { background: rgba(100, 116, 139, 0.7); }
    .no-scrollbar::-webkit-scrollbar { display: none; }
    .no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }

    .glassmorphism {
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
    }
    .aurora-glow {
      background: radial-gradient(circle at 50% 0%, rgba(13, 110, 253, 0.15) 0%, rgba(9, 13, 22, 0) 70%);
    }
    .phone-bezel {
      box-shadow: 0 25px 60px -15px rgba(0, 0, 0, 0.5), 0 0 0 12px #0f172a, 0 0 0 15px #334155;
    }
    
    /* Radar Pulses */
    @keyframes pulseSpam {
      0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.75); }
      70% { transform: scale(1.04); box-shadow: 0 0 0 24px rgba(239, 68, 68, 0); }
      100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
    }
    .radar-pulse-spam { animation: pulseSpam 1.8s infinite; }

    @keyframes pulseSafe {
      0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(13, 110, 253, 0.75); }
      70% { transform: scale(1.04); box-shadow: 0 0 0 24px rgba(13, 110, 253, 0); }
      100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(13, 110, 253, 0); }
    }
    .radar-pulse-safe { animation: pulseSafe 1.8s infinite; }

    @keyframes pulseGold {
      0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.75); }
      70% { transform: scale(1.04); box-shadow: 0 0 0 24px rgba(245, 158, 11, 0); }
      100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(245, 158, 11, 0); }
    }
    .radar-pulse-gold { animation: pulseGold 1.8s infinite; }

    @keyframes pulseGreen {
      0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.75); }
      70% { transform: scale(1.04); box-shadow: 0 0 0 24px rgba(16, 185, 129, 0); }
      100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
    }
    .radar-pulse-green { animation: pulseGreen 1.8s infinite; }

    /* Audio wave bars */
    .bar-1 { animation: waveBar 0.8s ease-in-out infinite alternate; }
    .bar-2 { animation: waveBar 0.6s ease-in-out infinite alternate 0.2s; }
    .bar-3 { animation: waveBar 1.0s ease-in-out infinite alternate 0.4s; }
    .bar-4 { animation: waveBar 0.7s ease-in-out infinite alternate 0.1s; }
    .bar-5 { animation: waveBar 0.9s ease-in-out infinite alternate 0.3s; }
  </style>
</head>
<body class="h-full bg-slate-100 dark:bg-cc-darkBg text-slate-900 dark:text-slate-100 font-sans transition-colors duration-200 flex flex-col overflow-x-hidden pb-16 md:pb-0">
'''

print("Head content prepared.")
