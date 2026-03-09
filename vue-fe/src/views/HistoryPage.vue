<template>
  <div class="app-layout">
    
    <AppSidebar />

    <main class="main-content">
      <div class="history-page-container">
        <header class="page-header">
          <h2>학습 히스토리</h2>
          <p>과거 학습 기록과 프롬프트 내역을 확인하세요.</p>
        </header>

        <div class="history-content">
          <div class="empty-state">
            <i class="fas fa-history"></i>
            <p>아직 기록된 히스토리가 없습니다.</p>
          </div>
          </div>
      </div>
    </main>
    
  </div>
</template>

<script setup>
// 사이드바 컴포넌트 불러오기 (필수)
import AppSidebar from '@/components/AppSidebar.vue'

// 필요한 데이터나 로직을 여기에 작성하세요
import { ref } from 'vue'

const historyList = ref([])
</script>

<style scoped>
/* MainPage.vue와 동일한 레이아웃 스타일 적용 */
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
  overflow-y: auto; /* 내용이 길어지면 스크롤 생성 */
  background: var(--bg-surface);
}

/* 히스토리 페이지 전용 스타일 */
.history-page-container {
  padding: 32px 40px;
  max-width: 1200px;
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

.history-content {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: 12px;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-state {
  text-align: center;
  color: var(--text-faint);
}

.empty-state i {
  font-size: 32px;
  margin-bottom: 12px;
  opacity: 0.5;
}
</style>