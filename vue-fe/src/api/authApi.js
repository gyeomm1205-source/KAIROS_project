import { springApi } from '@/services/api'

/**
 * 1. Auth API
 * Base: /api/v1/auth
 */

/**
 * @typedef {Object} GoogleLoginRequest
 * @property {string} code - Google OAuth2 authorization code
 */

/**
 * @typedef {Object} GoogleLoginResponse
 * @property {boolean} isNewUser - 신규 유저 여부
 * @property {string} [onboardingToken] - (신규 유저) 30분 만료 임시 토큰
 * @property {string} [email] - (신규 유저) 이메일
 * @property {string} [profileImageUrl] - (신규 유저) 프로필 이미지 URL
 * @property {string} [accessToken] - (기존 유저) 엑세스 토큰
 * @property {string} [tokenType] - (기존 유저) 토큰 타입
 */

/**
 * [GET] /auth/login/google
 * Google 로그인 처리 (신규/기존 분기)
 * GET 요청의 Request Body(JSON)에 code 전달
 *
 * @param {string} code - Google OAuth2 authorization code
 * @returns {Promise<{data: GoogleLoginResponse}>}
 */
export function loginWithGoogle(code) {
  return springApi.post('/api/v1/auth/login/google', { code })
}

/**
 * [GET] /auth/link-github
 * GitHub 계정 연동 — code와 state(=onboardingToken)를 query param으로 전달
 *
 * 백엔드가 GitHub으로부터 redirect된 code + state를 query param으로 받음
 *
 * @param {string} code - GitHub OAuth2 authorization code
 * @param {string} state - onboardingToken (GitHub OAuth 요청 시 state로 전달했던 값)
 * @returns {Promise<{accessToken: string, tokenType: string, userId: number}>}
 */
export function linkGithub(code, onboardingToken) {
  return springApi.post('/api/v1/auth/link-github', 
    { code }, 
    { headers: { Authorization: `Bearer ${onboardingToken}` } }
  )
}

/**
 * [POST] /auth/link-velog
 * Velog username을 받아 FastAPI Velog 수집을 트리거하고 velogTaskId 반환
 *
 * @param {string} velogUsername - Velog 사용자명
 * @returns {Promise<{velogTaskId: string}>}
 */
export function linkVelog(velogUsername) {
  return springApi.post('/api/v1/auth/link-velog', { velogUsername })
}

/**
 * [POST] /auth/reissue
 * Access Token 재발급 (Refresh Token은 HttpOnly Cookie로 자동 전송)
 * @returns {Promise<{accessToken: string, tokenType: string}>}
 */
export function reissueToken() {
  return springApi.post('/api/v1/auth/reissue', null, {
    withCredentials: true,
  })
}

/**
 * [POST] /auth/logout
 * 로그아웃 (AT Blacklist 등록, RT Cookie 만료)
 * @returns {Promise<void>} 204 No Content
 */
export function logout() {
  return springApi.post('/api/v1/auth/logout', null, {
    withCredentials: true,
  })
}
