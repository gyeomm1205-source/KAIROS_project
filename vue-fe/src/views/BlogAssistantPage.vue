<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content">
      <!-- ── 헤더 ── -->
      <div class="page-header">
        <div class="header-left">
          <div class="header-icon">
            <i class="fas fa-pen-fancy" />
          </div>
          <div>
            <h1 class="page-title">블로그 어시스턴트</h1>
            <p class="page-sub">학습 기록을 AI가 블로그 초안으로 변환합니다</p>
          </div>
        </div>
        <div class="header-actions">
          <RouterLink to="/calendar?view=prompt&filter=blog" class="btn-view-cal">
            <i class="fas fa-calendar-alt" /> 캘린더에서 보기
          </RouterLink>
        </div>
      </div>

      <!-- ── 본문 ── -->
      <div class="body-wrap">

        <!-- 왼쪽: 설정 & 입력 -->
        <div class="left-panel">

          <!-- 최근 학습 기록 -->
          <div class="section-card">
            <div class="section-title">
              <i class="fas fa-history" /> 최근 학습 기록
            </div>
            <div class="study-log-list">
              <div
                v-for="log in studyLogs" :key="log.id"
                class="study-log-item"
                :class="{ 'study-log-item--selected': selectedLogs.has(log.id) }"
                @click="toggleLog(log.id)"
              >
                <div class="log-track-dot" :style="{ background: log.color }" />
                <div class="log-info">
                  <div class="log-title">{{ log.title }}</div>
                  <div class="log-date">{{ log.day }}</div>
                </div>
                <div class="log-check" :class="{ 'log-check--on': selectedLogs.has(log.id) }">
                  <i class="fas fa-check" />
                </div>
              </div>
            </div>
          </div>

          <!-- 블로그 스타일 설정 -->
          <div class="section-card">
            <div class="section-title">
              <i class="fas fa-sliders-h" /> 글 스타일 설정
            </div>
            <div class="style-options">
              <div class="style-row">
                <label>글 형식</label>
                <div class="style-btns">
                  <button
                    v-for="s in styleOptions" :key="s.value"
                    class="style-btn"
                    :class="{ 'style-btn--active': blogStyle === s.value }"
                    @click="blogStyle = s.value"
                  >{{ s.label }}</button>
                </div>
              </div>
              <div class="style-row">
                <label>어조</label>
                <div class="style-btns">
                  <button
                    v-for="t in toneOptions" :key="t.value"
                    class="style-btn"
                    :class="{ 'style-btn--active': blogTone === t.value }"
                    @click="blogTone = t.value"
                  >{{ t.label }}</button>
                </div>
              </div>
              <div class="style-row">
                <label>플랫폼</label>
                <div class="style-btns">
                  <button
                    v-for="p in platformOptions" :key="p.value"
                    class="style-btn"
                    :class="{ 'style-btn--active': platform === p.value }"
                    @click="platform = p.value"
                  >
                    <i :class="p.icon" /> {{ p.label }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 추가 메모 -->
          <div class="section-card">
            <div class="section-title"><i class="fas fa-sticky-note" /> 추가 메모</div>
            <textarea
              v-model="extraNote"
              class="memo-textarea"
              placeholder="블로그에 포함하고 싶은 내용을 자유롭게 입력하세요..."
              rows="4"
            />
          </div>

          <button
            class="btn-generate"
            :disabled="selectedLogs.size === 0 || generating"
            @click="generate"
          >
            <i :class="generating ? 'fas fa-spinner fa-spin' : 'fas fa-magic'" />
            {{ generating ? 'AI가 초안을 작성 중...' : '블로그 초안 생성' }}
          </button>
        </div>

        <!-- 오른쪽: 결과 -->
        <div class="right-panel">
          <!-- 빈 상태 -->
          <div v-if="!result && !generating" class="empty-state">
            <div class="empty-icon"><i class="fas fa-feather-alt" /></div>
            <p class="empty-title">블로그 초안이 여기에 표시됩니다</p>
            <p class="empty-sub">왼쪽에서 학습 기록을 선택하고<br>초안 생성 버튼을 눌러보세요</p>
          </div>

          <!-- 생성 중 스켈레톤 -->
          <div v-else-if="generating" class="skeleton-wrap">
            <div class="skeleton-line skeleton-line--title" />
            <div class="skeleton-line" />
            <div class="skeleton-line skeleton-line--short" />
            <div class="skeleton-line" />
            <div class="skeleton-line skeleton-line--mid" />
            <div class="skeleton-line" />
            <div class="skeleton-line skeleton-line--short" />
          </div>

          <!-- 결과 -->
          <div v-else class="result-wrap fade-in">
            <div class="result-toolbar">
              <div class="result-platform-badge">
                <i :class="currentPlatformIcon" />
                {{ currentPlatformLabel }}
              </div>
              <div class="result-actions">
                <button class="tool-btn" @click="copyResult" :class="{ 'tool-btn--done': copied }">
                  <i :class="copied ? 'fas fa-check' : 'fas fa-copy'" />
                  {{ copied ? '복사됨' : '복사' }}
                </button>
                <button class="tool-btn" @click="result = null">
                  <i class="fas fa-redo" /> 다시 생성
                </button>
              </div>
            </div>

            <!-- 마크다운 미리보기 -->
            <div class="result-content" v-html="renderedResult" />
          </div>
        </div>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { useCalendarStore } from '@/stores/useCalendarStore'
import { storeToRefs } from 'pinia'

const calendarStore = useCalendarStore()
const { schedules } = storeToRefs(calendarStore)

// ── 학습 기록 (hl_blog 트랙 + 일반 트랙 스케줄) ──
const studyLogs = computed(() => {
  return [...schedules.value]
    .sort((a, b) => b.day.localeCompare(a.day))
    .slice(0, 20)
    .map(s => {
      const track = calendarStore.getTrackById(s.track)
      return {
        id:    s.id,
        title: s.tooltip?.title || s.text || '학습 기록',
        day:   s.day,
        color: track?.color || '#818cf8',
        track: track?.name || '',
      }
    })
})

const selectedLogs = ref(new Set())
function toggleLog(id) {
  const next = new Set(selectedLogs.value)
  if (next.has(id)) next.delete(id)
  else              next.add(id)
  selectedLogs.value = next
}

// ── 스타일 옵션 ──
const blogStyle = ref('tutorial')
const blogTone  = ref('friendly')
const platform  = ref('velog')
const extraNote = ref('')

const styleOptions   = [
  { value: 'tutorial',  label: '튜토리얼' },
  { value: 'review',    label: '회고' },
  { value: 'summary',   label: '요약 정리' },
  { value: 'essay',     label: '에세이' },
]
const toneOptions    = [
  { value: 'friendly',    label: '친근하게' },
  { value: 'professional',label: '전문적으로' },
  { value: 'simple',      label: '간결하게' },
]
const platformOptions = [
  { value: 'velog',   label: 'Velog',   icon: 'fas fa-blog' },
  { value: 'tistory', label: 'Tistory', icon: 'fas fa-t' },
  { value: 'github',  label: 'GitHub',  icon: 'fab fa-github' },
  { value: 'notion',  label: 'Notion',  icon: 'fas fa-file-alt' },
]

const currentPlatformIcon  = computed(() => platformOptions.find(p => p.value === platform.value)?.icon || 'fas fa-blog')
const currentPlatformLabel = computed(() => platformOptions.find(p => p.value === platform.value)?.label || 'Velog')

// ── 생성 ──
const generating = ref(false)
const result     = ref(null)
const copied     = ref(false)

const styleLabel = (v, arr) => arr.find(x => x.value === v)?.label || v

function generate() {
  if (selectedLogs.value.size === 0 || generating.value) return

  const logs = studyLogs.value.filter(l => selectedLogs.value.has(l.id))
  generating.value = true
  result.value = null

  // 실제 API 연동 전 샘플 생성 (타이머로 시뮬레이션)
  setTimeout(() => {
    const titles = logs.map(l => l.title).join(', ')
    const dateRange = logs.length > 1
      ? `${logs[logs.length-1].day} ~ ${logs[0].day}`
      : logs[0].day

    result.value = buildMockDraft({
      titles, dateRange,
      style: styleLabel(blogStyle.value, styleOptions),
      tone:  styleLabel(blogTone.value,  toneOptions),
      platform: platform.value,
      extraNote: extraNote.value,
    })
    generating.value = false
  }, 1800)
}

function buildMockDraft({ titles, dateRange, style, tone, platform, extraNote }) {
  const header = platform === 'velog' || platform === 'github'
    ? `# ${titles} 학습 회고\n\n`
    : `<h1>${titles} 학습 회고</h1>\n\n`

  return `# ${titles} — ${style} 정리

> 📅 학습 기간: ${dateRange}  
> ✍️ 글 형식: ${style} | 어조: ${tone}

---

## 들어가며

이번 포스팅에서는 **${titles}**을 학습하면서 정리한 내용을 ${style} 형태로 공유합니다.

## 핵심 개념 정리

### 1. 기본 개념 이해
학습 과정에서 가장 먼저 다룬 것은 기초 개념이었습니다.  
반복적인 실습을 통해 원리를 체화하는 과정이 중요했습니다.

### 2. 실습 내용
직접 코드를 작성하며 발생한 오류와 해결 과정을 기록했습니다.

\`\`\`js
// 예시 코드
const result = await learnSomething()
console.log(result)
\`\`\`

## 느낀 점 & 회고

${extraNote || '아직 추가 메모가 없습니다. 왼쪽 메모 영역을 활용해보세요.'}

---

**다음 학습 계획:** 오늘 배운 내용을 바탕으로 심화 주제를 학습할 예정입니다.

> 💡 *이 초안은 AI가 학습 기록을 바탕으로 생성했습니다. 내용을 검토하고 보완해 발행하세요.*
`
}

// 마크다운 → HTML (간단 변환)
const renderedResult = computed(() => {
  if (!result.value) return ''
  return result.value
    .replace(/^# (.+)$/gm,  '<h1>$1</h1>')
    .replace(/^## (.+)$/gm, '<h2>$1</h2>')
    .replace(/^### (.+)$/gm,'<h3>$1</h3>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g,  '<code>$1</code>')
    .replace(/```[\w]*\n([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    .replace(/^> (.+)$/gm,  '<blockquote>$1</blockquote>')
    .replace(/^---$/gm,     '<hr/>')
    .replace(/\n/g,         '<br/>')
})

function copyResult() {
  if (!result.value) return
  navigator.clipboard.writeText(result.value)
  copied.value = true
  setTimeout(() => { copied.value = false }, 2000)
}
</script>

<style scoped>
.app-layout {
  display: flex; width: 100%; height: 100vh; overflow: hidden;
  background: var(--bg-base); color: var(--text-primary);
  font-family: 'Escoredream', system-ui, sans-serif;
}
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; min-width: 0; }

/* ── 헤더 ── */
.page-header {
  height: 64px; background: var(--bg-surface); border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 28px; flex-shrink: 0; gap: 16px;
}
.header-left  { display: flex; align-items: center; gap: 14px; }
.header-icon  {
  width: 40px; height: 40px; border-radius: 12px; flex-shrink: 0;
  background: rgba(52,211,153,0.15); border: 1px solid rgba(52,211,153,0.3);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #34d399;
}
.page-title   { font-size: 17px; font-weight: 800; color: var(--text-primary); margin-bottom: 2px; }
.page-sub     { font-size: 11px; color: var(--text-faint); }
.header-actions { display: flex; gap: 10px; }

.btn-view-cal {
  display: flex; align-items: center; gap: 7px;
  padding: 8px 14px; border-radius: 10px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-muted); font-size: 12px; font-weight: 600;
  text-decoration: none; transition: all 0.15s;
  font-family: 'Escoredream', sans-serif;
}
.btn-view-cal:hover { background: var(--bg-hover); color: var(--text-primary); }

/* ── 바디 ── */
.body-wrap {
  flex: 1; display: grid; grid-template-columns: 340px 1fr;
  overflow: hidden; gap: 0;
}

/* ── 왼쪽 패널 ── */
.left-panel {
  border-right: 1px solid var(--border);
  overflow-y: auto; padding: 20px 16px;
  display: flex; flex-direction: column; gap: 14px;
  scrollbar-width: thin; scrollbar-color: var(--scrollbar-thumb) transparent;
}

.section-card {
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 14px; padding: 16px 18px;
}
.section-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 12px; font-weight: 700; color: var(--text-muted);
  margin-bottom: 14px; letter-spacing: 0.04em;
}
.section-title i { color: #34d399; }

/* 학습 기록 리스트 */
.study-log-list { display: flex; flex-direction: column; gap: 6px; }
.study-log-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 10px;
  border: 1.5px solid transparent;
  background: var(--bg-elevated); cursor: pointer; transition: all 0.13s;
}
.study-log-item:hover { border-color: rgba(52,211,153,0.3); background: var(--bg-hover); }
.study-log-item--selected { border-color: #34d399; background: rgba(52,211,153,0.07); }

.log-track-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.log-info { flex: 1; min-width: 0; }
.log-title { font-size: 12px; font-weight: 600; color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.log-date  { font-size: 10px; color: var(--text-faint); margin-top: 2px; }

.log-check {
  width: 22px; height: 22px; border-radius: 50%;
  border: 2px solid var(--border-mid); flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 10px; color: transparent; transition: all 0.15s;
}
.log-check--on { background: #34d399; border-color: #34d399; color: #fff; }

/* 스타일 설정 */
.style-options { display: flex; flex-direction: column; gap: 12px; }
.style-row { display: flex; flex-direction: column; gap: 6px; }
.style-row label { font-size: 11px; font-weight: 600; color: var(--text-faint); }
.style-btns { display: flex; flex-wrap: wrap; gap: 6px; }
.style-btn {
  padding: 5px 11px; border-radius: 8px; font-size: 11px; font-weight: 600;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-muted); cursor: pointer; transition: all 0.13s;
  font-family: 'Escoredream', sans-serif;
}
.style-btn:hover { background: var(--bg-hover); color: var(--text-primary); }
.style-btn--active { background: rgba(52,211,153,0.12); border-color: #34d399; color: #34d399; font-weight: 700; }

/* 메모 */
.memo-textarea {
  width: 100%; padding: 10px 12px; border-radius: 10px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-primary); font-size: 12px; line-height: 1.6;
  resize: vertical; outline: none; transition: border-color 0.15s;
  font-family: 'Escoredream', system-ui, sans-serif;
}
.memo-textarea:focus { border-color: #34d399; }

/* 생성 버튼 */
.btn-generate {
  width: 100%; padding: 14px; border-radius: 12px; border: none; cursor: pointer;
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff; font-size: 15px; font-weight: 700;
  display: flex; align-items: center; justify-content: center; gap: 9px;
  transition: opacity 0.15s; font-family: 'Escoredream', sans-serif;
}
.btn-generate:hover:not(:disabled) { opacity: 0.88; }
.btn-generate:disabled { opacity: 0.4; cursor: not-allowed; }

/* ── 오른쪽 패널 ── */
.right-panel {
  overflow-y: auto; padding: 24px 28px;
  scrollbar-width: thin; scrollbar-color: var(--scrollbar-thumb) transparent;
}

/* 빈 상태 */
.empty-state {
  height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 14px; text-align: center;
}
.empty-icon {
  width: 72px; height: 72px; border-radius: 20px;
  background: rgba(52,211,153,0.1); border: 1px solid rgba(52,211,153,0.2);
  display: flex; align-items: center; justify-content: center;
  font-size: 30px; color: #34d399;
}
.empty-title { font-size: 16px; font-weight: 700; color: var(--text-secondary); }
.empty-sub   { font-size: 13px; color: var(--text-faint); line-height: 1.6; }

/* 스켈레톤 */
.skeleton-wrap { display: flex; flex-direction: column; gap: 12px; padding-top: 8px; }
.skeleton-line {
  height: 14px; border-radius: 6px;
  background: linear-gradient(90deg, var(--bg-elevated) 25%, var(--bg-hover) 50%, var(--bg-elevated) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}
.skeleton-line--title { height: 24px; width: 60%; }
.skeleton-line--short { width: 45%; }
.skeleton-line--mid   { width: 70%; }
@keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }

/* 결과 */
@keyframes fadeIn { from{opacity:0;transform:translateY(6px)} to{opacity:1;transform:translateY(0)} }
.fade-in { animation: fadeIn 0.3s ease both; }

.result-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid var(--border);
}
.result-platform-badge {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 700; color: var(--text-secondary);
  background: var(--bg-elevated); border: 1px solid var(--border);
  padding: 6px 12px; border-radius: 8px;
}
.result-actions { display: flex; gap: 8px; }
.tool-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px; border-radius: 8px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-muted); font-size: 12px; font-weight: 600;
  cursor: pointer; transition: all 0.15s; font-family: 'Escoredream', sans-serif;
}
.tool-btn:hover { background: var(--bg-hover); color: var(--text-primary); }
.tool-btn--done { background: rgba(52,211,153,0.12); border-color: #34d399; color: #34d399; }

/* 마크다운 결과 */
.result-content {
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 14px; padding: 28px 32px;
  font-size: 14px; line-height: 1.8; color: var(--text-secondary);
}
.result-content :deep(h1) { font-size: 22px; font-weight: 800; color: var(--text-primary); margin-bottom: 16px; }
.result-content :deep(h2) { font-size: 17px; font-weight: 700; color: var(--text-primary); margin: 24px 0 12px; padding-left: 12px; border-left: 3px solid #34d399; }
.result-content :deep(h3) { font-size: 15px; font-weight: 700; color: var(--text-secondary); margin: 16px 0 8px; }
.result-content :deep(strong) { color: var(--text-primary); font-weight: 700; }
.result-content :deep(code) { background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 5px; padding: 2px 7px; font-size: 12px; color: #34d399; font-family: 'Fira Code', monospace; }
.result-content :deep(pre) { background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 10px; padding: 16px; margin: 12px 0; overflow-x: auto; }
.result-content :deep(pre code) { background: none; border: none; padding: 0; }
.result-content :deep(blockquote) { border-left: 3px solid var(--border-mid); padding-left: 14px; color: var(--text-faint); font-style: italic; margin: 12px 0; }
.result-content :deep(hr) { border: none; border-top: 1px solid var(--border); margin: 20px 0; }
</style>
