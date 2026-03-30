<template>
  <div class="loading-root">

    <div class="global-stepper-wrap">
      <div class="page-stepper">
        <div class="step done">1. 계정 연동</div>
        <div class="step done">2. 사전 설문</div>
        <div class="step active">3. 데이터 분석</div>
        <div class="step">4. 결과 확인</div>
      </div>
    </div>

    <div class="loading-card">
      <div v-if="loadError" class="error-banner">
        <p>{{ loadError }}</p>
        <button class="btn-outline-small" @click="router.push('/onboarding/connect')">다시 시도</button>
      </div>

      <div class="loading-header">
        <h2>ANALYZING DATA...</h2>
        <p>사용자의 활동 데이터와 기술 스택을 종합 분석 중입니다.</p>
      </div>

      <!-- Mock UI 일러스트 영역 -->
      <div class="illustration-area">
        <Transition name="fade-svg" mode="out-in">
          <div :key="currentStep" class="mock-screen">

            <!-- GitHub Mock UI -->
            <div v-if="steps[currentStep].icon === 'github'" class="mock-github">
              <div class="mock-titlebar">
                <div class="mock-dots"><span /><span /><span /></div>
                <div class="mock-url"><i class="fab fa-github" /> github.com</div>
              </div>
              <div class="mock-body">
                <div class="mock-commit" v-for="n in 5" :key="n" :class="'delay-' + n">
                  <div class="commit-dot" />
                  <div class="commit-line">
                    <div class="commit-hash" />
                    <div class="commit-msg" :style="{ width: (60 + Math.random() * 30) + '%' }" />
                  </div>
                  <div class="scan-bar" />
                </div>
              </div>
            </div>

            <!-- Velog Mock UI -->
            <div v-else-if="steps[currentStep].icon === 'velog'" class="mock-velog">
              <div class="mock-titlebar">
                <div class="mock-dots"><span /><span /><span /></div>
                <div class="mock-url"><i class="fas fa-v" /> velog.io</div>
              </div>
              <div class="mock-body mock-blog-body">
                <div class="mock-post" v-for="n in 3" :key="n" :class="'delay-' + n">
                  <div class="post-thumb" />
                  <div class="post-content">
                    <div class="post-title" :style="{ width: (50 + Math.random() * 40) + '%' }" />
                    <div class="post-desc" />
                    <div class="post-tags">
                      <span v-for="t in 3" :key="t" class="post-tag" />
                    </div>
                  </div>
                  <div class="scan-bar" />
                </div>
              </div>
            </div>

            <!-- Analysis Mock UI -->
            <div v-else-if="steps[currentStep].icon === 'analysis'" class="mock-analysis">
              <div class="mock-titlebar">
                <div class="mock-dots"><span /><span /><span /></div>
                <div class="mock-url"><i class="fas fa-brain" /> AI Analysis</div>
              </div>
              <div class="mock-body mock-analysis-body">
                <div class="analysis-row" v-for="n in 4" :key="n" :class="'delay-' + n">
                  <div class="analysis-label" />
                  <div class="analysis-bar-bg">
                    <div class="analysis-bar-fill" :style="{ width: (30 + n * 15) + '%' }" />
                  </div>
                  <div class="analysis-pct" />
                </div>
                <div class="analysis-summary delay-5">
                  <div class="summary-line" v-for="l in 3" :key="l" :style="{ width: (60 + l * 10) + '%' }" />
                </div>
              </div>
            </div>

            <!-- Complete Mock UI -->
            <div v-else class="mock-complete">
              <div class="mock-titlebar">
                <div class="mock-dots"><span /><span /><span /></div>
                <div class="mock-url"><i class="fas fa-check-circle" /> Complete</div>
              </div>
              <div class="mock-body mock-complete-body">
                <div class="complete-check">
                  <svg viewBox="0 0 60 60" class="complete-svg">
                    <circle cx="30" cy="30" r="25" fill="none" stroke="var(--text-primary)" stroke-width="2" />
                    <path d="M18 30l8 8 16-18" fill="none" stroke="var(--text-primary)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="check-draw" />
                  </svg>
                </div>
                <div class="complete-text-lines">
                  <div class="complete-line" v-for="n in 2" :key="n" :class="'delay-' + n" />
                </div>
              </div>
            </div>

          </div>
        </Transition>
      </div>

      <!-- 타이핑 메시지 -->
      <div class="typing-area">
        <p class="typing-text">{{ currentMessage }}<span class="cursor">▌</span></p>
      </div>

      <!-- 단계 dots + 프로그레스 -->
      <div class="progress-section">
        <div class="step-dots">
          <div v-for="(step, i) in steps" :key="i" class="dot-item">
            <div class="dot" :class="{ active: i === currentStep, done: i < currentStep }">
              <i v-if="i < currentStep" class="fas fa-check" />
              <i v-else-if="i === currentStep" class="fas fa-spinner fa-pulse" />
            </div>
            <span class="dot-label" :class="{ active: i <= currentStep }">{{ step.label }}</span>
          </div>
        </div>
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: totalProgress + '%' }" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getTaskStatus } from '@/api/aiApi'
import { useAuthStore } from '@/stores/useAuthStore'

const router = useRouter()
const authStore = useAuthStore()
const currentStep = ref(0)
const progressWidth = ref(0)
const loadError = ref('')

const steps = [
  {
    label: 'GitHub 수집',
    icon: 'github',
    messages: [
      'GitHub 레포지토리를 탐색하고 있어요...',
      '커밋 히스토리를 수집 중이에요...',
      '코드 패턴을 읽고 있어요...',
    ]
  },
  {
    label: 'Velog 분석',
    icon: 'velog',
    messages: [
      'Velog 게시글을 읽고 있어요...',
      '기술 키워드를 추출하고 있어요...',
      '학습 흐름을 파악 중이에요...',
    ]
  },
  {
    label: 'AI 분석',
    icon: 'analysis',
    messages: [
      '수집된 데이터를 종합하고 있어요...',
      '기술 숙련도를 계산 중이에요...',
      '추천 포지션을 분석하고 있어요...',
    ]
  },
  {
    label: '프로필 생성',
    icon: 'complete',
    messages: [
      '맞춤 프로필을 생성하고 있어요...',
      '거의 다 됐어요!',
    ]
  },
]

const totalProgress = computed(() => {
  const stepShare = 100 / steps.length
  return (currentStep.value * stepShare) + (progressWidth.value / 100 * stepShare)
})

// --- 타이핑 효과 ---
const currentMessage = ref('')
const messageIndex = ref(0)
const charIndex = ref(0)
let typingTimer = null

function stopTyping() {
  if (typingTimer) clearInterval(typingTimer)
  typingTimer = null
}

function startTyping() {
  stopTyping()
  const step = steps[currentStep.value]
  if (!step) return
  const msg = step.messages[messageIndex.value % step.messages.length]

  typingTimer = setInterval(() => {
    if (charIndex.value < msg.length) {
      currentMessage.value = msg.slice(0, charIndex.value + 1)
      charIndex.value++
    } else {
      stopTyping()
      setTimeout(() => {
        messageIndex.value = (messageIndex.value + 1) % step.messages.length
        charIndex.value = 0
        currentMessage.value = ''
        startTyping()
      }, 1200)
    }
  }, 45)
}

watch(currentStep, () => {
  stopTyping()
  messageIndex.value = 0
  charIndex.value = 0
  currentMessage.value = ''
  startTyping()
})

// --- 폴링 + 단계 타이머 ---
let pollTimer
let progressTimer
let stepTimer
const isCompleted = ref(false)

// 단계별 예상 체류 시간 (ms): 수집 빠르게, 분석 오래
const STEP_DURATIONS = [4000, 5000, 18000, 8000]

function startProgressAnimation() {
  progressTimer = setInterval(() => {
    if (progressWidth.value < 90) progressWidth.value += 3
  }, 400)
}

function startStepTimer() {
  function advanceStep() {
    if (isCompleted.value) return
    if (currentStep.value < steps.length - 2) {
      currentStep.value++
      progressWidth.value = 0
      stepTimer = setTimeout(advanceStep, STEP_DURATIONS[currentStep.value])
    }
  }
  stepTimer = setTimeout(advanceStep, STEP_DURATIONS[0])
}

function resolveTaskId() {
  return authStore.velogTaskId || ''
}

async function pollTaskStatus(taskId) {
  try {
    const { data } = await getTaskStatus(taskId)

    if (data.status === 'completed') {
      isCompleted.value = true
      clearInterval(pollTimer)
      clearInterval(progressTimer)
      if (stepTimer) clearTimeout(stepTimer)
      currentStep.value = steps.length - 1
      progressWidth.value = 100

      if (data.data) {
        localStorage.setItem('analysisResult', JSON.stringify(data.data))
      }
      console.log('데이터 도착', data.data)
      setTimeout(() => router.push('/onboarding/result'), 1200)
    } else if (data.status === 'failed') {
      isCompleted.value = true
      clearInterval(pollTimer)
      clearInterval(progressTimer)
      if (stepTimer) clearTimeout(stepTimer)
      loadError.value = data.error || '분석 작업이 실패했습니다. 다시 시도해 주세요.'
      console.error('분석 실패:', data.error)
    }
  } catch (e) {
    clearInterval(pollTimer)
    clearInterval(progressTimer)
    if (stepTimer) clearTimeout(stepTimer)
    loadError.value = '분석 상태를 확인하지 못했습니다. 서버 연결이나 작업 ID를 다시 확인해 주세요.'
    console.error('분석 상태 조회 실패:', e)
  }
}

onMounted(() => {
  startProgressAnimation()
  startTyping()
  startStepTimer()

  const taskId = resolveTaskId()
  if (taskId) {
    pollTimer = setInterval(() => pollTaskStatus(taskId), 2000)
    pollTaskStatus(taskId)
    return
  }

  clearInterval(progressTimer)
  loadError.value = 'Velog 분석 작업 ID를 찾지 못했습니다. Velog 연동을 다시 확인해 주세요.'
})

onUnmounted(() => {
  clearInterval(pollTimer)
  clearInterval(progressTimer)
  if (stepTimer) clearTimeout(stepTimer)
  stopTyping()
})
</script>

<style scoped>
.loading-root {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg-base); padding: 72px 24px 24px;
  font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; position: relative;
}
.loading-card {
  width: 100%; max-width: 520px;
  background: var(--bg-surface); border: 1px solid var(--border); padding: 48px 40px; border-radius: 16px;
  animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes fadeUp { from { opacity:0; transform:translateY(12px); } to { opacity:1; transform:translateY(0); } }

/* ── Stepper ── */
.global-stepper-wrap { position: fixed; top: 16px; left: 50%; transform: translateX(-50%); width: 100%; max-width: 640px; padding: 0 24px; z-index: 1000; }
.page-stepper { display: flex; gap: 0; width: 100%; border: 1px solid var(--border); overflow: hidden; }
.page-stepper .step {
  flex: 1; text-align: center; padding: 10px 4px;
  background: var(--bg-surface); color: var(--text-muted);
  font-size: 12px; font-weight: 700; font-family: inherit;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); white-space: nowrap;
  border-right: 1px solid var(--border);
}
.page-stepper .step:last-child { border-right: none; }
.page-stepper .step.active { background: var(--clr-primary, var(--text-primary)); color: var(--bg-base); }
.page-stepper .step.done { color: var(--clr-success, var(--text-primary)); background: transparent; }
@media (max-width: 640px) { .page-stepper .step { font-size: 10px; padding: 8px 2px; } }

/* ── Header ── */
.loading-header {
  text-align: center; margin-bottom: 32px;
}
.loading-header h2 {
  font-size: 20px; font-weight: 900; letter-spacing: 0.12em;
  color: var(--text-primary); margin-bottom: 8px;
}
.loading-header p { font-size: 13px; font-weight: 600; color: var(--text-muted); line-height: 1.6; }

/* ── Error ── */
.error-banner {
  border: 1px solid var(--border); padding: 16px 18px; margin-bottom: 28px;
  display: flex; flex-direction: column; gap: 12px;
  color: var(--text-primary); background: var(--bg-hover);
}
.error-banner p { font-size: 13px; line-height: 1.5; font-weight: 700; }

/* ── Mock UI 일러스트 ── */
.illustration-area {
  display: flex; justify-content: center; align-items: center;
  min-height: 220px; margin-bottom: 24px;
}

/* 전환 애니메이션 */
.fade-svg-enter-active, .fade-svg-leave-active { transition: all 0.4s ease; }
.fade-svg-enter-from { opacity: 0; transform: scale(0.95) translateY(8px); }
.fade-svg-leave-to { opacity: 0; transform: scale(1.02) translateY(-8px); }

/* 공통 Mock 화면 */
.mock-screen { width: 100%; max-width: 400px; }
.mock-github, .mock-velog, .mock-analysis, .mock-complete {
  border: 1px solid var(--border); border-radius: 10px; overflow: hidden;
  background: var(--bg-base);
}
.mock-titlebar {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px; border-bottom: 1px solid var(--border);
  background: var(--bg-surface);
}
.mock-dots { display: flex; gap: 5px; }
.mock-dots span {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--border);
}
.mock-dots span:nth-child(1) { background: #ef4444; }
.mock-dots span:nth-child(2) { background: #eab308; }
.mock-dots span:nth-child(3) { background: #22c55e; }
.mock-url {
  font-size: 11px; font-weight: 700; color: var(--text-muted);
  display: flex; align-items: center; gap: 6px;
}
.mock-body { padding: 16px; }

/* ── GitHub Mock: 커밋 로그 ── */
.mock-commit {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 0; border-bottom: 1px solid var(--border);
  position: relative; overflow: hidden;
  animation: commitAppear 0.5s ease both;
}
.mock-commit:last-child { border-bottom: none; }
.delay-1 { animation-delay: 0.2s; }
.delay-2 { animation-delay: 0.5s; }
.delay-3 { animation-delay: 0.8s; }
.delay-4 { animation-delay: 1.1s; }
.delay-5 { animation-delay: 1.4s; }
@keyframes commitAppear {
  from { opacity: 0; transform: translateX(-10px); }
  to { opacity: 1; transform: translateX(0); }
}
.commit-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--text-primary); flex-shrink: 0;
}
.commit-line { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.commit-hash {
  width: 50px; height: 8px; border-radius: 4px;
  background: var(--text-primary); opacity: 0.3;
}
.commit-msg {
  height: 8px; border-radius: 4px;
  background: var(--text-muted); opacity: 0.2;
}

/* 스캔 바 (공통) */
.scan-bar {
  position: absolute; top: 0; left: -100%; width: 100%; height: 100%;
  background: linear-gradient(90deg, transparent, var(--text-primary), transparent);
  opacity: 0.06; animation: scanMove 2.5s ease-in-out infinite;
}
.delay-1 .scan-bar { animation-delay: 0.3s; }
.delay-2 .scan-bar { animation-delay: 0.6s; }
.delay-3 .scan-bar { animation-delay: 0.9s; }
.delay-4 .scan-bar { animation-delay: 1.2s; }
.delay-5 .scan-bar { animation-delay: 1.5s; }
@keyframes scanMove {
  0% { left: -100%; }
  50% { left: 100%; }
  100% { left: 100%; }
}

/* ── Velog Mock: 블로그 카드 ── */
.mock-blog-body { display: flex; flex-direction: column; gap: 12px; }
.mock-post {
  display: flex; gap: 12px; padding: 10px;
  border: 1px solid var(--border); border-radius: 6px;
  position: relative; overflow: hidden;
  animation: commitAppear 0.5s ease both;
}
.post-thumb {
  width: 56px; height: 56px; border-radius: 4px;
  background: var(--text-primary); opacity: 0.08; flex-shrink: 0;
}
.post-content { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.post-title {
  height: 10px; border-radius: 4px;
  background: var(--text-primary); opacity: 0.25;
}
.post-desc {
  height: 8px; width: 90%; border-radius: 4px;
  background: var(--text-muted); opacity: 0.12;
}
.post-tags { display: flex; gap: 4px; }
.post-tag {
  width: 36px; height: 14px; border-radius: 8px;
  background: var(--text-primary); opacity: 0.1;
}

/* ── Analysis Mock: 숙련도 바 ── */
.mock-analysis-body { display: flex; flex-direction: column; gap: 14px; }
.analysis-row {
  display: flex; align-items: center; gap: 10px;
  animation: commitAppear 0.5s ease both;
}
.analysis-label {
  width: 50px; height: 10px; border-radius: 4px;
  background: var(--text-primary); opacity: 0.2;
}
.analysis-bar-bg {
  flex: 1; height: 8px; border-radius: 4px;
  background: var(--border); overflow: hidden;
}
.analysis-bar-fill {
  height: 100%; border-radius: 4px;
  background: var(--text-primary); opacity: 0.5;
  animation: barGrow 1.5s ease-out both;
}
.delay-1 .analysis-bar-fill { animation-delay: 0.3s; }
.delay-2 .analysis-bar-fill { animation-delay: 0.6s; }
.delay-3 .analysis-bar-fill { animation-delay: 0.9s; }
.delay-4 .analysis-bar-fill { animation-delay: 1.2s; }
@keyframes barGrow {
  from { width: 0; }
}
.analysis-pct {
  width: 24px; height: 10px; border-radius: 4px;
  background: var(--text-muted); opacity: 0.15;
}
.analysis-summary {
  display: flex; flex-direction: column; gap: 6px;
  padding-top: 10px; border-top: 1px solid var(--border);
  animation: commitAppear 0.5s ease both;
}
.summary-line {
  height: 8px; border-radius: 4px;
  background: var(--text-muted); opacity: 0.12;
}

/* ── Complete Mock ── */
.mock-complete-body {
  display: flex; flex-direction: column; align-items: center;
  gap: 16px; padding: 24px 16px;
}
.complete-check { width: 64px; height: 64px; }
.complete-svg { width: 100%; height: 100%; }
.check-draw {
  stroke-dasharray: 50; stroke-dashoffset: 50;
  animation: drawCheck 0.8s ease-out 0.3s forwards;
}
@keyframes drawCheck { to { stroke-dashoffset: 0; } }
.complete-text-lines {
  display: flex; flex-direction: column; align-items: center; gap: 6px; width: 100%;
}
.complete-line {
  height: 8px; border-radius: 4px; width: 60%;
  background: var(--text-muted); opacity: 0.15;
  animation: commitAppear 0.5s ease both;
}
.complete-line.delay-2 { width: 40%; }

/* ── 타이핑 영역 ── */
.typing-area {
  text-align: center; min-height: 48px;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 32px;
}
.typing-text {
  font-size: 15px; font-weight: 700; color: var(--text-primary);
  letter-spacing: 0.01em;
}
.cursor {
  color: var(--text-primary); animation: blink 0.8s step-end infinite;
  font-weight: 400; margin-left: 1px;
}
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }

/* ── 단계 dots + 프로그레스 ── */
.progress-section {
  border-top: 1px solid var(--border); padding-top: 24px;
}
.step-dots {
  display: flex; justify-content: space-between; margin-bottom: 16px;
}
.dot-item {
  display: flex; flex-direction: column; align-items: center; gap: 8px; flex: 1;
}
.dot {
  width: 28px; height: 28px; border: 1px solid var(--border);
  display: flex; align-items: center; justify-content: center;
  font-size: 10px; font-weight: 900; border-radius: 50%;
  transition: all 0.3s; color: var(--text-faint);
}
.dot.active { border-color: var(--text-primary); color: var(--text-primary); }
.dot.done { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }
.dot-label {
  font-size: 10px; font-weight: 700; color: var(--text-faint);
  transition: color 0.3s; text-align: center; white-space: nowrap;
}
.dot-label.active { color: var(--text-primary); }

.progress-track {
  width: 100%; height: 3px; background: var(--border); overflow: hidden; border-radius: 2px;
}
.progress-fill {
  height: 100%; background: var(--text-primary);
  transition: width 0.4s ease-out; border-radius: 2px;
}
</style>
