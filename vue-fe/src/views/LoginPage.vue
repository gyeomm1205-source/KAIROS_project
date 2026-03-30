<template>
  <div class="login-root">
    <div class="login-card">
      <div class="login-logo">
        <svg width="32" height="32" viewBox="0 0 28 28" fill="none">
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" fill="currentColor" opacity="0.15"/>
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linejoin="round"/>
          <circle cx="14" cy="14" r="2" fill="currentColor"/>
          <path d="M8 5h12l-4 4H12L8 5z" fill="currentColor" opacity="0.5"/>
        </svg>
        <span class="logo-text">KAIROS</span>
      </div>

      <h2 class="login-title">WELCOME BACK.</h2>
      <p class="login-sub">구글 계정으로 간편하게 시작하세요.</p>

      <button class="btn-google" @click="handleGoogleLoginClick">
        <div class="google-icon">G</div>
        CONTINUE WITH GOOGLE
      </button>

    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/useThemeStore'
import { useAuthStore } from '@/stores/useAuthStore'
import { loginWithGoogle } from '@/api/authApi'

const router = useRouter()
const route = useRoute()
const themeStore = useThemeStore()
const authStore = useAuthStore()

async function processGoogleLogin(code) {
  try {
    const response = await loginWithGoogle(code)
    const data = response.data

    if (data.is_new_user) {
      authStore.setOnboardingSession({
        onboardingToken: data.onboarding_token,
        email: data.email,
        profileImageUrl: data.profile_image_url
      })
      router.replace('/onboarding/connect')
    } else {
      authStore.setTokens({
        accessToken: data.access_token,
        userId: data.user_id
      })
      await authStore.fetchMe()
      router.replace('/calendar')
    }
  } catch (error) {
    console.error('구글 로그인 실패:', error)
    alert('구글 로그인에 실패했습니다. 다시 시도해 주세요.')
  }
}

function handleGoogleLoginClick() {
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID
  const redirectUri = window.location.origin + '/google/redirect'

  console.log('--- Google OAuth Debugging ---')
  console.log('환경 변수(import.meta.env.VITE_GOOGLE_CLIENT_ID):', clientId)
  console.log('사용할 Client ID:', clientId || '없음(초기화 필요)')
  console.log('사용할 Redirect URI:', redirectUri)

  if (!clientId || clientId === 'undefined') {
    console.error('🚨 Error: VITE_GOOGLE_CLIENT_ID가 설정되지 않았습니다. .env 파일을 확인해 주세요.')
    alert('구글 클라이언트 ID가 설정되지 않았습니다. 관리자에게 문의하시거나 .env 설정을 확인해 주세요.')
    return
  }

  const url = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${clientId}&redirect_uri=${redirectUri}&response_type=code&scope=openid profile email https://www.googleapis.com/auth/calendar`
  window.location.href = url
}

onMounted(() => {
  if (route.query.code) {
    processGoogleLogin(route.query.code)
  }
})
</script>

<style scoped>
.login-root {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-base);
  font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: transparent;
  border: 1px solid var(--border);
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  gap: 0;
  animation: fadeUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

.login-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 32px;
  color: var(--text-primary);
}
.logo-text {
  font-weight: 800;
  font-size: 20px;
  letter-spacing: 0.16em;
  color: var(--text-primary);
}

.login-title {
  font-size: 24px;
  font-weight: 900;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: 0.05em;
}
.login-sub {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 32px;
}

.btn-google {
  width: 100%;
  padding: 14px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.05em;
  cursor: pointer;
  font-family: inherit;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.btn-google:hover { 
  background: var(--bg-hover); 
  border-color: var(--text-primary);
}
.google-icon {
  width: 20px;
  height: 20px;
  border: 1px solid var(--border);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
  transition: border-color 0.3s;
}
.btn-google:hover .google-icon {
  border-color: var(--text-primary);
}
.terminal-input::placeholder { color: var(--k-text-muted); font-family: 'Mulmaru', sans-serif; font-size: 13px; }
.terminal-input:focus { outline: none; border-color: var(--k-acc-1-bg); background: var(--k-key-bg); color: var(--k-text); box-shadow: inset 0 2px 4px rgba(0,0,0,0.2), 0 0 10px rgba(209, 154, 102, 0.2); }


</style>