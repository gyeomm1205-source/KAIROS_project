<template>
  <div class="auth-root">
    
    <div class="global-stepper-wrap">
      <div class="page-stepper">
        <div class="step active">1. 계정 연동</div>
        <div class="step">2. 사전 설문</div>
        <div class="step">3. 데이터 분석</div>
        <div class="step">4. 결과 확인</div>
      </div>
    </div>

    <div class="auth-card custom-scroll">
      <div class="header-top">
        <button class="btn-back" @click="$router.push('/')">
          <i class="fas fa-arrow-left" /> BACK
        </button>
      </div>
      <div class="auth-header">
        <div class="auth-icon"><i class="fas fa-plug" /></div>
        <h2>ACCOUNT LINKING</h2>
        <p>서비스 이용을 위해 계정을 연동해 주세요.<br>(모든 항목 연동 필수)</p>
      </div>

      <div class="auth-section">
        <button class="link-btn is-connected" disabled>
          <div class="link-btn-left">
            <div class="link-icon"><i class="fab fa-google" /></div>
            <span class="text-blue-600">{{ userEmail }} 연동됨</span>
          </div>
          <i class="fas fa-check" />
        </button>
      </div>

      <div class="auth-section">
        <div class="section-label">DATA SYNCHRONIZATION</div>
        
        <button 
          class="link-btn"
          :class="{ 'is-connected': isGithubConnected }"
          :disabled="isGithubConnected"
          @click="handleGithubConnect"
        >
          <div class="link-btn-left">
            <div class="link-icon"><i class="fab fa-github" /></div>
            <span v-if="!isGithubConnected">GitHub 연동하기</span>
            <span v-else class="text-blue-600">{{ githubNickname }} 연동됨</span>
          </div>
          <i v-if="!isGithubConnected" class="fas fa-arrow-right" />
          <i v-else class="fas fa-check" />
        </button>

        <button 
          class="link-btn"
          :class="{ 'is-connected': isVelogConnected }"
          :disabled="!isGithubConnected"
          @click="handleVelogConnect"
        >
          <div class="link-btn-left">
            <div class="link-icon"><i class="fas fa-v" /></div>
            <span v-if="!isVelogConnected">Velog 연동하기</span>
            <span v-else class="text-blue-600">{{ velogUsername }} 연동됨</span>
          </div>
          <i v-if="!isVelogConnected" class="fas fa-arrow-right" />
          <i v-else class="fas fa-check" />
        </button>
      </div>

      <button 
        class="btn-primary" 
        :disabled="!canProceed"
        @click="$router.push('/onboarding/survey')"
      >
        NEXT STEP
      </button>

      <!-- ===== DEV TEST ONLY — 배포 전 반드시 제거 ===== -->
      <div class="dev-test-box">
        <div class="dev-test-label">🛠 DEV TEST — 파이프라인 전체 테스트</div>
        <div class="dev-test-note">
          ⚠ 먼저 DB에 userId=1 유저를 INSERT 하세요!<br>
          Host: 127.0.0.1 / DB: kairos_db / PW: kairosdb!!
        </div>
        <div v-if="devStatus" class="dev-status">{{ devStatus }}</div>
        <button class="dev-btn" :disabled="devLoading" @click="runDevTest">
          {{ devLoading ? '⏳ 실행 중...' : '▶ 전체 파이프라인 실행 (userId=1)' }}
        </button>
      </div>
      <!-- ===== DEV TEST END ===== -->
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { linkVelog } from '@/api/authApi'

const router = useRouter()
const authStore = useAuthStore()

// Velog 연동 상태 (Velog는 AT 획득 후 가능하므로 isAuthenticated 체크)
const isVelogConnected = computed(() => !!authStore.profile?.velogUsername || !!velogUsername.value)
const velogUsername = ref('')

// Google 정보 (보통 pendingEmail이나 profile.email에 있음)
const userEmail = computed(() => authStore.profile?.email || authStore.pendingEmail || 'Google 계정')

// GitHub 연동 상태: accessToken을 획득했다면 연동이 완료된 것임
const isGithubConnected = computed(() => authStore.isAuthenticated)
const githubNickname = computed(() => authStore.profile?.nickname || 'GitHub 계정')

// 진행 가능 여부: 필수 연동 완료 시 활성화
const canProceed = computed(() => isGithubConnected.value)

function handleGithubConnect() {
  if (isGithubConnected.value) return

  const clientId = import.meta.env.VITE_GITHUB_CLIENT_ID
  const redirectUri = window.location.origin + '/github/redirect'
  
  if (!clientId || clientId === 'undefined') {
    alert('GitHub Client ID 연동 설정이 필요합니다.')
    return
  }

  // GitHub OAuth 시 state에 onboardingToken을 실어 보냄
  const state = authStore.onboardingToken
  const url = `https://github.com/login/oauth/authorize?client_id=${clientId}&redirect_uri=${redirectUri}&scope=read:user public_repo&state=${state}`
  
  window.location.href = url
}

async function handleVelogConnect() {
  if (isVelogConnected.value) return
  if (!authStore.isAuthenticated) {
    alert('먼저 GitHub 연동을 완료해 주세요.')
    return
  }

  const username = prompt('Velog 사용자명을 입력해주세요 (예: @username)')
  if (username) {
    try {
      const { data } = await linkVelog(username)
      velogUsername.value = username
      if (data.velogTaskId) {
        authStore.setVelogTaskId(data.velogTaskId)
      }
      // 프로필 동기화
      await authStore.fetchMe()
      alert('Velog 연동 및 데이터 분석이 시작되었습니다.')
    } catch (err) {
      console.error('Velog 연동 실패:', err)
      alert('Velog 연동 중 오류가 발생했습니다.')
    }
  }
}
</script>

<style scoped>
/* 비율 보정: top padding 감소 및 max-height 정확한 calc 계산 */
.auth-root { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--bg-base); padding: 72px 24px 24px; font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; position: relative; }
.auth-card { 
  width: 100%; max-width: 520px; max-height: calc(100vh - 100px); 
  overflow-y: auto; background: var(--bg-surface); 
  border: 1px solid var(--border); padding: 40px; 
  animation: fadeUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) both; 
}
@keyframes fadeUp { from { opacity:0; transform:translateY(20px); } to { opacity:1; transform:translateY(0); } }

.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

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
.page-stepper .step.active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }
.page-stepper .step.done { color: var(--text-primary); background: transparent; }
@media (max-width: 640px) { .page-stepper .step { font-size: 10px; padding: 8px 2px; } }

.header-top { margin-bottom: 24px; }
.btn-back {
  background: transparent; border: 1px solid var(--border);
  font-weight: 800; font-size: 11px; color: var(--text-muted);
  cursor: pointer; transition: all 0.2s; letter-spacing: 0.1em;
  padding: 8px 14px; display: inline-flex; align-items: center; gap: 8px;
}
.btn-back:hover { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-hover); }

.auth-header { text-align: center; margin-bottom: 32px; }
.auth-icon { width: 48px; height: 48px; border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 20px; margin: 0 auto 16px; background: transparent; color: var(--text-primary); }
.auth-header h2 { font-size: 24px; font-weight: 900; letter-spacing: 0.05em; margin-bottom: 8px; color: var(--text-primary); }
.auth-header p { font-size: 13px; font-weight: 700; color: var(--text-muted); line-height: 1.5; }

.auth-section { margin-bottom: 24px; }
.section-label { font-size: 12px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-muted); margin-bottom: 12px; border-bottom: 1px solid var(--border); padding-bottom: 4px; display: inline-block; }

.link-btn { width: 100%; display: flex; align-items: center; justify-content: space-between; border: 1px solid var(--border); padding: 14px 16px; background: transparent; cursor: pointer; margin-bottom: 12px; color: var(--text-primary); font-weight: 700; font-size: 14px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.link-btn:hover:not(.is-connected):not(:disabled) { background: var(--bg-hover); border-color: var(--text-primary); }
.link-btn:hover:not(.is-connected):not(:disabled) .link-icon { border-color: var(--text-primary); }
.link-btn.is-connected { background: transparent; border-color: rgba(255,255,255,0.2); color: var(--text-secondary); cursor: default; }
.link-btn.is-connected .link-icon { border-color: rgba(255,255,255,0.2); color: var(--text-secondary); }
.link-btn:disabled { opacity: 0.4; cursor: not-allowed; filter: grayscale(1); }

.link-btn-left { display: flex; align-items: center; gap: 12px; }
.link-icon { width: 32px; height: 32px; border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 16px; transition: all 0.3s; }

.btn-primary { width: 100%; padding: 16px; border: 1px solid var(--text-primary); background: var(--text-primary); color: var(--bg-base); font-weight: 800; font-size: 14px; letter-spacing: 0.1em; margin-top: 12px; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; }
.btn-primary:disabled { background: transparent; border-color: var(--border); color: var(--text-muted); cursor: not-allowed; }
.btn-primary:not(:disabled):hover { background: transparent; color: var(--text-primary); }

/* DEV TEST */
.dev-test-box { margin-top: 24px; border: 1px dashed #f59e0b; padding: 16px; background: rgba(245,158,11,0.05); }
.dev-test-label { font-size: 11px; font-weight: 900; letter-spacing: 0.1em; color: #f59e0b; margin-bottom: 8px; }
.dev-test-note { font-size: 11px; color: #92400e; margin-bottom: 12px; line-height: 1.6; }
.dev-status { font-size: 11px; color: #d97706; margin-bottom: 10px; white-space: pre-wrap; border: 1px solid #f59e0b22; padding: 8px; background: rgba(245,158,11,0.08); }
.dev-btn { width: 100%; padding: 10px; border: 1px solid #f59e0b; background: transparent; color: #f59e0b; font-weight: 800; font-size: 12px; letter-spacing: 0.08em; cursor: pointer; font-family: inherit; transition: all 0.2s; }
.dev-btn:hover:not(:disabled) { background: #f59e0b; color: #000; }
.dev-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>