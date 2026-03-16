<template>
  <div class="loading-root">

    <div class="global-stepper-wrap">
      <div class="brutal-stepper">
        <div class="step done">1. 계정 연동</div>
        <div class="step done">2. 사전 설문</div>
        <div class="step active">3. 데이터 분석</div>
        <div class="step">4. 결과 확인</div>
      </div>
    </div>

    <div class="loading-card">
      <div class="loading-header">
        <i class="fas fa-cog fa-spin loading-icon" />
        <h2>ANALYZING DATA...</h2>
        <p>사용자의 활동 데이터와 기술 스택을 종합 분석 중입니다.</p>
      </div>

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
          <div class="brutal-progress">
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
import { useRouter } from 'vue-router'

const router = useRouter()
const currentStep = ref(0)
const progressWidth = ref(0)

const steps = [
  { label: 'GitHub 및 Velog 데이터 수집' },
  { label: '기술 숙련도 및 부족 역량 분석' },
  { label: 'AI 맞춤형 커리큘럼 생성' },
]

let timer
let progressTimer

onMounted(() => {
  progressTimer = setInterval(() => {
    if (progressWidth.value < 90) progressWidth.value += 15
  }, 150)

  timer = setInterval(() => {
    if (currentStep.value < steps.length - 1) {
      currentStep.value++
      progressWidth.value = 0
    } else {
      clearInterval(timer)
      clearInterval(progressTimer)
      progressWidth.value = 100
      setTimeout(() => router.push('/analyze'), 600)
    }
  }, 1200)
})

onUnmounted(() => {
  clearInterval(timer)
  clearInterval(progressTimer)
})

function statusClass(i) {
  if (i < currentStep.value) return 'done'
  if (i === currentStep.value) return 'active'
  return 'pending'
}
</script>

<style scoped>
/* 여백 비율 통일 */
.loading-root { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--bg-base); padding: 72px 24px 24px; font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; position: relative; }
.loading-card { width: 100%; max-width: 520px; background: var(--bg-surface); border: 2px solid var(--text-primary); border-radius: 0; padding: 40px; box-shadow: 12px 12px 0 #6b7280; }

.global-stepper-wrap { position: fixed; top: 16px; left: 50%; transform: translateX(-50%); width: 100%; max-width: 640px; padding: 0 24px; z-index: 100; }
.brutal-stepper { display: flex; gap: 8px; width: 100%; }
.brutal-stepper .step { flex: 1; text-align: center; padding: 12px 4px; border: 2px solid var(--border); background: var(--bg-surface); color: var(--text-muted); font-size: 13px; font-weight: 900; font-family: 'Escoredream', sans-serif; transition: all 0.2s; white-space: nowrap; }
.brutal-stepper .step.active { border-color: var(--text-primary); background: var(--text-primary); color: var(--bg-base); box-shadow: 4px 4px 0 #6b7280; transform: translate(-2px, -2px); }
.brutal-stepper .step.done { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-surface); }
@media (max-width: 640px) { .brutal-stepper .step { font-size: 11px; padding: 8px 2px; } }

.loading-header { text-align: center; border-bottom: 2px dashed var(--text-primary); padding-bottom: 24px; margin-bottom: 32px; }
.loading-icon { font-size: 40px; color: var(--text-primary); margin-bottom: 16px; }
.loading-header h2 { font-size: 24px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary); margin-bottom: 8px; }
.loading-header p { font-size: 13px; font-weight: 700; color: var(--text-muted); }

.steps-container { display: flex; flex-direction: column; gap: 24px; }
.step-item { display: flex; flex-direction: column; gap: 12px; }

.step-status { display: flex; align-items: center; gap: 12px; }
.status-box { width: 32px; height: 32px; border: 2px solid var(--text-primary); display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 900; }
.status-box.done { background: var(--text-primary); color: var(--bg-base); }
.status-box.active { background: transparent; color: var(--text-primary); }
.status-box.pending { border-color: var(--border); color: var(--text-faint); }

.step-label { font-size: 14px; font-weight: 800; color: var(--text-faint); transition: color 0.2s; }
.step-label.active { color: var(--text-primary); }

.brutal-progress { width: 100%; height: 12px; border: 2px solid var(--text-primary); background: var(--bg-base); padding: 2px; }
.progress-fill { height: 100%; background: var(--text-primary); transition: width 0.15s ease-out; }
</style>