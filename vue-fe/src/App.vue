<template>
  <div :class="['app-root', themeClass]">
    <RouterView />
    <!-- 전역 커서 — LandingPage와 동일한 dot + ring -->
    <div class="g-cur-dot" ref="dotEl" />
    <div class="g-cur-ring" ref="ringEl" :class="{ big: curBig }" />
  </div>
</template>

<script setup>
import { useThemeStore } from '@/stores/useThemeStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { storeToRefs } from 'pinia'
import { watch, onMounted, onUnmounted, ref } from 'vue'

// ── 전역 커서 ──
const dotEl  = ref(null)
const ringEl = ref(null)
const curBig = ref(false)

let cx = -300, cy = -300, rx = -300, ry = -300, rafId = null

function onMouse(e) { cx = e.clientX; cy = e.clientY }

function tickCursor() {
  if (dotEl.value)  dotEl.value.style.transform  = `translate(${cx - 4}px, ${cy - 4}px)`
  rx += (cx - rx) * 0.1
  ry += (cy - ry) * 0.1
  const sz = curBig.value ? 44 : 22
  if (ringEl.value) ringEl.value.style.transform = `translate(${rx - sz / 2}px, ${ry - sz / 2}px)`
  rafId = requestAnimationFrame(tickCursor)
}

// 클릭 가능한 요소에 hover 시 ring 확대
function onPointerOver(e) {
  const el = e.target.closest('a, button, [role="button"], input, select, textarea, label, [tabindex], .cursor-pointer')
  curBig.value = !!el
}

const themeStore = useThemeStore()
const authStore = useAuthStore()
const { themeClass } = storeToRefs(themeStore)

// Teleport된 요소(Legend 패널 등)도 테마 CSS 변수를 쓸 수 있도록 body에 클래스 동기화
watch(themeClass, (cls) => {
  document.body.classList.remove('theme-dark', 'theme-light')
  document.body.classList.add(cls)
}, { immediate: true })

onMounted(() => {
  // 사용자가 로그인 상태(토큰 있음)인데 프로필 정보가 없다면 서버에서 불러오기 (새로고침 대응)
  if (authStore.isAuthenticated && !authStore.profile) {
    authStore.fetchMe()
  }
  // 커서 시작
  tickCursor()
  window.addEventListener('mousemove', onMouse)
  document.addEventListener('pointerover', onPointerOver)
})

onUnmounted(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('mousemove', onMouse)
  document.removeEventListener('pointerover', onPointerOver)
})
</script>

<style>
/* ── 전역 커서 ── */
@media (hover: hover) and (pointer: fine) {
  * { cursor: none !important; }
}
.g-cur-dot {
  position: fixed; top: 0; left: 0;
  width: 8px; height: 8px;
  background: var(--clr-primary);
  border-radius: 50%;
  pointer-events: none;
  z-index: 99999;
  will-change: transform;
}
.g-cur-ring {
  position: fixed; top: 0; left: 0;
  width: 22px; height: 22px;
  border: 2px solid var(--clr-primary);
  border-radius: 50%;
  pointer-events: none;
  z-index: 99998;
  opacity: 0.5;
  transition: width .3s cubic-bezier(.16,1,.3,1),
              height .3s cubic-bezier(.16,1,.3,1),
              opacity .3s;
  will-change: transform;
}
.g-cur-ring.big { width: 44px; height: 44px; opacity: 0.8; }


@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-1Thin.woff') format('woff'); font-weight: 100; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-2ExtraLight.woff') format('woff'); font-weight: 200; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-3Light.woff') format('woff'); font-weight: 300; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-4Regular.woff') format('woff'); font-weight: 400; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-5Medium.woff') format('woff'); font-weight: 500; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-6Bold.woff') format('woff'); font-weight: 600; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-7ExtraBold.woff') format('woff'); font-weight: 700; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-8Heavy.woff') format('woff'); font-weight: 800; font-display: swap; }
@font-face { font-family: 'Escoredream'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_six@1.2/S-CoreDream-9Black.woff') format('woff'); font-weight: 900; font-display: swap; }

/* ── 기술 스택 아이콘 (Devicon) ── */
i.tech-icon { font-size: 14px; vertical-align: middle; line-height: 1; margin-right: 4px; flex-shrink: 0; }

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

  /* ── 컬러 시스템 ── */
  --clr-primary:         #60A5FA;
  --clr-primary-hover:   #3B82F6;
  --clr-primary-subtle:  rgba(96,165,250,0.10);
  --clr-label:           #38BDF8;
  --clr-label-subtle:    rgba(56,189,248,0.10);
  --clr-accent-text:     #93C5FD;
  --clr-success:         #34D399;
  --clr-success-subtle:  rgba(52,211,153,0.10);
  --clr-warning:         #FBBF24;
  --clr-warning-subtle:  rgba(251,191,36,0.10);
  --clr-danger:          #F87171;
  --clr-danger-subtle:   rgba(248,113,113,0.08);
  --clr-icon-calendar:   #38BDF8;
  --clr-icon-ai:         #A78BFA;
  --clr-icon-github:     #e6edf3;
  --clr-icon-velog:      #20C997;
  --clr-icon-growth:     #FBBF24;
  --clr-icon-history:    #FB923C;
  --clr-icon-sync:       #60A5FA;
  --clr-icon-alert:      #F87171;

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

  /* ── 컬러 시스템 ── */
  --clr-primary:         #2563EB;
  --clr-primary-hover:   #1D4ED8;
  --clr-primary-subtle:  rgba(37,99,235,0.08);
  --clr-label:           #0EA5E9;
  --clr-label-subtle:    rgba(14,165,233,0.10);
  --clr-accent-text:     #2563EB;
  --clr-success:         #10B981;
  --clr-success-subtle:  rgba(16,185,129,0.10);
  --clr-warning:         #F59E0B;
  --clr-warning-subtle:  rgba(245,158,11,0.10);
  --clr-danger:          #EF4444;
  --clr-danger-subtle:   rgba(239,68,68,0.08);
  --clr-icon-calendar:   #0EA5E9;
  --clr-icon-ai:         #8B5CF6;
  --clr-icon-github:     #1f2328;
  --clr-icon-velog:      #20C997;
  --clr-icon-growth:     #F59E0B;
  --clr-icon-history:    #EA580C;
  --clr-icon-sync:       #2563EB;
  --clr-icon-alert:      #EF4444;

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
  word-break: keep-all;
  overflow-wrap: break-word;
}

.app-root { width: 100%; min-height: 100vh; }

/* 전역 스크롤바 - 직각으로 변경 */
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: var(--scrollbar-track); }
::-webkit-scrollbar-thumb { background: var(--clr-primary-subtle); border-radius: 0; }

/* 전역 선택 색상 */
::selection { background: var(--clr-primary); color: var(--bg-base); }

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

/* 전역 input/textarea 포커스 */
input:focus, textarea:focus { border-color: var(--clr-primary) !important; outline: none; }

/* OnboardingConnect 연동 완료 표시 */
.text-blue-600 { color: var(--clr-success) !important; }

</style>