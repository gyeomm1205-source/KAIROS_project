import { fastApi, springApi } from '@/services/api'

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

/**
 * 커리큘럼 확정 저장 (Spring Boot)
 * POST /api/v1/curricula/confirm
 */
export const postCurriculaConfirm = (data) => {
  return springApi.post('/api/v1/curricula/confirm', data)
}

/**
 * 성장 통계 조회 (Spring Boot)
 * GET /api/v1/activities/growth-report
 */
export const getGrowthReport = () => {
  return springApi.get('/api/v1/activities/growth-report')
}

/**
 * 퀴즈 세션 시작 (Spring Boot)
 * POST /api/v1/quizzes/sessions
 */
export const postQuizSessionStart = (data) => {
  return springApi.post('/api/v1/quizzes/sessions', data)
}

/**
 * 퀴즈 답안 제출 (Spring Boot)
 * POST /api/v1/quizzes/sessions/{sessionId}/answers
 */
export const postQuizAnswer = (sessionId, data) => {
  return springApi.post(`/api/v1/quizzes/sessions/${sessionId}/answers`, data)
}

/**
 * 퀴즈 세션 완료 (Spring Boot)
 * POST /api/v1/quizzes/sessions/{sessionId}/complete
 */
export const postQuizComplete = (sessionId) => {
  return springApi.post(`/api/v1/quizzes/sessions/${sessionId}/complete`)
}
