import { fastApi } from '@/services/api'

/**
 * 비동기 작업 진행 상태 확인
 * GET /api/v1/ai/status/stream/{taskId}
 */
export const getTaskStatus = (taskId) => {
  return fastApi.get(`/api/v1/ai/status/stream/${taskId}`)
}

/**
 * 성장 일지 요약문 생성
 * POST /api/v1/ai/growth/summary
 */
export const postGrowthSummary = (data) => {
  return fastApi.post('/api/v1/ai/growth/summary', data, { timeout: 30000 })
}

/**
 * 재분석 요청 (피드백 반영)
 * POST /api/v1/ai/profile/feedback
 */
export const postProfileFeedback = (data) => {
  return fastApi.post('/api/v1/ai/profile/feedback', data, { timeout: 60000 })
}

/**
 * 커리큘럼 생성
 * POST /api/v1/ai/curriculum/generate
 */
export const postCurriculumGenerate = (data) => {
  return fastApi.post('/api/v1/ai/curriculum/generate', data, { timeout: 120000 })
}
