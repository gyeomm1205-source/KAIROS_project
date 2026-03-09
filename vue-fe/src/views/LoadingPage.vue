<template>
  <div class="loading-root">
    <div class="loading-inner">

      <!-- Spinner -->
      <div class="spinner-wrap">
        <div class="spinner-ring" />
        <div class="spinner-logo">
          <svg width="32" height="32" viewBox="0 0 28 28" fill="none">
            <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="url(#kgLoad)" stroke-width="1.5" fill="none" stroke-linejoin="round"/>
            <circle cx="14" cy="14" r="2.5" fill="url(#kgLoad)"/>
            <defs>
              <linearGradient id="kgLoad" x1="6" y1="3" x2="22" y2="25" gradientUnits="userSpaceOnUse">
                <stop stop-color="#818cf8"/><stop offset="1" stop-color="#38bdf8"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
      </div>

      <h2>{{ userName }}님에 대해<br>알아가고 있어요...</h2>
      <p class="loading-sub">분석이 완료되면 자동으로 이동합니다</p>

      <!-- Steps -->
      <div class="steps">
        <div v-for="(step, i) in steps" :key="step.label" class="step">
          <div class="step-header">
            <div class="step-icon" :class="stepClass(i)">
              <i v-if="i < currentStep" class="fas fa-check" />
              <div v-else-if="i === currentStep" class="step-active-dot" />
              <div v-else class="step-pending-dot" />
            </div>
            <span class="step-label" :class="{ 'step-done': i < currentStep, 'step-active': i === currentStep }">
              {{ step.label }}
            </span>
          </div>
          <div class="step-bar-wrap">
            <div class="step-bar">
              <div
                class="step-bar-fill"
                :class="{ 'step-bar-pulse': i === currentStep }"
                :style="{ width: i < currentStep ? '100%' : i === currentStep ? '60%' : '0%' }"
              />
            </div>
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
const userName = '홍길동'
const currentStep = ref(0)
const steps = [
  { label: '데이터 수집' },
  { label: '분석 진행' },
  { label: '추천 생성' },
]

let timer
onMounted(() => {
  timer = setInterval(() => {
    if (currentStep.value < steps.length - 1) {
      currentStep.value++
    } else {
      clearInterval(timer)
      setTimeout(() => router.push('/analysis'), 600)
    }
  }, 1000)
})
onUnmounted(() => clearInterval(timer))

function stepClass(i) {
  if (i < currentStep.value) return 'icon-done'
  if (i === currentStep.value) return 'icon-active'
  return 'icon-pending'
}
</script>

<style scoped>
.loading-root {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg-base); font-family: 'Escoredream', system-ui, sans-serif;
  padding: 24px;
}
.loading-inner {
  display: flex; flex-direction: column; align-items: center; gap: 0;
  animation: fadeIn 0.4s ease both;
}
@keyframes fadeIn { from{opacity:0;transform:translateY(8px)} to{opacity:1;transform:translateY(0)} }

/* Spinner */
.spinner-wrap {
  width: 96px; height: 96px; position: relative;
  display: flex; align-items: center; justify-content: center; margin-bottom: 32px;
}
.spinner-ring {
  position: absolute; inset: 0; border-radius: 50%;
  border: 4px solid var(--border);
  border-top-color: #818cf8;
  animation: spin 1s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.spinner-logo {
  display: flex; align-items: center; justify-content: center;
  filter: drop-shadow(0 0 8px rgba(129,140,248,0.5));
}

.loading-inner h2 {
  font-size: 24px; font-weight: 800; color: var(--text-primary);
  text-align: center; line-height: 1.4; margin-bottom: 10px;
}
.loading-sub { font-size: 13px; color: var(--text-faint); margin-bottom: 44px; }

/* Steps */
.steps { display: flex; flex-direction: column; gap: 20px; width: 280px; }
.step-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.step-icon {
  width: 22px; height: 22px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 10px;
}
.icon-done  { background: #818cf8; color: #fff; }
.icon-active { background: transparent; border: 2px solid #818cf8; }
.icon-pending { background: transparent; border: 2px solid var(--border); }
.step-active-dot { width: 8px; height: 8px; border-radius: 50%; background: #818cf8; }
.step-pending-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--text-faint); }

.step-label { font-size: 13px; font-weight: 600; color: var(--text-faint); transition: color 0.3s; }
.step-label.step-done { color: var(--text-primary); }
.step-label.step-active { color: #818cf8; }

.step-bar-wrap { padding-left: 34px; }
.step-bar { width: 100%; height: 4px; background: var(--border); border-radius: 2px; overflow: hidden; }
.step-bar-fill {
  height: 100%; border-radius: 2px;
  background: linear-gradient(90deg, #818cf8, #38bdf8);
  transition: width 0.8s ease;
}
.step-bar-pulse { animation: barPulse 1s ease infinite alternate; }
@keyframes barPulse { to { opacity: 0.6; } }
</style>
