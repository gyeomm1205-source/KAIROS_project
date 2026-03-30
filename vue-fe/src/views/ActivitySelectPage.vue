<template>
  <div class="activity-root">
    <!-- 헤더 -->
    <header class="activity-header">
      <button class="btn-back" @click="$router.push('/curriculum-suggest')">
        <i class="fas fa-arrow-left" /> 활동 선택
      </button>
    </header>

    <main class="activity-body">
      <h1 class="activity-title">이런 활동도 도와드릴 수 있어요</h1>

      <!-- 퀴즈 풀어보기 -->
      <section class="activity-section">
        <h2 class="section-label">퀴즈 풀어보기</h2>
        <div class="card-grid">
          <div
            v-for="quiz in quizzes"
            :key="quiz.title"
            class="activity-card"
            :class="{ selected: selectedActivity === quiz.title }"
            @click="selectedActivity = quiz.title"
          >
            <div class="card-icon">{{ quiz.icon }}</div>
            <div>
              <div class="card-title">{{ quiz.title }}</div>
              <div class="card-desc">{{ quiz.desc }}</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 레퍼런스 추천 -->
      <section class="activity-section">
        <h2 class="section-label">레퍼런스 추천</h2>
        <div
          class="activity-card ref-card"
          :class="{ selected: selectedActivity === 'ref' }"
          @click="selectedActivity = 'ref'"
        >
          <div class="card-icon">💡</div>
          <div class="ref-content">
            <div class="card-title">맞춤 레퍼런스 추천</div>
            <div class="card-desc">사용자님의 활동 기록을 바탕으로 추천해드려요</div>
            <div class="ref-tags-label">관심 분야</div>
            <div class="ref-tags">
              <span v-for="tag in refTags" :key="tag" class="ref-tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </section>

      <button class="btn-start" @click="handleStart">선택한 활동 시작하기</button>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const selectedActivity = ref(null)

const quizzes = [
  {
    icon: '🕐',
    title: '지난 달 블로그 기반 복습 퀴즈',
    desc: '작성한 블로그 내용을 바탕으로 복습할 수 있는 퀴즈입니다',
  },
  {
    icon: '💼',
    title: '다음 주 면접 대비 CS 퀴즈',
    desc: '면접에서 자주 나오는 컴퓨터 과학 문제들을 연습해보세요',
  },
]

const refTags = ['금융', '인공지능', '통계']

function handleStart() {
  if (selectedActivity.value) {
    router.push('/quiz-activity')
  }
}
</script>

<style scoped>
.activity-root {
  min-height: 100vh;
  background: var(--bg-base);
  font-family: 'Escoredream', system-ui, sans-serif;
  color: var(--text-primary);
  display: flex;
  flex-direction: column;
}

.activity-header {
  height: 56px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  padding: 0 24px;
  background: var(--bg-surface);
  flex-shrink: 0;
}
.btn-back {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'Escoredream', sans-serif;
}
.btn-back:hover { opacity: 0.7; }

.activity-body {
  flex: 1;
  max-width: 720px;
  width: 100%;
  margin: 0 auto;
  padding: 60px 24px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.activity-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  text-align: center;
  margin-bottom: 48px;
}

.activity-section {
  width: 100%;
  margin-bottom: 36px;
}
.section-label {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.card-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.activity-card {
  padding: 20px 18px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--bg-surface);
  display: flex;
  align-items: flex-start;
  gap: 14px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.activity-card:hover {
  border-color: rgba(129, 140, 248, 0.4);
}
.activity-card.selected {
  border-color: #818cf8;
  background: rgba(129, 140, 248, 0.06);
}
.ref-card {
  grid-column: 1 / -1;
  display: flex;
  align-items: flex-start;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--bg-elevated);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.card-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.card-desc {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.6;
}

.ref-content { flex: 1; }
.ref-tags-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 12px;
  margin-bottom: 8px;
}
.ref-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.ref-tag {
  padding: 5px 14px;
  border: 1px solid var(--border);
  border-radius: 999px;
  font-size: 12px;
  color: var(--text-muted);
  background: var(--bg-elevated);
}

.btn-start {
  margin-top: 8px;
  padding: 15px 40px;
  border: none;
  border-radius: 14px;
  background: #111;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: 'Escoredream', sans-serif;
  transition: opacity 0.15s;
}
.btn-start:hover { opacity: 0.85; }
</style>
