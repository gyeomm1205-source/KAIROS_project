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
      <div class="loading-header">
        <div class="loading-icon-wrap">
          <i class="fas fa-cog fa-spin" />
        </div>
        <h2>ANALYZING DATA...</h2>
        <p>사용자의 활동 데이터와 기술 스택을 종합 분석 중입니다.</p>
      </div>

      <p class="loading-wait-text">잠시만 기다려 주세요.<br>최근 활동을 바탕으로 맞춤 분석을 준비하고 있어요.</p>

      <div class="steps-container">
        <div v-for="(step, i) in steps" :key="step.label" class="step-item">
          <div class="step-status">
            <div class="status-box" :class="statusClass(i)">
              <i v-if="i < currentStep" class="fas fa-check" />
              <i v-else-if="i === currentStep" class="fas fa-spinner fa-pulse" />
              <span v-else>{{ i + 1 }}</span>
            </div>
            <span class="step-label" :class="{ active: i <= currentStep }">{{ step.label }}</span>
          </div>
          <div class="progress-track">
            <div 
              class="progress-fill" 
              :style="{ width: i < currentStep ? '100%' : (i === currentStep ? progressWidth + '%' : '0%') }"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getTaskStatus } from '@/api/aiApi'

const router = useRouter()
const route = useRoute()
const currentStep = ref(0)
const progressWidth = ref(0)

const steps = [
  { label: '계정 데이터 확인 중' },
  { label: '활동 요약 중' },
  { label: '기술 스택 분석 중' },
  { label: '추천 준비 중' },
]

let pollTimer
let progressTimer

function startProgressAnimation() {
  progressTimer = setInterval(() => {
    if (progressWidth.value < 90) progressWidth.value += 5
  }, 300)
}

async function pollTaskStatus(taskId) {
  try {
    const { data } = await getTaskStatus(taskId)

    if (data.status === 'pending') {
      currentStep.value = 0
    } else if (data.status === 'processing') {
      if (currentStep.value < 2) currentStep.value++
      progressWidth.value = 0
    } else if (data.status === 'completed') {
      clearInterval(pollTimer)
      clearInterval(progressTimer)
      currentStep.value = steps.length - 1
      progressWidth.value = 100

      if (data.data) {
        localStorage.setItem('analysisResult', JSON.stringify(data.data))
      }
      console.log('데이터 도착', data.data)
      setTimeout(() => router.push('/onboarding/result'), 600)
    } else if (data.status === 'failed') {
      clearInterval(pollTimer)
      clearInterval(progressTimer)
      console.error('분석 실패:', data.error)
    }
  } catch (e) {
    console.error('상태 조회 실패:', e)
  }
}

function startMockTimer() {
  let mockTimer = setInterval(() => {
    if (currentStep.value < steps.length - 1) {
      currentStep.value++
      progressWidth.value = 0
    } else {
      clearInterval(mockTimer)
      clearInterval(progressTimer)
      progressWidth.value = 100
      setTimeout(() => router.push('/onboarding/result'), 600)
    }
  }, 1200)
  return mockTimer
}

onMounted(() => {
  startProgressAnimation()

  const taskId = route.query.taskId
  if (taskId) {
    pollTimer = setInterval(() => pollTaskStatus(taskId), 2000)
    pollTaskStatus(taskId)
  } else {
    pollTimer = startMockTimer()
  }
})

onUnmounted(() => {
  clearInterval(pollTimer)
  clearInterval(progressTimer)
})

function statusClass(i) {
  if (i < currentStep.value) return 'done'
  if (i === currentStep.value) return 'active'
  return 'pending'
}
</script>

<style scoped>
.loading-root {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg-base); padding: 72px 24px 24px;
  font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; position: relative;
}
.loading-card {
  width: 100%; max-width: 560px;
  background: var(--bg-surface); border: 1px solid var(--border); padding: 56px 48px; border-radius: 8px;
  animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes fadeUp { from { opacity:0; transform:translateY(12px); } to { opacity:1; transform:translateY(0); } }

/* ── Stepper ── */
.global-stepper-wrap { position: fixed; top: 16px; left: 50%; transform: translateX(-50%); width: 100%; max-width: 640px; padding: 0 24px; z-index: 100; }
.page-stepper { display: flex; gap: 0; width: 100%; border: 1px solid var(--border); overflow: hidden; }
.page-stepper .step {
  flex: 1; text-align: center; padding: 10px 4px;
  background: var(--bg-surface); color: var(--text-muted);
  font-size: 12px; font-weight: 700; font-family: inherit;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); white-space: nowrap;
  border-right: 1px solid var(--border);
}
.page-stepper .step:last-child { border-right: none; }
.page-stepper .step.active { background: var(--clr-primary); color: var(--bg-base); border-color: var(--clr-primary); }
.page-stepper .step.done { color: var(--clr-success); background: transparent; }
@media (max-width: 640px) { .page-stepper .step { font-size: 10px; padding: 8px 2px; } }

/* ── Loading header ── */
.loading-header {
  text-align: center; border-bottom: 1px solid var(--border);
  padding-bottom: 40px; margin-bottom: 48px;
}
.loading-icon-wrap {
  width: 72px; height: 72px; border: 1px solid var(--border);
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; color: var(--text-primary); margin: 0 auto 28px;
  animation: rotate 2s linear infinite;
}
@keyframes rotate { from { border-color: var(--text-primary); } 50% { border-color: var(--border); } to { border-color: var(--text-primary); } }
.loading-icon-wrap i { animation: none; }

.loading-header h2 {
  font-size: 22px; font-weight: 900; letter-spacing: 0.12em;
  color: var(--text-primary); margin-bottom: 10px;
}
.loading-header p { font-size: 13px; font-weight: 600; color: var(--text-muted); line-height: 1.6; }

/* ── Steps ── */
.steps-container { display: flex; flex-direction: column; gap: 32px; }
.step-item { display: flex; flex-direction: column; gap: 12px; }

.step-status { display: flex; align-items: center; gap: 16px; }
.status-box {
  width: 32px; height: 32px; border: 1px solid var(--text-primary);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 900; flex-shrink: 0;
  transition: all 0.3s;
}
.status-box.done { background: var(--text-primary); color: var(--bg-base); }
.status-box.active { background: transparent; color: var(--text-primary); border-color: var(--text-primary); }
.status-box.pending { border-color: var(--border); color: var(--text-faint); }

.step-label { font-size: 14px; font-weight: 700; color: var(--text-faint); transition: color 0.3s; }
.step-label.active { color: var(--text-primary); }

/* ── Progress bar ── */
.progress-track {
  width: 100%; height: 2px;
  background: var(--border); overflow: hidden;
  margin-left: 48px; /* 32px box + 16px gap */
  width: calc(100% - 48px);
}
.progress-fill { height: 100%; background: var(--clr-primary); transition: width 0.15s ease-out; }

.loading-wait-text { font-size: 13px; font-weight: 600; color: var(--text-muted); line-height: 1.6; text-align: center; margin-bottom: 32px; }
</style>