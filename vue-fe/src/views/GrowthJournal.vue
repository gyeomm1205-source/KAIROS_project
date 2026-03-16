<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content custom-scroll">
      <header class="page-header">
        <div class="header-title"><i class="fas fa-chart-line" /> GROWTH JOURNAL</div>
      </header>

      <div class="content-inner max-w-xl mx-auto">
        <button class="btn-back" @click="$router.push('/history')">
          <i class="fas fa-arrow-left" /> 히스토리로 돌아가기
        </button>

        <!-- Summary Cards -->
        <div class="grid-4 col-gap mb-lg">
          <div class="brutal-panel stat-card" v-for="s in summaryStats" :key="s.label">
            <div class="flex-align gap-sm mb-sm text-xs text-muted font-bold">
              <div class="icon-box-small"><i :class="s.icon" /></div>
              <span>{{ s.label }}</span>
            </div>
            <div class="stat-value">{{ s.value }}</div>
            <div v-if="s.sub" class="stat-sub mt-xs">{{ s.sub }}</div>
          </div>
        </div>

        <div class="grid-2 col-gap gap-y mb-lg">
          <!-- Left: Skill Radar & Top Skills -->
          <div class="flex-col gap-lg">
            <!-- Spider Chart -->
            <div class="brutal-panel p-md shadow-normal text-center h-full flex-col">
              <h3 class="panel-title-sm flex-align gap-sm justify-center mb-md">
                <i class="fas fa-bullseye text-muted" /> 기술별 성장 그래프
              </h3>
              
              <div class="radar-container mt-auto mb-auto">
                <svg viewBox="0 0 300 300" class="radar-svg">
                  <!-- Guide circles/polygons -->
                  <polygon
                    v-for="r in [0.25, 0.5, 0.75, 1]"
                    :key="'guide-'+r"
                    :points="getPolygonPoints(r)"
                    fill="none" stroke="var(--border)" stroke-width="1"
                  />
                  <!-- Axis lines -->
                  <line
                    v-for="(s, i) in skillRadar"
                    :key="'axis-'+i"
                    x1="150" y1="150"
                    :x2="150 + Math.cos(getAngle(i)) * 120"
                    :y2="150 + Math.sin(getAngle(i)) * 120"
                    stroke="var(--border)" stroke-width="1"
                  />
                  <!-- Previous (lighter) -->
                  <polygon
                    :points="skillRadar.map((s, i) => getPoint(i, s.prev / 100)).join(' ')"
                    fill="var(--bg-surface)" stroke="var(--text-muted)" stroke-width="1.5" stroke-dasharray="4 2"
                  />
                  <!-- Current (darker) -->
                  <polygon
                    :points="skillRadar.map((s, i) => getPoint(i, s.curr / 100)).join(' ')"
                    fill="rgba(25,25,25,0.1)" stroke="var(--text-primary)" stroke-width="2"
                  />
                  <!-- Dots -->
                  <circle
                    v-for="(s, i) in skillRadar"
                    :key="'dot-'+i"
                    :cx="getPointCoords(i, s.curr / 100).x"
                    :cy="getPointCoords(i, s.curr / 100).y"
                    r="4" fill="var(--text-primary)"
                  />
                  <!-- Labels -->
                  <text
                    v-for="(s, i) in skillRadar"
                    :key="'label-'+i"
                    :x="getPointCoords(i, 1.15).x"
                    :y="getPointCoords(i, 1.15).y"
                    text-anchor="middle" dominant-baseline="middle"
                    fill="var(--text-muted)" font-size="11" font-weight="700"
                  >
                    {{ s.skill }}
                  </text>
                </svg>
              </div>

              <div class="flex-align gap-lg justify-center mt-md">
                <div class="flex-align gap-sm">
                  <div class="legend-line border-dashed"></div>
                  <span class="text-xs text-muted font-bold">가입 시점</span>
                </div>
                <div class="flex-align gap-sm">
                  <div class="legend-line bg-dark"></div>
                  <span class="text-xs text-muted font-bold">현재</span>
                </div>
              </div>
            </div>

            <!-- Top Skills Table -->
            <div class="brutal-panel p-md shadow-normal">
              <h3 class="panel-title-sm mb-md">기술별 활동 현황</h3>
              <div class="flex-col gap-sm">
                <div v-for="(s, i) in topSkills" :key="s.name" class="flex-align gap-md">
                  <span class="text-xs text-muted font-bold w-4 text-right">{{ i + 1 }}</span>
                  <span class="text-sm font-bold w-20 truncate">{{ s.name }}</span>
                  <div class="track-bg flex-1 relative">
                    <div class="track-fill" :style="{ width: `${(s.count / 18) * 100}%` }"></div>
                    <span class="absolute right-2 top-0 text-[10px] text-muted font-bold lh-full">{{ s.count }}회</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Right: Monthly Activity -->
          <div class="flex-col gap-lg h-full">
            <div class="brutal-panel p-md shadow-normal h-full flex-col">
              <h3 class="panel-title-sm flex-align gap-sm justify-center mb-lg">
                <i class="fas fa-chart-bar text-muted" /> 월별 학습량 변화
              </h3>
              
              <div class="flex-col gap-md mb-md flex-1 justify-center">
                <div v-for="m in monthlyActivity" :key="m.month">
                  <div class="flex-between mb-xs">
                    <span class="text-sm font-bold">{{ m.month }}</span>
                    <span class="text-xs text-muted font-bold">{{ m.total }}회</span>
                  </div>
                  <div class="bar-chart-track flex">
                    <div class="bar-segment color-1" :style="{ width: `${(m.study / maxBar) * 100}%` }" />
                    <div class="bar-segment color-2" :style="{ width: `${(m.dev / maxBar) * 100}%` }" />
                    <div class="bar-segment color-3" :style="{ width: `${(m.blog / maxBar) * 100}%` }" />
                    <div class="bar-segment color-4" :style="{ width: `${(m.review / maxBar) * 100}%` }" />
                  </div>
                </div>
              </div>

              <div class="flex-align gap-md flex-wrap mt-auto pt-md border-t justify-center">
                <div v-for="l in chartLegend" :key="l.label" class="flex-align gap-sm">
                  <div class="legend-box" :class="l.colorClass" />
                  <span class="text-xs text-muted font-bold">{{ l.label }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Bottom Summary -->
        <div class="brutal-panel summary-box">
          <div class="flex-start gap-md">
            <i class="fas fa-arrow-trend-up text-muted mt-xs text-lg shrink-0" />
            <div>
              <h4 class="font-bold text-sm mb-xs">성장 요약</h4>
              <p class="text-sm text-muted font-bold lh-lg m-0">
                최근 4주 동안 <strong class="text-primary">React, Next.js, TypeScript</strong> 관련 활동이 꾸준히 증가했습니다.
                특히 <strong class="text-primary">SSR/SSG 영역</strong>이 가입 시점 대비 가장 큰 성장폭(+45pt)을 보이고 있으며,
                상태관리와 프론트엔드 전반에서 중급 수준으로 성장하고 있습니다.
                다음 단계로 <strong class="text-primary">DevOps와 백엔드</strong> 영역을 확장하면 풀스택 역량을 키울 수 있습니다.
              </p>
            </div>
          </div>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import AppSidebar from '@/components/AppSidebar.vue'

const summaryStats = [
  { icon: "fas fa-bolt", label: "가장 자주 다룬 기술", value: "React", sub: "총 18회 활동" },
  { icon: "fas fa-chart-bar", label: "누적 활동 수", value: "36회", sub: "가입 후 총 활동 수" },
  { icon: "fas fa-fire", label: "최근 성장 도메인", value: "SSR/SSG", sub: "" },
  { icon: "fas fa-chart-bar", label: "연속 학습 최장 기록", value: "10일", sub: "1월 기록 · 현재 7일째" },
]

const topSkills = [
  { name: "React", count: 18 },
  { name: "TypeScript", count: 12 },
  { name: "Next.js", count: 9 },
  { name: "Docker", count: 5 },
  { name: "REST API", count: 4 },
]

const skillRadar = [
  { skill: "프론트엔드", prev: 45, curr: 78 },
  { skill: "상태관리", prev: 30, curr: 72 },
  { skill: "SSR/SSG", prev: 10, curr: 55 },
  { skill: "DevOps", prev: 5, curr: 35 },
  { skill: "백엔드", prev: 20, curr: 28 },
  { skill: "CS 기초", prev: 40, curr: 52 },
]

const monthlyActivity = [
  { month: "1월", study: 8, dev: 4, blog: 2, review: 1, total: 15 },
  { month: "2월", study: 4, dev: 3, blog: 1, review: 1, total: 9 },
  { month: "3월", study: 5, dev: 3, blog: 2, review: 2, total: 12 },
]

const maxBar = Math.max(...monthlyActivity.map((m) => m.total))

const chartLegend = [
  { label: "학습", colorClass: "color-1" },
  { label: "개발", colorClass: "color-2" },
  { label: "블로그", colorClass: "color-3" },
  { label: "복습", colorClass: "color-4" },
]

// Radar chart logic
const getAngle = (i) => (Math.PI * 2 * i) / skillRadar.length - Math.PI / 2
const getPointCoords = (i, ratio) => ({
  x: 150 + Math.cos(getAngle(i)) * 100 * ratio,
  y: 150 + Math.sin(getAngle(i)) * 100 * ratio
})
const getPoint = (i, ratio) => {
  const c = getPointCoords(i, ratio)
  return `${c.x},${c.y}`
}
const getPolygonPoints = (ratio) => {
  return skillRadar.map((_, i) => getPoint(i, ratio)).join(" ")
}

</script>

<style scoped>
.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow-y: auto; position: relative; }
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

.page-header { display: flex; align-items: center; padding: 20px 32px; border-bottom: 2px solid var(--text-primary); background: var(--bg-surface); position: sticky; top: 0; z-index: 10; }
.header-title { font-size: 16px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary); display: flex; align-items: center; gap: 10px; }

.content-inner { padding: 48px 32px; width: 100%; }
.max-w-xl { max-width: 1000px; }
.mx-auto { margin-left: auto; margin-right: auto; }

/* Utils */
.flex-align { display: flex; align-items: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-col { display: flex; flex-direction: column; }
.flex-wrap { display: flex; flex-wrap: wrap; }
.flex-start { display: flex; align-items: flex-start; }
.justify-center { justify-content: center; }
.flex-1 { flex: 1; }
.h-full { height: 100%; }

.gap-sm { gap: 8px; }
.gap-md { gap: 16px; }
.gap-lg { gap: 24px; }
.gap-y { gap: 24px; }

.mb-xs { margin-bottom: 8px; }
.mb-sm { margin-bottom: 12px; }
.mb-md { margin-bottom: 16px; }
.mb-lg { margin-bottom: 24px; }
.mt-xs { margin-top: 4px; }
.mt-md { margin-top: 16px; }
.mt-auto { margin-top: auto; }
.mb-auto { margin-bottom: auto; }
.p-md { padding: 24px; }
.pt-md { padding-top: 16px; }
.m-0 { margin: 0; }
.border-t { border-top: 1px solid var(--border); }

.grid-4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); gap: 24px; }
@media (max-width: 1000px) {
  .grid-4 { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .grid-2, .grid-4 { grid-template-columns: 1fr; }
}

.text-center { text-align: center; }
.text-right { text-align: right; }
.font-bold { font-weight: 800; }
.text-xs { font-size: 11px; }
.text-sm { font-size: 13px; }
.text-lg { font-size: 18px; }
.text-\[10px\] { font-size: 10px; }
.text-muted { color: var(--text-muted); }
.text-primary { color: var(--text-primary); }
.truncate { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.lh-lg { line-height: 1.6; }
.lh-full { line-height: 24px; }
.shrink-0 { flex-shrink: 0; }

/* Panels & Shadows */
.brutal-panel { background: var(--bg-base); border: 2px solid var(--text-primary); border-radius: 0; transition: transform 0.1s, box-shadow 0.1s; }
.stat-card { padding: 20px; box-shadow: 4px 4px 0 #6b7280; }
.stat-card:hover { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; }
.shadow-normal { box-shadow: 6px 6px 0 #6b7280; }
.shadow-normal:hover { transform: translate(-2px, -2px); box-shadow: 8px 8px 0 #6b7280; }

.panel-title-sm { font-size: 15px; font-weight: 900; color: var(--text-primary); margin: 0; }

/* Elements */
.btn-back { background: transparent; border: none; font-size: 13px; font-weight: 700; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px; padding: 0; margin-bottom: 24px; transition: color 0.1s; font-family: inherit; }
.btn-back:hover { color: var(--text-primary); }

.icon-box-small { width: 32px; height: 32px; background: var(--bg-surface); border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 14px; color: var(--text-muted); }
.stat-value { font-size: 24px; font-weight: 900; color: var(--text-primary); font-family: monospace; }
.stat-sub { font-size: 11px; color: var(--text-muted); font-weight: 600; }

/* Bars & Tracks */
.track-bg { height: 24px; background: var(--bg-surface); border: 2px solid var(--border); width: 100%; border-radius: 0; overflow: hidden; }
.track-fill { height: 100%; background: var(--text-primary); transition: width 0.3s; }

.bar-chart-track { height: 24px; border: 2px solid var(--border); border-radius: 0; overflow: hidden; width: 100%; gap: 1px; background: var(--border); }
.bar-segment { height: 100%; transition: width 0.3s; }
.color-1 { background: var(--text-primary); } 
.color-2 { background: #6b7280; } 
.color-3 { background: #9ca3af; } 
.color-4 { background: #e5e7eb; }

.legend-box { width: 12px; height: 12px; border-radius: 2px; }
.legend-line { width: 16px; height: 2px; }
.border-dashed { border-top: 2px dashed var(--text-muted); height: 0; background: transparent; }
.bg-dark { background: var(--text-primary); }

/* SVG Canvas */
.radar-container { width: 100%; max-width: 280px; aspect-ratio: 1/1; margin: 0 auto; position: relative; }
.radar-svg { width: 100%; height: 100%; display: block; overflow: visible; }

/* Bottom Summary */
.summary-box { background: var(--bg-surface); padding: 24px; border-color: var(--border); outline: 2px dashed var(--border); box-shadow: none; outline-offset: -10px; }

</style>
