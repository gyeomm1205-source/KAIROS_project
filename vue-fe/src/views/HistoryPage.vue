<template>
  <div class="app-layout">
    
    <AppSidebar />

    <main class="main-content">
      <div class="history-page-container">
        <header class="page-header">
          <h2>학습 히스토리</h2>
          <p>과거 활동 기록과 추천 내역을 확인하고, 원치 않는 추천 맥락은 제외할 수 있습니다.</p>
        </header>

        <div class="history-list-wrapper">
          <div v-if="historyList.length === 0" class="empty-state">
            <i class="fas fa-history"></i>
            <p>아직 기록된 히스토리가 없습니다.</p>
          </div>
          
          <ul v-else class="history-list">
            <li 
              v-for="item in sortedHistoryList" 
              :key="item.id" 
              class="history-item"
              :class="{ 'is-excluded': item.is_excluded }"
            >
              <div class="item-icon" :class="getIconClass(item.type)">
                <i :class="getIconForType(item.type)"></i>
              </div>
              <div class="item-details">
                <div class="item-header">
                  <span class="item-type">{{ item.type }}</span>
                  <span class="item-date">{{ formatDate(item.activity_date) }}</span>
                </div>
                <p class="item-content">{{ item.content }}</p>
                <p v-if="item.is_excluded" class="excluded-badge">
                  <i class="fas fa-eye-slash"></i> 추후 추천 문맥에서 제외되었습니다.
                </p>
              </div>
              <div class="item-actions">
                <button 
                  class="btn-toggle" 
                  :class="item.is_excluded ? 'btn-restore' : 'btn-exclude'"
                  @click="toggleExclude(item)"
                  :title="item.is_excluded ? '추천 맥락에 다시 포함하기' : '추천 맥락에서 삭제(제외)하기'"
                >
                  <i :class="item.is_excluded ? 'fas fa-undo' : 'fas fa-trash-alt'"></i>
                  {{ item.is_excluded ? '복구' : '제외' }}
                </button>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </main>
    
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import AppSidebar from '@/components/AppSidebar.vue'

// DB의 user_activity 테이블을 조회한 결과라고 가정하는 더미 데이터
const historyList = ref([
  {
    id: 1,
    type: 'Velog',
    activity_date: '2026-03-09T14:30:00',
    content: 'Vue3 Composition API와 Pinia를 활용한 상태 관리 글 작성',
    is_excluded: false
  },
  {
    id: 2,
    type: 'GitHub',
    activity_date: '2026-03-08T11:20:00',
    content: 'main 브랜치에 리뷰어 피드백 반영 및 컴포넌트 모듈화 커밋',
    is_excluded: false
  },
  {
    id: 3,
    type: '추천 레퍼런스',
    activity_date: '2026-03-07T09:15:00',
    content: 'React Server Components 개념 학습 레퍼런스 열람',
    is_excluded: true // 과거에 제외 처리된 샘플
  },
  {
    id: 4,
    type: '퀴즈',
    activity_date: '2026-03-05T16:45:00',
    content: '비동기 처리(Promise, async/await) 퀴즈 완료 (정답률 80%)',
    is_excluded: false
  },
  {
    id: 5,
    type: '추천 커리큘럼',
    activity_date: '2026-03-04T10:00:00',
    content: '프론트엔드 성능 최적화(Lighthouse, Lazy Loading) 학습 플랜 제안됨',
    is_excluded: false
  }
])

// activity_date 기준 내림차순 정렬 (최신순)
const sortedHistoryList = computed(() => {
  return [...historyList.value].sort((a, b) => new Date(b.activity_date) - new Date(a.activity_date))
})

// 제외/복구 토글 함수 (Soft Delete 개념)
const toggleExclude = (item) => {
  // 실제 연동 시 API로 UPDATE user_activity SET is_excluded = !item.is_excluded WHERE id = item.id 날려주면 됩니다.
  item.is_excluded = !item.is_excluded
}

// 날짜 포맷팅 유틸
const formatDate = (dateString) => {
  const date = new Date(dateString)
  return `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 타입별 아이콘 매핑
const getIconForType = (type) => {
  switch(type) {
    case 'Velog': return 'fab fa-vimeo-v' // velog 아이콘과 유사한 v 
    case 'GitHub': return 'fab fa-github'
    case '퀴즈': return 'fas fa-question-circle'
    case '추천 레퍼런스': return 'fas fa-book'
    case '추천 커리큘럼': return 'fas fa-map-signs'
    default: return 'fas fa-history'
  }
}

// 타입별 배경색 클래스 매핑
const getIconClass = (type) => {
  switch(type) {
    case 'Velog': return 'icon-velog'
    case 'GitHub': return 'icon-github'
    case '퀴즈': return 'icon-quiz'
    case '추천 레퍼런스': return 'icon-reference'
    case '추천 커리큘럼': return 'icon-curriculum'
    default: return 'icon-default'
  }
}
</script>

<style scoped>
/* 기본 레이아웃 (MainPage.vue와 동일) */
.app-layout {
  display: flex; 
  width: 100%; 
  height: 100vh; 
  overflow: hidden;
  background: var(--bg-base); 
  color: var(--text-primary);
  font-family: 'Escoredream', system-ui, sans-serif;
}

.main-content {
  flex: 1; 
  display: flex; 
  flex-direction: column; 
  overflow-y: auto;
  background: var(--bg-surface);
}

.history-page-container {
  padding: 32px 40px;
  max-width: 960px;
  margin: 0 auto;
  width: 100%;
}

.page-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}

.page-header h2 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 8px;
}

.page-header p {
  color: var(--text-muted);
  font-size: 14px;
}

.history-list-wrapper {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
  min-height: 400px;
  padding: 24px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 350px;
  color: var(--text-faint);
}
.empty-state i {
  font-size: 32px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.history-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-item {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 20px 24px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  transition: all 0.2s ease;
}

.history-item:hover {
  border-color: rgba(129, 140, 248, 0.4);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

/* Soft Delete(제외됨) 상태일 때 비활성화 처리 */
.history-item.is-excluded {
  background: var(--bg-base); /* 요소가 좀 더 묻히는 효과 */
  opacity: 0.5;
  filter: grayscale(100%);
}
.history-item.is-excluded:hover {
  opacity: 0.7;
  border-color: var(--border); /* 호버 시에도 파란색 테두리 생기지 않도록 방지 */
}

.item-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  flex-shrink: 0;
}

/* 아이콘별 브랜드/포인트 색상 */
.icon-velog { background: #20c997; } /* velog 민트색 느낌 */
.icon-github { background: #24292e; } /* github 검정 */
.icon-quiz { background: #f59f00; } /* 퀴즈 주황/노랑 */
.icon-reference { background: #3b82f6; } /* 레퍼런스 파란색 */
.icon-curriculum { background: #8b5cf6; } /* 커리큘럼 보라색 */
.icon-default { background: #868e96; }

.item-details {
  flex: 1;
}

.item-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.item-type {
  font-size: 13px;
  font-weight: 700;
  color: #818cf8;
  background: rgba(129, 140, 248, 0.1);
  padding: 4px 8px;
  border-radius: 6px;
}

.item-date {
  font-size: 13px;
  color: var(--text-faint);
}

.item-content {
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.5;
  margin-bottom: 0;
  word-break: keep-all;
}

.excluded-badge {
  margin-top: 10px;
  font-size: 12px;
  font-weight: 600;
  color: #ef4444; /* 빨간색 텍스트 경고 */
  display: flex;
  align-items: center;
  gap: 4px;
}

.item-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
}

.btn-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--border);
  background: transparent;
  font-family: inherit;
}

/* 제외 버튼 (기본 상태) */
.btn-exclude {
  color: var(--text-muted);
}
.btn-exclude:hover {
  color: #ef4444;
  border-color: #ef4444;
  background: rgba(239, 68, 68, 0.05); /* 옅은 빨강 배경 */
}

/* 복구 버튼 (제외된 상태) */
.btn-restore {
  color: #3b82f6; /* 파란색 활성화 유도 */
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.05);
}
.btn-restore:hover {
  background: #3b82f6;
  color: #fff;
}
</style>