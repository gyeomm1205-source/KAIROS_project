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
          <div v-if="!completed" class="brutal-panel p-lg">
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
          <div v-else class="brutal-panel p-lg text-center">
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
      <div class="brutal-modal max-w-sm">
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
      <div class="brutal-modal max-w-sm text-center">
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

.page-header { display: flex; align-items: center; padding: 20px 32px; border-bottom: 2px solid var(--text-primary); background: var(--bg-surface); position: sticky; top: 0; z-index: 10; }
.header-title { font-size: 16px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary); display: flex; align-items: center; gap: 10px; }

.content-inner { padding: 48px 32px; width: 100%; }
.max-w-xl { max-width: 900px; }
.max-w-md { max-width: 560px; }
.mx-auto { margin-left: auto; margin-right: auto; }

/* Typography */
.form-title { font-size: 20px; font-weight: 900; color: var(--text-primary); }
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
.btn-back { background: transparent; border: none; font-size: 13px; font-weight: 700; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px; padding: 0; transition: color 0.1s; }
.btn-back:hover { color: var(--text-primary); }
.btn-close { background: transparent; border: none; font-size: 20px; color: var(--text-muted); cursor: pointer; padding: 4px; transition: color 0.1s; }
.btn-close:hover { color: var(--text-primary); transform: scale(1.1); }

.track-bg { height: 8px; background: var(--bg-surface); border: 1px solid var(--border); width: 100%; }
.track-fill { height: 100%; background: var(--text-primary); transition: width 0.3s; }

.brutal-panel { background: var(--bg-base); border: 2px solid var(--text-primary); box-shadow: 6px 6px 0 #6b7280; display: flex; flex-direction: column; border-radius: 0; }
.brutal-tag { padding: 4px 8px; border: 1px solid var(--border); background: var(--bg-surface); font-size: 11px; font-weight: 800; color: var(--text-muted); }
.inline-tag { display: inline-block; }

.quiz-q { font-size: 16px; font-weight: 800; line-height: 1.5; margin: 0 0 24px 0; }
.options-list { display: flex; flex-direction: column; gap: 12px; }
.option-btn { background: var(--bg-base); border: 2px solid var(--border); padding: 16px; cursor: pointer; text-align: left; display: flex; align-items: flex-start; gap: 12px; transition: all 0.1s; border-radius: 0; font-family: inherit; }
.option-btn:hover { border-color: var(--text-primary); background: var(--bg-surface); }
.option-btn.selected { border-color: var(--text-primary); background: var(--bg-surface); }
.radio-circle { width: 20px; height: 20px; border: 2px solid var(--border); border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-top: 2px; }
.option-btn.selected .radio-circle { border-color: var(--text-primary); }
.radio-inner { width: 10px; height: 10px; background: var(--text-primary); border-radius: 50%; }
.option-btn span { font-size: 14px; font-weight: 600; color: var(--text-primary); line-height: 1.5; }

.btn-primary-block { width: 100%; display: flex; justify-content: center; align-items: center; gap: 8px; background: var(--text-primary); color: var(--bg-base); border: 2px solid var(--text-primary); padding: 14px; font-size: 14px; font-weight: 900; box-shadow: 4px 4px 0 #6b7280; font-family: inherit; cursor: pointer; transition: all 0.1s; }
.btn-primary-block:hover:not(:disabled) { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; background: transparent; color: var(--text-primary); }
.btn-primary-block:disabled { background: var(--bg-hover); border-color: var(--border); color: var(--text-muted); cursor: not-allowed; box-shadow: none; transform: none; }

.btn-primary { background: var(--text-primary); color: var(--bg-base); border: 2px solid var(--text-primary); padding: 12px 20px; font-size: 14px; font-weight: 900; box-shadow: 4px 4px 0 #6b7280; font-family: inherit; cursor: pointer; transition: all 0.1s; display: inline-flex; align-items: center; justify-content: center; gap: 8px; }
.btn-primary:hover { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; background: transparent; color: var(--text-primary); }
.btn-outline { background: transparent; color: var(--text-primary); border: 2px solid var(--text-primary); padding: 12px 20px; font-size: 14px; font-weight: 800; box-shadow: 4px 4px 0 #6b7280; font-family: inherit; cursor: pointer; transition: all 0.1s; display: inline-flex; align-items: center; justify-content: center; gap: 8px; }
.btn-outline:hover { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; background: var(--bg-surface); }

.score-circle { width: 72px; height: 72px; background: var(--bg-surface); border: 4px solid var(--text-primary); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 900; }
.icon-box-muted { width: 64px; height: 64px; background: var(--bg-surface); border: 2px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 28px; color: var(--text-muted); border-radius: 50%; }

/* Modals */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); backdrop-filter: blur(2px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 24px; }
.brutal-modal { width: 100%; display: flex; flex-direction: column; background: var(--bg-base); border: 2px solid var(--text-primary); box-shadow: 8px 8px 0 #6b7280; animation: popUp 0.15s ease-out; }
.max-w-sm { max-width: 400px; }
@keyframes popUp { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 2px solid var(--border); }
.modal-header h3 { font-size: 16px; font-weight: 900; margin: 0; display: flex; align-items: center; gap: 8px; color: var(--text-primary); }
</style>
