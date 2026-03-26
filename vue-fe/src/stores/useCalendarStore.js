import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCalendar, createSchedule as apiCreateSchedule, updateSchedule as apiUpdateSchedule, deleteSchedule as apiDeleteSchedule, patchCurriculumNode } from '@/api/aiApi'

// 커리큘럼별 자동 배정 색상
const CURRICULUM_COLORS = ['#334155', '#991B1B', '#065F46', '#854D0E', '#701A75', '#1E3A8A', '#0E7490', '#4338CA']
const PERSONAL_COLOR = '#6B7280'

// Mock 데이터 (API 실패 시 폴백)
const MOCK_TRACKS = [
  { id: 'main', name: 'FRONTEND', color: '#334155', index: 0, isEnded: false },
  { id: 'algo', name: 'ALGORITHM', color: '#991B1B', index: 1, isEnded: false },
  { id: 'portfolio', name: 'PROJECT', color: '#065F46', index: 2, isEnded: false },
  { id: 'cs', name: 'CS STUDY', color: '#854D0E', index: 3, isEnded: false },
  { id: 'prompt', name: 'PROMPT', color: '#701A75', index: 4, isEnded: false },
  { id: 'blog', name: 'BLOG', color: '#1E3A8A', index: 5, isEnded: false },
]

const MOCK_SCHEDULES = [
  { id: 'p1', day: '2026-03-05', track: 'portfolio', text: '주제 선정 및 기획', tooltip: { title: '요구사항 정의서 작성', time: '10:00' }, progress: 100 },
  { id: 'c1', day: '2026-03-08', track: 'main', text: 'React 기초', tooltip: { title: '공식문서 1회독', time: '09:00' } },
  { id: 'a1', day: '2026-03-10', track: 'algo', text: '자료구조 리뷰', tooltip: { title: 'Stack, Queue', time: '14:00' } },
  { id: 's1', day: '2026-03-15', track: 'cs', text: '운영체제 스케줄링', tooltip: { title: '라운드로빈, SJF 등', time: '19:00' } },
  { id: 'p2', day: '2026-03-18', track: 'portfolio', text: 'DB ERD 설계', tooltip: { title: 'MySQL 테이블 관계 설정', time: '14:00' } },
]

const MOCK_CONNECTIONS = [
  { from: 'p1', to: 'p2' },
  { from: 'c1', to: 'c1' },
]

export const useCalendarStore = defineStore('calendar', () => {
  // ----------------------------------------------------------------
  // 1. 핵심 상태: API 데이터 또는 Mock
  // ----------------------------------------------------------------
  const curricula = ref([])
  const personalSchedules = ref([])
  const schedules = ref([...MOCK_SCHEDULES])
  const useMock = ref(true)

  // tracks: API 데이터가 있으면 curricula에서 자동 생성, 없으면 Mock
  const tracks = computed(() => {
    if (useMock.value) return MOCK_TRACKS

    const currTracks = curricula.value.map((c, i) => ({
      id: `cur-${c.curriculumId}`,
      name: c.displayName || `커리큘럼 ${c.curriculumId}`,
      color: CURRICULUM_COLORS[i % CURRICULUM_COLORS.length],
      index: i,
      isEnded: c.status !== 'ACTIVE',
      curriculumId: c.curriculumId,
      displayName: c.displayName || `커리큘럼 ${c.curriculumId}`,
      curriculumStartDate: c.curriculumStartDate || null,
      curriculumEndDate: c.curriculumEndDate || null,
      totalNodeCount: c.totalNodeCount ?? c.nodes?.length ?? 0,
    }))

    currTracks.push({
      id: 'personal',
      name: 'PERSONAL',
      color: PERSONAL_COLOR,
      index: currTracks.length,
      isEnded: false,
    })

    return currTracks
  })

  // connections: API 데이터가 있으면 노드 순서에서 자동 생성, 없으면 Mock
  const connections = computed(() => {
    if (useMock.value) return MOCK_CONNECTIONS

    const conns = []
    curricula.value.forEach(c => {
      const sortedNodes = [...c.nodes].sort((a, b) => a.scheduledDate.localeCompare(b.scheduledDate))
      for (let i = 0; i < sortedNodes.length - 1; i++) {
        conns.push({
          from: `cn-${sortedNodes[i].curriculumNodeId}`,
          to: `cn-${sortedNodes[i + 1].curriculumNodeId}`,
        })
      }
    })
    return conns
  })

  // --- CONNECTIVITY SETTINGS ---
  const showConnections = ref(true)
  const lowIntensityLines = ref(true)

  // ----------------------------------------------------------------
  // 2. AI 통합용 상태 (기존 유지)
  // ----------------------------------------------------------------
  const analysisResult = ref(null)
  const alternativeCurriculum = ref([])
  const curriculumResult = ref(null)
  const curriculumPreviewKey = ref(null)
  const isLoadingAI = ref(false)

  // ----------------------------------------------------------------
  // 3. Computed (하위 호환)
  // ----------------------------------------------------------------
  const allTracks = computed(() => {
    return [...tracks.value].sort((a, b) => a.index - b.index)
  })

  const HIGHLIGHT_TRACKS = []
  const activeTracks = computed(() => allTracks.value.filter(t => !t.isEnded && !t.isHighlight))
  const endedTracks = computed(() => allTracks.value.filter(t => t.isEnded && !t.isHighlight))

  const holidays = ref({})
  const fetchedYears = ref(new Set())

  // ----------------------------------------------------------------
  // 4. API 연동: fetchCalendar
  // ----------------------------------------------------------------
  const fetchCalendar = async (year, month) => {
    try {
      const { data } = await getCalendar({ year, month })

      // curricula 저장
      curricula.value = data.curricula || []
      personalSchedules.value = data.personalSchedules || []

      // 통합 schedules 배열 생성
      const newSchedules = []

      // curriculum 노드 변환
      curricula.value.forEach(c => {
        (c.nodes || []).forEach(node => {
          newSchedules.push({
            id: `cn-${node.curriculumNodeId}`,
            type: 'curriculum',
            curriculumNodeId: node.curriculumNodeId,
            curriculumId: c.curriculumId,
            day: node.scheduledDate,
            track: `cur-${c.curriculumId}`,
            text: node.title,
            tooltip: { title: node.title, time: null },
            progressStatus: node.progressStatus,
            expectedMinutes: node.expectedMinutes,
          })
        })
      })

      // personal schedule 변환
      personalSchedules.value.forEach(ps => {
        newSchedules.push({
          id: `ps-${ps.scheduleId}`,
          type: 'personal',
          scheduleId: ps.scheduleId,
          day: ps.startDate,
          endDay: ps.endDate,
          track: 'personal',
          text: ps.title,
          tooltip: { title: ps.title, time: null },
          googleEventId: ps.googleEventId,
        })
      })

      schedules.value = newSchedules
      useMock.value = false
    } catch (e) {
      console.error('calendar 조회 실패 (Mock 유지):', e)
      // Mock 유지
    }
  }

  // ----------------------------------------------------------------
  // 5. 기존 Actions (하위 호환 유지)
  // ----------------------------------------------------------------
  const getHoliday = (dateStr) => holidays.value[dateStr] || null

  const fetchHolidaysForYear = async (year) => {
    if (fetchedYears.value.has(year)) return
    try {
      const API_KEY = import.meta.env.VITE_GOV_API_KEY || 'TEST_KEY'
      if (API_KEY === 'TEST_KEY' || !API_KEY) {
        Object.assign(holidays.value, {
          [`${year}-01-01`]: '신정', [`${year}-03-01`]: '삼일절', [`${year}-05-05`]: '어린이날',
          [`${year}-06-06`]: '현충일', [`${year}-08-15`]: '광복절', [`${year}-10-03`]: '개천절',
          [`${year}-10-09`]: '한글날', [`${year}-12-25`]: '기독탄신일'
        })
        fetchedYears.value.add(year)
        return
      }
      const url = `https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo?solYear=${year}&ServiceKey=${API_KEY}&_type=json&numOfRows=100`
      const response = await fetch(url)
      const data = await response.json()
      const items = data.response?.body?.items?.item
      if (items) {
        const holidayArray = Array.isArray(items) ? items : [items]
        holidayArray.forEach(item => {
          const dateStr = item.locdate.toString()
          const formattedDate = `${dateStr.substring(0, 4)}-${dateStr.substring(4, 6)}-${dateStr.substring(6, 8)}`
          holidays.value[formattedDate] = item.dateName
        })
      }
      fetchedYears.value.add(year)
    } catch (error) {
      console.error(`${year}년 공휴일 데이터를 불러오는데 실패했습니다:`, error)
    }
  }

  const getSchedulesForDay = (dateStr) => schedules.value.filter(s => s.day === dateStr)
  const getTrackById = (id) => allTracks.value.find(t => t.id === id)

  // ----------------------------------------------------------------
  // 6. CRUD Actions (type 분기)
  // ----------------------------------------------------------------
  const createSchedule = async (data) => {
    // personal 일정만 생성 가능 (curriculum 노드는 BE에서 생성)
    const newId = 'ps_' + Date.now()
    const newSchedule = {
      id: newId,
      type: 'personal',
      day: data.day,
      track: 'personal',
      text: data.text || data.tooltip?.title,
      tooltip: data.tooltip,
    }
    schedules.value.push(newSchedule)

    if (!useMock.value) {
      try {
        const { data: resp } = await apiCreateSchedule({
          title: data.tooltip?.title || data.text,
          description: data.text,
          startDate: data.day,
          endDate: data.day,
        })
        // 서버 ID로 교체
        const idx = schedules.value.findIndex(s => s.id === newId)
        if (idx !== -1) {
          schedules.value[idx].id = `ps-${resp.scheduleId}`
          schedules.value[idx].scheduleId = resp.scheduleId
        }
        return `ps-${resp.scheduleId}`
      } catch (e) {
        console.error('일정 생성 API 실패:', e)
      }
    }
    return newId
  }

  const updateSchedule = async (id, data) => {
    const idx = schedules.value.findIndex(s => s.id === id)
    if (idx !== -1) {
      schedules.value[idx] = { ...schedules.value[idx], ...data }
    }

    if (!useMock.value) {
      const schedule = schedules.value.find(s => s.id === id)
      try {
        if (schedule?.type === 'curriculum') {
          await patchCurriculumNode(schedule.curriculumNodeId, {
            title: data.tooltip?.title || data.text,
            description: data.text,
            scheduledDate: data.day,
          })
        } else if (schedule?.type === 'personal') {
          await apiUpdateSchedule(schedule.scheduleId, {
            title: data.tooltip?.title || data.text,
            description: data.text,
            startDate: data.day,
            endDate: data.day,
          })
        }
      } catch (e) {
        console.error('일정 수정 API 실패:', e)
      }
    }
  }

  const deleteSchedule = async (id) => {
    const schedule = schedules.value.find(s => s.id === id)
    schedules.value = schedules.value.filter(s => s.id !== id)

    if (!useMock.value && schedule) {
      try {
        if (schedule.type === 'personal' && schedule.scheduleId) {
          await apiDeleteSchedule(schedule.scheduleId)
        }
        // curriculum 노드 삭제는 BE에서 지원하지 않음
      } catch (e) {
        console.error('일정 삭제 API 실패:', e)
      }
    }
  }

  const updateConnectionsForSchedule = (nodeId, parentIds, childIds) => {
    // Mock 모드에서만 수동 연결 관리
    // API 모드에서는 connections가 computed이므로 무시
    if (useMock.value) {
      // Mock에서는 기존 로직 유지하지 않음 (connections가 computed이므로)
    }
  }

  // track 관련 (Mock 모드에서만 의미있음)
  const toggleTrackEnded = (id) => {}
  const addTrack = (newTrack) => {}
  const updateTrackObj = (id, data) => {}

  // ----------------------------------------------------------------
  // 7. AI 통합 Actions (기존 유지)
  // ----------------------------------------------------------------
  const loadAnalysisResult = async () => {
    isLoadingAI.value = true
    try {
      const cached = localStorage.getItem('analysisResult')
      if (cached) {
        let raw = JSON.parse(cached)
        // FastAPI에서 { profile, activities } 형태로 내려주면 profile 부분만 추출
        raw = raw.profile || raw
        analysisResult.value = mapAnalysisData(raw)
      } else {
        analysisResult.value = {
          recentTechs: ['React', 'TypeScript', 'Next.js', 'TailwindCSS'],
          skillLevels: [
            { name: 'JavaScript', level: 78 }, { name: 'React', level: 72 }, { name: 'TypeScript', level: 55 }
          ],
          repeatedTechs: [
            { name: 'React', count: 23 }, { name: 'TypeScript', count: 18 }
          ],
          recommendedPositions: [
            { title: '프론트엔드 개발자', isHighMatch: true }, { title: '풀스택 개발자', isHighMatch: false }
          ],
          summary: '..최근 3개월간 React와 TypeScript 중심의 프론트엔드 학습을 꾸준히 진행하고 있습니다.'
        }      
      }
    } finally {
      isLoadingAI.value = false
    }
  }

  function mapAnalysisData(raw) {
    if (!raw) return null

    // FastAPI 형태 (LLM 분석 등에서 skill_frequency를 반환하는 경우)
    if (raw.skill_frequency) {
      const skillFreq = raw.skill_frequency || {}
      const techs = Object.keys(skillFreq).sort((a, b) => skillFreq[b] - skillFreq[a])
      const maxCount = Math.max(...Object.values(skillFreq), 1)

      return {
        recentTechs: techs.slice(0, 10),
        skillLevels: techs.map(t => ({
          name: t,
          level: Math.round((skillFreq[t] / maxCount) * 100)
        })),
        repeatedTechs: techs.map(t => ({ name: t, count: skillFreq[t] })),
        recommendedPositions: (raw.possible_positions || []).map(p => ({
          title: p,
          isHighMatch: true
        })),
        summary: [raw.headline, raw.experience_summary, raw.learning_attitude].filter(Boolean).join('\n\n')
      }
    }

    // Spring Boot / 기존 Mock 등 일반 데이터 형태 대응
    const techDetails = raw.techDetails || []
    const positions = raw.recommendedPositions || []
    return {
      recentTechs: techDetails.map(t => t.techName),
      skillLevels: techDetails.map(t => ({ name: t.techName, level: t.proficiencyPercentage || 0 })),
      repeatedTechs: techDetails.map(t => ({ name: t.techName, count: t.usageCount || 0 })),
      recommendedPositions: positions.map(p => ({
        title: p.positionName,
        isHighMatch: (p.fitLevel || '').toUpperCase() === 'HIGH'
      })),
      summary: raw.summary || ''
    }
  }

  const loadAlternativeCurriculum = async (params) => {
    isLoadingAI.value = true
    try {
      await new Promise(resolve => setTimeout(resolve, 1500))
      alternativeCurriculum.value = [
        { date: "3월 11일 (수)", title: "TanStack Query로 서버 상태 설계하기", duration: "1시간 30분 예상" },
        { date: "3월 12일 (목)", title: "폼 검증 흐름 개선과 에러 UX 정리", duration: "2시간 예상" },
        { date: "3월 13일 (금)", title: "공통 컴포넌트 API와 접근성 기준 정리", duration: "2시간 예상" },
      ]
    } finally {
      isLoadingAI.value = false
    }
  }

  const addAiGeneratedSchedule = async (promptData) => {
    isLoadingAI.value = true
    try {
      await new Promise(resolve => setTimeout(resolve, 1000))
    } catch (error) {
      console.error('AI 일정 생성 실패:', error)
    } finally {
      isLoadingAI.value = false
    }
  }

  return {
    curricula, tracks, allTracks, activeTracks, endedTracks, HIGHLIGHT_TRACKS, schedules, connections,
    getSchedulesForDay, getTrackById, getHoliday, fetchHolidaysForYear,
    toggleTrackEnded, addTrack, updateTrackObj,
    createSchedule, updateSchedule, deleteSchedule, updateConnectionsForSchedule,
    fetchCalendar,
    showConnections, lowIntensityLines,
    analysisResult, alternativeCurriculum, curriculumResult, curriculumPreviewKey, isLoadingAI,
    loadAnalysisResult, loadAlternativeCurriculum, addAiGeneratedSchedule
  }
})
