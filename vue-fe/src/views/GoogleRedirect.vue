<template>
  <div class="redirect-root">
    <!-- 환영 모달 -->
    <Transition name="welcome">
      <div v-if="showWelcome" class="welcome-overlay">
        <div class="welcome-card">
          <div class="welcome-avatar">
            <img v-if="authStore.displayProfileImage" :src="authStore.displayProfileImage" :alt="welcomeName" class="avatar-img" />
            <i v-else class="fas fa-user-circle" />
          </div>
          <p class="welcome-sub">{{ isSignup ? 'WELCOME TO KAIROS' : 'WELCOME BACK' }}</p>
          <h2 class="welcome-name">{{ isSignup ? '안녕하세요, ' : '환영합니다, ' }}{{ welcomeName }}님</h2>
          <p class="welcome-hint">{{ isSignup ? 'GitHub 연동 페이지로 이동 중...' : '캘린더로 이동 중...' }}</p>
          <div class="welcome-progress">
            <div class="progress-fill" :style="{ width: progressWidth + '%' }" />
          </div>
        </div>
      </div>
    </Transition>

    <!-- 처리 중 / 에러 화면 -->
    <div class="redirect-card" v-if="!showWelcome">
      <div class="redirect-logo">
        <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" fill="currentColor" opacity="0.15"/>
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linejoin="round"/>
          <circle cx="14" cy="14" r="2" fill="currentColor"/>
        </svg>
        <span>KAIROS</span>
      </div>

      <div v-if="!errorMsg" class="redirect-status">
        <i class="fas fa-spinner fa-spin" />
        <p>Google 인증 처리 중...</p>
      </div>

      <div v-else class="redirect-error">
        <i class="fas fa-exclamation-triangle" />
        <p>{{ errorMsg }}</p>
        <button class="btn-back" @click="$router.push('/login')">
          로그인으로 돌아가기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { loginWithGoogle } from '@/api/authApi'
import { useAuthStore } from '@/stores/useAuthStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const errorMsg = ref('')
const showWelcome = ref(false)
const welcomeName = ref('')
const isSignup = ref(false)
const progressWidth = ref(0)

let progressTimer = null

function startProgress() {
  progressTimer = setInterval(() => {
    progressWidth.value = Math.min(progressWidth.value + 2, 100)
  }, 30)
}

onUnmounted(() => {
  if (progressTimer) clearInterval(progressTimer)
})

onMounted(async () => {
  const code = route.query.code

  if (!code) {
    errorMsg.value = 'Google 인증이 취소되었습니다.'
    return
  }

  try {
    const { data } = await loginWithGoogle(code)
    console.log('[GoogleRedirect] 응답:', data)

    if (data.is_new_user) {
      // 신규 유저 → 가입 안내 메시지 잠깐 보여주고 온보딩으로
      welcomeName.value =
        data.email ? data.email.split('@')[0] : '새 사용자'

      showWelcome.value = true
      isSignup.value = true
      startProgress()

      authStore.setOnboardingSession({
        onboardingToken: data.onboarding_token,
        email: data.email,
        profileImageUrl: data.profile_image_url,
      })

      setTimeout(() => {
        if (progressTimer) clearInterval(progressTimer)
        router.replace('/onboarding/connect')
      }, 1800)
    } else {
      // 기존 유저 → 환영 모달 → calendar
      // 백엔드 GoogleOAuthCallbackResponse는 @JsonProperty로 snake_case 반환
      // forExistingUser()는 userId를 포함하지 않으므로 JWT에서 파싱하거나 fetchMe 이후 획득
      authStore.setTokens({
        accessToken: data.access_token,
        userId: data.user_id ?? null,
      })
      await authStore.fetchMe()

      // 표시할 이름 결정 (프로필 nickname > email 앞부분)
      welcomeName.value =
        authStore.displayName ||
        (data.email ? data.email.split('@')[0] : '사용자')

      // 환영 모달 표시 + 프로그레스 바 시작
      showWelcome.value = true
      startProgress()

      // 1.8초 후 캘린더로 이동
      setTimeout(() => {
        if (progressTimer) clearInterval(progressTimer)
        router.replace('/calendar')
      }, 1800)
    }
  } catch (err) {
    console.error('[GoogleRedirect] 로그인 실패')
    console.error('  status:', err.response?.status)
    console.error('  data:', err.response?.data)
    console.error('  message:', err.message)
    console.error('  baseURL:', err.config?.baseURL)
    console.error('  url:', err.config?.url)

    if (!err.response) {
      errorMsg.value = `백엔드 서버 연결 실패 (${err.message}) — 서버가 실행 중인지 확인하세요.`
    } else {
      const errCode = err.response?.data?.code
      const status = err.response?.status
      if (errCode === 'INVALID_TOKEN' || status === 401) {
        errorMsg.value = '인증 코드가 만료되었습니다. 다시 시도해 주세요.'
      } else {
        errorMsg.value = `로그인 처리 중 오류가 발생했습니다. (${status}: ${errCode ?? err.message})`
      }
    }
  }
})
</script>

<style scoped>
.redirect-root {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg-base); font-family: 'Space Grotesk', system-ui, sans-serif;
}

/* ── 처리 중 / 에러 카드 ── */
.redirect-card {
  display: flex; flex-direction: column; align-items: center; gap: 24px;
  padding: 48px; border: 1px solid var(--border); min-width: 320px;
  animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}
.redirect-logo { display: flex; align-items: center; gap: 10px; color: var(--text-primary); font-weight: 800; font-size: 18px; letter-spacing: 0.12em; }
.redirect-status { display: flex; flex-direction: column; align-items: center; gap: 12px; color: var(--text-muted); }
.redirect-status i { font-size: 24px; }
.redirect-status p { font-size: 14px; font-weight: 600; }
.redirect-error { display: flex; flex-direction: column; align-items: center; gap: 12px; color: #ef4444; text-align: center; }
.redirect-error i { font-size: 24px; }
.redirect-error p { font-size: 14px; font-weight: 600; }
.btn-back { margin-top: 8px; padding: 10px 20px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-size: 13px; font-weight: 700; cursor: pointer; font-family: inherit; transition: all 0.2s; }
.btn-back:hover { border-color: var(--text-primary); background: var(--bg-hover); }

/* ── 환영 모달 ── */
.welcome-overlay {
  position: fixed; inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; backdrop-filter: blur(4px);
}

.welcome-card {
  display: flex; flex-direction: column; align-items: center; gap: 16px;
  padding: 56px 64px;
  border: 1px solid var(--border);
  background: var(--bg-surface);
  min-width: 360px; text-align: center;
  animation: popIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes popIn {
  from { opacity: 0; transform: scale(0.92) translateY(16px); }
  to   { opacity: 1; transform: scale(1) translateY(0); }
}

.welcome-avatar {
  width: 72px; height: 72px;
  border: 2px solid var(--text-primary);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; font-size: 32px; color: var(--text-muted);
  background: var(--bg-base);
  margin-bottom: 4px;
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; }

.welcome-sub {
  font-size: 11px; font-weight: 900; letter-spacing: 0.2em;
  color: var(--text-muted); margin: 0;
}

.welcome-name {
  font-size: 26px; font-weight: 900; color: var(--text-primary);
  margin: 0; letter-spacing: -0.01em;
}

.welcome-hint {
  font-size: 12px; font-weight: 700; color: var(--text-faint);
  margin: 0;
}

.welcome-progress {
  width: 100%; height: 2px;
  background: var(--border);
  margin-top: 8px;
  overflow: hidden;
}
.progress-fill {
  height: 100%; background: var(--text-primary);
  transition: width 0.03s linear;
}

/* ── 트랜지션 ── */
.welcome-enter-active { transition: opacity 0.3s ease; }
.welcome-leave-active { transition: opacity 0.2s ease; }
.welcome-enter-from, .welcome-leave-to { opacity: 0; }
</style>
