<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content custom-scroll">
      <header class="page-header">
        <div class="header-title"><i class="fas fa-history" /> HISTORY </div>
      </header>

      <div class="content-inner max-w-xl mx-auto">
        <div class="flex-between mb-lg flex-wrap gap-md">
          <div class="flex-align flex-wrap gap-lg">
            <div class="stat-item">
              <span class="stat-lbl">전체 기록</span>
              <span class="stat-val">{{ store.totalCount }}건</span>
            </div>
            <div class="stat-item">
              <span class="stat-lbl">이번 주 활동</span>
              <span class="stat-val">{{ store.thisWeekCount }}건</span>
            </div>
            <div class="stat-item">
              <span class="stat-lbl">제외된 기록</span>
              <span class="stat-val">{{ store.excludedCount }}건</span>
            </div>
          </div>
          <div style="display:flex; gap:6px;">
            <button class="btn-outline-small border-dark font-bold px-md" @click="openTechExcludeModal">
              <i class="fas fa-layer-group" /> 기술 스택 제외
            </button>
            <button class="btn-outline-small border-dark font-bold px-md" @click="$router.push('/history/growth')">
              <i class="fas fa-chart-line" /> 나의 성장 일지 <i class="fas fa-arrow-right text-[10px]" />
            </button>
          </div>
        </div>

        <div class="base-panel p-md mb-md shadow-normal" ref="filterPanelRef">
          <div class="flex-between flex-wrap gap-md">
            <div class="tag-group-small flex-wrap">
              <button 
                v-for="cat in categoryFilters" 
                :key="cat.value"
                class="filter-tag"
                :class="{ 'active': store.filter === cat.value }"
                @click="store.filter = cat.value"
              >
                {{ cat.label }}
              </button>
            </div>

            <div class="flex-align flex-wrap gap-sm">
              <div class="search-box" :class="{ 'focused': showSearchPanel }">
                <i class="fas fa-search search-icon" />
                <input 
                  v-model="store.keyword"
                  @focus="showSearchPanel = true"
                  class="search-input"
                   :placeholder="showSearchPanel ? '키워드 입력' : '활동·추천 기록 검색'"
                />
              </div>

              <div class="relative">
                <button class="btn-outline-small" @click="showSortMenu = !showSortMenu">
                  {{ sortLabel }} <i class="fas fa-chevron-down text-[10px]" />
                </button>
                <div v-if="showSortMenu" class="dropdown-menu">
                  <button 
                    v-for="opt in sortOptions" 
                    :key="opt.value"
                    class="dropdown-item"
                    :class="{ 'text-primary font-bold': store.sort === opt.value }"
                    @click="store.sort = opt.value; showSortMenu = false"
                  >
                    {{ opt.label }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Extended Search Panel -->
          <div v-if="showSearchPanel" class="search-extended mt-md">
            <div class="flex-align flex-wrap gap-md items-end">
              <div class="form-group mb-0">
                <label>년도</label>
                <select v-model="store.selectedYear" @change="store.selectedMonth = ''" class="base-select">
                  <option value="">전체</option>
                  <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                </select>
              </div>
              <div class="form-group mb-0">
                <label>월</label>
                <select v-model="store.selectedMonth" class="base-select">
                  <option value="">전체</option>
                  <option v-for="m in monthOptions" :key="m" :value="m">{{ m }}월</option>
                </select>
              </div>
            </div>
            <button class="btn-text-muted font-bold ml-auto" @click="store.resetQueries">
              조회 초기화
            </button>
          </div>
        </div>

        <div class="base-panel p-0 shadow-heavy overflow-hidden">
          <div class="table-header">
            <span>날짜</span>
            <span>활동</span>
            <span>유형</span>
            <span>태그</span>
            <span>관리</span>
          </div>

          <div v-if="store.filteredItems.length === 0" class="empty-state">
            해당 조건의 기록이 없습니다
          </div>

          <div
            v-for="item in store.paginatedItems"
            :key="item.id"
            class="table-row"
            :class="{ 'is-excluded': item.excluded }"
          >
            <span class="row-date">{{ item.date }}</span>
            <div class="row-main">
              <div class="flex-align gap-sm mb-xs">
                <span class="row-title">{{ item.title }}</span>
                <span class="row-badge">{{ recordKindConfig[item.recordKind].label }}</span>
              </div>
              <p class="row-desc">{{ item.summary }}</p>
            </div>
            <div class="row-type">
              <span class="type-badge"><i :class="typeConfig[item.type].icon" /> {{ typeConfig[item.type].label }}</span>
            </div>
            <div class="row-tags">
              <span v-for="tag in item.tags" :key="tag" class="small-tag"><i :class="getTechIcon(tag)" class="tech-icon" /> {{ tag }}</span>
            </div>
            <div class="row-actions">
              <button 
                class="btn-exclude w-full flex justify-center items-center" 
                :class="{ 'btn-restore': item.excluded }"
                @click="store.toggleExclude(item.id)"
              >
                <template v-if="item.excluded"><i class="fas fa-undo-alt" /> 제외 취소</template>
                <template v-else><i class="fas fa-ban" /> 제외</template>
              </button>
            </div>
          </div>
        </div>

      <div v-if="store.totalPages > 1" class="pagination mt-md">
          <button
            class="page-btn"
            :disabled="store.currentPage === 1"
            @click="store.currentPage--"
          ><i class="fas fa-chevron-left" /></button>
          <button
            v-for="(page, index) in pageNumbers"
            :key="`${page}-${index}`"
            class="page-btn"
            :class="{ 'active': page === store.currentPage, 'ellipsis': page === '...' }"
            :disabled="page === '...'"
            @click="page !== '...' && (store.currentPage = page)"
          >{{ page }}</button>
          <button
            class="page-btn"
            :disabled="store.currentPage === store.totalPages"
            @click="store.currentPage++"
          ><i class="fas fa-chevron-right" /></button>
        </div>

      <p class="text-xs text-muted text-center mt-md font-bold">
          제외된 기록은 이후 추천 분석에 반영되지 않습니다. 필요하면 언제든 다시 복원할 수 있습니다.
        </p>

      </div>
    </main>

    <div v-if="showTechExcludeModal" class="modal-overlay" @click.self="closeTechExcludeModal">
      <div class="tech-modal">
        <div class="modal-header">
          <div class="modal-title">
            <i class="fas fa-layer-group" />
            <span>기술 스택 일괄 제외</span>
            <span v-if="selectedTechStacks.length > 0" class="modal-count">{{ selectedTechStacks.length }}개 선택</span>
          </div>
          <button class="btn-close" @click="closeTechExcludeModal"><i class="fas fa-times" /></button>
        </div>

        <div class="tech-modal-body custom-scroll">
          <p class="tech-modal-desc">
            선택한 기술 스택이 포함된 히스토리 기록을 한 번에 제외합니다. 추천 맥락에서도 빠집니다.
          </p>
          <div class="tech-chip-toolbar">
            <div class="flex-align gap-sm">
              <button class="chip-action-btn" @click="selectAllTechStacks">전체 선택</button>
              <button class="chip-action-btn" @click="clearSelectedTechStacks">선택 초기화</button>
            </div>
            <span class="chip-count-hint">총 {{ availableTechStacks.length }}개</span>
          </div>
          <div class="tech-chip-grid">
            <button
              v-for="tag in availableTechStacks"
              :key="tag"
              class="tech-chip"
              :class="{ selected: selectedTechStacks.includes(tag) }"
              @click="toggleTechStack(tag)"
            >
              <i v-if="selectedTechStacks.includes(tag)" class="fas fa-check chip-check" />
              {{ tag }}
            </button>
          </div>
        </div>

        <div class="tech-modal-footer">
          <button class="btn-outline-small" @click="closeTechExcludeModal">취소</button>
          <button
            class="btn-primary-modal"
            :disabled="selectedTechStacks.length === 0 || isApplyingTechExclusion"
            @click="applyTechExclusion"
          >
            <i class="fas fa-ban" />
            {{ isApplyingTechExclusion ? '적용 중...' : `제외 적용 (${selectedTechStacks.length})` }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import { useHistoryStore } from '@/stores/useHistoryStore'
import { getTechIcon } from '@/utils/techIcons'

const router = useRouter()
const store = useHistoryStore()
const showTechExcludeModal = ref(false)
const selectedTechStacks = ref([])
const isApplyingTechExclusion = ref(false)

onMounted(() => {
  store.loadActivities()
})

const availableTechStacks = computed(() => {
  const seen = new Set()
  const list = []
  store.items
    .flatMap((item) => item.tags || [])
    .forEach((tag) => {
      const value = (tag || '').trim()
      const key = value.toLowerCase()
      if (value && !seen.has(key)) {
        seen.add(key)
        list.push(value)
      }
    })
  return list.sort((a, b) => a.localeCompare(b))
})

const openTechExcludeModal = () => {
  selectedTechStacks.value = [...store.excludedTechStacks]
  showTechExcludeModal.value = true
}

const closeTechExcludeModal = () => {
  showTechExcludeModal.value = false
}

const toggleTechStack = (tag) => {
  if (selectedTechStacks.value.includes(tag)) {
    selectedTechStacks.value = selectedTechStacks.value.filter(t => t !== tag)
  } else {
    selectedTechStacks.value = [...selectedTechStacks.value, tag]
  }
}

const selectAllTechStacks = () => {
  selectedTechStacks.value = [...availableTechStacks.value]
}

const clearSelectedTechStacks = () => {
  selectedTechStacks.value = []
}

const applyTechExclusion = async () => {
  if (!selectedTechStacks.value.length) return

  isApplyingTechExclusion.value = true
  try {
    await store.bulkExcludeByTechStacks(selectedTechStacks.value)
    selectedTechStacks.value = []
    showTechExcludeModal.value = false
  } catch (e) {
    console.error('기술 스택 일괄 제외 실패:', e)
  } finally {
    isApplyingTechExclusion.value = false
  }
}

const categoryFilters = [
  { value: "all", label: "전체" },
  { value: "study", label: "학습" },
  { value: "dev", label: "개발" },
  { value: "career", label: "취준" },
]

const sortOptions = [
  { value: "newest", label: "최신순" },
  { value: "oldest", label: "오래된 순" },
]

const sortLabel = computed(() => {
  return sortOptions.find(opt => opt.value === store.sort)?.label || ''
})

const typeConfig = {
  study: { label: "학습", icon: "fas fa-book-open" },
  dev: { label: "개발", icon: "fas fa-code" },
  blog: { label: "블로그", icon: "fas fa-file-alt" },
  review: { label: "퀴즈", icon: "fas fa-calendar-alt" },
  career: { label: "취준", icon: "fas fa-chart-line" },
}

const recordKindConfig = {
  recommendation: { label: "과거 추천 내역", icon: "fas fa-magic" },
  learning: { label: "학습 기록", icon: "fas fa-book-open" },
  quiz: { label: "퀴즈 진행 결과", icon: "fas fa-brain" },
}

const showSortMenu = ref(false)
const showSearchPanel = ref(false)
const filterPanelRef = ref(null)

const onDocClick = (e) => {
  if (showSearchPanel.value && filterPanelRef.value && !filterPanelRef.value.contains(e.target)) {
    showSearchPanel.value = false
  }
}
onMounted(() => document.addEventListener('mousedown', onDocClick))
onUnmounted(() => document.removeEventListener('mousedown', onDocClick))

const pageNumbers = computed(() => {
  const total = store.totalPages
  const current = store.currentPage
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)

  const pages = []
  pages.push(1)
  if (current > 3) pages.push('...')
  for (let p = Math.max(2, current - 1); p <= Math.min(total - 1, current + 1); p++) {
    pages.push(p)
  }
  if (current < total - 2) pages.push('...')
  pages.push(total)
  return pages
})

const currentDate = new Date()
const currentYear = currentDate.getFullYear()
const currentMonth = currentDate.getMonth() + 1

const yearOptions = Array.from({ length: 6 }, (_, index) => String(currentYear - index))
const monthOptions = computed(() => {
  if (store.selectedYear === String(currentYear)) {
    return Array.from({ length: currentMonth }, (_, index) => String(index + 1))
  }
  return Array.from({ length: 12 }, (_, index) => String(index + 1))
})

</script>

<style scoped>
.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow-y: auto; position: relative; }
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

.page-header { 
  display: flex !important; align-items: center; 
  height: 64px !important; min-height: 64px; max-height: 64px; 
  flex-shrink: 0;
  padding: 0 24px; border-bottom: 1px solid var(--border); 
  background: var(--bg-surface); position: sticky; top: 0; z-index: 10; 
  box-sizing: border-box; 
}
.header-title { 
  font-size: 15px; font-weight: 900; letter-spacing: 0.15em; 
  color: var(--text-primary); display: flex; align-items: center; gap: 12px; 
  text-transform: uppercase;
}
.header-title i { font-size: 18px; width: 24px; text-align: center; }

.content-inner { padding: 48px 32px; width: 100%; }
.max-w-xl { max-width: 1000px; }
.mx-auto { margin-left: auto; margin-right: auto; }

/* Utils */
.flex-align { display: flex; align-items: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-wrap { display: flex; flex-wrap: wrap; }
.items-end { align-items: flex-end; }
.gap-sm { gap: 8px; }
.gap-md { gap: 16px; }
.gap-lg { gap: 32px; }
.mb-xs { margin-bottom: 4px; }
.mb-md { margin-bottom: 24px; }
.mb-lg { margin-bottom: 32px; }
.mt-md { margin-top: 24px; }
.p-md { padding: 20px; }
.p-lg { padding: 28px; }
.p-0 { padding: 0 !important; }
.px-md { padding-left: 20px; padding-right: 20px; }
.mb-0 { margin-bottom: 0 !important; }
.ml-auto { margin-left: auto; }
.relative { position: relative; }
.overflow-hidden { overflow: hidden; }

.font-bold { font-weight: 700; }
.text-xs { font-size: 11px; }
.text-muted { color: var(--text-muted); }
.text-primary { color: var(--text-primary); }
.text-center { text-align: center; }
.text-\[10px\] { font-size: 10px; }
.text-\[8px\] { font-size: 8px; }

.base-panel { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 0; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.shadow-normal { box-shadow: none; border: 1px solid var(--border); }
.shadow-normal:hover { border-color: var(--text-primary); background: var(--bg-hover);}
.shadow-heavy { box-shadow: none; border: 1px solid var(--border); }

/* Header Stats */
.stat-item { display: flex; align-items: center; gap: 12px; }
.stat-lbl { font-size: 13px; color: var(--text-muted); font-weight: 700; text-transform: uppercase; letter-spacing: 0.1em;}
.stat-val { font-size: 18px; font-weight: 800; color: var(--clr-accent-text); letter-spacing: 0.05em;}
.btn-outline-small .fa-chart-line { color: var(--clr-icon-growth); }

.border-dark { border-color: var(--border) !important; color: var(--text-primary) !important; }
.btn-outline-small { background: transparent; color: var(--text-primary); border: 1px solid var(--border); padding: 10px 16px; font-size: 13px; font-weight: 800; display: inline-flex; align-items: center; gap: 8px; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; border-radius: 40px; letter-spacing: 0.05em;}
.btn-outline-small:hover { border-color: var(--text-primary); background: var(--bg-hover); }

/* Filters & Search */
.tag-group-small { display: flex; gap: 8px; }
.filter-tag { padding: 8px 16px; border: 1px solid var(--border); background: transparent; font-size: 13px; font-weight: 800; color: var(--text-muted); cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 40px; letter-spacing: 0.05em;}
.filter-tag:hover { background: var(--bg-hover); color: var(--text-primary); border-color: var(--text-primary);}
.filter-tag.active { background: transparent; color: var(--text-primary); border-color: var(--text-primary); font-weight: 800; }

.search-box { display: flex; align-items: center; gap: 12px; background: transparent; border: 1px solid var(--border); padding: 10px 16px; min-width: 240px; max-width: 320px; border-radius: 40px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.search-box.focused { border-color: var(--text-primary); background: var(--bg-hover); }
.search-icon { font-size: 14px; color: var(--text-muted); flex-shrink: 0; }
.search-input { width: 100%; border: none; background: transparent; font-size: 14px; color: var(--text-primary); outline: none; font-family: inherit; font-weight: 600; }
.search-input::placeholder { color: var(--text-muted); font-weight: 500; }

.dropdown-menu { position: absolute; top: 100%; right: 0; margin-top: 8px; z-index: 10; width: 140px; background: var(--bg-surface); border: 1px solid var(--border); border-radius: 0; padding: 8px 0; box-shadow: none; animation: fadeUp 0.2s cubic-bezier(0.16, 1, 0.3, 1) both; }
@keyframes fadeUp { from { transform: translateY(10px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
.dropdown-item { width: 100%; padding: 10px 16px; text-align: left; background: transparent; border: none; font-size: 13px; color: var(--text-muted); font-weight: 600; cursor: pointer; transition: all 0.2s; font-family: inherit; }
.dropdown-item:hover { background: var(--bg-hover); color: var(--text-primary); }

.search-extended { background: transparent; border: 1px dashed var(--border); padding: 24px; border-radius: 0; display: flex; align-items: flex-end; gap: 24px; }
.form-group { display: flex; flex-direction: column; gap: 8px; }
.form-group label { font-size: 12px; font-weight: 800; color: var(--text-muted); letter-spacing: 0.1em; text-transform: uppercase;}
.base-select { padding: 10px 16px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-size: 13px; font-weight: 700; outline: none; cursor: pointer; font-family: inherit; border-radius: 0; min-width: 120px; transition: all 0.3s; }
.base-select:focus { border-color: var(--text-primary); background: var(--bg-hover);}
.btn-text-muted { background: transparent; border: none; font-size: 13px; color: var(--text-muted); display: flex; align-items: center; gap: 8px; padding: 10px 16px; cursor: pointer; transition: all 0.3s; border-radius: 40px; font-family: inherit; font-weight: 800;}
.btn-text-muted:hover { color: var(--text-primary); background: var(--bg-hover); }

/* Table */
.table-header { display: grid; grid-template-columns: 80px minmax(0, 1fr) 80px 120px 100px; gap: 20px; background: transparent; border-bottom: 1px solid var(--border); padding: 14px 24px; font-size: 11px; font-weight: 800; color: var(--text-muted); letter-spacing: 0.1em; text-transform: uppercase;}
.empty-state { padding: 64px; text-align: center; color: var(--text-muted); font-size: 14px; font-weight: 700; border-bottom: 1px solid var(--border);}

.table-row { display: grid; grid-template-columns: 80px minmax(0, 1fr) 80px 120px 100px; gap: 20px; padding: 20px 24px; border-bottom: 1px solid var(--border); transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); align-items: center; }
.table-row:last-child { border-bottom: none; }
.table-row:hover:not(.is-excluded) { background: var(--bg-hover); }
.table-row.is-excluded { background: transparent; opacity: 0.4; }

.row-date { font-size: 13px; font-weight: 700; color: var(--text-secondary); font-family: 'Space Grotesk', monospace; }
.table-row.is-excluded .row-date { color: var(--text-muted); opacity: 0.7; }

.row-main { min-width: 0; }
.row-title { font-size: 14px; font-weight: 800; color: var(--text-primary); margin-bottom: 6px; display: block; letter-spacing: 0.03em; line-height: 1.4; }
.table-row.is-excluded .row-title { color: var(--text-muted); }

.row-badge { background: transparent; border: 1px solid var(--border); color: var(--text-primary); padding: 4px 8px; font-size: 10px; font-weight: 800; border-radius: 40px; margin-left: 8px;}
.row-desc { font-size: 13px; color: var(--text-muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; margin: 0; font-weight: 600; line-height: 1.6; white-space: pre-line; }

.type-badge { display: inline-flex; align-items: center; gap: 8px; background: transparent; border: 1px solid var(--border); padding: 6px 12px; font-size: 11px; font-weight: 800; color: var(--text-primary); border-radius: 40px; }
.row-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.small-tag { display: flex; align-items: center; gap: 6px; background: transparent; border: 1px solid var(--border); padding: 6px 12px; font-size: 11px; font-weight: 700; color: var(--text-muted); border-radius: 40px; }

.btn-exclude { flex: 1; text-align: center; background: transparent; border: 1px solid var(--border); padding: 8px 12px; font-size: 11px; font-weight: 800; color: var(--text-primary); cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 40px; display: flex; align-items: center; justify-content: center; gap: 6px; font-family: inherit; white-space: nowrap; }
.btn-exclude:hover { background: var(--bg-hover); border-color: var(--text-primary); }
.btn-restore { background: transparent; color: var(--text-muted); border-style: dashed;}
.btn-exclude i { margin-top: 0; }

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 200;
  backdrop-filter: blur(2px);
}

.tech-modal {
  width: min(680px, 100%);
  max-height: min(82vh, 720px);
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: 0 32px 80px rgba(0, 0, 0, 0.28);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: 0.05em;
}

.modal-title i { font-size: 14px; color: var(--text-muted); }

.modal-count {
  font-size: 11px;
  font-weight: 700;
  color: var(--bg-base);
  background: var(--text-primary);
  padding: 3px 10px;
  border-radius: 40px;
}

.btn-close {
  width: 32px;
  height: 32px;
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  border-radius: 40px;
  transition: all 0.2s;
  flex-shrink: 0;
  font-family: inherit;
}

.btn-close:hover { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-hover); }

.tech-modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.tech-modal-desc {
  margin: 0 0 20px;
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 600;
  line-height: 1.6;
}

.tech-chip-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.chip-action-btn {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  padding: 6px 14px;
  cursor: pointer;
  border-radius: 40px;
  transition: all 0.2s;
  font-family: inherit;
}

.chip-action-btn:hover { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-hover); }

.chip-count-hint { font-size: 12px; font-weight: 600; color: var(--text-muted); }

.tech-chip-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tech-chip {
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-muted);
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tech-chip:hover { background: var(--bg-hover); border-color: var(--text-primary); color: var(--text-primary); }

.tech-chip.selected { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }

.chip-check { font-size: 10px; }

.tech-modal-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
  flex-shrink: 0;
}

.btn-primary-modal {
  background: var(--text-primary);
  color: var(--bg-base);
  border: 1px solid var(--text-primary);
  padding: 10px 20px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: inherit;
  border-radius: 40px;
  letter-spacing: 0.05em;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-primary-modal:hover:not(:disabled) { opacity: 0.82; }
.btn-primary-modal:disabled { opacity: 0.3; cursor: default; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 6px; }
.page-btn { min-width: 36px; height: 36px; padding: 0 10px; background: transparent; border: 1px solid var(--border); color: var(--text-muted); font-size: 13px; font-weight: 700; cursor: pointer; transition: all 0.2s; border-radius: 40px; font-family: inherit; display: inline-flex; align-items: center; justify-content: center; }
.page-btn:hover:not(:disabled):not(.ellipsis) { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-hover); }
.page-btn.active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); font-weight: 800; }
.page-btn:disabled { opacity: 0.3; cursor: default; }
.page-btn.ellipsis { border: none; cursor: default; }

@media (max-width: 768px) {
  .table-header, .table-row { grid-template-columns: 1fr; gap: 8px; }
  .row-date { display: none; }
}
</style>
