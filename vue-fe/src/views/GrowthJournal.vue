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

        <div class="grid-4 col-gap mb-lg">
          <div class="base-panel stat-card" v-for="s in summaryStats" :key="s.label">
            <div class="flex-align gap-sm mb-sm text-xs text-muted font-bold">
              <div class="icon-box-small"><i :class="s.icon" /></div>
              <span>{{ s.label }}</span>
            </div>
            <div class="stat-value">{{ s.value }}</div>
            <div v-if="s.sub" class="stat-sub mt-xs">{{ s.sub }}</div>
          </div>
        </div>

        <div class="grid-2 col-gap gap-y mb-lg">
          <div class="flex-col gap-lg">
            <div class="base-panel p-md shadow-normal text-center h-full flex-col">
              <h3 class="panel-title-sm flex-align gap-sm justify-center mb-md">
                <i class="fas fa-bullseye text-muted" /> 기술별 성장 그래프
              </h3>

              <div v-if="skillRadar.length" class="radar-container mt-auto mb-auto">
                <svg viewBox="0 0 300 300" class="radar-svg">
                  <polygon
                    v-for="r in [0.25, 0.5, 0.75, 1]"
                    :key="'guide-' + r"
                    :points="getPolygonPoints(r)"
                    fill="none"
                    stroke="var(--border)"
                    stroke-width="1"
                  />
                  <line
                    v-for="(s, i) in skillRadar"
                    :key="'axis-' + i"
                    x1="150"
                    y1="150"
                    :x2="150 + Math.cos(getAngle(i)) * 120"
                    :y2="150 + Math.sin(getAngle(i)) * 120"
                    stroke="var(--border)"
                    stroke-width="1"
                  />
                  <polygon
                    :points="skillRadar.map((s, i) => getPoint(i, s.prev / 100)).join(' ')"
                    fill="transparent"
                    stroke="var(--text-muted)"
                    stroke-width="2"
                    stroke-dasharray="5 3"
                  />
                  <circle
                    v-for="(s, i) in skillRadar"
                    :key="'prev-dot-' + i"
                    :cx="getPointCoords(i, s.prev / 100).x"
                    :cy="getPointCoords(i, s.prev / 100).y"
                    r="3"
                    fill="var(--bg-surface)"
                    stroke="var(--text-muted)"
                    stroke-width="1.5"
                  />
                  <polygon
                    :points="skillRadar.map((s, i) => getPoint(i, s.curr / 100)).join(' ')"
                    fill="rgba(25,25,25,0.1)"
                    stroke="var(--clr-primary)"
                    stroke-width="2"
                  />
                  <circle
                    v-for="(s, i) in skillRadar"
                    :key="'dot-' + i"
                    :cx="getPointCoords(i, s.curr / 100).x"
                    :cy="getPointCoords(i, s.curr / 100).y"
                    r="4"
                    fill="var(--clr-primary)"
                  />
                  <text
                    v-for="(s, i) in skillRadar"
                    :key="'label-' + i"
                    :x="getPointCoords(i, 1.15).x"
                    :y="getPointCoords(i, 1.15).y"
                    text-anchor="middle"
                    dominant-baseline="middle"
                    fill="var(--text-muted)"
                    font-size="11"
                    font-weight="700"
                  >
                    {{ s.skill }}
                  </text>
                </svg>
              </div>
              <div v-else class="empty-panel mt-auto mb-auto">
                비교할 성장 데이터가 아직 없어요.
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

            <div class="base-panel p-md shadow-normal">
              <h3 class="panel-title-sm mb-md">기술별 숙련도 현황</h3>
              <div class="flex-col gap-sm">
                <div v-if="topSkills.length === 0" class="empty-panel">
                  표시할 기술 데이터가 아직 없어요.
                </div>
                <div v-for="(s, i) in topSkills" v-else :key="s.name" class="flex-align gap-md">
                  <span class="text-xs text-muted font-bold w-4 text-right">{{ i + 1 }}</span>
                  <span class="text-sm font-bold skill-name truncate">{{ s.name }}</span>
                  <div class="track-bg flex-1 relative">
                    <div class="track-fill" :style="{ width: `${s.ratio}%` }"></div>
                    <span class="metric-value">{{ s.displayValue }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="flex-col gap-lg h-full">
            <div class="base-panel p-md shadow-normal growth-monthly-panel h-full flex-col">
              <h3 class="panel-title-sm flex-align gap-sm justify-center mb-lg">
                <i class="fas fa-chart-bar text-muted" /> 월별 활동량 변화
              </h3>

              <div v-if="monthlyActivity.length" class="monthly-activity-list flex-col gap-md mb-md flex-1">
                <div v-for="m in monthlyActivity" :key="m.month">
                  <div class="flex-between mb-xs">
                    <div class="flex-align gap-sm">
                      <span class="text-sm font-bold">{{ m.month }}</span>
                      <span v-if="m.isJoinMonth" class="join-month-badge">가입 월</span>
                    </div>
                    <span class="text-xs text-muted font-bold">{{ m.total }}회</span>
                  </div>
                  <div class="bar-chart-track">
                    <div class="bar-chart-fill" :style="{ width: `${m.fillRatio}%` }">
                      <div
                        v-for="segment in m.segments"
                        :key="`${m.month}-${segment.key}`"
                        class="bar-segment"
                        :class="segment.colorClass"
                        :style="{ width: `${segment.ratio}%` }"
                      />
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="empty-panel flex-1">
                월별 활동 데이터가 아직 없어요.
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

        <div class="base-panel summary-box">
          <div class="flex-start gap-md">
            <i class="fas fa-arrow-trend-up text-muted mt-xs text-lg shrink-0" />
            <div>
              <h4 class="font-bold text-sm mb-xs">성장 요약</h4>
              <p class="text-sm text-muted font-bold lh-lg m-0">
                {{ growthSummary }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { getGrowthReport, postGrowthSummary } from '@/api/aiApi'

const growthSummary = ref('성장 요약을 불러오는 중...')
const growthReport = ref({
  topTechStacks: [],
  userCreatedAt: null,
  totalActivityCount: 0,
  recentGrowthTech: null,
  recentGrowthDelta: null,
  maxStreakDays: 0,
  techScoreSnapshot: [],
  techGrowthComparisons: [],
  topSkillPercentile: null,
  monthlyActivityCounts: [],
  techActivityRanking: [],
})

const chartLegend = [
  { label: '커밋', colorClass: 'color-1' },
  { label: 'PR', colorClass: 'color-2' },
  { label: 'Velog', colorClass: 'color-3' },
  { label: '퀴즈', colorClass: 'color-4' },
  { label: '추천', colorClass: 'color-5' },
]

const formatMonthLabel = (year, month) => `${year}.${String(month).padStart(2, '0')}`
const formatScore = (score) => `${Number(score || 0).toFixed(1)}점`
const clampPercent = (value) => Math.max(0, Math.min(100, value))
const MIN_BASELINE_DISPLAY_PERCENT = 6
const readCount = (counts, ...keys) => {
  for (const key of keys) {
    if (counts?.[key] != null) {
      return Number(counts[key] || 0)
    }
  }
  return 0
}

const summaryStats = computed(() => {
  const topTech = growthReport.value.topTechStacks?.[0]
  const percentile = growthReport.value.topSkillPercentile
  const recentGrowthLabel = growthReport.value.recentGrowthTech?.techName || '아직 집계 전'
  const recentGrowthDelta = growthReport.value.recentGrowthDelta

  return [
    {
      icon: 'fas fa-bolt',
      label: '가장 자주 다룬 기술',
      value: topTech?.techName || '-',
      sub: topTech ? `총 ${topTech.count}회 활동` : '기록이 쌓이면 표시돼요',
    },
    {
      icon: 'fas fa-chart-bar',
      label: '누적 활동 수',
      value: `${growthReport.value.totalActivityCount || 0}회`,
      sub: '가입 후 총 활동 수',
    },
    {
      icon: 'fas fa-fire',
      label: '최근 급성장 기술',
      value: recentGrowthLabel,
      sub: recentGrowthDelta == null ? '성장 비교 데이터 준비 중' : `가입 직후 대비 +${recentGrowthDelta.toFixed(1)}점`,
    },
    {
      icon: 'fas fa-ranking-star',
      label: '대표 기술 백분위',
      value: percentile?.topPercentile ? `상위 ${percentile.topPercentile}%` : '-',
      sub: percentile ? `${percentile.techName} 보유 사용자 기준` : '비교할 사용자 풀이 부족해요',
    },
  ]
})

const skillRadar = computed(() => {
  const comparisons = (growthReport.value.techGrowthComparisons || []).slice(0, 6)
  if (!comparisons.length) {
    return []
  }

  const maxScore = Math.max(
    1,
    ...comparisons.flatMap((item) => [Number(item.baselineScore || 0), Number(item.currentScore || 0)]),
  )

  return comparisons.map((item) => ({
    skill: item.techName,
    prev: Number(item.baselineScore || 0) > 0
      ? clampPercent((Number(item.baselineScore || 0) / maxScore) * 100)
      : MIN_BASELINE_DISPLAY_PERCENT,
    curr: clampPercent((Number(item.currentScore || 0) / maxScore) * 100),
  }))
})

const topSkills = computed(() => {
  const scoreSnapshot = growthReport.value.techScoreSnapshot || []
  if (scoreSnapshot.length) {
    const maxScore = Math.max(1, ...scoreSnapshot.map((item) => Number(item.score || 0)))
    return scoreSnapshot.map((item) => ({
      name: item.techName,
      ratio: clampPercent((Number(item.score || 0) / maxScore) * 100),
      displayValue: formatScore(item.score),
    }))
  }

  const ranking = growthReport.value.techActivityRanking || []
  const maxCount = Math.max(1, ...ranking.map((item) => Number(item.count || 0)))
  return ranking.map((item) => ({
    name: item.techName,
    ratio: clampPercent((Number(item.count || 0) / maxCount) * 100),
    displayValue: `${item.count || 0}회`,
  }))
})

const rawMonthlyActivity = computed(() => {
  const items = growthReport.value.monthlyActivityCounts || []
  const createdAt = growthReport.value.userCreatedAt
  const joinDate = createdAt ? new Date(createdAt) : null
  const joinMonthKey = joinDate && !Number.isNaN(joinDate.getTime())
    ? `${joinDate.getFullYear()}-${String(joinDate.getMonth() + 1).padStart(2, '0')}`
    : null

  return items.map((item) => {
    const counts = item.counts || {}
    const commit = readCount(counts, 'GITHUB_COMMIT', 'githubCommit')
    const pr = readCount(counts, 'GITHUB_PR', 'githubPr')
    const velog = readCount(counts, 'VELOG_POST', 'velogPost')
    const quiz = readCount(counts, 'QUIZ', 'quiz')
    const reference = readCount(counts, 'REFERENCE', 'reference')
    const total = commit + pr + velog + quiz + reference
    const monthKey = `${item.year}-${String(item.month).padStart(2, '0')}`
    const segments = total > 0
      ? [
          { key: 'commit', ratio: (commit / total) * 100, colorClass: 'color-1' },
          { key: 'pr', ratio: (pr / total) * 100, colorClass: 'color-2' },
          { key: 'velog', ratio: (velog / total) * 100, colorClass: 'color-3' },
          { key: 'quiz', ratio: (quiz / total) * 100, colorClass: 'color-4' },
          { key: 'reference', ratio: (reference / total) * 100, colorClass: 'color-5' },
        ].filter((segment) => segment.ratio > 0)
      : []

    return {
      month: formatMonthLabel(item.year, item.month),
      commit,
      pr,
      velog,
      quiz,
      reference,
      total,
      segments,
      isJoinMonth: monthKey === joinMonthKey,
    }
  })
})

const maxBar = computed(() => {
  if (!rawMonthlyActivity.value.length) {
    return 1
  }
  return Math.max(1, ...rawMonthlyActivity.value.map((item) => item.total))
})

const monthlyActivity = computed(() => {
  return rawMonthlyActivity.value.map((item) => ({
    ...item,
    fillRatio: clampPercent((item.total / maxBar.value) * 100),
  }))
})

const getAngle = (i) => (Math.PI * 2 * i) / Math.max(skillRadar.value.length, 1) - Math.PI / 2
const getPointCoords = (i, ratio) => ({
  x: 150 + Math.cos(getAngle(i)) * 100 * ratio,
  y: 150 + Math.sin(getAngle(i)) * 100 * ratio,
})
const getPoint = (i, ratio) => {
  const coords = getPointCoords(i, ratio)
  return `${coords.x},${coords.y}`
}
const getPolygonPoints = (ratio) => skillRadar.value.map((_, i) => getPoint(i, ratio)).join(' ')

const buildGrowthSummaryStats = (report) => ({
  topTechStacks: (report.topTechStacks || []).map((item) => item.techName),
  totalActivityCount: report.totalActivityCount || 0,
  recentGrowthTech: report.recentGrowthTech?.techName || null,
  maxStreakDays: report.maxStreakDays || 0,
  techScoreSnapshot: Object.fromEntries(
    (report.techScoreSnapshot || []).map((item) => [item.techName, Math.round(Number(item.score || 0))]),
  ),
  monthlyActivityCounts: Object.fromEntries(
    (report.monthlyActivityCounts || []).map((item) => [
      `${item.year}-${String(item.month).padStart(2, '0')}`,
      item.counts || {},
    ]),
  ),
  techActivityRanking: (report.techActivityRanking || []).map((item) => item.techName),
})

onMounted(async () => {
  let statsForSummary = buildGrowthSummaryStats(growthReport.value)

  try {
    const { data: report } = await getGrowthReport()
    growthReport.value = {
      ...growthReport.value,
      ...report,
    }
    statsForSummary = buildGrowthSummaryStats(growthReport.value)
  } catch (error) {
    console.error('growth-report 조회 실패:', error)
  }

  try {
    const { data } = await postGrowthSummary({ userId: 1, stats: statsForSummary })
    growthSummary.value = data.summary
  } catch (error) {
    growthSummary.value = '성장 요약을 불러오지 못했습니다.'
    console.error('growth/summary 호출 실패:', error)
  }
})
</script>

<style scoped>
.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow-y: auto; position: relative; }
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

.page-header {
  display: flex !important; align-items: center;
  height: 64px !important; min-height: 64px; max-height: 64px;
  flex-shrink: 0;
  padding: 0 24px; border-bottom: 1px solid var(--border);
  background: var(--bg-surface); position: sticky; top: 0; z-index: 10;
  box-sizing: border-box;
}
.header-title {
  font-size: 15px; font-weight: 900; letter-spacing: 0.15em;
  color: var(--text-primary); display: flex; align-items: center; gap: 12px;
  text-transform: uppercase;
}
.header-title i { font-size: 18px; width: 24px; text-align: center; }

.content-inner { padding: 48px 32px; width: 100%; }
.max-w-xl { max-width: 1000px; }
.mx-auto { margin-left: auto; margin-right: auto; }

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
.text-muted { color: var(--text-muted); }
.truncate { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.skill-name { width: 90px; flex-shrink: 0; }
.lh-lg { line-height: 1.6; }
.shrink-0 { flex-shrink: 0; }
.w-4 { width: 16px; }

/* Panels & Shadows */
.base-panel { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 0px; transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s; }
.stat-card { padding: 24px; box-shadow: none; }
.stat-card:hover { transform: translateY(-2px); background: var(--bg-hover); }
.shadow-normal { box-shadow: none; }
.shadow-normal:hover { background: var(--bg-hover); }

.panel-title-sm { font-size: 15px; font-weight: 900; color: var(--text-primary); margin: 0; }

.btn-back { background: transparent; border: none; font-size: 13px; font-weight: 700; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px; padding: 0; margin-bottom: 24px; transition: color 0.1s; font-family: inherit; }
.btn-back:hover { color: var(--text-primary); }

.icon-box-small { width: 32px; height: 32px; background: var(--bg-surface); border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 14px; color: var(--text-muted); }
.stat-value { font-size: 24px; font-weight: 900; color: var(--clr-accent-text); font-family: 'Escoredream', system-ui, sans-serif; }
.stat-sub { font-size: 11px; color: var(--text-muted); font-weight: 600; }

.track-bg { height: 12px; background: var(--bg-elevated); border: 1px solid var(--border); width: 100%; border-radius: 0; overflow: hidden; position: relative; }
.track-fill { height: 100%; background: var(--text-primary); transition: width 0.3s; opacity: 0.8; }
.metric-value { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); font-size: 10px; color: var(--text-muted); font-weight: 700; white-space: nowrap; }

.bar-chart-track { height: 16px; border: 1px solid var(--border); border-radius: 0; overflow: hidden; width: 100%; background: #d9d9d9; }
.bar-chart-fill { height: 100%; display: flex; overflow: hidden; }
.bar-segment { height: 100%; transition: width 0.3s; }
.color-1 { background: var(--clr-primary); }
.color-2 { background: #22c55e; }
.color-3 { background: var(--clr-icon-velog); }
.color-4 { background: #8b5cf6; }
.color-5 { background: var(--clr-warning); }

.legend-box { width: 12px; height: 12px; border-radius: 2px; }
.legend-line { width: 16px; height: 2px; }
.border-dashed { border-top: 2px dashed var(--text-muted); height: 0; background: transparent; }
.bg-dark { background: var(--text-primary); }
.join-month-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.radar-container { width: 100%; max-width: 280px; aspect-ratio: 1 / 1; margin: 0 auto; position: relative; }
.radar-svg { width: 100%; height: 100%; display: block; overflow: visible; }

.summary-box { background: var(--bg-elevated); padding: 32px; border: 1px dashed var(--border); box-shadow: none; position: relative; }
.summary-box::before { content: ''; position: absolute; inset: 0; border: 1px solid var(--border); pointer-events: none; }

.growth-monthly-panel {
  max-height: 570px;
  min-height: 570px;
}

.monthly-activity-list {
  overflow-y: auto;
  padding-right: 6px;
  min-height: 0;
}

.monthly-activity-list::-webkit-scrollbar {
  width: 6px;
}

.monthly-activity-list::-webkit-scrollbar-thumb {
  background: #c7c7c7;
}

.empty-panel {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
  text-align: center;
  line-height: 1.6;
}
</style>
