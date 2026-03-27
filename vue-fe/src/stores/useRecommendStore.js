import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getRecommendations, getRecommendationDetail, getCurriculumReason, getCalendar, postQuizSessionStart, postQuizAnswer, postQuizComplete } from '@/api/aiApi'

export const useRecommendStore = defineStore('recommend', () => {
  const isLoading = ref(false)

  const formatMonthDay = (value) => {
    if (!value) return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return ''
    return `${date.getMonth() + 1}/${date.getDate()}`
  }

  const formatDateRange = (startDate, endDate) => {
    const start = formatMonthDay(startDate)
    const end = formatMonthDay(endDate)
    if (start && end) return `${start}~${end}`
    if (start) return start
    if (end) return end
    return ''
  }

  const recentActivities = ref([
    {
      id: "rsc",
      title: "React Server Components 심화 학습",
      type: "study",
      date: "3월 9일 ~ 진행 중",
      status: "in-progress",
      tags: ["React", "SSR", "Next.js"],
    },
    {
      id: "zustand",
      title: "Zustand 상태관리 실습",
      type: "dev",
      date: "3월 3일",
      status: "done",
      tags: ["React", "Zustand"],
    },
    {
      id: "blog-ssr",
      title: "SSR vs CSR 비교 블로그 작성",
      type: "blog",
      date: "3월 5일",
      status: "done",
      tags: ["SSR", "CSR", "블로그"],
    },
    {
      id: "quiz-state",
      title: "React 상태관리 복습 퀴즈",
      type: "review",
      date: "3월 7일",
      status: "done",
      tags: ["React", "복습"],
    },
    {
      id: "ts-generic",
      title: "TypeScript 제네릭 학습",
      type: "study",
      date: "3월 9일 ~ 진행 중",
      status: "in-progress",
      tags: ["TypeScript", "제네릭"],
    },
  ])

  const recommendationDetail = ref(null)
  const curriculumReason = ref(null)
  const selectedCurriculumNodes = ref([])

  const recentFlow = ref([
    { label: "React 상태관리 학습", status: "done" },
    { label: "Zustand 실습", status: "done" },
    { label: "블로그 작성", status: "done" },
    { label: "복습 퀴즈", status: "done" },
    { label: "TypeScript 제네릭", status: "in-progress" },
  ])

  const missions = ref([
    {
      key: "quiz",
      icon: "fas fa-brain",
      title: "복습 퀴즈",
      desc: "React Server Components와 렌더링 흐름 핵심 개념 점검",
      time: "15분",
      tag: "복습",
      badge: "추천",
    },
    {
      key: "velog",
      icon: "fas fa-pen-nib",
      title: "velog 글 작성하기",
      desc: "글을 바로 써주기보다, 글 작성에 필요한 레퍼런스를 추천합니다",
      time: "40분",
      tag: "정리",
      badge: "레퍼런스",
    },
  ])

  const references = ref([
    {
      title: "React 공식 문서 - Server Components",
      reason: "RSC의 핵심 개념과 제약 사항을 가장 정확하게 확인할 수 있습니다.",
      type: "공식 문서",
      freshness: "2025.12 업데이트",
    },
    {
      title: "Next.js App Router 실전 가이드",
      reason: "현재 진행 중인 Next.js 흐름과 바로 연결되는 실전 예제가 포함되어 있습니다.",
      type: "실전 가이드",
      freshness: "2026.01 작성",
    },
    {
      title: "RSC vs Client Components 비교 글",
      reason: "velog 글을 쓰실 때 비교 기준을 잡기 좋은 정리형 레퍼런스입니다.",
      type: "비교 아티클",
      freshness: "2026.02 작성",
    },
  ])

  const quizQuestions = ref([
    {
      question: "React Server Components의 가장 큰 목적은 무엇인가요?",
      options: [
        "모든 상태를 브라우저에서만 관리하기 위해",
        "서버에서 렌더링 가능한 부분을 분리해 클라이언트 번들을 줄이기 위해",
        "모든 컴포넌트를 CSR로 통일하기 위해",
        "라우팅만 서버에서 처리하기 위해",
      ],
      correct: 1,
    },
    {
      question: "Client Component가 필요한 대표 상황은 무엇인가요?",
      options: [
        "정적 텍스트만 보여줄 때",
        "사용자 인터랙션과 브라우저 이벤트 처리가 필요할 때",
        "서버 데이터만 읽어올 때",
        "메타데이터만 정의할 때",
      ],
      correct: 1,
    },
  ])

  const loadRecommendations = async () => {
    isLoading.value = true
    try {
      const { data } = await getRecommendations()
      if (data.items && data.items.length > 0) {
        recentActivities.value = data.items.map(item => ({
          id: item.curriculumId,
          title: item.displayName || item.techStacks?.map(t => t.techName).join(', ') || '커리큘럼',
          displayName: item.displayName || '',
          startDate: item.startDate || null,
          endDate: item.endDate || null,
          type: 'study',
          date: formatDateRange(item.startDate, item.endDate),
          status: item.status === 'ACTIVE' ? 'in-progress' : 'done',
          tags: item.techStacks?.map(t => t.techName) || [],
          hasRecommendation: item.hasRecommendation
        }))
      }
    } catch (e) {
      console.error('recommendations 조회 실패 (Mock 유지):', e)
    } finally {
      isLoading.value = false
    }
  }

  const loadRecommendationDetail = async (curriculumId) => {
    isLoading.value = true
    try {
      const { data } = await getRecommendationDetail(curriculumId)
      recommendationDetail.value = data

      if (data.currentStatus?.topSkills?.length > 0) {
        recentFlow.value = data.currentStatus.topSkills.map((skill, index) => ({
          label: skill,
          status: index === 0 ? 'in-progress' : 'done',
        }))
      } else if (data.nextNodes?.length > 0) {
        recentFlow.value = data.nextNodes.map((node, index) => ({
          label: node.title,
          status: index === 0 ? 'in-progress' : 'done',
        }))
      }

      if (data.quizzes && data.quizzes.length > 0) {
        const quiz = data.quizzes[0]
        quizQuestions.value = (quiz.questions || []).map(q => ({
          question: q.question,
          options: q.options || [],
          correct: 0,
          quizType: q.quizType
        }))
      }

      if (data.references && data.references.length > 0) {
        references.value = data.references.map(r => ({
          title: r.title,
          reason: r.recommendationReason,
          type: r.referenceType,
          freshness: r.publishedAt || '',
          techStacks: r.techStacks || [],
          url: r.url || ''
        }))
      }

      missions.value = [
        {
          key: "quiz",
          icon: "fas fa-brain",
          title: data.quizzes?.[0]?.title || "복습 퀴즈",
          desc: data.quizzes?.[0]?.description || "추천 커리큘럼을 바탕으로 핵심 개념을 점검합니다",
          time: data.quizzes?.[0]?.expectedMinutes ? `${data.quizzes[0].expectedMinutes}분` : "15분",
          tag: "복습",
          badge: data.quizzes?.length ? "AI 생성" : "추천",
        },
        {
          key: "velog",
          icon: "fas fa-pen-nib",
          title: "velog 글 작성하기",
          desc: data.references?.length
            ? `추천 레퍼런스 ${data.references.length}개를 바탕으로 글 구조를 잡아봅니다`
            : "글을 바로 써주기보다, 글 작성에 필요한 레퍼런스를 추천합니다",
          time: data.references?.length ? `${Math.max(20, data.references.length * 10)}분` : "40분",
          tag: "정리",
          badge: "레퍼런스",
        },
      ]

      return data
    } catch (e) {
      console.error('recommendation detail 조회 실패 (Mock 유지):', e)
      recommendationDetail.value = null
      return null
    } finally {
      isLoading.value = false
    }
  }

  const loadCurriculumReason = async (curriculumId) => {
    try {
      const { data } = await getCurriculumReason(curriculumId)
      curriculumReason.value = data
      return data
    } catch (e) {
      console.error('curriculum reason 조회 실패:', e)
      curriculumReason.value = null
      return null
    }
  }

  const getMonthRange = (startDate, endDate) => {
    const start = startDate ? new Date(startDate) : new Date()
    const end = endDate ? new Date(endDate) : start

    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      const today = new Date()
      return [{ year: today.getFullYear(), month: today.getMonth() + 1 }]
    }

    const ranges = []
    const cursor = new Date(start.getFullYear(), start.getMonth(), 1)
    const last = new Date(end.getFullYear(), end.getMonth(), 1)

    while (cursor <= last) {
      ranges.push({ year: cursor.getFullYear(), month: cursor.getMonth() + 1 })
      cursor.setMonth(cursor.getMonth() + 1)
    }

    return ranges
  }

  const loadCurriculumNodes = async (curriculumId, startDate, endDate) => {
    selectedCurriculumNodes.value = []

    try {
      const ranges = getMonthRange(startDate, endDate)
      const results = await Promise.all(ranges.map(params => getCalendar(params)))

      const foundNodes = []
      const seenNodeIds = new Set()

      results.forEach(({ data }) => {
        const curriculum = (data.curricula || []).find(item => item.curriculumId === curriculumId)
        if (!curriculum) return

        ;(curriculum.nodes || []).forEach(node => {
          if (seenNodeIds.has(node.curriculumNodeId)) return
          seenNodeIds.add(node.curriculumNodeId)
          foundNodes.push(node)
        })
      })

      selectedCurriculumNodes.value = foundNodes.sort((a, b) => {
        return (a.scheduledDate || '').localeCompare(b.scheduledDate || '')
      })

      return selectedCurriculumNodes.value
    } catch (e) {
      console.error('curriculum nodes 조회 실패:', e)
      selectedCurriculumNodes.value = []
      return []
    }
  }

  // Quiz session state
  const quizResults = ref([])
  const quizTotalScore = ref(null)
  const quizCurriculumId = ref(null)
  const isSubmitting = ref(false)

  const startQuizSession = async (curriculumId) => {
    isLoading.value = true
    try {
      const { data } = await postQuizSessionStart({ curriculumId })
      quizQuestions.value = (data.questions || []).map(q => ({
        questionNumber: q.questionNumber,
        question: q.question,
        options: q.options || [],
        quizType: q.quizType,
      }))
      quizCurriculumId.value = curriculumId
      quizResults.value = []
      quizTotalScore.value = null
      return true
    } catch (e) {
      console.error('퀴즈 세션 시작 실패 (Mock 유지):', e)
      quizCurriculumId.value = curriculumId
      return false
    } finally {
      isLoading.value = false
    }
  }

  const submitQuizAnswer = async (questionIndex) => {
    const question = quizQuestions.value[questionIndex]
    if (!question || !quizCurriculumId.value) return null

    isSubmitting.value = true
    try {
      const { data } = await postQuizAnswer(quizCurriculumId.value, {
        questionNumber: question.questionNumber || (questionIndex + 1),
        selectedAnswer: question.options[question._selectedOption],
      })
      quizResults.value.push({
        questionNumber: data.questionNumber,
        isCorrect: data.isCorrect,
        correctAnswer: data.correctAnswer,
        selectedAnswer: data.selectedAnswer,
      })
      return data
    } catch (e) {
      console.error('답안 제출 실패:', e)
      return null
    } finally {
      isSubmitting.value = false
    }
  }

  const completeQuizSession = async () => {
    if (!quizCurriculumId.value) return null

    isLoading.value = true
    try {
      const { data } = await postQuizComplete(quizCurriculumId.value)
      quizTotalScore.value = data.totalScore
      return data
    } catch (e) {
      console.error('퀴즈 완료 실패:', e)
      // 로컬 계산 폴백
      const correctCount = quizResults.value.filter(r => r.isCorrect).length
      quizTotalScore.value = Math.round((correctCount / quizQuestions.value.length) * 100)
      return null
    } finally {
      isLoading.value = false
    }
  }

  return {
    isLoading, isSubmitting,
    recentActivities,
    recommendationDetail,
    curriculumReason,
    selectedCurriculumNodes,
    recentFlow,
    missions,
    references,
    quizQuestions,
    quizResults, quizTotalScore, quizCurriculumId,
    loadRecommendations,
    loadRecommendationDetail,
    loadCurriculumReason,
    loadCurriculumNodes,
    startQuizSession, submitQuizAnswer, completeQuizSession,
  }
})
