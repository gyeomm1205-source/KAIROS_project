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
 * 활동 기록 목록 조회 (Spring Boot)
 * GET /api/v1/activities
 */
export const getActivities = (params) => {
  return springApi.get('/api/v1/activities', { params })
}

/**
 * 활동 기록 제외/복원 (Spring Boot)
 * PATCH /api/v1/activities/{activityId}/inclusion
 */
export const patchActivityInclusion = (activityId, data) => {
  return springApi.patch(`/api/v1/activities/${activityId}/inclusion`, data)
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
 * POST /api/v1/quizzes/sessions/{curriculumId}/answers
 */
export const postQuizAnswer = (curriculumId, data) => {
  return springApi.post(`/api/v1/quizzes/sessions/${curriculumId}/answers`, data)
}

/**
 * 퀴즈 세션 완료 (Spring Boot)
 * POST /api/v1/quizzes/sessions/{curriculumId}/complete
 */
export const postQuizComplete = (curriculumId) => {
  return springApi.post(`/api/v1/quizzes/sessions/${curriculumId}/complete`)
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

/**
 * 개인 일정 생성 (Spring Boot)
 * POST /api/v1/schedules
 */
export const createSchedule = (data) => {
  return springApi.post('/api/v1/schedules', data)
}

/**
 * 개인 일정 수정 (Spring Boot)
 * PATCH /api/v1/schedules/{scheduleId}
 */
export const updateSchedule = (scheduleId, data) => {
  return springApi.patch(`/api/v1/schedules/${scheduleId}`, data)
}

/**
 * 개인 일정 삭제 (Spring Boot)
 * DELETE /api/v1/schedules/{scheduleId}
 */
export const deleteSchedule = (scheduleId) => {
  return springApi.delete(`/api/v1/schedules/${scheduleId}`)
}

/**
 * 캘린더 조회 (Spring Boot)
 * GET /api/v1/calendar?year=&month=
 */
export const getCalendar = (params) => {
  return springApi.get('/api/v1/calendar', { params })
}

/**
 * Google Calendar 내보내기 (Spring Boot)
 * POST /api/v1/calendar/sync/export
 */
export const exportCalendar = () => {
  return springApi.post('/api/v1/calendar/sync/export')
}

/**
 * Google Calendar 가져오기 (Spring Boot)
 * POST /api/v1/calendar/sync/import
 */
export const importCalendar = () => {
  return springApi.post('/api/v1/calendar/sync/import')
}

/**
 * GitHub/Velog 활동 동기화 (Spring Boot)
 * POST /api/v1/activities/sync/{provider}
 */
export const syncActivities = (provider) => {
  return springApi.post(`/api/v1/activities/sync/${provider}`)
}

// Auth/Onboarding API는 authApi.js, onboardingApi.js에서 관리
