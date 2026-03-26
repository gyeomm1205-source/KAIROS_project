import { springApi } from '@/services/api'

/**
 * 3. Onboarding API
 * Base: /api/v1/onboarding
 */

/**
 * [GET] /onboarding/meta
 * 설문조사 선택지 조회 (기술 스택 · 포지션 목록)
 * 인증: Bearer {accessToken} 필요
 * @returns {Promise<{techStacks: TechStack[], devPositions: DevPosition[]}>}
 *
 * TechStack: { techStackId: number, techName: string, iconUrl: string, color: string }
 * DevPosition: { devPositionId: number, positionName: string }
 */
export function getOnboardingMeta() {
  return springApi.get('/api/v1/onboarding/meta')
}

/**
 * [POST] /onboarding/survey
 * 사전 설문조사 제출
 * 인증: Bearer {accessToken} 필요 (status = GUEST)
 *
 * @param {object} payload
 * @param {'STUDENT'|'JOB_SEEKER'|'JUNIOR'|'SENIOR'|'OTHER'} payload.position - 현재 직업
 * @param {number[]} payload.desiredPositionIds - 희망 포지션 ID 배열
 * @param {number[]} payload.techStackIds - 기술 스택 ID 배열
 * @param {('THEORY'|'PRACTICE'|'EMPLOYMENT')[]} payload.curriculumCategories - 커리큘럼 카테고리 (최소 1개)
 * @param {string} payload.githubTaskId - GitHub 수집 작업 ID
 * @param {string} [payload.velogTaskId] - Velog 수집 작업 ID (선택)
 *
 * @returns {Promise<{userId: number, status: 'SURVEYED', considerPersonalSchedule: boolean}>}
 */
export function submitSurvey(payload) {
  return springApi.post('/api/v1/onboarding/survey', payload)
}
