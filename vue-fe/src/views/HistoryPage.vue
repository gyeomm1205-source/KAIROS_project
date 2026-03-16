<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content custom-scroll">
      <header class="page-header">
        <div class="header-title"><i class="fas fa-history" /> HISTORY & GROWTH</div>
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
          <button class="btn-outline-small border-dark font-bold px-md" @click="$router.push('/history/growth')">
            <i class="fas fa-chart-line" /> 나의 성장 일지 <i class="fas fa-arrow-right text-[10px]" />
          </button>
        </div>

        <div class="brutal-panel p-md mb-md shadow-normal">
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
                  :placeholder="showSearchPanel ? '키워드 입력' : '지난 최근 활동과 추천 기록을 검색할 수 있어요!'"
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
                <select v-model="store.selectedYear" @change="store.selectedMonth = ''" class="brutal-select">
                  <option value="">전체</option>
                  <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
                </select>
              </div>
              <div class="form-group mb-0">
                <label>월</label>
                <select v-model="store.selectedMonth" class="brutal-select">
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

        <div class="brutal-panel p-0 shadow-heavy overflow-hidden">
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
            v-for="item in store.filteredItems" 
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
              <span v-for="tag in item.tags" :key="tag" class="small-tag"><i class="fas fa-tag text-[8px]" /> {{ tag }}</span>
            </div>
            <div class="row-actions flex gap-sm h-full">
              <button 
                class="btn-exclude flex-1 justify-center items-center" 
                :class="{ 'btn-restore': item.excluded }"
                @click="store.toggleExclude(item.id)"
              >
                <template v-if="item.excluded"><i class="fas fa-undo-alt" /> 제외 취소</template>
                <template v-else><i class="fas fa-ban" /> 제외</template>
              </button>
              <button 
                class="btn-delete flex-1"
                @click="store.deleteItem(item.id)"
                title="삭제"
              >
                <i class="fas fa-trash mb-0" />
              </button>
            </div>
          </div>
        </div>

        <p class="text-xs text-muted text-center mt-md font-bold">
          제외된 기록은 이후 추천 분석에 반영되지 않습니다. 필요하면 언제든 다시 복원할 수 있습니다.
        </p>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import { useHistoryStore } from '@/stores/useHistoryStore'

const router = useRouter()
const store = useHistoryStore()

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

.page-header { display: flex; align-items: center; padding: 20px 32px; border-bottom: 2px solid var(--text-primary); background: var(--bg-surface); position: sticky; top: 0; z-index: 10; }
.header-title { font-size: 16px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary); display: flex; align-items: center; gap: 10px; }

.content-inner { padding: 48px 32px; width: 100%; }
.max-w-xl { max-width: 1000px; }
.mx-auto { margin-left: auto; margin-right: auto; }

/* Utils */
.flex-align { display: flex; align-items: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-wrap { display: flex; flex-wrap: wrap; }
.items-end { align-items: flex-end; }
.gap-sm { gap: 8px; }
.gap-md { gap: 12px; }
.gap-lg { gap: 24px; }
.mb-xs { margin-bottom: 4px; }
.mb-md { margin-bottom: 16px; }
.mb-lg { margin-bottom: 24px; }
.mt-md { margin-top: 16px; }
.p-md { padding: 16px; }
.p-lg { padding: 24px; }
.p-0 { padding: 0 !important; }
.px-md { padding-left: 16px; padding-right: 16px; }
.mb-0 { margin-bottom: 0 !important; }
.ml-auto { margin-left: auto; }
.relative { position: relative; }
.overflow-hidden { overflow: hidden; }

.font-bold { font-weight: 800; }
.text-xs { font-size: 11px; }
.text-muted { color: var(--text-muted); }
.text-primary { color: var(--text-primary); }
.text-center { text-align: center; }
.text-\[10px\] { font-size: 10px; }
.text-\[8px\] { font-size: 8px; }

.brutal-panel { background: var(--bg-base); border: 2px solid var(--text-primary); border-radius: 0; transition: transform 0.1s, box-shadow 0.1s; }
.shadow-normal { box-shadow: 4px 4px 0 #6b7280; }
.shadow-normal:hover { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; }
.shadow-heavy { box-shadow: 6px 6px 0 #6b7280; }

/* Header Stats */
.stat-item { display: flex; align-items: center; gap: 8px; }
.stat-lbl { font-size: 12px; color: var(--text-muted); font-weight: 700; }
.stat-val { font-size: 14px; font-weight: 900; color: var(--text-primary); }

.border-dark { border-color: var(--text-primary) !important; color: var(--text-primary) !important; }
.btn-outline-small { background: transparent; color: var(--text-muted); border: 1px solid var(--border); padding: 6px 12px; font-size: 12px; font-weight: 700; display: inline-flex; align-items: center; gap: 6px; cursor: pointer; transition: all 0.1s; font-family: inherit; }
.btn-outline-small:hover { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-surface); }

/* Filters & Search */
.tag-group-small { display: flex; gap: 6px; }
.filter-tag { padding: 6px 12px; border: 1px solid var(--border); background: var(--bg-base); font-size: 12px; font-weight: 700; color: var(--text-muted); cursor: pointer; transition: all 0.1s; border-radius: 20px; }
.filter-tag:hover { background: var(--bg-surface); color: var(--text-primary); }
.filter-tag.active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); font-weight: 800; }

.search-box { display: flex; align-items: center; gap: 8px; background: var(--bg-surface); border: 1px solid var(--border); padding: 8px 12px; min-width: 320px; border-radius: 8px; transition: all 0.1s; }
.search-box.focused { border-color: var(--text-primary); background: var(--bg-base); }
.search-icon { font-size: 12px; color: var(--text-muted); flex-shrink: 0; }
.search-input { width: 100%; border: none; background: transparent; font-size: 13px; color: var(--text-primary); outline: none; font-family: inherit; font-weight: 600; }
.search-input::placeholder { color: var(--text-muted); font-weight: 500; }

.dropdown-menu { position: absolute; top: 100%; right: 0; margin-top: 4px; z-index: 10; width: 120px; background: var(--bg-base); border: 1px solid var(--border); border-radius: 8px; padding: 4px 0; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
.dropdown-item { width: 100%; padding: 6px 12px; text-align: left; background: transparent; border: none; font-size: 12px; color: var(--text-muted); font-weight: 600; cursor: pointer; transition: background 0.1s; font-family: inherit; }
.dropdown-item:hover { background: var(--bg-surface); color: var(--text-primary); }

.search-extended { background: var(--bg-surface); border: 1px solid var(--border); padding: 16px; border-radius: 8px; display: flex; align-items: flex-end; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 4px; }
.form-group label { font-size: 11px; font-weight: 800; color: var(--text-primary); }
.brutal-select { padding: 8px 12px; border: 1px solid var(--border); background: var(--bg-base); color: var(--text-primary); font-size: 12px; font-weight: 700; outline: none; cursor: pointer; font-family: inherit; border-radius: 6px; min-width: 100px; }
.brutal-select:focus { border-color: var(--text-primary); }
.btn-text-muted { background: transparent; border: 1px solid var(--border); font-size: 12px; color: var(--text-muted); display: flex; align-items: center; gap: 4px; padding: 8px 12px; cursor: pointer; transition: color 0.1s; border-radius: 6px; font-family: inherit; }
.btn-text-muted:hover { color: var(--text-primary); background: var(--bg-base); border-color: var(--text-primary); }

/* Table */
.table-header { display: grid; grid-template-columns: 100px minmax(0, 1.3fr) 100px 140px 220px; gap: 12px; background: var(--bg-surface); border-bottom: 2px solid var(--text-primary); padding: 10px 16px; font-size: 11px; font-weight: 800; color: var(--text-muted); }
.empty-state { padding: 48px; text-align: center; color: var(--text-muted); font-size: 13px; font-weight: 700; }

.table-row { display: grid; grid-template-columns: 100px minmax(0, 1.3fr) 100px 140px 220px; gap: 12px; padding: 16px; border-bottom: 1px solid var(--border); transition: background 0.1s; align-items: start; }
.table-row:last-child { border-bottom: none; }
.table-row:hover:not(.is-excluded) { background: var(--bg-surface); }
.table-row.is-excluded { background: var(--bg-surface); opacity: 0.6; }

.row-date { font-size: 12px; font-weight: 700; color: var(--text-muted); font-family: monospace; }
.table-row.is-excluded .row-date { color: var(--text-muted); opacity: 0.7; }

.row-main { min-width: 0; }
.row-title { font-size: 14px; font-weight: 800; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.table-row.is-excluded .row-title { color: var(--text-muted); }

.row-badge { background: var(--bg-surface); border: 1px solid var(--border); color: var(--text-muted); padding: 2px 6px; font-size: 10px; font-weight: 800; border-radius: 12px; }
.row-desc { font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin: 0; font-weight: 600; line-height: 1.5; }

.type-badge { display: inline-flex; align-items: center; gap: 4px; background: var(--bg-surface); border: 1px solid var(--border); padding: 2px 6px; font-size: 10px; font-weight: 800; color: var(--text-muted); border-radius: 4px; }
.row-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.small-tag { display: flex; align-items: center; gap: 4px; background: var(--bg-surface); border: 1px solid var(--border); padding: 2px 6px; font-size: 10px; font-weight: 700; color: var(--text-muted); border-radius: 4px; }

.btn-exclude { flex: 1; text-align: center; background: var(--bg-base); border: 1px solid var(--border); padding: 8px 12px; font-size: 11px; font-weight: 800; color: var(--text-primary); cursor: pointer; transition: all 0.1s; border-radius: 6px; display: flex; align-items: center; justify-content: center; gap: 6px; font-family: inherit; }
.btn-exclude:hover { background: var(--bg-surface); border-color: var(--text-primary); }
.btn-restore { background: var(--bg-surface); color: var(--text-muted); }
.btn-exclude i { margin-top: 0; }
.btn-delete { flex: 1; background: var(--bg-base); border: 1px solid var(--border); padding: 8px 12px; font-size: 11px; color: var(--text-muted); cursor: pointer; transition: all 0.1s; border-radius: 6px; display: flex; align-items: center; justify-content: center; }
.btn-delete:hover { border-color: #ef4444; color: #ef4444; background: #fef2f2; }

@media (max-width: 768px) {
  .table-header, .table-row { grid-template-columns: 1fr; gap: 8px; }
  .row-date { display: none; }
}
</style>