import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// ─── 날짜 유틸 ───
export function toDateStr(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function parseDate(str) {
  const [y, m, d] = str.split('-').map(Number)
  return new Date(y, m - 1, d)
}

export const useCalendarStore = defineStore('calendar', () => {

  // ─── 사용자 트랙 (최대 4개, 인덱스 0~3) ───
  const tracks = ref([
    { id: 'main',      name: 'CS 기초',   color: '#0ea5e9', index: 0 },
    { id: 'algo',      name: '알고리즘',   color: '#ec4899', index: 1 },
    { id: 'react',     name: 'React',      color: '#22c55e', index: 2 },
    { id: 'portfolio', name: '포트폴리오', color: '#f97316', index: 3 },
  ])

  // ─── 시스템(형광펜) 트랙 2개 — 항상 맨 아래 고정, 삭제/이동 불가 ───
  // index는 사용자 트랙 최대값(3) + 1 부터 시작
  const HIGHLIGHT_TRACKS = [
    { id: 'hl_prompt', name: '프롬프트',       color: '#facc15', index: 4, isHighlight: true },
    { id: 'hl_blog',   name: '블로그 어시스턴트', color: '#34d399', index: 5, isHighlight: true },
  ]

  // ─── 일정 ───
  const today = new Date()
  const y = today.getFullYear()
  const m = today.getMonth() + 1

  function d(day) {
    return `${y}-${String(m).padStart(2,'0')}-${String(day).padStart(2,'0')}`
  }

  const schedules = ref([
    { id: 'n1',  day: d(1),  track: 'main',      text: '운영체제 기초', tooltip: { time: '09:00', title: '프로세스 & 스레드',      tags: ['#OS','#CS'] } },
    { id: 'n2',  day: d(2),  track: 'main',      text: '네트워크',     tooltip: { time: '10:00', title: 'TCP/IP 개념 정리',        tags: ['#Network','#CS'] } },
    { id: 'n3',  day: d(2),  track: 'algo',      text: '',             tooltip: { time: '14:00', title: '정렬 알고리즘',            tags: ['#Algorithm','#Sort'] } },
    { id: 'n4',  day: d(4),  track: 'main',      text: '',             tooltip: { time: '10:00', title: '데이터베이스 기초',        tags: ['#DB','#CS'] } },
    { id: 'n5',  day: d(4),  track: 'algo',      text: 'BFS/DFS',     tooltip: { time: '15:00', title: '그래프 탐색 구현',         tags: ['#Algorithm','#Graph'] } },
    { id: 'n6',  day: d(6),  track: 'react',     text: 'React 기초',  tooltip: { time: '11:00', title: 'Hooks & 컴포넌트 설계',   tags: ['#React','#Frontend'] } },
    { id: 'n7',  day: d(7),  track: 'algo',      text: '',             tooltip: { time: '14:00', title: '동적 프로그래밍',          tags: ['#Algorithm','#DP'] } },
    { id: 'n8',  day: d(9),  track: 'react',     text: 'Pinia',       tooltip: { time: '13:00', title: 'Vue3 + Pinia 실습',       tags: ['#Vue','#Pinia'] } },
    { id: 'n9',  day: d(10), track: 'portfolio', text: 'README',      tooltip: { time: '16:00', title: '깃허브 포트폴리오 정리',   tags: ['#Portfolio','#GitHub'] } },
    { id: 'n10', day: d(13), track: 'main',      text: '',             tooltip: { time: '10:00', title: '자료구조 총정리',          tags: ['#CS','#DataStructure'] } },
    { id: 'n11', day: d(14), track: 'hl_blog',   text: '블로그 초안', tooltip: { time: '20:00', title: 'React 학습 후기 포스팅',   tags: ['#Blog','#Writing'] } },
    { id: 'n12', day: d(17), track: 'portfolio', text: '',             tooltip: { time: '15:00', title: '기술 스택 스캔 & 정리',   tags: ['#Portfolio','#GitHub'] } },
    { id: 'n13', day: d(5),  track: 'hl_prompt', text: '프롬프트',    tooltip: { time: '10:00', title: 'GPT 프롬프트 작성 실습',   tags: ['#Prompt','#AI'] } },
  ])

  // ─── 연결선 ───
  const connections = ref([
    { id: 'e1',  from: 'n1',  to: 'n2'  },
    { id: 'e2',  from: 'n2',  to: 'n4'  },
    { id: 'e3',  from: 'n4',  to: 'n10' },
    { id: 'e4',  from: 'n2',  to: 'n3'  },
    { id: 'e5',  from: 'n3',  to: 'n5'  },
    { id: 'e6',  from: 'n5',  to: 'n7'  },
    { id: 'e7',  from: 'n6',  to: 'n8'  },
    { id: 'e8',  from: 'n9',  to: 'n12' },
    { id: 'e9',  from: 'n7',  to: 'n12' },
    { id: 'e10', from: 'n10', to: 'n12' },
  ])

  const isLoading = ref(false)
  const error     = ref(null)

  // ─── Getters ───
  // 모든 트랙 (사용자 + 시스템), index 오름차순
  const allTracks = computed(() =>
    [...tracks.value, ...HIGHLIGHT_TRACKS].sort((a, b) => a.index - b.index)
  )

  const getTrackById = (id) =>
    allTracks.value.find(t => t.id === id) || tracks.value[0]

  const getSchedulesForDay = (dateStr) =>
    schedules.value.filter(s => s.day === dateStr)

  const getConnectionsForSchedule = (scheduleId) => ({
    incoming: connections.value.filter(e => e.to   === scheduleId),
    outgoing: connections.value.filter(e => e.from === scheduleId),
  })

  // ─── Actions ───
  async function createSchedule(payload) {
    const newId = 's_' + Date.now()
    schedules.value.push({ id: newId, ...payload })
    return newId
  }

  async function updateSchedule(id, payload) {
    const idx = schedules.value.findIndex(s => s.id === id)
    if (idx !== -1) schedules.value[idx] = { ...schedules.value[idx], ...payload }
  }

  async function deleteSchedule(id) {
    const incoming = connections.value.filter(e => e.to   === id)
    const outgoing = connections.value.filter(e => e.from === id)
    incoming.forEach(inc => outgoing.forEach(out =>
      connections.value.push({ id: 'e_' + Date.now() + Math.random(), from: inc.from, to: out.to })
    ))
    connections.value = connections.value.filter(e => e.from !== id && e.to !== id)
    schedules.value   = schedules.value.filter(s => s.id !== id)
  }

  function deleteAllSchedules() {
    schedules.value   = []
    connections.value = []
  }

  function updateConnectionsForSchedule(scheduleId, prevIds, nextIds) {
    const pArr = Array.isArray(prevIds) ? prevIds : (prevIds ? [prevIds] : [])
    const nArr = Array.isArray(nextIds) ? nextIds : (nextIds ? [nextIds] : [])
    connections.value = connections.value.filter(e => e.to !== scheduleId)
    pArr.forEach((pid, i) => {
      if (pid) connections.value.push({ id: 'e_' + Date.now() + i, from: pid, to: scheduleId })
    })
    connections.value = connections.value.filter(e => e.from !== scheduleId)
    nArr.forEach((nid, i) => {
      if (nid) connections.value.push({ id: 'e_' + Date.now() + 100 + i, from: scheduleId, to: nid })
    })
  }

  function addTrack() {
    // 사용자 트랙은 최대 4개 (index 0~3)
    const userTracks = tracks.value
    if (userTracks.length >= 4) {
      alert('사용자 트랙은 최대 4개까지 추가할 수 있습니다.')
      return
    }
    const COLORS = ['#f43f5e','#8b5cf6','#06b6d4','#10b981','#f59e0b']
    const usedIndices = userTracks.map(t => t.index)
    let newIdx = 0
    while (usedIndices.includes(newIdx)) newIdx++
    tracks.value.push({
      id: 'track_' + Date.now(), name: '새 트랙',
      color: COLORS[newIdx % COLORS.length], index: newIdx
    })
  }

  function deleteTrack(id) {
    // 시스템 트랙은 삭제 불가
    if (HIGHLIGHT_TRACKS.find(t => t.id === id)) return
    const ids = schedules.value.filter(s => s.track === id).map(s => s.id)
    connections.value = connections.value.filter(e => !ids.includes(e.from) && !ids.includes(e.to))
    schedules.value   = schedules.value.filter(s => s.track !== id)
    tracks.value      = tracks.value.filter(t => t.id !== id)
  }

  return {
    tracks, schedules, connections, isLoading, error,
    allTracks, HIGHLIGHT_TRACKS,
    getTrackById, getSchedulesForDay, getConnectionsForSchedule,
    createSchedule, updateSchedule, deleteSchedule, deleteAllSchedules,
    updateConnectionsForSchedule, addTrack, deleteTrack
  }
})
