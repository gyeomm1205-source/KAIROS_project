<template>
  <div :class="['app-root', themeClass]">
    <RouterView />
  </div>
</template>

<script setup>
import { useThemeStore } from '@/stores/useThemeStore'
import { storeToRefs } from 'pinia'
import { watch } from 'vue'

const themeStore = useThemeStore()
const { themeClass } = storeToRefs(themeStore)

// Teleport된 요소(Legend 패널 등)도 테마 CSS 변수를 쓸 수 있도록 body에 클래스 동기화
watch(themeClass, (cls) => {
  document.body.classList.remove('theme-dark', 'theme-light')
  document.body.classList.add(cls)
}, { immediate: true })
</script>

<style>
/* ── S-CoreDream 폰트 ── */
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-1Thin.woff') format('woff'); font-weight: 100; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-2ExtraLight.woff') format('woff'); font-weight: 200; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-3Light.woff') format('woff'); font-weight: 300; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-4Regular.woff') format('woff'); font-weight: 400; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-5Medium.woff') format('woff'); font-weight: 500; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-6Bold.woff') format('woff'); font-weight: 600; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-7ExtraBold.woff') format('woff'); font-weight: 700; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-8Heavy.woff') format('woff'); font-weight: 800; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-9Black.woff') format('woff'); font-weight: 900; font-display: swap; }

*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

:root {
  --month-row-height: 150px;
}

/* ── 다크 테마 (Monochrome Minimalism 기본) ── */
.theme-dark, body.theme-dark {
  --bg-base:      #000000;
  --bg-surface:   #000000;
  --bg-elevated:  #0a0a0a;
  --bg-hover:     rgba(255,255,255,0.05);
  --border:       rgba(255,255,255,0.15);
  --border-mid:   rgba(255,255,255,0.3);
  --text-primary: #ffffff;
  --text-secondary:rgba(255,255,255,0.8);
  --text-muted:   rgba(255,255,255,0.5);
  --text-faint:   rgba(255,255,255,0.3);
  --accent:       #ffffff;
  --accent-glow:  rgba(255,255,255,0.1);
  --scrollbar-thumb: rgba(255,255,255,0.2);
  --scrollbar-track: #000000;
  
  /* ── 모달 전용 ── */
  --modal-bg:            #000000;
  --modal-border:        rgba(255,255,255,0.15);
  --modal-text:          #ffffff;
  --modal-sub:           rgba(255,255,255,0.5);
  --modal-divider:       rgba(255,255,255,0.15);
  --modal-input-bg:      transparent;
  --modal-input-focus-bg:rgba(255,255,255,0.05);
  --modal-shadow:        0 0 0 1px rgba(255,255,255,0.15);
  color-scheme: dark;
}

/* ── 라이트 테마 (Monochrome Minimalism 옵션) ── */
.theme-light, body.theme-light {
  --bg-base:      #ffffff;
  --bg-surface:   #ffffff;
  --bg-elevated:  #f5f5f5;
  --bg-hover:     rgba(0,0,0,0.05);
  --border:       rgba(0,0,0,0.15);
  --border-mid:   rgba(0,0,0,0.3);
  --text-primary: #000000;
  --text-secondary:rgba(0,0,0,0.8);
  --text-muted:   rgba(0,0,0,0.5);
  --text-faint:   rgba(0,0,0,0.3);
  --accent:       #000000;
  --accent-glow:  rgba(0,0,0,0.1);
  --scrollbar-thumb: rgba(0,0,0,0.2);
  --scrollbar-track: #ffffff;
  
  /* ── 모달 전용 ── */
  --modal-bg:            #ffffff;
  --modal-border:        rgba(0,0,0,0.15);
  --modal-text:          #000000;
  --modal-sub:           rgba(0,0,0,0.5);
  --modal-divider:       rgba(0,0,0,0.15);
  --modal-input-bg:      transparent;
  --modal-input-focus-bg:rgba(0,0,0,0.05);
  --modal-shadow:        0 0 0 1px rgba(0,0,0,0.15);
  color-scheme: light;
}

/* Teleport to="body" 대응 — body에도 같은 변수 전달 */
body.is-dark {
  --modal-bg:            #0a0a0a;
  --modal-border:        #333333;
  --modal-divider:       #222222;
  --modal-input-bg:      #000000;
  --modal-input-focus-bg:#111111;
  --modal-shadow:
    0 0 0 1px rgba(255,255,255,0.1) inset,
    0 32px 72px rgba(0,0,0,0.9);
  --text-primary: #ffffff;
  --text-muted:   #888888;
  --text-faint:   #444444;
  --accent:       #ffffff;
  --accent-glow:  rgba(255,255,255,0.15);
  --bg-hover:     #1a1a1a;
  --border-mid:   #555555;
  --scrollbar-thumb: #333333;
}
body.is-light {
  --modal-bg:            #ffffff;
  --modal-border:        #000000;
  --modal-text:          #000000;
  --modal-sub:           #666666;
  --modal-divider:       #cccccc;
  --modal-input-bg:      #f9f9f9;
  --modal-input-focus-bg:#ffffff;
  --modal-shadow:
    0 0 0 1px rgba(0,0,0,1) inset,
    0 16px 40px rgba(0,0,0,0.1);
  --text-primary: #000000;
  --text-muted:   #666666;
  --text-faint:   #999999;
  --accent:       #000000;
  --accent-glow:  rgba(0,0,0,0.1);
  --bg-hover:     #e0e0e0;
  --border-mid:   #666666;
  --scrollbar-thumb: #cccccc;
}

body {
  font-family: 'Escoredream', 'Segoe UI', system-ui, sans-serif;
  background: var(--bg-base);
  color: var(--text-primary);
  font-weight: 400;
  line-height: 1.5;
  overflow: hidden;
}

.app-root { width: 100%; height: 100vh; overflow: hidden; }

/* 전역 스크롤바 - 직각으로 변경 */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: var(--scrollbar-track); }
::-webkit-scrollbar-thumb { background: var(--scrollbar-thumb); border-radius: 0; }

/* 전역 선택 색상 - 흑백 반전 */
::selection { background: var(--text-primary); color: var(--bg-base); }

/* ────────────────────────────────────
   GLOBAL UTILITIES (Monochrome & Locomotive)
──────────────────────────────────── */

/* 1. Scroll Reveal utilities */
.reveal-wrap { overflow: hidden; display: block; }
.reveal-elem {
  opacity: 0; transform: translateY(110%);
  transition: transform 1.2s cubic-bezier(0.16, 1, 0.3, 1), opacity 1.2s ease;
  will-change: transform, opacity;
}
.reveal-elem.is-revealed { opacity: 1; transform: translateY(0); }
.delay-1 { transition-delay: 0.15s; }
.delay-2 { transition-delay: 0.3s; }

/* 2. Magnetic Buttons Base */
.btn-magnetic {
  padding: 12px 24px; background: transparent; color: var(--text-primary);
  border: 1px solid var(--border); border-radius: 40px;
  font-size: 13px; font-weight: 700; letter-spacing: 0.05em;
  transition: background 0.3s, color 0.3s, border-color 0.3s, transform 0.1s ease-out;
  display: inline-flex; align-items: center; justify-content: center;
  font-family: inherit; white-space: nowrap; cursor: pointer;
}
.btn-magnetic:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.btn-magnetic.btn-primary { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }
.btn-magnetic.btn-primary:hover { background: var(--bg-hover); color: var(--text-primary); }

/* 3. Global Section Defaults */
.content-section { padding: 120px 40px; max-width: 1400px; margin: 0 auto; }
.section-title { font-size: clamp(32px, 5vw, 56px); font-weight: 900; letter-spacing: -0.02em; line-height: 1.1; margin: 0 0 16px 0; }
.section-sub { font-size: 18px; color: var(--text-muted); font-weight: 500; }
.border-y { border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); }


/* ────────────────────────────────────
   Teleport 전역 패널: BranchLegend 드롭다운
──────────────────────────────────── */
.legend-panel-teleport {
  position: fixed;
  min-width: 320px;
  width: max-content; 
  max-width: 90vw; 

  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 0; /* 직각 */
  padding: 16px 18px;
  box-shadow: 0 16px 48px rgba(0,0,0,0.6), 0 0 0 1px var(--border);
  z-index: 9999;
  font-family: 'Escoredream', system-ui, sans-serif;
}

.legend-panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}
.legend-panel-title i { color: var(--text-primary); }

.legend-panel-close {
  margin-left: auto;
  width: 24px; height: 24px;
  border-radius: 0; /* 직각 */
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-faint);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  font-size: 10px;
  transition: all 0.15s;
}
.legend-panel-close:hover { 
  color: var(--bg-base); 
  background: var(--text-primary); 
  border-color: var(--text-primary);
}

/* Teleport 전환 애니메이션 */
.legend-fade-enter-active { transition: all 0.18s cubic-bezier(0.34,1.2,0.64,1); }
.legend-fade-leave-active { transition: all 0.12s ease; }
.legend-fade-enter-from,
.legend-fade-leave-to   { opacity: 0; transform: translateY(-8px) scale(0.97); }
</style>