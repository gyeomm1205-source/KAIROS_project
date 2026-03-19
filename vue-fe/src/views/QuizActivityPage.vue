<template>
  <div class="page-root">
    <header class="top-nav">
      <button class="btn-back" @click="$router.push('/activity-select')"><i class="fas fa-arrow-left" /> 활동 진행 중</button>
      <button class="btn-more"><i class="fas fa-ellipsis-v" /></button>
    </header>

    <main class="content">
      <h2 class="quiz-title">지난 달 작성한 블로그 내용을 기반으로 한 복습 퀴즈</h2>

      <div class="progress-box">
        <div class="progress-text">
          <span>문제 2 / 5</span>
          <span>40% 완료</span>
        </div>
        <div class="progress-bar-bg">
          <div class="progress-bar-fill" style="width: 40%" />
        </div>
      </div>

      <div class="question-box">
        <h3 class="question-text">REST API에서 GET과 POST의 주요 차이점은 무엇인가요?</h3>
        
        <div class="options">
          <label class="option-label" :class="{ selected: answer === 1 }">
            <input type="radio" v-model="answer" :value="1" hidden />
            <div class="radio-circle"><div v-if="answer === 1" class="dot" /></div>
            <span>GET은 데이터를 조회할 때 사용된다</span>
          </label>
          <label class="option-label" :class="{ selected: answer === 2 }">
            <input type="radio" v-model="answer" :value="2" hidden />
            <div class="radio-circle"><div v-if="answer === 2" class="dot" /></div>
            <span>POST는 데이터를 생성할 때 사용된다</span>
          </label>
          <label class="option-label" :class="{ selected: answer === 3 }">
            <input type="radio" v-model="answer" :value="3" hidden />
            <div class="radio-circle"><div v-if="answer === 3" class="dot" /></div>
            <span>GET은 요청 본문을 포함할 수 있다</span>
          </label>
          <label class="option-label" :class="{ selected: answer === 4 }">
            <input type="radio" v-model="answer" :value="4" hidden />
            <div class="radio-circle"><div v-if="answer === 4" class="dot" /></div>
            <span>POST는 캐싱이 기본 동작이다</span>
          </label>
        </div>
      </div>

      <div class="bottom-actions">
        <button class="btn-outline"><i class="fas fa-arrow-left" /> 이전</button>
        <div class="right-btns">
          <button class="btn-dark">다음 <i class="fas fa-arrow-right" /></button>
          <button class="btn-outline btn-submit" @click="$router.push('/calendar')">제출</button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const answer = ref(null)
</script>

<style scoped>
.page-root { min-height: 100vh; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; display: flex; flex-direction: column; }
.top-nav { display: flex; justify-content: space-between; align-items: center; padding: 16px 24px; background: transparent; border-bottom: 1px solid var(--border); }
.btn-back, .btn-more { background: transparent; border: none; font-size: 14px; font-weight: 700; cursor: pointer; color: var(--text-muted); font-family: inherit; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.btn-back { display: flex; align-items: center; gap: 8px; }
.btn-back:hover, .btn-more:hover { color: var(--text-primary); }
.btn-back:hover { transform: translateX(-4px); }

.content { flex: 1; max-width: 800px; margin: 0 auto; padding: 48px 24px; width: 100%; }
.quiz-title { font-size: 20px; font-weight: 800; color: var(--text-primary); margin-bottom: 32px; letter-spacing: 0.05em; }

.progress-box { background: transparent; border: 1px solid var(--border); border-radius: 0; padding: 24px; margin-bottom: 24px; }
.progress-text { display: flex; justify-content: space-between; font-size: 13px; font-weight: 700; color: var(--text-muted); margin-bottom: 12px; }
.progress-bar-bg { width: 100%; height: 4px; background: transparent; border: 1px solid var(--border); border-radius: 4px; overflow: hidden; }
.progress-bar-fill { height: 100%; background: var(--text-primary); transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1); }

.question-box { background: transparent; border: 1px solid var(--border); border-radius: 0; padding: 32px; margin-bottom: 32px; }
.question-text { font-size: 18px; font-weight: 800; color: var(--text-primary); margin-bottom: 24px; line-height: 1.6; letter-spacing: 0.05em; }

.options { display: flex; flex-direction: column; gap: 16px; }
.option-label { display: flex; align-items: center; gap: 16px; border: 1px solid var(--border); padding: 20px; border-radius: 0; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); background: transparent; }
.option-label:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.option-label.selected { border-color: var(--text-primary); background: transparent; }
.radio-circle { width: 18px; height: 18px; border: 1px solid var(--border); border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.option-label.selected .radio-circle { border-color: var(--text-primary); }
.dot { width: 8px; height: 8px; background: var(--text-primary); border-radius: 50%; }
.option-label span { font-size: 14px; font-weight: 600; color: var(--text-primary); line-height: 1.5; }

.bottom-actions { display: flex; justify-content: space-between; align-items: center; }
.right-btns { display: flex; gap: 12px; }
button { padding: 14px 24px; border-radius: 40px; font-size: 13px; font-weight: 800; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; font-family: inherit; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); letter-spacing: 0.05em; }
.btn-outline { background: transparent; border: 1px solid var(--border); color: var(--text-primary); }
.btn-outline:hover { background: var(--bg-hover); border-color: var(--text-primary); }
.btn-dark { background: transparent; border: 1px solid var(--text-primary); color: var(--text-primary); }
.btn-dark:hover { background: var(--text-primary); color: var(--bg-base); }
.btn-submit { color: var(--text-muted); }
.btn-submit:hover { color: var(--text-primary); }
</style>