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
.page-root { min-height: 100vh; background: #fafafa; font-family: 'Escoredream', sans-serif; }
.top-nav { display: flex; justify-content: space-between; align-items: center; padding: 16px 24px; background: #fff; border-bottom: 1px solid #eaeaea; }
.btn-back, .btn-more { background: none; border: none; font-size: 14px; font-weight: 600; cursor: pointer; color: #333; font-family: inherit; }
.btn-back { display: flex; align-items: center; gap: 8px; }

.content { max-width: 800px; margin: 0 auto; padding: 40px 20px; }
.quiz-title { font-size: 18px; font-weight: 700; color: #111; margin-bottom: 24px; }

.progress-box { background: #fff; border: 1px solid #eaeaea; border-radius: 10px; padding: 16px 20px; margin-bottom: 24px; }
.progress-text { display: flex; justify-content: space-between; font-size: 12px; font-weight: 600; color: #666; margin-bottom: 10px; }
.progress-bar-bg { width: 100%; height: 6px; background: #eee; border-radius: 3px; overflow: hidden; }
.progress-bar-fill { height: 100%; background: #111; border-radius: 3px; }

.question-box { background: #fff; border: 1px solid #eaeaea; border-radius: 12px; padding: 32px; margin-bottom: 32px; }
.question-text { font-size: 16px; font-weight: 700; color: #222; margin-bottom: 24px; }

.options { display: flex; flex-direction: column; gap: 12px; }
.option-label { display: flex; align-items: center; gap: 12px; border: 1px solid #eaeaea; padding: 16px 20px; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
.option-label:hover { background: #fafafa; }
.option-label.selected { border-color: #111; background: #fafafa; }
.radio-circle { width: 16px; height: 16px; border: 1px solid #ccc; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.option-label.selected .radio-circle { border-color: #111; }
.dot { width: 8px; height: 8px; background: #111; border-radius: 50%; }
.option-label span { font-size: 14px; color: #333; }

.bottom-actions { display: flex; justify-content: space-between; align-items: center; }
.right-btns { display: flex; gap: 12px; }
button { padding: 12px 20px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 8px; font-family: inherit; transition: opacity 0.2s; }
.btn-outline { background: #fff; border: 1px solid #ddd; color: #333; }
.btn-dark { background: #111; border: none; color: #fff; }
.btn-submit { color: #666; }
button:hover { opacity: 0.8; }
</style>