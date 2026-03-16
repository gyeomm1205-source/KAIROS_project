<template>
  <div class="auth-root">
    
    <div class="global-stepper-wrap">
      <div class="brutal-stepper">
        <div class="step active">1. 계정 연동</div>
        <div class="step">2. 사전 설문</div>
        <div class="step">3. 데이터 분석</div>
        <div class="step">4. 결과 확인</div>
      </div>
    </div>

    <div class="auth-card custom-scroll">
      <div class="auth-header">
        <div class="auth-icon"><i class="fas fa-plug" /></div>
        <h2>ACCOUNT LINKING</h2>
        <p>서비스 이용을 위해 계정을 연동해 주세요.<br>(모든 항목 연동 필수)</p>
      </div>

      <div class="auth-section">
        <button 
          class="link-btn" 
          :class="{ 'is-connected': isGoogleConnected }"
          @click="isGoogleConnected = true"
        >
          <div class="link-btn-left">
            <div class="link-icon"><i class="fab fa-google" /></div>
            <span v-if="!isGoogleConnected">Google 계정으로 시작하기</span>
            <span v-else class="text-blue-600">user@gmail.com 연동됨</span>
          </div>
          <i v-if="!isGoogleConnected" class="fas fa-arrow-right" />
          <i v-else class="fas fa-check" />
        </button>
      </div>

      <div class="auth-section">
        <div class="section-label">DATA SYNCHRONIZATION</div>
        
        <button 
          class="link-btn"
          :class="{ 'is-connected': isGithubConnected }"
          @click="isGithubConnected = true"
        >
          <div class="link-btn-left">
            <div class="link-icon"><i class="fab fa-github" /></div>
            <span v-if="!isGithubConnected">GitHub 연동하기</span>
            <span v-else class="text-blue-600">GitHub 연동 완료</span>
          </div>
          <i v-if="!isGithubConnected" class="fas fa-arrow-right" />
          <i v-else class="fas fa-check" />
        </button>

        <button 
          class="link-btn"
          :class="{ 'is-connected': isVelogConnected }"
          @click="isVelogConnected = true"
        >
          <div class="link-btn-left">
            <div class="link-icon"><i class="fas fa-v" /></div>
            <span v-if="!isVelogConnected">Velog 연동하기</span>
            <span v-else class="text-blue-600">Velog 연동 완료</span>
          </div>
          <i v-if="!isVelogConnected" class="fas fa-arrow-right" />
          <i v-else class="fas fa-check" />
        </button>
      </div>

      <button 
        class="btn-primary" 
        :disabled="!canProceed"
        @click="$router.push('/setup')"
      >
        NEXT STEP
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const isGoogleConnected = ref(false)
const isGithubConnected = ref(false)
const isVelogConnected = ref(false)

const canProceed = computed(() => isGoogleConnected.value && isGithubConnected.value && isVelogConnected.value)
</script>

<style scoped>
/* 비율 보정: top padding 감소 및 max-height 정확한 calc 계산 */
.auth-root { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--bg-base); padding: 72px 24px 24px; font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; position: relative; }
.auth-card { width: 100%; max-width: 520px; max-height: calc(100vh - 100px); overflow-y: auto; background: var(--bg-surface); border: 2px solid var(--text-primary); border-radius: 0; padding: 40px; box-shadow: 12px 12px 0 #6b7280; animation: fadeUp 0.3s ease both; }
@keyframes fadeUp { from { opacity:0; transform:translateY(10px); } to { opacity:1; transform:translateY(0); } }

.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

/* 비율 보정: 스텝퍼 상단 여백 최소화 */
.global-stepper-wrap { position: fixed; top: 16px; left: 50%; transform: translateX(-50%); width: 100%; max-width: 640px; padding: 0 24px; z-index: 100; }
.brutal-stepper { display: flex; gap: 8px; width: 100%; }
.brutal-stepper .step { flex: 1; text-align: center; padding: 12px 4px; border: 2px solid var(--border); background: var(--bg-surface); color: var(--text-muted); font-size: 13px; font-weight: 900; font-family: 'Escoredream', sans-serif; transition: all 0.2s; white-space: nowrap; }
.brutal-stepper .step.active { border-color: var(--text-primary); background: var(--text-primary); color: var(--bg-base); box-shadow: 4px 4px 0 #6b7280; transform: translate(-2px, -2px); }
.brutal-stepper .step.done { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-surface); }
@media (max-width: 640px) { .brutal-stepper .step { font-size: 11px; padding: 8px 2px; } }

.auth-header { text-align: center; margin-bottom: 32px; }
.auth-icon { width: 56px; height: 56px; border: 2px solid var(--text-primary); display: flex; align-items: center; justify-content: center; font-size: 24px; margin: 0 auto 16px; background: var(--text-primary); color: var(--bg-base); box-shadow: 4px 4px 0 #6b7280; }
.auth-header h2 { font-size: 24px; font-weight: 900; letter-spacing: 0.05em; margin-bottom: 8px; color: var(--text-primary); }
.auth-header p { font-size: 13px; font-weight: 700; color: var(--text-muted); line-height: 1.5; }

.auth-section { margin-bottom: 24px; }
.section-label { font-size: 12px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary); margin-bottom: 12px; border-bottom: 2px solid var(--text-primary); padding-bottom: 4px; display: inline-block; }

.link-btn { width: 100%; display: flex; align-items: center; justify-content: space-between; border: 2px solid var(--text-primary); padding: 14px 16px; background: var(--bg-base); cursor: pointer; margin-bottom: 12px; color: var(--text-primary); font-weight: 800; font-size: 14px; transition: all 0.1s; box-shadow: 4px 4px 0 #6b7280; }
.link-btn:hover:not(.is-connected) { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; background: var(--text-primary); color: var(--bg-base); }
.link-btn:hover:not(.is-connected) .link-icon { border-color: var(--bg-base); color: var(--bg-base); }
.link-btn.is-connected { background: var(--bg-surface); border-color: #2563eb; color: #2563eb; box-shadow: 4px 4px 0 #2563eb; cursor: default; }
.link-btn.is-connected .link-icon { border-color: #2563eb; color: #2563eb; }

.link-btn-left { display: flex; align-items: center; gap: 12px; }
.link-icon { width: 32px; height: 32px; border: 2px solid var(--text-primary); display: flex; align-items: center; justify-content: center; font-size: 16px; transition: all 0.1s; }

.btn-primary { width: 100%; padding: 16px; border: 2px solid var(--text-primary); background: var(--text-primary); color: var(--bg-base); font-weight: 900; font-size: 16px; letter-spacing: 0.05em; margin-top: 12px; cursor: pointer; transition: all 0.1s; font-family: 'Space Grotesk', 'Escoredream', sans-serif; }
.btn-primary:disabled { background: var(--bg-base); border-color: var(--border); color: var(--text-muted); cursor: not-allowed; box-shadow: none; }
.btn-primary:not(:disabled):hover { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; background: transparent; color: var(--text-primary); }
</style>