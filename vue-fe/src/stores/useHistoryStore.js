import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useHistoryStore = defineStore('history', () => {
  const items = ref([
    {
      id: 1,
      date: "2026.03.09",
      title: "React 상태관리 복습 퀴즈",
      type: "review",
      category: "study",
      recordKind: "quiz",
      summary: "Zustand, Jotai 핵심 개념 퀴즈 결과와 취약 포인트 요약",
      tags: ["React", "상태관리"],
      excluded: false,
    },
    {
      id: 2,
      date: "2026.03.07",
      title: "SSR vs CSR 블로그 정리 기반 추천 재구성",
      type: "blog",
      category: "study",
      recordKind: "recommendation",
      summary: "Velog 글 작성 내용을 반영해 렌더링 전략 심화 커리큘럼 추천",
      tags: ["SSR", "Next.js"],
      excluded: false,
    },
    {
      id: 4,
      date: "2026.03.05",
      title: "Next.js 마이그레이션 기록",
      type: "dev",
      category: "dev",
      recordKind: "learning",
      summary: "Pages Router에서 App Router로 전환한 작업 흐름 요약",
      tags: ["Next.js", "React"],
      excluded: false,
    },
    {
      id: 5,
      date: "2026.03.03",
      title: "Docker 학습 추천 재배치",
      type: "dev",
      category: "dev",
      recordKind: "recommendation",
      summary: "최근 배포 경험과 학습 로그를 기반으로 Docker 재입문 커리큘럼 추천",
      tags: ["Docker", "배포"],
      excluded: false,
    },
    {
      id: 6,
      date: "2026.02.28",
      title: "TypeScript 제네릭 퀴즈",
      type: "review",
      category: "study",
      recordKind: "quiz",
      summary: "유틸리티 타입과 제네릭 추론 관련 퀴즈 결과",
      tags: ["TypeScript"],
      excluded: false,
    },
    {
      id: 7,
      date: "2026.02.24",
      title: "REST API 설계 문서 읽기",
      type: "study",
      category: "dev",
      recordKind: "learning",
      summary: "레퍼런스 문서 완독 후 RESTful 설계 원칙 정리",
      tags: ["API", "Backend"],
      excluded: false,
    },
    {
      id: 8,
      date: "2026.02.22",
      title: "이력서 첨삭 피드백 반영",
      type: "career",
      category: "career",
      recordKind: "learning",
      summary: "프로젝트 섹션 보완과 포트폴리오 링크 정리 기록",
      tags: ["이력서", "취준"],
      excluded: false,
    },
    {
      id: 9,
      date: "2025.12.18",
      title: "면접 대비 CS 추천 기록",
      type: "career",
      category: "career",
      recordKind: "recommendation",
      summary: "네트워크/운영체제 약점 기반 면접 보완 추천 생성",
      tags: ["CS", "면접"],
      excluded: true,
    },
    {
      id: 10,
      date: "2025.11.30",
      title: "JavaScript 비동기 퀴즈",
      type: "review",
      category: "study",
      recordKind: "quiz",
      summary: "Promise 체이닝과 async/await 예외 처리 퀴즈 결과",
      tags: ["JavaScript"],
      excluded: false,
    },
  ])

  const filter = ref('all')
  const sort = ref('newest')
  const keyword = ref('')
  const selectedYear = ref('')
  const selectedMonth = ref('')

  const toggleExclude = (id) => {
    const item = items.value.find(i => i.id === id)
    if (item) {
      item.excluded = !item.excluded
    }
  }

  const parseHistoryDate = (dateStr) => {
    const [year, month, day] = dateStr.split(".").map(Number)
    return { year, month, day }
  }

  const filteredItems = computed(() => {
    const normalizedKeyword = keyword.value.trim().toLowerCase()

    return items.value
      .filter((item) => filter.value === "all" || item.category === filter.value)
      .filter((item) => {
        if (!normalizedKeyword) return true
        const haystack = [item.title, item.summary, ...item.tags].join(" ").toLowerCase()
        return haystack.includes(normalizedKeyword)
      })
      .filter((item) => {
        const { year, month } = parseHistoryDate(item.date)
        const matchesYear = !selectedYear.value || String(year) === selectedYear.value
        const matchesMonth = !selectedMonth.value || String(month) === selectedMonth.value
        return matchesYear && matchesMonth
      })
      .sort((a, b) => {
        if (sort.value === "newest") return b.date.localeCompare(a.date)
        return a.date.localeCompare(b.date)
      })
  })

  const totalCount = computed(() => items.value.length)
  const thisWeekCount = computed(() => items.value.filter((item) => item.date >= "2026.03.03").length)
  const excludedCount = computed(() => items.value.filter((item) => item.excluded).length)

  const resetQueries = () => {
    keyword.value = ""
    selectedYear.value = ""
    selectedMonth.value = ""
  }

  const deleteItem = (id) => {
    items.value = items.value.filter(i => i.id !== id)
  }

  return {
    items, filter, sort, keyword, selectedYear, selectedMonth,
    toggleExclude, deleteItem, filteredItems, totalCount, thisWeekCount, excludedCount, resetQueries
  }
})
