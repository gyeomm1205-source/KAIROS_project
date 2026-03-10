import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCalendarStore = defineStore('calendar', () => {
  const tracks = ref([
    { id: 'project',   name: '졸업작품 (Spring Boot)', color: '#818cf8', index: 0, isEnded: false },
    { id: 'cert',      name: '정보처리기사 준비',      color: '#f472b6', index: 1, isEnded: false },
    { id: 'algo',      name: '코딩테스트 스터디',      color: '#34d399', index: 2, isEnded: false },
    { id: 'cs',        name: 'CS 전공지식',            color: '#fbbf24', index: 3, isEnded: false },
  ])

  const schedules = ref([
    { id: 'p1', day: '2026-03-05', track: 'project', text: '주제 선정 및 기획', tooltip: { title: '요구사항 정의서 작성', time: '10:00' } },
    { id: 'c1', day: '2026-03-08', track: 'cert', text: '정처기 필기 시작', tooltip: { title: '수제비 교재 1회독', time: '09:00' } },
    { id: 'a1', day: '2026-03-10', track: 'algo', text: '자료구조 리뷰', tooltip: { title: 'Stack, Queue', time: '14:00' } },
    { id: 'a2', day: '2026-03-10', track: 'algo', text: 'DFS/BFS 기초', tooltip: { title: '그래프 탐색 (같은 날 2개 일정)', time: '20:00' } },
    { id: 's1', day: '2026-03-15', track: 'cs', text: '운영체제 스케줄링', tooltip: { title: '라운드로빈, SJF 등', time: '19:00' } },
    { id: 'p2', day: '2026-03-18', track: 'project', text: 'DB ERD 설계', tooltip: { title: 'MySQL 테이블 관계 설정', time: '14:00' } },
    { id: 'c2', day: '2026-03-22', track: 'cert', text: '필기 기출 풀이', tooltip: { title: '최근 3개년 기출', time: '10:00' } },
    { id: 'a3', day: '2026-03-26', track: 'algo', text: '다익스트라 알고리즘', tooltip: { title: '최단경로 기초', time: '21:00' } },
    
    { id: 'p3', day: '2026-04-02', track: 'project', text: 'Spring 세팅', tooltip: { title: '의존성 추가 및 환경구성', time: '13:00' } },
    { id: 'c3', day: '2026-04-05', track: 'cert', text: '필기 시험일', tooltip: { title: '가채점 결과 확인', time: '09:00' } },
    { id: 's2', day: '2026-04-08', track: 'cs', text: '네트워크(OSI 7)', tooltip: { title: 'TCP/IP 계층 모델', time: '21:00' } },
    { id: 'a4', day: '2026-04-12', track: 'algo', text: '동적계획법(DP)', tooltip: { title: '점화식 세우기 연습', time: '19:00' } },
    { id: 'p4', day: '2026-04-16', track: 'project', text: 'REST API 개발', tooltip: { title: '로그인 및 게시판 API', time: '15:00' } },
    { id: 'c4', day: '2026-04-20', track: 'cert', text: '실기 SQL 연습', tooltip: { title: '실기 대비 DDL/DML', time: '11:00' } },
    { id: 's3', day: '2026-04-26', track: 'cs', text: 'DB 트랜잭션', tooltip: { title: 'ACID 및 락(Lock)', time: '20:00' } },

    { id: 'a5', day: '2026-05-02', track: 'algo', text: '카카오 기출 풀이', tooltip: { title: '문자열 파싱 및 구현', time: '14:00' } },
    { id: 'p5', day: '2026-05-07', track: 'project', text: '보안(Security)', tooltip: { title: 'JWT 토큰 발급 및 인가', time: '11:00' } },
    { id: 'c5', day: '2026-05-15', track: 'cert', text: '실기 모의고사', tooltip: { title: '정보처리기 실기 최종 점검', time: '10:00' } },
    { id: 'p6', day: '2026-05-21', track: 'project', text: 'AWS 배포 테스트', tooltip: { title: 'EC2, RDS 활용 서버 배포', time: '16:00' } },
    { id: 's4', day: '2026-05-28', track: 'cs', text: '면접 대비 총정리', tooltip: { title: '포트폴리오 기반 CS 질문 리스트', time: '22:00' } },

    { id: 'pr1', day: '2026-03-07', track: 'prompt', text: 'Spring Security 설정 질문', tooltip: { title: 'JWT 필터 체인 구현 방법', time: '10:30' } },
    { id: 'bl1', day: '2026-03-24', track: 'blog', text: 'DB 설계 회고', tooltip: { title: 'ERD 설계 과정 블로그 포스팅', time: '22:00' } },
    { id: 'pr2', day: '2026-04-18', track: 'prompt', text: 'SQL 튜닝 문의', tooltip: { title: '인덱스 타는 쿼리 작성법', time: '14:00' } },
    { id: 'bl2', day: '2026-05-10', track: 'blog', text: 'JWT 트러블슈팅', tooltip: { title: 'Refresh Token 갱신 로직 에러 해결', time: '23:00' } },
  ])

  const connections = ref([
    { from: 'p1', to: 'p2' }, { from: 'p2', to: 'p3' }, { from: 'p3', to: 'p4' }, { from: 'p4', to: 'p5' }, { from: 'p5', to: 'p6' },
    { from: 'c1', to: 'c2' }, { from: 'c2', to: 'c3' }, { from: 'c3', to: 'c4' }, { from: 'c4', to: 'c5' },
    { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' }, { from: 'a3', to: 'a4' }, { from: 'a4', to: 'a5' },
    { from: 's1', to: 's2' }, { from: 's2', to: 's3' }, { from: 's3', to: 's4' },
    { from: 'p2', to: 's3' }, { from: 'c4', to: 'p4' }, { from: 's2', to: 'p5' }, { from: 'a2', to: 'c3' }, 
  ])

  const allTracks = computed(() => {
    const list = [...tracks.value]
    if (!list.some(t => t.id === 'prompt')) list.push({ id: 'prompt', name: '프롬프트', color: '#facc15', index: 98, isHighlight: true, isEnded: false })
    if (!list.some(t => t.id === 'blog')) list.push({ id: 'blog', name: '블로그 어시스턴트', color: '#10b981', index: 99, isHighlight: true, isEnded: false })
    return list.sort((a, b) => a.index - b.index)
  })

  const HIGHLIGHT_TRACKS = [
    { id: 'prompt', name: '프롬프트', color: '#facc15' },
    { id: 'blog', name: '블로그 어시스턴트', color: '#10b981' }
  ]

  const activeTracks = computed(() => tracks.value.filter(t => !t.isEnded))
  const endedTracks  = computed(() => tracks.value.filter(t => t.isEnded))

  const holidays = ref({})
  const fetchedYears = ref(new Set())

  const getHoliday = (dateStr) => holidays.value[dateStr] || null

  const fetchHolidaysForYear = async (year) => {
    if (fetchedYears.value.has(year)) return
    try {
      const API_KEY = import.meta.env.VITE_GOV_API_KEY || 'TEST_KEY'
      // ★ 어떤 연도로 이동하든 해당 연도의 고정 공휴일이 동적으로 들어가게 수정!
      if (API_KEY === 'TEST_KEY' || !API_KEY) {
        Object.assign(holidays.value, {
          [`${year}-01-01`]: '신정',
          [`${year}-03-01`]: '삼일절',
          [`${year}-05-05`]: '어린이날',
          [`${year}-06-06`]: '현충일',
          [`${year}-08-15`]: '광복절',
          [`${year}-10-03`]: '개천절',
          [`${year}-10-09`]: '한글날',
          [`${year}-12-25`]: '기독탄신일'
        });
        fetchedYears.value.add(year);
        return;
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
    } catch (error) {
      console.error(`${year}년 공휴일 데이터를 불러오는데 실패했습니다:`, error)
    }
  }

  const getSchedulesForDay = (dateStr) => schedules.value.filter(s => s.day === dateStr)
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
        if (activeTracks.value.length >= 4) {
          alert('현재 진행 중인 트랙이 4개입니다. 다른 트랙을 종료한 후 다시 활성화해주세요.')
          return
        }
        t.index = getAvailableIndex() 
        t.isEnded = false
      } else {
        t.isEnded = true
      }
    }
  }

  const addTrack = (newTrack) => {
    if (activeTracks.value.length >= 4) {
      alert('진행 중인 트랙은 최대 4개까지만 생성할 수 있습니다.')
      return
    }
    tracks.value.push({ ...newTrack, isEnded: false, index: getAvailableIndex() })
  }

  const updateTrackObj = (id, data) => {
    const idx = tracks.value.findIndex(t => t.id === id)
    if (idx !== -1) tracks.value[idx] = { ...tracks.value[idx], ...data }
  }

  const createSchedule = async (data) => {
    const newId = 's_' + Date.now()
    schedules.value.push({ id: newId, day: data.day, track: data.track, text: data.text || data.tooltip?.title, tooltip: data.tooltip })
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

  return {
    tracks, allTracks, activeTracks, endedTracks, HIGHLIGHT_TRACKS, schedules, connections,
    getSchedulesForDay, getTrackById, getHoliday, fetchHolidaysForYear,
    toggleTrackEnded, addTrack, updateTrackObj,
    createSchedule, updateSchedule, deleteSchedule, updateConnectionsForSchedule
  }
})