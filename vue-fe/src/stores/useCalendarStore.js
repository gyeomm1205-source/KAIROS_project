import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useThemeStore } from '@/stores/useThemeStore'

export const useCalendarStore = defineStore('calendar', () => {
  // ----------------------------------------------------------------
  // 1. 요청하신 신규 트랙 및 색상 팔레트 적용
  // ----------------------------------------------------------------
  const tracks = ref([
    { id: 'main',      name: 'FRONTEND', color: '#334155', index: 0, isEnded: false }, // Slate 700
    { id: 'algo',      name: 'ALGORITHM',color: '#991B1B', index: 1, isEnded: false }, // Red 800
    { id: 'portfolio', name: 'PROJECT',  color: '#065F46', index: 2, isEnded: false }, // Teal 800
    { id: 'cs',        name: 'CS STUDY', color: '#854D0E', index: 3, isEnded: false }, // Amber 800
    { id: 'prompt',    name: 'PROMPT',   color: '#701A75', index: 4, isEnded: false }, // Fuchsia 900
    { id: 'blog',      name: 'BLOG',     color: '#1E3A8A', index: 5, isEnded: false }, // Blue 900
  ])

  // --- CONNECTIVITY SETTINGS ---
  const showConnections = ref(true)
  const lowIntensityLines = ref(true)

  // 트랙 ID 변경에 맞춰 Mock 데이터의 track 속성도 수정 완료
  const schedules = ref([
    { id: 'p1', day: '2026-03-05', track: 'portfolio', text: '주제 선정 및 기획', tooltip: { title: '요구사항 정의서 작성', time: '10:00' }, progress: 100, reasoning: 'AI: 최근 검색 활동 기반 필수 초기 구성' },
    { id: 'c1', day: '2026-03-08', track: 'main', text: 'React 기초', tooltip: { title: '공식문서 1회독', time: '09:00' } },
    { id: 'a1', day: '2026-03-10', track: 'algo', text: '자료구조 리뷰', tooltip: { title: 'Stack, Queue', time: '14:00' } },
    { id: 'a2', day: '2026-03-10', track: 'algo', text: 'DFS/BFS 기초', tooltip: { title: '그래프 탐색 (같은 날 2개 일정)', time: '20:00' } },
    { id: 's1', day: '2026-03-15', track: 'cs', text: '운영체제 스케줄링', tooltip: { title: '라운드로빈, SJF 등', time: '19:00' } },
    { id: 'p2', day: '2026-03-18', track: 'portfolio', text: 'DB ERD 설계', tooltip: { title: 'MySQL 테이블 관계 설정', time: '14:00' } },
    { id: 'c2', day: '2026-03-22', track: 'main', text: 'TypeScript 도입', tooltip: { title: '기본 타입 및 인터페이스', time: '10:00' } },
    { id: 'a3', day: '2026-03-26', track: 'algo', text: '다익스트라 알고리즘', tooltip: { title: '최단경로 기초', time: '21:00' } },
    { id: 'p3', day: '2026-04-02', track: 'portfolio', text: 'Next.js 세팅', tooltip: { title: '의존성 추가 및 환경구성', time: '13:00' } },
    { id: 'c3', day: '2026-04-05', track: 'main', text: '상태관리', tooltip: { title: 'Zustand 적용 테스트', time: '09:00' } },
    { id: 's2', day: '2026-04-08', track: 'cs', text: '네트워크(OSI 7)', tooltip: { title: 'TCP/IP 계층 모델', time: '21:00' } },
    { id: 'a4', day: '2026-04-12', track: 'algo', text: '동적계획법(DP)', tooltip: { title: '점화식 세우기 연습', time: '19:00' } },
    { id: 'p4', day: '2026-04-16', track: 'portfolio', text: 'REST API 개발', tooltip: { title: '로그인 및 게시판 API 연동', time: '15:00' } },
    { id: 'c4', day: '2026-04-20', track: 'main', text: '성능 최적화', tooltip: { title: '메모이제이션 및 랜더링 최적화', time: '11:00' } },
    { id: 's3', day: '2026-04-26', track: 'cs', text: 'DB 트랜잭션', tooltip: { title: 'ACID 및 락(Lock)', time: '20:00' } },
    { id: 'a5', day: '2026-05-02', track: 'algo', text: '카카오 기출 풀이', tooltip: { title: '문자열 파싱 및 구현', time: '14:00' } },
    { id: 'p5', day: '2026-05-07', track: 'portfolio', text: '보안(Security)', tooltip: { title: 'JWT 토큰 발급 및 인가', time: '11:00' } },
    { id: 'c5', day: '2026-05-15', track: 'main', text: '웹 접근성', tooltip: { title: 'WAI-ARIA 적용 및 점검', time: '10:00' } },
    { id: 'p6', day: '2026-05-21', track: 'portfolio', text: 'AWS 배포 테스트', tooltip: { title: 'Vercel, AWS S3 배포', time: '16:00' } },
    { id: 's4', day: '2026-05-28', track: 'cs', text: '면접 대비 총정리', tooltip: { title: '포트폴리오 기반 CS 질문 리스트', time: '22:00' } },
    { id: 'pr1', day: '2026-03-07', track: 'prompt', text: 'React Hooks 질문', tooltip: { title: 'useEffect 의존성 배열 관리', time: '10:30' } },
    { id: 'bl1', day: '2026-03-24', track: 'blog', text: 'Next.js 라우팅 회고', tooltip: { title: 'App Router 구조 블로그 포스팅', time: '22:00' } },
    { id: 'pr2', day: '2026-04-18', track: 'prompt', text: '상태관리 라이브러리 비교', tooltip: { title: 'Zustand vs Redux', time: '14:00' } },
    { id: 'bl2', day: '2026-05-10', track: 'blog', text: '배포 트러블슈팅', tooltip: { title: 'CORS 에러 해결 과정', time: '23:00' } },
  ])

  const connections = ref([
    { from: 'p1', to: 'p2' }, { from: 'p2', to: 'p3' }, { from: 'p3', to: 'p4' }, { from: 'p4', to: 'p5' }, { from: 'p5', to: 'p6' },
    { from: 'c1', to: 'c2' }, { from: 'c2', to: 'c3' }, { from: 'c3', to: 'c4' }, { from: 'c4', to: 'c5' },
    { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' }, { from: 'a3', to: 'a4' }, { from: 'a4', to: 'a5' },
    { from: 's1', to: 's2' }, { from: 's2', to: 's3' }, { from: 's3', to: 's4' },
    { from: 'p2', to: 's3' }, { from: 'c4', to: 'p4' }, { from: 's2', to: 'p5' }, { from: 'a2', to: 'c3' }, 
  ])

  // ----------------------------------------------------------------
  // 2. 신규 와이어프레임(AI) 통합용 상태 (State)
  // ----------------------------------------------------------------
  const analysisResult = ref(null)
  const alternativeCurriculum = ref([])
  const isLoadingAI = ref(false)

  // ----------------------------------------------------------------
  // 3. 기존 Computed 유지 (형광펜 색상 적용 완료)
  // ----------------------------------------------------------------
  const allTracks = computed(() => {
    return [...tracks.value].sort((a, b) => a.index - b.index)
  })

  const HIGHLIGHT_TRACKS = []

  // Computed로 allTracks를 구독하여 색상 동기화
  const activeTracks = computed(() => allTracks.value.filter(t => !t.isEnded && !t.isHighlight))
  const endedTracks  = computed(() => allTracks.value.filter(t => t.isEnded && !t.isHighlight))

  const holidays = ref({})
  const fetchedYears = ref(new Set())

  // ----------------------------------------------------------------
  // 4. 기존 캘린더 Actions 유지
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
        });
        fetchedYears.value.add(year); return;
      }
      const url = `https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo?solYear=${year}&ServiceKey=${API_KEY}&_type=json&numOfRows=100`
      const response = await fetch(url)
      const data = await response.json()
      
      const items = data.response?.body?.items?.item
      if (items) {
        const holidayArray = Array.isArray(items) ? items : [items]
        holidayArray.forEach(item => {
          const dateStr = item.locdate.toString()
          const formattedDate = `${dateStr.substring(0,4)}-${dateStr.substring(4,6)}-${dateStr.substring(6,8)}`
          holidays.value[formattedDate] = item.dateName 
        })
      }
      fetchedYears.value.add(year)
    } catch (error) { console.error(`${year}년 공휴일 데이터를 불러오는데 실패했습니다:`, error) }
  }

  const getSchedulesForDay = (dateStr) => schedules.value.filter(s => s.day === dateStr)
  
  // allTracks에서 조회하여 다크모드 색상까지 완벽히 가져옴
  const getTrackById = (id) => allTracks.value.find(t => t.id === id)

  const getAvailableIndex = () => {
    const activeIndices = activeTracks.value.map(t => t.index)
    let newIdx = 0
    while (activeIndices.includes(newIdx)) newIdx++
    return newIdx
  }

  const toggleTrackEnded = (id) => {
    const t = tracks.value.find(x => x.id === id)
    if (t) {
      if (t.isEnded) {
        if (activeTracks.value.length >= 6) { alert('현재 진행 중인 트랙이 6개입니다. 다른 트랙을 종료한 후 다시 활성화해주세요.'); return }
        t.index = getAvailableIndex(); t.isEnded = false
      } else {
        t.isEnded = true
      }
    }
  }

  const addTrack = (newTrack) => {
    if (activeTracks.value.length >= 6) { alert('진행 중인 트랙은 최대 6개까지만 생성할 수 있습니다.'); return }
    tracks.value.push({ ...newTrack, isEnded: false, index: getAvailableIndex() })
  }

  const updateTrackObj = (id, data) => {
    const idx = tracks.value.findIndex(t => t.id === id)
    if (idx !== -1) tracks.value[idx] = { ...tracks.value[idx], ...data }
  }

  const createSchedule = async (data) => {
    const newId = 's_' + Date.now()
    schedules.value.push({ 
      id: newId, day: data.day, track: data.track, text: data.text || data.tooltip?.title, tooltip: data.tooltip,
      reasoning: data.reasoning // 신규 데이터 유지
    })
    return newId
  }
  const updateSchedule = async (id, data) => {
    const idx = schedules.value.findIndex(s => s.id === id)
    if (idx !== -1) schedules.value[idx] = { ...schedules.value[idx], ...data }
  }
  const deleteSchedule = async (id) => {
    schedules.value = schedules.value.filter(s => s.id !== id)
    connections.value = connections.value.filter(c => c.from !== id && c.to !== id)
  }
  const updateConnectionsForSchedule = (nodeId, parentIds, childIds) => {
    connections.value = connections.value.filter(c => c.from !== nodeId && c.to !== nodeId)
    parentIds.forEach(pId => connections.value.push({ from: pId, to: nodeId }))
    childIds.forEach(cId => connections.value.push({ from: nodeId, to: cId }))
  }

  // ----------------------------------------------------------------
  // 5. 신규 와이어프레임(AI) 통합용 Actions (독립적인 Mock 데이터 처리)
  // ----------------------------------------------------------------
  const loadAnalysisResult = async (userId) => {
    isLoadingAI.value = true
    try {
      const cached = localStorage.getItem('analysisResult')
      if (cached) {
        const raw = JSON.parse(cached)
        analysisResult.value = mapAnalysisData(raw)
      } else {
        // localStorage에 데이터 없으면 Mock 폴백
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
          summary: '최근 3개월간 React와 TypeScript 중심의 프론트엔드 학습을 꾸준히 진행하고 있습니다. 백엔드 관련 활동은 상대적으로 적어, 프론트엔드 전문성 강화를 추천합니다.'
        }
      }
    } finally {
      isLoadingAI.value = false
    }
  }

  function mapAnalysisData(raw) {
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
      // 필요 시 목데이터 생성 로직 추가
    } catch (error) {
      console.error('AI 일정 생성 실패:', error)
    } finally {
      isLoadingAI.value = false
    }
  }

  return {
    tracks, allTracks, activeTracks, endedTracks, HIGHLIGHT_TRACKS, schedules, connections,
    getSchedulesForDay, getTrackById, getHoliday, fetchHolidaysForYear,
    toggleTrackEnded, addTrack, updateTrackObj,
    createSchedule, updateSchedule, deleteSchedule, updateConnectionsForSchedule,
    // [신규 반환값]
    showConnections, lowIntensityLines,
    analysisResult, alternativeCurriculum, isLoadingAI,
    loadAnalysisResult, loadAlternativeCurriculum, addAiGeneratedSchedule
  }
})