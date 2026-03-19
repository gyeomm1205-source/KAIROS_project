<template>
  <div class="app-layout" :class="themeStore.isDark ? 'theme-dark' : 'theme-light'">
    <div class="crt-scanlines"></div>
    <div class="retro-dot-bg"></div>

    <AppSidebar />
    <main class="main-content custom-scroll">
      
      <header class="page-header">
        <div>
          <h1 class="page-title retro-text-hover">DASHBOARD_</h1>
          <p class="page-desc">현재 시스템 학습 진행 상황 및 AI 분석 리포트</p>
        </div>
      </header>

      <div class="page-body">
        <section class="ai-summary-card retro-panel">
          <div class="ai-header">
            <i class="fas fa-robot ai-icon pulse-anim"></i>
            <h3>SYSTEM AI ANALYSIS</h3>
          </div>
          <p class="ai-text terminal-input">
            > 최근 <strong class="text-accent-1">'Spring Boot'</strong> 트랙의 진척도가 매우 우수합니다.<br>
            > 반면 <strong class="text-accent-2">'CS 전공지식'</strong> 트랙은 지난주 대비 처리량이 30% 감소했습니다.<br>
            > [권장] 이번 주말 메모리 가용 시간을 활용하여 운영체제 섹션을 복습하십시오. <span class="blink-cursor">_</span>
          </p>
        </section>

        <div class="stats-grid">
          <div class="stat-card retro-panel">
            <div class="stat-title">ACTIVE TRACKS</div>
            <div class="stat-value text-accent-1">4<span class="stat-unit">EA</span></div>
          </div>
          <div class="stat-card retro-panel">
            <div class="stat-title">WEEKLY MISSIONS</div>
            <div class="stat-value text-accent-2">12<span class="stat-unit">DONE</span></div>
          </div>
          <div class="stat-card retro-panel">
            <div class="stat-title">STREAK</div>
            <div class="stat-value text-accent-3">5<span class="stat-unit">DAYS</span></div>
          </div>
        </div>

        <div class="grid-2col">
          <section class="dash-section retro-panel">
            <h2 class="section-title">TRACK PROGRESS</h2>
            <div class="progress-list">
              <div v-for="track in store.activeTracks" :key="track.id" class="progress-item">
                <div class="prog-header">
                  <div class="prog-info">
                    <span class="prog-dot" :style="{ background: track.color }"></span>
                    <span class="prog-name">{{ track.name }}</span>
                  </div>
                  <span class="prog-pct">{{ Math.floor(Math.random() * 40) + 30 }}%</span>
                </div>
                <div class="bar-track">
                  <div class="bar-fill" :style="{ width: Math.floor(Math.random() * 40) + 30 + '%', background: track.color }"></div>
                </div>
              </div>
            </div>
          </section>

          <section class="dash-section retro-panel">
            <h2 class="section-title">RECENT LOGS</h2>
            <div class="recent-list">
              <div class="recent-item terminal-bg">
                <div class="recent-date">YESTERDAY</div>
                <div class="recent-content">
                  <span class="recent-badge" style="background: var(--k-acc-2-bg)">졸업작품</span>
                  <span class="recent-title">DB ERD 설계 완료</span>
                </div>
              </div>
              <div class="recent-item terminal-bg">
                <div class="recent-date">2 DAYS AGO</div>
                <div class="recent-content">
                  <span class="recent-badge" style="background: var(--k-acc-3-bg)">알고리즘</span>
                  <span class="recent-title">다익스트라 알고리즘 문제 풀이</span>
                </div>
              </div>
              <div class="recent-item terminal-bg">
                <div class="recent-date">3 DAYS AGO</div>
                <div class="recent-content">
                  <span class="recent-badge" style="background: var(--k-acc-1-bg)">정처기</span>
                  <span class="recent-title">필기 1과목 기출 오답노트</span>
                </div>
              </div>
            </div>
          </section>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { useCalendarStore } from '@/stores/useCalendarStore'
import { useThemeStore } from '@/stores/useThemeStore'

const store = useCalendarStore()
const themeStore = useThemeStore()

onMounted(() => {
  if (themeStore.isDark) themeStore.isDark = false;
})
</script>

<style scoped>
@font-face { font-family: 'Mulmaru'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/2601-4@1.1/Mulmaru.woff2') format('woff2'); font-weight: normal; font-display: swap; }
@font-face { font-family: 'NeoDunggeunmo'; src: url('https://cdn.jsdelivr.net/gh/projectnoonnu/noonfonts_2001@1.3/NeoDunggeunmoPro-Regular.woff2') format('woff2'); font-weight: normal; font-display: swap; }

/* ── 테마 변수 (명도 대비 극대화) ── */
.theme-light { --k-bg: #F4F0EB; --k-housing: #E6DFD3; --k-key-bg: #FFFFFF; --k-key-border: #1A1A1A; --k-key-shadow: #1A1A1A; --k-border-main: #1A1A1A; --bg-base: #F4F0EB; --bg-surface: #E6DFD3; --bg-elevated: #FFFFFF; --border: #1A1A1A; --border-mid: #1A1A1A; --text-primary: #1A1A1A; --text-secondary: #333333; --text-muted: #555555; --text-faint: #777777; --accent: #E53935; --k-acc-1-bg: #E53935; --k-acc-1-shadow: #B71C1C; --k-acc-2-bg: #1E88E5; --k-acc-2-shadow: #1565C0; --k-acc-3-bg: #43A047; --k-acc-3-shadow: #2E7D32; }
.theme-dark { --k-bg: #1A1A1A; --k-housing: #2C2C2C; --k-key-bg: #3D3D3D; --k-key-border: #000000; --k-key-shadow: #000000; --k-border-main: #000000; --bg-base: #1A1A1A; --bg-surface: #2C2C2C; --bg-elevated: #3D3D3D; --border: #000000; --border-mid: #000000; --text-primary: #F0F0F0; --text-secondary: #CCCCCC; --text-muted: #999999; --text-faint: #666666; --accent: #FF5252; --k-acc-1-bg: #FF5252; --k-acc-1-shadow: #D50000; --k-acc-2-bg: #448AFF; --k-acc-2-shadow: #2962FF; --k-acc-3-bg: #69F0AE; --k-acc-3-shadow: #00E676; }

.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); font-family: 'Mulmaru', sans-serif; position: relative; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow-y: auto; z-index: 10; }

/* 📺 배경 효과 */
.crt-scanlines { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: linear-gradient(rgba(18,16,16,0) 50%, rgba(0,0,0,0.1) 50%); background-size: 100% 4px; z-index: 9999; pointer-events: none; opacity: 0.15; }
.theme-dark .crt-scanlines { opacity: 0.3; }
.retro-dot-bg { position: absolute; inset: 0; background-image: radial-gradient(var(--border-mid) 1px, transparent 1px); background-size: 20px 20px; opacity: 0.1; z-index: 0; pointer-events: none; }

/* ── 브루탈리즘 공통 패널 ── */
.retro-panel { background: var(--bg-surface); border: 2px solid var(--border); box-shadow: 4px 4px 0 var(--border); border-radius: 6px; }
.terminal-input { background: var(--k-key-shadow); color: var(--text-primary); border: 2px solid var(--border); border-radius: 4px; box-shadow: inset 0 4px 8px rgba(0,0,0,0.3); padding: 16px; font-family: 'NeoDunggeunmo', sans-serif; line-height: 1.8;}
.terminal-bg { background: var(--bg-elevated); border: 2px solid var(--border); border-radius: 4px; box-shadow: inset 0 2px 4px rgba(0,0,0,0.1); }

/* ── 직관적인 레트로 텍스트 호버 (글리치 대체) ── */
.retro-text-hover { transition: transform 0.1s, text-shadow 0.1s; display: inline-block; }
.retro-text-hover:hover { transform: translate(-2px, -2px); text-shadow: 3px 3px 0px var(--accent); color: var(--text-primary); cursor: pointer; }

/* 텍스트 컬러 */
.text-accent-1 { color: var(--k-acc-1-bg); }
.text-accent-2 { color: var(--k-acc-2-bg); }
.text-accent-3 { color: var(--k-acc-3-bg); }

/* ── 레이아웃 ── */
.page-header { padding: 32px 40px; border-bottom: 2px solid var(--border); background: var(--k-housing); position: sticky; top: 0; z-index: 10; }
.page-title { font-size: 32px; font-weight: 900; font-family: 'NeoDunggeunmo', sans-serif; color: var(--text-primary); margin-bottom: 8px; }
.page-desc { font-size: 14px; font-weight: 600; color: var(--text-muted); }

.page-body { padding: 32px 40px; display: flex; flex-direction: column; gap: 32px; max-width: 1200px; width: 100%; margin: 0 auto; }

/* AI 요약 카드 */
.ai-summary-card { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.ai-header { display: flex; align-items: center; gap: 10px; }
.ai-icon { color: var(--accent); font-size: 20px; }
.ai-header h3 { font-size: 18px; font-weight: 900; font-family: 'NeoDunggeunmo', sans-serif; color: var(--text-primary); }
.ai-text { font-size: 14px; color: var(--k-acc-3-bg); line-height: 1.8; }
.pulse-anim { animation: pulse 1.5s infinite; }
@keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.2); } }
.blink-cursor { animation: blink 1s step-end infinite; }
@keyframes blink { 50% { opacity: 0; } }

/* 통계 그리드 */
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 24px; }
.stat-card { padding: 24px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; background: var(--bg-elevated); transition: 0.2s;}
.stat-card:hover { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 var(--border); }
.stat-title { font-size: 14px; font-weight: 800; font-family: 'NeoDunggeunmo', sans-serif; color: var(--text-muted); }
.stat-value { font-size: 40px; font-weight: 900; font-family: 'NeoDunggeunmo', sans-serif; }
.stat-unit { font-size: 16px; font-weight: 800; color: var(--text-faint); margin-left: 8px; }

/* 2단 그리드 */
.grid-2col { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
@media (max-width: 900px) { .grid-2col { grid-template-columns: 1fr; } }

.dash-section { padding: 24px; background: var(--bg-elevated); }
.section-title { font-size: 18px; font-weight: 900; font-family: 'NeoDunggeunmo', sans-serif; margin-bottom: 24px; color: var(--text-primary); border-bottom: 2px dashed var(--border); padding-bottom: 12px; }

/* 트랙 프로그레스바 */
.progress-list { display: flex; flex-direction: column; gap: 20px; }
.progress-item { display: flex; flex-direction: column; gap: 10px; }
.prog-header { display: flex; justify-content: space-between; align-items: center; }
.prog-info { display: flex; align-items: center; gap: 8px; }
.prog-dot { width: 10px; height: 10px; border-radius: 50%; border: 2px solid var(--border); }
.prog-name { font-size: 14px; font-weight: 800; font-family: 'NeoDunggeunmo', sans-serif; color: var(--text-primary); }
.prog-pct { font-size: 14px; font-weight: 900; font-family: 'NeoDunggeunmo', sans-serif; color: var(--text-primary); }

.bar-track { width: 100%; height: 16px; background: var(--k-key-shadow); border: 2px solid var(--border); border-radius: 4px; overflow: hidden; display: flex; box-shadow: inset 0 2px 4px rgba(0,0,0,0.3); }
.bar-fill { height: 100%; transition: width 1s cubic-bezier(0.25, 0.8, 0.25, 1); border-right: 2px solid var(--border); }

/* 최근 활동 */
.recent-list { display: flex; flex-direction: column; gap: 12px; }
.recent-item { display: flex; align-items: center; gap: 16px; padding: 16px; transition: 0.1s; cursor: pointer; }
.recent-item:hover { border-color: var(--accent); transform: translateX(4px); }
.recent-date { font-size: 12px; font-weight: 800; font-family: 'NeoDunggeunmo', sans-serif; color: var(--text-faint); width: 80px; flex-shrink: 0; }
.recent-content { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.recent-badge { font-size: 11px; font-weight: 800; font-family: 'NeoDunggeunmo', sans-serif; color: #fff; padding: 4px 8px; border-radius: 4px; border: 2px solid var(--border); white-space: nowrap; }
.recent-title { font-size: 14px; font-weight: 700; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.custom-scroll::-webkit-scrollbar { width: 8px; }
.custom-scroll::-webkit-scrollbar-track { background: var(--bg-base); border-left: 2px solid var(--border); }
.custom-scroll::-webkit-scrollbar-thumb { background: var(--border-mid); border: 2px solid var(--border); }
</style>