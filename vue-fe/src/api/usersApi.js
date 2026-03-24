import { springApi } from '@/services/api'

/**
 * Users API
 * Base: /api/v1/users
 */

/**
 * [GET] /users/me
 * 내 프로필 조회
 *
 * @returns {Promise<{userId, email, nickname, profileImageUrl, position, considerPersonalSchedule, darkModeEnabled, desiredPositions, techStacks, curriculumCategories}>}
 */
export function getMe() {
  return springApi.get('/api/v1/users/me')
}

/**
 * [PATCH] /users/me/profile
 * 프로필 수정 (전송한 항목만 업데이트)
 *
 * @param {{ position?, desiredPositionIds?, techStackIds?, curriculumCategories?, considerPersonalSchedule? }} payload
 */
export function updateProfile(payload) {
  return springApi.patch('/api/v1/users/me/profile', payload)
}

/**
 * [GET] /users/me/external-accounts
 * 외부 계정 연동 현황 조회 (GitHub, Velog)
 *
 * @returns {Promise<{ github: {...}, velog: {...} }>}
 */
export function getExternalAccounts() {
  return springApi.get('/api/v1/users/me/external-accounts')
}

/**
 * [PATCH] /users/me/settings/dark-mode
 * 다크모드 토글
 *
 * @param {boolean} darkModeEnabled
 */
export function updateDarkMode(darkModeEnabled) {
  return springApi.patch('/api/v1/users/me/settings/dark-mode', { darkModeEnabled })
}

/**
 * [DELETE] /users/me
 * 회원 탈퇴
 */
export function deleteMe() {
  return springApi.delete('/api/v1/users/me')
}
