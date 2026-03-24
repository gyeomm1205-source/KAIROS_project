import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getActivities, patchActivityInclusion } from '@/api/aiApi'

const TYPE_MAP = {
  GITHUB_COMMIT: 'dev',
  GITHUB_PR: 'dev',
  VELOG_POST: 'blog',
  QUIZ: 'review',
  REFERENCE: 'study',
}

const CATEGORY_MAP = {
  THEORY: 'study',
  PRACTICE: 'dev',
  EMPLOYMENT: 'career',
}

function formatDate(isoDate) {
  if (!isoDate) return ''
  const d = new Date(isoDate)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}.${m}.${day}`
}

function mapActivityToItem(activity) {
  return {
    id: activity.activityHistoryId,
    date: formatDate(activity.activityDate),
    title: activity.title || '',
    type: TYPE_MAP[activity.activityType] || 'study',
    category: CATEGORY_MAP[activity.category] || 'study',
    summary: activity.description || '',
    tags: (activity.techStacks || []).map(t => t.techName),
    excluded: !activity.isIncluded,
  }
}

export const useHistoryStore = defineStore('history', () => {
  const mockItems = [
    { id: 1, date: "2026.03.09", title: "React 상태관리 복습 퀴즈", type: "review", category: "study", summary: "Zustand, Jotai 핵심 개념 퀴즈 결과와 취약 포인트 요약", tags: ["React", "상태관리"], excluded: false },
    { id: 2, date: "2026.03.07", title: "SSR vs CSR 블로그 정리", type: "blog", category: "study", summary: "Velog 글 작성 내용을 반영해 렌더링 전략 심화 커리큘럼 추천", tags: ["SSR", "Next.js"], excluded: false },
    { id: 4, date: "2026.03.05", title: "Next.js 마이그레이션 기록", type: "dev", category: "dev", summary: "Pages Router에서 App Router로 전환한 작업 흐름 요약", tags: ["Next.js", "React"], excluded: false },
    { id: 5, date: "2026.03.03", title: "Docker 학습 추천 재배치", type: "dev", category: "dev", summary: "최근 배포 경험과 학습 로그를 기반으로 Docker 재입문 커리큘럼 추천", tags: ["Docker", "배포"], excluded: false },
    { id: 6, date: "2026.02.28", title: "TypeScript 제네릭 퀴즈", type: "review", category: "study", summary: "유틸리티 타입과 제네릭 추론 관련 퀴즈 결과", tags: ["TypeScript"], excluded: false },
  ]

  const items = ref([...mockItems])

  const filter = ref('all')
  const sort = ref('newest')
  const keyword = ref('')
  const selectedYear = ref('')
  const selectedMonth = ref('')

  const loadActivities = async () => {
    try {
      const { data } = await getActivities({ size: 9999 })
      if (data.items && data.items.length > 0) {
        items.value = data.items.map(mapActivityToItem)
      }
    } catch (e) {
      console.error('activities 조회 실패 (Mock 유지):', e)
    }
  }

  const toggleExclude = async (id) => {
    const item = items.value.find(i => i.id === id)
    if (!item) return

    item.excluded = !item.excluded

    try {
      await patchActivityInclusion(id, { isIncluded: !item.excluded })
    } catch (e) {
      console.error('제외/복원 실패, 롤백:', e)
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
  const thisWeekCount = computed(() => {
    const now = new Date()
    const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
    const weekAgoStr = formatDate(weekAgo.toISOString())
    return items.value.filter((item) => item.date >= weekAgoStr).length
  })
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
    loadActivities, toggleExclude, deleteItem, filteredItems, totalCount, thisWeekCount, excludedCount, resetQueries
  }
})
