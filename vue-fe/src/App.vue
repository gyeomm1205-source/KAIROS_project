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

/* ── 다크 테마 (기본) — .app-root + body 모두 적용 (Teleport 지원) ── */
.theme-dark, body.theme-dark {
  --bg-base:      #18181e;
  --bg-surface:   #1e1e24;
  --bg-elevated:  #23232c;
  --bg-hover:     #2a2a35;
  --border:       #2d2d38;
  --border-mid:   #374151;
  --text-primary: #f1f5f9;
  --text-secondary:#c0c8d8;
  --text-muted:   #8896a8;
  --text-faint:   #4b5563;
  --accent:       #3b82f6;
  --accent-glow:  rgba(59,130,246,0.35);
  --sat-color:    #60a5fa;
  --sun-color:    #f87171;
  --today-bg:     rgba(59,130,246,0.08);
  --today-border: rgba(59,130,246,0.4);
  --node-border:  var(--bg-surface);
  --scrollbar-thumb: #374151;
  --scrollbar-track: #17171d;
  /* ── 모달 전용 ── */
  --modal-bg:            #2c2c3e;
  --modal-border:        #4e4e6a;
  --modal-text:          #f1f5f9;
  --modal-sub:           #94a3b8;
  --modal-text:          #f1f5f9;
  --modal-sub:           #94a3b8;
  --modal-divider:       #3a3a52;
  --modal-input-bg:      #1e1e2c;
  --modal-input-focus-bg:#23233a;
  --modal-shadow:
    0 0 0 1px rgba(255,255,255,0.07) inset,
    0 32px 72px rgba(0,0,0,0.75),
    0 12px 32px rgba(0,0,0,0.5);
  color-scheme: dark;
}

/* ── 라이트 테마 ── */
.theme-light, body.theme-light {
  --bg-base:      #eef2f7;
  --bg-surface:   #ffffff;
  --bg-elevated:  #f1f5fb;
  --bg-hover:     #e4eaf4;
  --border:       #d0d8e8;
  --border-mid:   #b0bccf;
  --text-primary: #0f1923;
  --text-secondary:#2c3d52;
  --text-muted:   #506070;
  --text-faint:   #8a99aa;
  --accent:       #2563eb;
  --accent-glow:  rgba(37,99,235,0.2);
  --sat-color:    #2563eb;
  --sun-color:    #dc2626;
  --today-bg:     rgba(37,99,235,0.06);
  --today-border: rgba(37,99,235,0.35);
  --node-border:  #ffffff;
  --scrollbar-thumb: #c4cdd8;
  --scrollbar-track: #edf2f7;
  /* ── 모달 전용 ── */
  --modal-bg:            #ffffff;
  --modal-border:        #e2e8f0;
  --modal-text:          #111827;
  --modal-sub:           #6b7280;
  --modal-divider:       #d8e4f0;
  --modal-input-bg:      #f0f5fb;
  --modal-input-focus-bg:#ffffff;
  --modal-shadow:
    0 0 0 1px rgba(255,255,255,1) inset,
    0 32px 72px rgba(10,20,50,0.22),
    0 12px 32px rgba(10,20,50,0.12);
  color-scheme: light;
}

/* Teleport to="body" 대응 — body에도 같은 변수 전달 */
body.is-dark {
  --modal-bg:            #2c2c3e;
  --modal-border:        #4e4e6a;
  --modal-divider:       #3a3a52;
  --modal-input-bg:      #1e1e2c;
  --modal-input-focus-bg:#23233a;
  --modal-shadow:
    0 0 0 1px rgba(255,255,255,0.07) inset,
    0 32px 72px rgba(0,0,0,0.75),
    0 12px 32px rgba(0,0,0,0.5);
  --text-primary: #f1f5f9;
  --text-muted:   #8896a8;
  --text-faint:   #4b5563;
  --accent:       #3b82f6;
  --accent-glow:  rgba(59,130,246,0.35);
  --bg-hover:     #2a2a35;
  --border-mid:   #374151;
  --scrollbar-thumb: #374151;
}
body.is-light {
  --modal-bg:            #ffffff;
  --modal-border:        #e2e8f0;
  --modal-text:          #111827;
  --modal-sub:           #6b7280;
  --modal-divider:       #d8e4f0;
  --modal-input-bg:      #f0f5fb;
  --modal-input-focus-bg:#ffffff;
  --modal-shadow:
    0 0 0 1px rgba(255,255,255,1) inset,
    0 32px 72px rgba(10,20,50,0.22),
    0 12px 32px rgba(10,20,50,0.12);
  --text-primary: #0f1923;
  --text-muted:   #506070;
  --text-faint:   #8a99aa;
  --accent:       #2563eb;
  --accent-glow:  rgba(37,99,235,0.2);
  --bg-hover:     #e4eaf4;
  --border-mid:   #b0bccf;
  --scrollbar-thumb: #c4cdd8;
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

/* 전역 스크롤바 */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: var(--scrollbar-track); }
::-webkit-scrollbar-thumb { background: var(--scrollbar-thumb); border-radius: 3px; }

/* 전역 선택 색상 */
::selection { background: var(--accent); color: #fff; }

/* ────────────────────────────────────
   Teleport 전역 패널: BranchLegend 드롭다운
──────────────────────────────────── */
.legend-panel-teleport {
  position: fixed;
  min-width: 320px;
  
  /* ★ 핵심 수정: 트랙 개수만큼 넓이가 자동으로 늘어나게 함 */
  width: max-content; 
  /* 화면(브라우저) 넓이보다 커지는 것만 방지 */
  max-width: 90vw; 

  background: #1e1e24;
  background: var(--bg-surface, #1e1e24);
  border: 1px solid #2d2d38;
  border: 1px solid var(--border, #2d2d38);
  border-radius: 14px;
  padding: 16px 18px;
  box-shadow: 0 16px 48px rgba(0,0,0,0.45), 0 0 0 1px rgba(255,255,255,0.06);
  z-index: 9999;
  font-family: 'Escoredream', system-ui, sans-serif;
}

.legend-panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #8896a8;
  color: var(--text-muted, #8896a8);
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #2d2d38;
  border-bottom: 1px solid var(--border, #2d2d38);
}
.legend-panel-title i { color: #818cf8; }

.legend-panel-close {
  margin-left: auto;
  width: 24px; height: 24px;
  border-radius: 6px;
  background: var(--bg-hover);
  border: 1px solid var(--border);
  color: var(--text-faint);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  font-size: 10px;
  transition: all 0.15s;
}
.legend-panel-close:hover { color: var(--text-primary); background: var(--bg-elevated); }

/* Teleport 전환 애니메이션 */
.legend-fade-enter-active { transition: all 0.18s cubic-bezier(0.34,1.2,0.64,1); }
.legend-fade-leave-active { transition: all 0.12s ease; }
.legend-fade-enter-from,
.legend-fade-leave-to   { opacity: 0; transform: translateY(-8px) scale(0.97); }
</style>
