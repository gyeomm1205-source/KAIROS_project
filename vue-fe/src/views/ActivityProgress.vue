<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content custom-scroll">
      <header class="page-header">
        <div class="header-title"><i class="fas fa-tasks" /> ACTIVITY PROGRESS</div>
      </header>

      <div class="content-inner max-w-xl mx-auto">
        <!-- Top bar -->
        <div class="flex-between mb-lg">
          <button class="btn-back m-0" @click="showWarning = true">
            <i class="fas fa-arrow-left" /> 뒤로
          </button>
          <button class="btn-close" @click="showWarning = true">
            <i class="fas fa-times" />
          </button>
        </div>

        <div class="max-w-md mx-auto">
          <!-- Activity Title -->
          <div class="text-center mb-lg">
            <h2 class="form-title m-0">React 상태관리 복습 퀴즈</h2>
            <p class="form-sub m-0 mt-xs">최근 학습한 상태관리 패턴을 점검합니다</p>
          </div>

          <!-- Progress -->
          <div class="mb-lg">
            <div class="flex-between mb-xs">
              <span class="text-xs text-muted font-bold">진행률</span>
              <span class="text-xs text-primary font-bold">{{ progress }}%</span>
            </div>
            <div class="track-bg">
              <div class="track-fill" :style="{ width: `${progress}%` }"></div>
            </div>
            <div class="flex-between mt-sm text-[11px] text-muted font-bold">
              <span>{{ completed ? totalQ : currentQ }} / {{ totalQ }} 문제</span>
            </div>
          </div>

          <!-- Quiz Content -->
        <div v-if="!completed" class="base-panel p-lg">
            <div class="brutal-tag inline-tag mb-md text-xs">문제 {{ currentQ + 1 }}</div>
            <h3 class="quiz-q">{{ question.question }}</h3>

            <div class="options-list mb-lg">
              <button
                v-for="(opt, i) in question.options"
                :key="i"
                class="option-btn"
                :class="{ 'selected': selected === i }"
                @click="selected = i"
              >
                <div class="radio-circle">
                  <div class="radio-inner" v-if="selected === i"></div>
                </div>
                <span>{{ opt }}</span>
              </button>
            </div>

            <button
              class="btn-primary-block"
              :disabled="selected === null"
              @click="handleSubmit"
            >
              {{ currentQ < totalQ - 1 ? "다음 문제" : "제출하기" }}
            </button>
          </div>

          <!-- Completion -->
          <div v-else class="base-panel p-lg text-center">
            <div class="score-circle mx-auto mb-md flex-center">
              {{ correctCount }}/{{ totalQ }}
            </div>
            <h3 class="form-title mb-sm">퀴즈를 완료했어요!</h3>
            <p class="text-sm text-muted font-bold mb-lg">
              {{ totalQ }}문제 중 {{ correctCount }}문제를 맞혔습니다
            </p>
            <button class="btn-primary-block" @click="handleComplete">
              완료하고 캘린더에 반영하기
            </button>
          </div>
        </div>
      </div>
    </main>

    <!-- Warning Modal -->
    <div v-if="showWarning" class="modal-overlay" @click.self="showWarning = false">
      <div class="base-modal max-w-sm">
        <div class="modal-header border-none pb-0">
          <h3><i class="fas fa-exclamation-triangle text-muted" /> 진행 중인 타이머 종료</h3>
          <button class="btn-close" @click="showWarning = false"><i class="fas fa-times"/></button>
        </div>
        <div class="p-lg pt-sm">
          <p class="text-sm font-bold text-muted mb-lg">지금 나가시면 진행 중이던 내역이 저장되지 않습니다. 정말 나가시겠어요?</p>
          <div class="flex-align gap-sm justify-end">
            <button class="btn-outline flex-1" @click="showWarning = false">취소</button>
            <button class="btn-primary flex-1" @click="$router.push('/recommend')">나가기</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Calendar Upload Modal -->
    <div v-if="showUpload" class="modal-overlay">
      <div class="base-modal max-w-sm text-center">
        <div class="p-lg">
          <div class="icon-box-muted mx-auto mb-md"><i class="fas fa-cloud-upload-alt" /></div>
          <h3 class="font-bold text-lg mb-sm">캘린더에 연동 완료!</h3>
          <p class="text-sm font-bold text-muted mb-lg">지금 완료한 활동이 캘린더에 기록되었습니다.</p>
          <button class="btn-primary-block" @click="goToCalendar">캘린더 확인하기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'

const router = useRouter()

const quizQuestions = [
  { question: "React에서 상태를 여러 컴포넌트가 공유해야 할 때, 가장 적절한 패턴은?", options: ["모든 상태를 최상위 컴포넌트에 정의한다", "Context API 또는 전역 상태 관리 라이브러리를 사용한다", "각 컴포넌트에서 동일한 상태를 따로 관리한다", "LocalStorage에 상태를 저장하고 각자 읽는다"], correct: 1 },
  { question: "Zustand에서 상태를 구독하는 컴포넌트의 리렌더링을 최소화하려면?", options: ["useStore 훅에서 셀렉터를 사용해 필요한 상태만 구독한다", "전체 스토어를 항상 구독한다", "React.memo로 모든 컴포넌트를 감싼다", "상태가 변경될 때마다 forceUpdate를 호출한다"], correct: 0 },
  { question: "TypeScript에서 제네릭의 주된 목적은?", options: ["코드 실행 속도를 향상시킨다", "타입 안전성을 유지하면서 재사용 가능한 코드를 작성한다", "JavaScript와의 호환성을 위해 사용한다", "런타임에 타입을 검사한다"], correct: 1 },
]

const currentQ = ref(0)
const selected = ref(null)
const answers = ref([])
const showWarning = ref(false)
const showUpload = ref(false)
const completed = ref(false)

const totalQ = quizQuestions.length
const progress = computed(() => Math.round(((currentQ.value + (completed.value ? 1 : 0)) / totalQ) * 100))
const question = computed(() => quizQuestions[currentQ.value])

const correctCount = computed(() => {
  return answers.value.filter((a, i) => a === quizQuestions[i].correct).length
})

const handleSubmit = () => {
  if (selected.value === null) return
  answers.value.push(selected.value)
  if (currentQ.value < totalQ - 1) {
    currentQ.value++
    selected.value = null
  } else {
    completed.value = true
  }
}

const handleComplete = () => {
  showUpload.value = true
}

const goToCalendar = () => {
  showUpload.value = false
  router.push('/calendar')
}
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
.max-w-xl { max-width: 900px; }
.max-w-md { max-width: 560px; }
.mx-auto { margin-left: auto; margin-right: auto; }

/* Typography */
.form-title { font-size: 20px; font-weight: 800; color: var(--text-primary); margin: 0; letter-spacing: 0.05em; }
.form-sub { font-size: 14px; color: var(--text-muted); font-weight: 600; }

/* Utils */
.flex-align { display: flex; align-items: center; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-center { display: flex; align-items: center; justify-content: center; }
.justify-end { justify-content: flex-end; }
.flex-1 { flex: 1; }

.text-center { text-align: center; }
.font-bold { font-weight: 800; }
.text-xs { font-size: 12px; }
.text-sm { font-size: 14px; }
.text-lg { font-size: 18px; }
.text-\[11px\] { font-size: 11px; }

.text-primary { color: var(--text-primary); }
.text-muted { color: var(--text-muted); }

.m-0 { margin: 0; }
.mt-xs { margin-top: 4px; }
.mt-sm { margin-top: 8px; }
.mb-xs { margin-bottom: 8px; }
.mb-sm { margin-bottom: 12px; }
.mb-md { margin-bottom: 16px; }
.mb-lg { margin-bottom: 24px; }
.gap-sm { gap: 8px; }
.px-md { padding-left: 16px; padding-right: 16px; }
.p-lg { padding: 24px; }
.pt-sm { padding-top: 12px; }
.pb-0 { padding-bottom: 0; }
.border-none { border: none !important; }

/* Custom elements */
.btn-back { background: transparent; border: none; font-size: 13px; font-weight: 700; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px; padding: 0; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.btn-back:hover { color: var(--text-primary); transform: translateX(-4px); }
.btn-close { background: transparent; border: none; font-size: 20px; color: var(--text-muted); cursor: pointer; padding: 4px; transition: color 0.3s; }
.btn-close:hover { color: var(--text-primary); }

.track-bg { height: 4px; background: transparent; border: 1px solid var(--border); width: 100%; border-radius: 4px; overflow: hidden; }
.track-fill { height: 100%; background: var(--text-primary); transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1); }

.base-panel { background: transparent; border: 1px solid var(--border); display: flex; flex-direction: column; border-radius: 0; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);}
.base-panel:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.brutal-tag { padding: 4px 10px; border: 1px solid var(--border); background: transparent; font-size: 11px; font-weight: 700; color: var(--text-primary); border-radius: 40px;}
.inline-tag { display: inline-block; }

.quiz-q { font-size: 16px; font-weight: 800; line-height: 1.6; margin: 0 0 24px 0; letter-spacing: 0.05em;}
.options-list { display: flex; flex-direction: column; gap: 12px; }
.option-btn { background: var(--bg-surface); border: 1px solid var(--border); padding: 24px; cursor: pointer; text-align: left; display: flex; align-items: flex-start; gap: 16px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 0; font-family: inherit; }
.option-btn:hover { border-color: var(--text-primary); background: var(--bg-hover); transform: translateX(4px); }
.option-btn.selected { border-color: var(--text-primary); background: var(--bg-surface); box-shadow: inset 4px 0 0 var(--text-primary); }
.option-btn.selected .radio-circle { border-color: var(--text-primary); }
.radio-inner { width: 8px; height: 8px; background: var(--text-primary); border-radius: 50%; }
.option-btn span { font-size: 14px; font-weight: 600; color: var(--text-primary); line-height: 1.5; }

.btn-primary-block { width: 100%; display: flex; justify-content: center; align-items: center; gap: 8px; background: transparent; color: var(--text-primary); border: 1px solid var(--text-primary); padding: 16px; font-size: 14px; font-weight: 800; font-family: inherit; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 40px; letter-spacing: 0.1em; }
.btn-primary-block:hover:not(:disabled) { background: var(--text-primary); color: var(--bg-base); }
.btn-primary-block:disabled { background: transparent; border-color: var(--border); color: var(--text-faint); cursor: not-allowed; }

.btn-primary { background: transparent; color: var(--text-primary); border: 1px solid var(--text-primary); padding: 14px 24px; font-size: 13px; font-weight: 800; font-family: inherit; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); display: inline-flex; align-items: center; justify-content: center; gap: 8px; border-radius: 40px; letter-spacing: 0.05em; }
.btn-primary:hover { background: var(--text-primary); color: var(--bg-base); }
.btn-outline { background: transparent; color: var(--text-primary); border: 1px solid var(--border); padding: 14px 24px; font-size: 13px; font-weight: 800; font-family: inherit; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); display: inline-flex; align-items: center; justify-content: center; gap: 8px; border-radius: 40px; letter-spacing: 0.05em; }
.btn-outline:hover { background: var(--bg-hover); border-color: var(--text-primary); }

.score-circle { width: 80px; height: 80px; background: transparent; border: 2px solid var(--text-primary); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 800; color: var(--text-primary); }
.icon-box-muted { width: 64px; height: 64px; background: transparent; border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 28px; color: var(--text-muted); border-radius: 50%; }

/* Modals */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.8); backdrop-filter: blur(8px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 24px; }
.base-modal { width: 100%; display: flex; flex-direction: column; background: var(--bg-surface); border: 1px solid var(--border); animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both; border-radius: 0; }
.max-w-sm { max-width: 440px; }
@keyframes fadeUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 24px 32px; border-bottom: 1px solid var(--border); }
.modal-header h3 { font-size: 16px; font-weight: 800; margin: 0; display: flex; align-items: center; gap: 8px; color: var(--text-primary); letter-spacing: 0.05em; }
</style>
