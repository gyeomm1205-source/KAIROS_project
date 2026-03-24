// src/api/calendarApi.js (기존 파일 하단에 추가)

// --- [신규 추가] AI 분석 및 커리큘럼 추천 API ---

// 1. 사용자 학습 분석 결과 가져오기 (AnalysisResultPage 용)
export const getAnalysisResult = async (userId) => {
  return await api.get(`/analysis/${userId}`)
}

// 2. 분석 결과 피드백 전송 (모달창 "피드백 제출" 버튼 용)
export const submitAnalysisFeedback = async (feedbackData) => {
  return await api.post('/analysis/feedback', feedbackData)
}

// 3. 대체 커리큘럼 제안 가져오기 (AlternativeCurriculumPage 용)
export const getAlternativeCurriculum = async (params) => {
  return await api.get('/curriculum/alternative', { params })
}

// 4. 새로운 일정 AI 자동 생성 및 Reasoning(추천 근거) 받아오기
export const generateAiSchedule = async (promptData) => {
  return await api.post('/ai/generate-schedule', promptData)
}

export default {
  // 기존 메서드들...
  getAnalysisResult,
  submitAnalysisFeedback,
  getAlternativeCurriculum,
  generateAiSchedule
}