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

/**
 * 커리큘럼 미리보기 생성 (Spring Boot → FastAPI 경유)
 * POST /api/v1/curricula/preview
 */
export const postCurriculumPreview = (data) => {
  return springApi.post('/api/v1/curricula/preview', data, { timeout: 120000 })
}

/**
 * 커리큘럼 추천 근거 조회 (Spring Boot)
 * GET /api/v1/curricula/{curriculumId}/reason
 */
export const getCurriculumReason = (curriculumId) => {
  return springApi.get(`/api/v1/curricula/${curriculumId}/reason`)
}

/**
 * 커리큘럼 노드 상세 조회 (Spring Boot)
 * GET /api/v1/curricula/nodes/{nodeId}
 */
export const getCurriculumNode = (nodeId) => {
  return springApi.get(`/api/v1/curricula/nodes/${nodeId}`)
}

/**
 * 커리큘럼 노드 수정 (Spring Boot)
 * PATCH /api/v1/curricula/nodes/{nodeId}
 */
export const patchCurriculumNode = (nodeId, data) => {
  return springApi.patch(`/api/v1/curricula/nodes/${nodeId}`, data)
}

/**
 * 사용자 프로필 조회 (Spring Boot)
 * GET /api/v1/users/me
 */
export const getUserProfile = () => {
  return springApi.get('/api/v1/users/me')
}

/**
 * 사용자 프로필 수정 (Spring Boot)
 * PATCH /api/v1/users/me/profile
 */
export const updateUserProfile = (data) => {
  return springApi.patch('/api/v1/users/me/profile', data)
}

/**
 * 다크모드 설정 변경 (Spring Boot)
 * PATCH /api/v1/users/me/settings/dark-mode
 */
export const updateDarkModeSetting = (data) => {
  return springApi.patch('/api/v1/users/me/settings/dark-mode', data)
}

/**
 * 회원 탈퇴 (Spring Boot)
 * DELETE /api/v1/users/me
 */
export const deleteUser = () => {
  return springApi.delete('/api/v1/users/me')
}

/**
 * 추천 커리큘럼 목록 조회 (Spring Boot)
 * GET /api/v1/recommendations
 */
export const getRecommendations = () => {
  return springApi.get('/api/v1/recommendations')
}

/**
 * 추천 상세 조회 (Spring Boot)
 * GET /api/v1/recommendations/{curriculumId}
 */
export const getRecommendationDetail = (curriculumId) => {
  return springApi.get(`/api/v1/recommendations/${curriculumId}`)
}
