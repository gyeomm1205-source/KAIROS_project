import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCalendarStore = defineStore('calendar', () => {
  const tracks = ref([
    { id: 'project',   name: '졸업작품 (Spring Boot)', color: '#818cf8', index: 0, isEnded: false },
    { id: 'cert',      name: '정보처리기사 준비',      color: '#f472b6', index: 1, isEnded: false },
    { id: 'algo',      name: '코딩테스트 스터디',      color: '#34d399', index: 2, isEnded: false },
    { id: 'cs',        name: 'CS 전공지식',            color: '#fbbf24', index: 3, isEnded: false },
  ])

  // ★ 시각적으로 화려한 라인 교차와 병합을 보여주기 위한 더미 데이터 세팅
  const schedules = ref([
    // 3월
    { id: 'p1', day: '2026-03-05', track: 'project', text: '주제 선정 및 기획', tooltip: { title: '요구사항 정의서 작성', time: '10:00' } },
    { id: 'c1', day: '2026-03-08', track: 'cert', text: '정처기 필기 시작', tooltip: { title: '수제비 교재 1회독', time: '09:00' } },
    { id: 'a1', day: '2026-03-10', track: 'algo', text: '자료구조 리뷰', tooltip: { title: 'Stack, Queue', time: '14:00' } },
    { id: 'a2', day: '2026-03-10', track: 'algo', text: 'DFS/BFS 기초', tooltip: { title: '그래프 탐색 (같은 날 2개 일정)', time: '20:00' } },
    { id: 's1', day: '2026-03-15', track: 'cs', text: '운영체제 스케줄링', tooltip: { title: '라운드로빈, SJF 등', time: '19:00' } },
    { id: 'p2', day: '2026-03-18', track: 'project', text: 'DB ERD 설계', tooltip: { title: 'MySQL 테이블 관계 설정', time: '14:00' } },
    { id: 'c2', day: '2026-03-22', track: 'cert', text: '필기 기출 풀이', tooltip: { title: '최근 3개년 기출', time: '10:00' } },
    { id: 'a3', day: '2026-03-26', track: 'algo', text: '다익스트라 알고리즘', tooltip: { title: '최단경로 기초', time: '21:00' } },
    
    // 4월
    { id: 'p3', day: '2026-04-02', track: 'project', text: 'Spring 세팅', tooltip: { title: '의존성 추가 및 환경구성', time: '13:00' } },
    { id: 'c3', day: '2026-04-05', track: 'cert', text: '필기 시험일', tooltip: { title: '가채점 결과 확인', time: '09:00' } },
    { id: 's2', day: '2026-04-08', track: 'cs', text: '네트워크(OSI 7)', tooltip: { title: 'TCP/IP 계층 모델', time: '21:00' } },
    { id: 'a4', day: '2026-04-12', track: 'algo', text: '동적계획법(DP)', tooltip: { title: '점화식 세우기 연습', time: '19:00' } },
    { id: 'p4', day: '2026-04-16', track: 'project', text: 'REST API 개발', tooltip: { title: '로그인 및 게시판 API', time: '15:00' } },
    { id: 'c4', day: '2026-04-20', track: 'cert', text: '실기 SQL 연습', tooltip: { title: '실기 대비 DDL/DML', time: '11:00' } },
    { id: 's3', day: '2026-04-26', track: 'cs', text: 'DB 트랜잭션', tooltip: { title: 'ACID 및 락(Lock)', time: '20:00' } },

    // 5월
    { id: 'a5', day: '2026-05-02', track: 'algo', text: '카카오 기출 풀이', tooltip: { title: '문자열 파싱 및 구현', time: '14:00' } },
    { id: 'p5', day: '2026-05-07', track: 'project', text: '보안(Security)', tooltip: { title: 'JWT 토큰 발급 및 인가', time: '11:00' } },
    { id: 'c5', day: '2026-05-15', track: 'cert', text: '실기 모의고사', tooltip: { title: '정보처리기사 실기 최종 점검', time: '10:00' } },
    { id: 'p6', day: '2026-05-21', track: 'project', text: 'AWS 배포 테스트', tooltip: { title: 'EC2, RDS 활용 서버 배포', time: '16:00' } },
    { id: 's4', day: '2026-05-28', track: 'cs', text: '면접 대비 총정리', tooltip: { title: '포트폴리오 기반 CS 질문 리스트', time: '22:00' } },

    // 프롬프트 & 블로그 (형광펜 시스템 트랙)
    { id: 'pr1', day: '2026-03-07', track: 'prompt', text: 'Spring Security 설정 질문', tooltip: { title: 'JWT 필터 체인 구현 방법', time: '10:30' } },
    { id: 'bl1', day: '2026-03-24', track: 'blog', text: 'DB 설계 회고', tooltip: { title: 'ERD 설계 과정 블로그 포스팅', time: '22:00' } },
    { id: 'pr2', day: '2026-04-18', track: 'prompt', text: 'SQL 튜닝 문의', tooltip: { title: '인덱스 타는 쿼리 작성법', time: '14:00' } },
    { id: 'bl2', day: '2026-05-10', track: 'blog', text: 'JWT 트러블슈팅', tooltip: { title: 'Refresh Token 갱신 로직 에러 해결', time: '23:00' } },
  ])

  // ★ 다채로운 브랜칭(분기), 머지(병합), 크로스(타 트랙 참조) 엣지 설정
  const connections = ref([
    // 같은 트랙 직진 선
    { from: 'p1', to: 'p2' }, { from: 'p2', to: 'p3' }, { from: 'p3', to: 'p4' }, { from: 'p4', to: 'p5' }, { from: 'p5', to: 'p6' },
    { from: 'c1', to: 'c2' }, { from: 'c2', to: 'c3' }, { from: 'c3', to: 'c4' }, { from: 'c4', to: 'c5' },
    { from: 'a1', to: 'a2' }, { from: 'a2', to: 'a3' }, { from: 'a3', to: 'a4' }, { from: 'a4', to: 'a5' },
    { from: 's1', to: 's2' }, { from: 's2', to: 's3' }, { from: 's3', to: 's4' },
    
    // ★ 교차선 (트랙 간 시너지 연결: 선들이 이리저리 꼬이며 예쁘게 그려집니다)
    { from: 'p2', to: 's3' }, // 프로젝트 DB 설계(p2)를 하면서 -> CS 트랜잭션 공부(s3) 파생
    { from: 'c4', to: 'p4' }, // 정처기 SQL 공부(c4)한 걸 -> 프로젝트 API(p4)에 적용 (합쳐짐)
    { from: 's2', to: 'p5' }, // 네트워크 공부(s2) 후 -> Spring Security(p5) 설정에 적용
    { from: 'a2', to: 'c3' }, // 알고리즘 머리 식힐 겸 정처기 필기(c3) 응시
  ])

  const allTracks = computed(() => {
    const list = [...tracks.value]
    if (!list.some(t => t.id === 'prompt')) list.push({ id: 'prompt', name: '프롬프트', color: '#facc15', index: 98, isHighlight: true, isEnded: false })
    if (!list.some(t => t.id === 'blog')) list.push({ id: 'blog', name: '블로그 어시스턴트', color: '#10b981', index: 99, isHighlight: true, isEnded: false })
    return list.sort((a, b) => a.index - b.index)
  })

  const activeTracks = computed(() => tracks.value.filter(t => !t.isEnded))
  const endedTracks  = computed(() => tracks.value.filter(t => t.isEnded))

  // ─────────────────────────────────────────────────────────────
  // ★ 동적 공휴일 관리 시스템 (공공데이터 API 통신 및 캐싱)
  // ─────────────────────────────────────────────────────────────
  const holidays = ref({})
  const fetchedYears = ref(new Set())

  const getHoliday = (dateStr) => holidays.value[dateStr] || null

  const fetchHolidaysForYear = async (year) => {
    if (fetchedYears.value.has(year)) return
    
    try {
      const API_KEY = import.meta.env.VITE_GOV_API_KEY || 'TEST_KEY'
      
      // 테스트용: API 키가 없거나 에러 방지용으로 2026년 하드코딩 Fallback 제공
      if (API_KEY === 'TEST_KEY' && year === 2026) {
        Object.assign(holidays.value, {
          '2026-01-01': '신정', '2026-02-16': '설날 연휴', '2026-02-17': '설날', '2026-02-18': '설날 연휴',
          '2026-03-01': '삼일절', '2026-03-02': '대체공휴일', '2026-05-05': '어린이날', '2026-05-24': '부처님오신날',
          '2026-05-25': '대체공휴일', '2026-06-06': '현충일', '2026-08-15': '광복절', '2026-09-24': '추석 연휴',
          '2026-09-25': '추석', '2026-09-26': '추석 연휴', '2026-10-03': '개천절', '2026-10-09': '한글날', '2026-12-25': '기독탄신일'
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

  const toggleTrackEnded = (id) => {
    const t = tracks.value.find(x => x.id === id)
    if (t) t.isEnded = !t.isEnded
  }
  const addTrack = (newTrack) => {
    if (activeTracks.value.length >= 4) return alert('진행 중인 트랙은 최대 4개까지만 생성할 수 있습니다.')
    tracks.value.push({ ...newTrack, isEnded: false, index: tracks.value.length })
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
    tracks, allTracks, activeTracks, endedTracks, schedules, connections,
    getSchedulesForDay, getTrackById, getHoliday, fetchHolidaysForYear,
    toggleTrackEnded, addTrack, updateTrackObj,
    createSchedule, updateSchedule, deleteSchedule, updateConnectionsForSchedule
  }
})