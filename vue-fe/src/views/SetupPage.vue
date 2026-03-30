<template>
  <div class="auth-root">
    <div class="auth-card">

      <!-- Progress -->
      <div class="progress-wrap">
        <div class="progress-bars">
          <div v-for="i in 4" :key="i" class="progress-bar" :class="{ active: i <= 3 }" />
        </div>
        <span class="progress-label">3/4 단계</span>
      </div>

      <div class="auth-logo-wrap">
        <h2>맞춤 설정</h2>
        <p>당신에게 최적화된 경험을 제공하기 위해<br>몇 가지 정보가 필요해요</p>
      </div>

      <!-- 직업 선택 -->
      <div class="form-section">
        <label class="form-label">직업 선택</label>
        <label
          v-for="job in jobs" :key="job"
          class="radio-option"
          :class="{ selected: selectedJob === job }"
          @click="selectedJob = job"
        >
          <div class="radio-circle" :class="{ checked: selectedJob === job }">
            <div v-if="selectedJob === job" class="radio-dot" />
          </div>
          <span>{{ job }}</span>
        </label>
      </div>

      <!-- 기술 스택 -->
      <div class="form-section">
        <label class="form-label">기술 스택 입력</label>
        <div class="stack-input-row">
          <input
            v-model="stackInput"
            type="text"
            class="stack-input"
            placeholder="예: React, Node.js, Python"
            @keydown.enter.prevent="addStack"
          />
          <button class="btn-add" @click="addStack">
            <i class="fas fa-plus" />
          </button>
        </div>
        <div class="stack-pills">
          <span v-for="s in stacks" :key="s" class="stack-pill">
            {{ s }}
            <button @click="removeStack(s)"><i class="fas fa-times" /></button>
          </span>
        </div>
      </div>

      <!-- 페르소나 -->
      <div class="form-section">
        <label class="form-label">에이전트 페르소나 선택</label>
        <div class="select-wrap">
          <select v-model="persona" class="field-select">
            <option value="">선택해주세요</option>
            <option>친절한 멘토</option>
            <option>엄격한 코치</option>
            <option>전문 컨설턴트</option>
          </select>
          <i class="fas fa-chevron-down select-icon" />
        </div>
      </div>

      <button class="btn-primary" @click="$router.push('/loading')">다음</button>
      <div style="text-align:center; margin-top: 14px;">
        <button class="btn-skip" @click="$router.push('/loading')">건너뛰기</button>
      </div>
    </div>

    <p class="bottom-hint">입력하신 정보는 언제든지 설정에서 변경할 수 있습니다</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const jobs = ['취준생', '개발자', '개발자 희망 대학생']
const selectedJob = ref('취준생')
const stacks = ref(['React', 'TypeScript'])
const stackInput = ref('')
const persona = ref('')

function addStack() {
  const v = stackInput.value.trim()
  if (v && !stacks.value.includes(v)) stacks.value.push(v)
  stackInput.value = ''
}
function removeStack(s) { stacks.value = stacks.value.filter(x => x !== s) }
</script>

<style scoped>
.auth-root {
  min-height: 100vh; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  background: var(--bg-base); padding: 24px;
  font-family: 'Escoredream', system-ui, sans-serif;
}
.auth-card {
  width: 100%; max-width: 440px;
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 20px; padding: 40px 36px;
  animation: fadeUp 0.3s ease both;
}
@keyframes fadeUp { from { opacity:0; transform:translateY(10px); } to { opacity:1; transform:translateY(0); } }

.progress-wrap { display: flex; flex-direction: column; align-items: center; margin-bottom: 28px; gap: 6px; }
.progress-bars { display: flex; gap: 6px; }
.progress-bar { width: 32px; height: 4px; border-radius: 2px; background: var(--border); transition: background 0.3s; }
.progress-bar.active { background: #818cf8; }
.progress-label { font-size: 11px; color: var(--text-faint); }

.auth-logo-wrap { text-align: center; margin-bottom: 28px; }
.auth-logo-wrap h2 { font-size: 24px; font-weight: 800; color: var(--text-primary); }
.auth-logo-wrap p  { font-size: 13px; color: var(--text-muted); margin-top: 8px; line-height: 1.6; }

.form-section { margin-bottom: 22px; }
.form-label { font-size: 13px; font-weight: 600; color: var(--text-secondary); display: block; margin-bottom: 10px; }

.radio-option {
  display: flex; align-items: center; gap: 12px;
  padding: 13px 16px; border-radius: 12px; margin-bottom: 8px;
  border: 1.5px solid var(--border); background: var(--bg-elevated);
  cursor: pointer; transition: all 0.15s;
}
.radio-option.selected { border-color: #818cf8; background: rgba(129,140,248,0.08); }
.radio-option span { font-size: 14px; font-weight: 500; color: var(--text-secondary); }
.radio-circle {
  width: 18px; height: 18px; border-radius: 50%;
  border: 2px solid var(--border); display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; transition: border-color 0.15s;
}
.radio-circle.checked { border-color: #818cf8; }
.radio-dot { width: 8px; height: 8px; border-radius: 50%; background: #818cf8; }

.stack-input-row { display: flex; gap: 8px; margin-bottom: 10px; }
.stack-input {
  flex: 1; padding: 10px 14px; border-radius: 10px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-primary); font-size: 13px; outline: none;
  font-family: 'Escoredream', sans-serif; transition: border-color 0.15s;
}
.stack-input:focus { border-color: #818cf8; }
.stack-input::placeholder { color: var(--text-faint); }
.btn-add {
  width: 40px; height: 40px; border-radius: 10px; border: none; cursor: pointer;
  background: rgba(129,140,248,0.15); color: #818cf8;
  display: flex; align-items: center; justify-content: center; transition: all 0.15s;
}
.btn-add:hover { background: rgba(129,140,248,0.25); }
.stack-pills { display: flex; flex-wrap: wrap; gap: 8px; }
.stack-pill {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 5px 10px 5px 12px; border-radius: 8px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-secondary); font-size: 12px; font-weight: 500;
}
.stack-pill button {
  background: none; border: none; cursor: pointer; color: var(--text-faint);
  font-size: 10px; display: flex; align-items: center; padding: 0;
  transition: color 0.15s;
}
.stack-pill button:hover { color: #ef4444; }

.select-wrap { position: relative; }
.field-select {
  width: 100%; padding: 12px 40px 12px 14px; border-radius: 10px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-primary); font-size: 14px; outline: none;
  appearance: none; cursor: pointer; font-family: 'Escoredream', sans-serif;
  transition: border-color 0.15s;
}
.field-select:focus { border-color: #818cf8; }
.select-icon {
  position: absolute; right: 14px; top: 50%; transform: translateY(-50%);
  color: var(--text-faint); font-size: 11px; pointer-events: none;
}

.btn-primary {
  width: 100%; padding: 14px; border-radius: 12px; border: none; cursor: pointer;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  color: #fff; font-weight: 700; font-size: 15px;
  transition: opacity 0.15s; font-family: 'Escoredream', sans-serif;
}
.btn-primary:hover { opacity: 0.87; }
.btn-skip {
  background: none; border: none; color: var(--text-faint); font-size: 13px;
  cursor: pointer; text-decoration: underline;
  font-family: 'Escoredream', sans-serif;
}

.bottom-hint {
  font-size: 12px; color: var(--text-faint); margin-top: 20px;
}
</style>
