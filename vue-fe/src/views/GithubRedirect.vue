<template>
  <div class="redirect-root">
    <div class="redirect-card">
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
        <p>GitHub 연동 처리 중...</p>
      </div>

      <div v-else class="redirect-error">
        <i class="fas fa-exclamation-triangle" />
        <p>{{ errorMsg }}</p>
        <button class="btn-back" @click="$router.push('/onboarding/connect')">
          돌아가기
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { linkGithub } from '@/api/authApi'
import { useAuthStore } from '@/stores/useAuthStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const errorMsg = ref('')

onMounted(async () => {
  // GitHub OAuth 흐름:
  // OnboardingConnect → GitHub OAuth URL (state=onboardingToken)
  // → GitHub → GET /github/redirect?code=xxx&state=onboardingToken
  // → 프론트가 GET /api/v1/auth/link-github?code=xxx&state=onboardingToken 호출
  //   (백엔드가 @GetMapping("/link-github")로 code, state를 query param으로 받음)

  const code = route.query.code
  const state = route.query.state   // = onboardingToken

  if (!code) {
    errorMsg.value = 'GitHub 인증이 취소되었습니다.'
    return
  }

  // state가 없으면 onboardingToken 없이 진행 불가 → store에서 꺼내서 fallback
  const onboardingToken = state || authStore.onboardingToken

  if (!onboardingToken) {
    errorMsg.value = '온보딩 세션 정보가 없습니다. 다시 로그인해 주세요.'
    setTimeout(() => router.push('/login'), 2000)
    return
  }

  try {
    const { data } = await linkGithub(code, onboardingToken)
    console.log('[GithubRedirect] 응답:', data)

    // completeOnboarding: onboardingToken 삭제 + 정식 AT/userId 저장
    authStore.completeOnboarding({
      accessToken: data.accessToken,
      userId: data.userId,
    })

    // githubTaskId는 백엔드 현재 응답에 없음 → 있으면 저장, 없으면 skip
    if (data.githubTaskId) {
      authStore.setGithubTaskId(data.githubTaskId)
    }

    // 프로필 로드 (실패해도 진행)
    await authStore.fetchMe().catch(err => {
      console.warn('[GithubRedirect] fetchMe 실패 (무시):', err.message)
    })

    router.replace('/onboarding/connect')
  } catch (err) {
    console.error('[GithubRedirect] GitHub 연동 실패')
    console.error('  status:', err.response?.status)
    console.error('  data:', err.response?.data)
    console.error('  message:', err.message)

    const errCode = err.response?.data?.code
    if (!err.response) {
      errorMsg.value = `백엔드 서버 연결 실패 (${err.message})`
    } else if (errCode === 'INVALID_TOKEN' || err.response?.status === 401) {
      errorMsg.value = '온보딩 토큰이 만료되었습니다. (30분) 다시 로그인해 주세요.'
      setTimeout(() => router.push('/login'), 2500)
    } else if (errCode === 'DUPLICATE_USER' || err.response?.status === 409) {
      errorMsg.value = '이미 가입된 GitHub 계정입니다. 로그인 페이지로 이동합니다.'
      setTimeout(() => router.push('/login'), 2500)
    } else {
      errorMsg.value = `GitHub 연동 중 오류가 발생했습니다. (${err.response?.status}: ${errCode ?? err.message})`
    }
  }
})
</script>

<style scoped>
.redirect-root {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg-base); font-family: 'Space Grotesk', system-ui, sans-serif;
}
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
.btn-back {
  margin-top: 8px; padding: 10px 20px; border: 1px solid var(--border);
  background: transparent; color: var(--text-primary); font-size: 13px;
  font-weight: 700; cursor: pointer; font-family: inherit; transition: all 0.2s;
}
.btn-back:hover { border-color: var(--text-primary); background: var(--bg-hover); }
</style>
