import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { setAuthToken, clearAuthToken, getAuthToken } from '@/services/authToken'
import { logout as apiLogout } from '@/api/authApi'
import { getMe } from '@/api/usersApi'

const ONBOARDING_TOKEN_KEY = 'kairos.onboarding_token'
const USER_ID_KEY = 'kairos.user_id'
const GITHUB_TASK_ID_KEY = 'kairos.github_task_id'
const VELOG_TASK_ID_KEY = 'kairos.velog_task_id'

export const useAuthStore = defineStore('auth', () => {
  // ── State ──────────────────────────────────────────────
  const accessToken = ref(getAuthToken() || '')
  const userId = ref(Number(localStorage.getItem(USER_ID_KEY)) || null)

  /** 온보딩 임시 토큰 */
  const onboardingToken = ref(localStorage.getItem(ONBOARDING_TOKEN_KEY) || '')

  /** 온보딩 중 Google 로그인 응답에서 받은 임시 유저 정보 */
  const pendingEmail = ref('')
  const pendingProfileImageUrl = ref('')

  /** 온보딩 중 수집 작업 ID */
  const githubTaskId = ref(sessionStorage.getItem(GITHUB_TASK_ID_KEY) || '')
  const velogTaskId = ref(sessionStorage.getItem(VELOG_TASK_ID_KEY) || '')

  /**
   * 로그인 완료 후 GET /users/me 로 받아오는 실제 유저 프로필
   * 앱 전체에서 이 값을 참조한다 (사이드바, 마이페이지 등)
   */
  const profile = ref(null)
  // profile 구조:
  // {
  //   userId, email, nickname, profileImageUrl,
  //   position, considerPersonalSchedule, darkModeEnabled,
  //   desiredPositions: [{devPositionId, positionName}],
  //   techStacks: [{techStackId, techName, iconUrl, color}],
  //   curriculumCategories: [ENUM]
  // }

  // ── Getters ────────────────────────────────────────────
  const isAuthenticated = computed(() => !!accessToken.value)
  const hasOnboardingToken = computed(() => !!onboardingToken.value)

  /** 사이드바/헤더에서 쓸 유저 표시 이름 */
  const displayName = computed(() => {
    if (profile.value?.nickname) return profile.value.nickname
    if (profile.value?.email) return profile.value.email.split('@')[0]
    if (pendingEmail.value) return pendingEmail.value.split('@')[0]
    return ''
  })
  const displayEmail = computed(() => profile.value?.email || '')
  const displayProfileImage = computed(() => profile.value?.profileImageUrl || '')

  // ── Actions ────────────────────────────────────────────

  /**
   * 로그인/토큰 갱신 후 GET /users/me 호출해서 프로필 저장
   * 실패해도 로그인 흐름은 계속 진행 (백엔드 미구현 시에도 안전)
   */
  async function fetchMe() {
    if (!accessToken.value) return
    try {
      const { data } = await getMe()
      profile.value = data
    } catch (err) {
      // 백엔드 미구현(404) 또는 일시 오류 → 프로필 없이 계속 진행
      console.warn('[useAuthStore] fetchMe 실패 (무시):', err.response?.status, err.message)
    }
  }

  /** 기존 유저 로그인 처리 (AT/RT 발급 완료) */
  function setTokens({ accessToken: at, userId: uid } = {}) {
    accessToken.value = at || ''
    setAuthToken(at || '')
    if (uid != null) {
      userId.value = uid
      localStorage.setItem(USER_ID_KEY, uid)
    }
  }

  /** 신규 유저 Google 로그인 처리 — onboardingToken 저장 */
  function setOnboardingSession({ onboardingToken: ot, email, profileImageUrl } = {}) {
    onboardingToken.value = ot || ''
    pendingEmail.value = email || ''
    pendingProfileImageUrl.value = profileImageUrl || ''
    if (ot) {
      localStorage.setItem(ONBOARDING_TOKEN_KEY, ot)
    } else {
      localStorage.removeItem(ONBOARDING_TOKEN_KEY)
    }
  }

  function setGithubTaskId(taskId) {
    githubTaskId.value = taskId || ''
    if (taskId) sessionStorage.setItem(GITHUB_TASK_ID_KEY, taskId)
    else sessionStorage.removeItem(GITHUB_TASK_ID_KEY)
  }

  function setVelogTaskId(taskId) {
    velogTaskId.value = taskId || ''
    if (taskId) sessionStorage.setItem(VELOG_TASK_ID_KEY, taskId)
    else sessionStorage.removeItem(VELOG_TASK_ID_KEY)
  }

  /** GitHub 연동 완료 후 정식 AT 발급 */
  function completeOnboarding({ accessToken: at, userId: uid } = {}) {
    onboardingToken.value = ''
    localStorage.removeItem(ONBOARDING_TOKEN_KEY)
    pendingEmail.value = ''
    pendingProfileImageUrl.value = ''
    setTokens({ accessToken: at, userId: uid })
  }

  /** Access Token 갱신 */
  function refreshAccessToken(newAt) {
    accessToken.value = newAt || ''
    setAuthToken(newAt || '')
  }

  /** 로그아웃 */
  async function signOut() {
    try {
      await apiLogout()
    } catch {
      // 서버 오류여도 클라이언트 상태 초기화
    } finally {
      accessToken.value = ''
      userId.value = null
      onboardingToken.value = ''
      githubTaskId.value = ''
      velogTaskId.value = ''
      pendingEmail.value = ''
      pendingProfileImageUrl.value = ''
      profile.value = null
      clearAuthToken()
      localStorage.removeItem(USER_ID_KEY)
      localStorage.removeItem(ONBOARDING_TOKEN_KEY)
      sessionStorage.removeItem(GITHUB_TASK_ID_KEY)
      sessionStorage.removeItem(VELOG_TASK_ID_KEY)
    }
  }

  return {
    accessToken,
    userId,
    onboardingToken,
    githubTaskId,
    velogTaskId,
    pendingEmail,
    pendingProfileImageUrl,
    profile,
    displayName,
    displayEmail,
    displayProfileImage,
    isAuthenticated,
    hasOnboardingToken,
    fetchMe,
    setTokens,
    setOnboardingSession,
    setGithubTaskId,
    setVelogTaskId,
    completeOnboarding,
    refreshAccessToken,
    signOut,
  }
})
