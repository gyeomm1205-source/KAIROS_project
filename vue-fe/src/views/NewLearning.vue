<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content custom-scroll">
      <header class="page-header">
        <div class="header-title"><i class="fas fa-plus-circle" /> NEW LEARNING</div>
      </header>

      <div class="content-inner max-w-xl mx-auto align-center">
        <!-- 상단 헤더 및 뒤로가기 -->
        <button class="btn-back" @click="$router.go(-1)">
          <i class="fas fa-arrow-left" /> 뒤로
        </button>

        <h2 class="form-title">새로운 학습을 시작해볼까요?</h2>
        <p class="form-sub">주제와 목표를 입력하면 맞춤 커리큘럼을 생성합니다</p>

        <!-- 학습 주제 입력 폼 -->
        <div class="base-panel form-panel mb-gap">
          <h3 class="field-label"><i class="fas fa-search" /> 학습 주제</h3>
          <p class="field-desc">배우고 싶은 기술이나 주제를 입력하세요</p>
          <TechTagAutocomplete
            v-model="topic"
            class="brutal-input mb-sm"
            placeholder="예: GraphQL, Docker, 디자인 시스템..."
            @select="val => topic = val"
          />
          <div class="tag-group-small flex-wrap">
            <button
              v-for="st in suggestedTopics"
              :key="st"
              class="brutal-tag clickable-tag"
              :class="{ 'active': topic === st }"
              @click="topic = st"
            >
              <i v-if="hasTechIcon(st)" :class="getTechIcon(st)" class="tech-icon" />{{ st }}
            </button>
          </div>
        </div>

        <!-- 학습 목표 입력 폼 -->
        <div class="base-panel form-panel mb-gap">
          <h3 class="field-label"><i class="fas fa-bullseye" /> 학습 목표</h3>
          <p class="field-desc">어떤 방향으로 학습하고 싶은지 선택하세요</p>
          <div class="grid-2 col-gap">
            <button
              v-for="goalType in goalTypes"
              :key="goalType.value"
              class="goal-card"
              :class="{ 'active': selectedGoalType === goalType.value }"
              @click="selectedGoalType = goalType.value"
            >
              <div class="goal-label">{{ goalType.label }}</div>
              <div class="goal-desc">{{ goalType.desc }}</div>
            </button>
          </div>
        </div>

        <!-- 구체적 목표 입력 -->
        <div class="base-panel form-panel mb-gap">
          <h3 class="field-label">구체적인 목표 (선택)</h3>
          <p class="field-desc">자유롭게 목표를 적어주세요</p>
          <textarea
            v-model="goal"
            class="brutal-textarea"
            rows="3"
            placeholder="예: Docker를 활용해서 개인 프로젝트를 배포할 수 있게 되고 싶어요"
          />
        </div>

        <!-- 안내 문구 -->
        <div class="info-box mb-lg">
          <div class="flex-align gap-md">
            <div class="question-icon">?</div>
            <div>
              <div class="info-title">사전 퀴즈로 현재 상태를 먼저 파악해요</div>
              <p class="info-text">입력한 주제와 목표를 바탕으로 짧은 사전 퀴즈를 진행합니다. 결과를 바탕으로 AI가 학습 강도와 액션 미션을 조정해서 맞춤 커리큘럼을 구성합니다.</p>
            </div>
          </div>
        </div>

        <!-- 액션 버튼 -->
        <button
          class="btn-primary-block"
          :disabled="!canProceed"
          @click="handleStartDiagnostic"
        >
          사전 퀴즈 시작하기 <i class="fas fa-arrow-right" />
        </button>
      </div>
    </main>

    <!-- 모달: 진단 퀴즈 및 결과 -->
    <div v-if="showDiagnosticModal" class="modal-overlay" @click.self="resetModal">
      <div class="base-modal modal-large custom-scroll">
        <div class="modal-header">
          <div>
            <h3>{{ quizStage === 'quiz' ? `${displayTopic} 사전 퀴즈` : `${displayTopic} 맞춤 커리큘럼 제안` }}</h3>
            <p class="text-xs text-muted mt-xs">
              {{ quizStage === 'quiz' ? '짧은 진단을 통해 현재 이해 수준과 학습 방향을 파악합니다.' : '사전 퀴즈 결과와 입력한 목표를 기반으로 AI가 구성한 학습 제안입니다.' }}
            </p>
          </div>
          <button class="btn-close" @click="resetModal"><i class="fas fa-times" /></button>
        </div>

        <div class="modal-body p-lg">
          <!-- 퀴즈 단계 -->
          <template v-if="quizStage === 'quiz'">
            <div class="max-w-2xl mx-auto">
              <div class="progress-bar-wrap mb-lg">
                <div class="flex-between mb-xs">
                  <span class="lbl-light">문제 {{ currentQuestionIndex + 1 }} / {{ questions.length }}</span>
                  <span class="lbl-bold">진단 진행 중</span>
                </div>
                <div class="track-bg">
                  <div class="track-fill" :style="{ width: ((currentQuestionIndex + 1) / questions.length) * 100 + '%' }"></div>
                </div>
              </div>

              <div class="base-panel p-lg mb-lg">
                <div class="brutal-tag mb-md inline-tag">사전 진단</div>
                <h4 class="quiz-q">{{ currentQuestion.question }}</h4>
                <div class="options-list">
                  <button
                    v-for="(option, index) in currentQuestion.options"
                    :key="option.label"
                    class="option-btn"
                    :class="{ 'selected': selectedOptionIndex === index }"
                    @click="selectedOptionIndex = index"
                  >
                    <div class="radio-circle">
                      <div class="radio-inner" v-if="selectedOptionIndex === index"></div>
                    </div>
                    <div>
                      <div class="opt-label">{{ option.label }}</div>
                      <div class="opt-desc">{{ option.desc }}</div>
                    </div>
                  </button>
                </div>
              </div>

              <button
                class="btn-primary-block"
                :disabled="selectedOptionIndex === null"
                @click="handleNextQuestion"
              >
                {{ currentQuestionIndex < questions.length - 1 ? '다음 문제' : '결과 확인' }}
              </button>
            </div>
          </template>

          <!-- 결과 단계 -->
          <template v-else>
            <div class="space-y-lg">
              
              <!-- AI 판단 요약 -->
              <div class="base-panel bg-surface p-md mb-gap">
                <div class="info-title mb-md"><i class="fas fa-brain icon-ai" /> AI가 사용자 상태를 이렇게 해석했어요</div>
                <div class="grid-3 col-gap gap-y">
                  <div class="stat-box bg-white">
                    <span class="stat-lbl">입력으로 본 상태</span>
                    <div class="stat-list">
                      <div>학습 주제: {{ displayTopic }}</div>
                      <div>학습 목표: {{ selectedGoalObj?.label }}</div>
                      <div>사전 퀴즈 진단: {{ computedLevel.label }}</div>
                      <div v-if="goal.trim()">구체 목표: {{ goal }}</div>
                    </div>
                  </div>
                  <div class="stat-box bg-white">
                    <span class="stat-lbl">AI 판단 요약</span>
                    <p class="text-xs">{{ computedLevel.summary }}</p>
                  </div>
                  <div class="stat-box bg-white">
                    <span class="stat-lbl">커리큘럼 구성 원칙</span>
                    <p class="text-xs">{{ computedLevel.focus }}</p>
                  </div>
                </div>
              </div>

              <!-- 추천 커리큘럼 -->
              <div class="base-panel p-md mb-gap">
                <div class="flex-between mb-md items-start">
                  <div>
                    <h4 class="font-bold text-lg">추천 커리큘럼</h4>
                    <p class="text-xs text-muted">사전 퀴즈 결과를 반영해서 {{ displayTopic }} 학습을 아래 순서로 진행하는 구성을 제안합니다.</p>
                  </div>
                  <div class="stat-box light-box inline-block p-sm text-right">
                    <span class="stat-lbl">추천 학습 강도</span>
                    <span class="stat-val text-sm">{{ computedLevel.label }}</span>
                  </div>
                </div>

                <div class="grid-3 col-gap gap-y">
                  <div v-for="step in computedCurriculum" :key="step.phase" class="base-panel bg-surface p-md">
                    <div class="flex-between mb-sm">
                      <span class="brutal-tag bg-white font-bold flex-align gap-xs">
                        <i v-if="hasTechIcon(displayTopic)" :class="getTechIcon(displayTopic)" class="tech-icon" />
                        {{ step.phase }}
                      </span>
                      <span class="text-xs text-muted">{{ step.duration }}</span>
                    </div>
                    <div class="font-bold mb-xs text-sm text-accent">{{ step.title }}</div>
                    <p class="text-xs text-muted mb-md pb-md border-b">{{ step.desc }}</p>
                    <div class="bg-white p-sm border-normal">
                      <div class="text-[10px] text-muted mb-xs">이 단계가 끝나면</div>
                      <div class="text-xs">{{ step.outcome }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 액션 미션 및 레퍼런스 -->
              <div class="grid-2 col-gap gap-y">
                <!-- 액션 미션 -->
                <div>
                  <div class="flex-align gap-xs mb-md">
                    <i class="fas fa-lightbulb icon-mission" /> <span class="font-bold text-md text-mission">액션 미션</span>
                  </div>
                  <div class="flex-col gap-sm">
                    <div v-for="mission in computedMissions" :key="mission.title" class="base-panel p-md bg-white">
                      <div class="flex-between mb-xs items-start">
                        <span class="font-bold text-sm">{{ mission.title }}</span>
                        <span class="brutal-tag bg-surface border-muted">{{ mission.tag }}</span>
                      </div>
                      <p class="text-xs text-muted mb-sm">{{ mission.desc }}</p>
                      <div class="text-xs text-muted flex-align gap-xs">
                        <i class="fas fa-clock icon-clock" /> {{ mission.time }}
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 레퍼런스 -->
                <div>
                  <div class="flex-align gap-xs mb-md">
                    <i class="fas fa-book-open icon-ref" /> <span class="font-bold text-md text-ref">기초 레퍼런스</span>
                  </div>
                  <div class="flex-col gap-sm">
                    <div v-for="ref in computedReferences" :key="ref.title" class="base-panel p-md bg-white">
                      <div class="flex-between mb-xs items-start">
                        <span class="font-bold text-sm">{{ ref.title }}</span>
                        <span class="brutal-tag bg-surface border-muted">{{ ref.type }}</span>
                      </div>
                      <p class="text-xs text-muted mb-sm">{{ ref.reason }}</p>
                      <div class="text-xs text-muted flex-align gap-md">
                        <div class="flex-align gap-xs"><i class="fas fa-history icon-doc" /> {{ ref.freshness }}</div>
                        <div class="flex-align gap-xs cursor-pointer font-bold text-primary" @click="$router.push('/recommend/new-curriculum')">
                          <i class="fas fa-external-link-alt" /> 자료 보기
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

            </div>
          </template>
        </div>

        <div class="modal-footer" v-if="quizStage === 'result'">
          <button class="btn-primary-block" @click="$router.push('/recommend/new-curriculum')">
            <i class="fas fa-calendar-plus" /> 캘린더에 커리큘럼 일정 반영
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import { getTechIcon, hasTechIcon } from '@/utils/techIcons'
import TechTagAutocomplete from '@/components/TechTagAutocomplete.vue'

const router = useRouter()

const suggestedTopics = [
  "GraphQL", "Docker", "Kubernetes", "Rust", "Go",
  "Swift", "Machine Learning", "데이터 시각화", "보안 기초", "디자인 시스템",
]

const goalTypes = [
  { value: "understand", label: "개념 이해", desc: "기본 개념과 원리를 파악하고 싶어요" },
  { value: "practice", label: "실습 중심", desc: "직접 만들어보면서 익히고 싶어요" },
  { value: "career", label: "취업 준비", desc: "포트폴리오나 면접에 활용하고 싶어요" },
  { value: "deep", label: "심화 학습", desc: "이미 기초는 알고 있어서 깊이 파고 싶어요" },
]

const topic = ref("")
const goal = ref("")
const selectedGoalType = ref("")

const canProceed = computed(() => topic.value.trim() !== "" && selectedGoalType.value !== "")
const selectedGoalObj = computed(() => goalTypes.find(g => g.value === selectedGoalType.value))
const displayTopic = computed(() => topic.value.trim() || '새로운 주제')

// Modal State
const showDiagnosticModal = ref(false)
const quizStage = ref('quiz') // 'quiz' | 'result'
const currentQuestionIndex = ref(0)
const selectedOptionIndex = ref(null)
const diagnosticScores = ref([])

const handleStartDiagnostic = () => {
  if (!canProceed.value) return
  showDiagnosticModal.value = true
  quizStage.value = 'quiz'
  currentQuestionIndex.value = 0
  selectedOptionIndex.value = null
  diagnosticScores.value = []
}

const resetModal = () => {
  showDiagnosticModal.value = false
}

const handleNextQuestion = () => {
  if (selectedOptionIndex.value === null) return
  
  const nextScore = questions.value[currentQuestionIndex.value].options[selectedOptionIndex.value].score
  diagnosticScores.value.push(nextScore)
  
  if (currentQuestionIndex.value < questions.value.length - 1) {
    currentQuestionIndex.value++
    selectedOptionIndex.value = null
  } else {
    quizStage.value = 'result'
  }
}

// Data builders based on topic
const questions = computed(() => [
  {
    id: 1,
    question: `${displayTopic.value}의 핵심 개념을 지금 어느 정도 설명할 수 있나요?`,
    options: [
      { label: "처음 접해요", score: 0, desc: "용어나 개념 자체가 아직 낯선 상태" },
      { label: "기본 개념은 알아요", score: 1, desc: "대략적인 정의와 맥락은 알고 있는 상태" },
      { label: "비교와 설명이 가능해요", score: 2, desc: "장단점과 활용 맥락까지 설명 가능한 상태" },
    ],
  },
  {
    id: 2,
    question: `${displayTopic.value}를 직접 써보거나 적용해본 경험은 어느 정도인가요?`,
    options: [
      { label: "거의 없어요", score: 0, desc: "실습이나 프로젝트 적용 경험이 거의 없는 상태" },
      { label: "간단한 예제는 해봤어요", score: 1, desc: "튜토리얼 또는 짧은 예제를 따라한 경험이 있는 상태" },
      { label: "실제 프로젝트에 연결해봤어요", score: 2, desc: "실무 또는 개인 프로젝트 맥락에 적용해본 상태" },
    ],
  },
  {
    id: 3,
    question: `${displayTopic.value}를 이번에 배우는 가장 큰 이유와 연결되는 상태는 무엇인가요?`,
    options: [
      { label: "개념 정리가 먼저 필요해요", score: 0, desc: "기초 개념을 먼저 잡아야 다음 단계가 가능한 상태" },
      { label: "실전 연결이 필요해요", score: 1, desc: "배운 내용을 프로젝트/포트폴리오에 연결하고 싶은 상태" },
      { label: "심화 포인트를 찾고 싶어요", score: 2, desc: "기초보다 더 깊은 판단 기준과 응용을 찾는 상태" },
    ],
  },
])

const computedLevel = computed(() => {
  const score = diagnosticScores.value.reduce((a, b) => a + b, 0)
  if (score <= 2) {
    return {
      label: "입문 단계",
      summary: "기초 개념을 먼저 안정적으로 잡아야 하는 상태로 판단했습니다. 개념 이해와 짧은 실습을 먼저 연결하는 구성이 적합합니다.",
      focus: "개념 이해 -> 짧은 실습 -> 적용 포인트 정리",
    }
  }
  if (score <= 4) {
    return {
      label: "기초 적용 단계",
      summary: "기본 개념은 이미 어느 정도 갖추고 있고, 이를 실제 문제 해결이나 프로젝트 구조로 연결하는 학습이 더 효과적인 상태입니다.",
      focus: "핵심 복습 -> 실전 연결 -> 적용 기준 정리",
    }
  }
  return {
    label: "심화 확장 단계",
    summary: "기초 개념과 초기 적용 경험이 있어, 더 깊은 판단 기준과 실전 응용으로 넘어가는 커리큘럼이 적합한 상태입니다.",
    focus: "심화 개념 -> 설계 판단 -> 응용 시나리오 정리",
  }
})

const currentQuestion = computed(() => questions.value[currentQuestionIndex.value] || questions.value[0])

const computedCurriculum = computed(() => {
  const goalLabel = selectedGoalObj.value?.label || "학습 목표"
  const lvl = computedLevel.value.label

  if (lvl === "입문 단계") {
    return [
      { phase: "1단계", title: `${displayTopic.value} 개념 지형 익히기`, desc: `핵심 개념과 헷갈리는 포인트를 정리해 ${goalLabel}의 바닥을 맞춥니다.`, outcome: "공통 언어 확보", duration: "1일차 · 35분" },
      { phase: "2단계", title: `${displayTopic.value} 미니 실습으로 감 잡기`, desc: "짧은 예제를 따라가며 개념이 어떻게 쓰이는지 확인합니다.", outcome: "기초 사용 흐름 확보", duration: "2일차 · 45분" },
      { phase: "3단계", title: `${displayTopic.value} 현재 맥락에 연결하기`, desc: "프로젝트/취업 준비 맥락에서 어디 적용할지 정리합니다.", outcome: "추후 적용 포인트 1개 도출", duration: "3일차 · 20분" },
    ]
  } else if (lvl === "기초 적용 단계") {
     return [
      { phase: "1단계", title: `${displayTopic.value} 핵심 흐름 재정리`, desc: "알고 있는 개념을 점검하고, 자주 놓치는 기준을 보완합니다.", outcome: "개념 공백 최소화", duration: "1일차 · 25분" },
      { phase: "2단계", title: `${displayTopic.value} 실전 예제 해부`, desc: `${goalLabel} 방향 연결 구조를 예제로 분석하고 결정 지점을 확인합니다.`, outcome: "실전 구조 재현 기준 확보", duration: "2일차 · 55분" },
      { phase: "3단계", title: `${displayTopic.value} 내 작업 흐름에 적용`, desc: "포트폴리오에 바로 옮길 수 있는 설계 포인트를 정리합니다.", outcome: "적용 계획 수립", duration: "3일차 · 25분" },
    ]
  }
  return [
    { phase: "1단계", title: `${displayTopic.value} 심화 기준 정리`, desc: "설계 상의 판단 기준과 트레이드오프를 먼저 정리합니다.", outcome: "구조 판단 기준 명확화", duration: "1일차 · 30분" },
    { phase: "2단계", title: `${displayTopic.value} 응용 시나리오 실습`, desc: "더 복잡한 예제로 단순 사용을 넘는 응용 패턴을 익힙니다.", outcome: "심화 사용 사례 1개 이상 재현", duration: "2일차 · 65분" },
    { phase: "3단계", title: `${displayTopic.value} 회고와 문서화`, desc: "판단 기준과 재사용 가능한 패턴을 문서로 남깁니다.", outcome: "개인 레퍼런스 완성", duration: "3일차 · 30분" },
  ]
})

const computedMissions = computed(() => {
  const lvl = computedLevel.value.label
  if (lvl === "입문 단계") {
    return [
      { title: `${displayTopic.value} 개념 빠르게 정리`, desc: "기본 개념과 헷갈리는 포인트를 짧게 정리합니다.", time: "35분", tag: "개념" },
      { title: `${displayTopic.value} 미니 예제 따라하기`, desc: "짧은 예제로 감을 잡습니다.", time: "45분", tag: "실습" },
    ]
  } else if (lvl === "기초 적용 단계") {
    return [
      { title: `${displayTopic.value} 예제 분석`, desc: "실제 구조 맥락을 따라갑니다.", time: "55분", tag: "실전" },
      { title: `${displayTopic.value} 적용 설계 정리`, desc: "다음 프로젝트 적용 기준을 메모합니다.", time: "25분", tag: "설계" },
    ]
  }
  return [
    { title: `${displayTopic.value} 응용 실습`, desc: "확장된 예제를 통해 실전 감각을 높입니다.", time: "65분", tag: "응용" },
    { title: `${displayTopic.value} 문서화`, desc: "판단 기준을 언제든 참고하도록 문서화합니다.", time: "30분", tag: "회고" },
  ]
})

const computedReferences = computed(() => [
  { title: `${displayTopic.value} 공식 문서`, reason: "핵심 개념과 용어를 가장 정확히 확인합니다.", type: "공식 문서", freshness: "기본 레퍼런스" },
  { title: `${displayTopic.value} 실전 튜토리얼`, reason: "초반 학습 효율을 높이는 예제입니다.", type: "튜토리얼", freshness: "실습 참고" },
])
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
.max-w-xl { max-width: 680px; }
.mx-auto { margin-left: auto; margin-right: auto; }
.align-center { text-align: center; }

/* Typography */
.form-title { font-size: 26px; font-weight: 900; color: var(--text-primary); margin-bottom: 8px; letter-spacing: 0.05em; }
.form-sub { font-size: 14px; color: var(--text-muted); font-weight: 600; margin-bottom: 40px; }
.field-label { font-size: 15px; font-weight: 800; color: var(--text-primary); display: flex; align-items: center; gap: 8px; margin-bottom: 4px; text-align: left; }
.field-desc { font-size: 12px; color: var(--text-muted); font-weight: 600; margin-bottom: 24px; text-align: left; }

/* Utils */
.flex-align { display: flex; align-items: center; gap: 8px; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-col { display: flex; flex-direction: column; }
.flex-wrap { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); }
.grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); }
.col-gap { gap: 12px; }
.gap-y { gap: 16px; }
.gap-xs { gap: 4px; }
.gap-sm { gap: 8px; }
.gap-md { gap: 16px; }
.mb-xs { margin-bottom: 4px; }
.mb-sm { margin-bottom: 12px; }
.mb-md { margin-bottom: 16px; }
.mb-lg { margin-bottom: 24px; }
.mb-gap { margin-bottom: 24px; text-align: left; }
.p-sm { padding: 8px 12px; }
.p-md { padding: 16px; }
.p-lg { padding: 24px; }
.mt-xs { margin-top: 4px; }
.pl-lg { padding-left: 28px; }
.pb-md { padding-bottom: 16px; }
.border-b { border-bottom: 1px solid var(--border); }
.border-normal { border: 1px solid var(--border); }
.bg-surface { background: var(--bg-surface); }
.bg-white { background: var(--bg-base); }
.font-bold { font-weight: 800; }
.text-sm { font-size: 13px; }
.text-xs { font-size: 12px; }
.text-lg { font-size: 18px; }
.text-muted { color: var(--text-muted); }
.items-start { align-items: flex-start; }
.shrink-0 { flex-shrink: 0; }
.inline-block { display: inline-block; }
.text-right { text-align: right; }

/* Buttons & Inputs & Panels */
.btn-back { background: transparent; border: none; font-size: 13px; font-weight: 700; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px; margin-bottom: 24px; padding: 0; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.btn-back:hover { color: var(--text-primary); transform: translateX(-4px); }

.base-panel { background: transparent; border: 1px solid var(--border); padding: 32px; display: flex; flex-direction: column; border-radius: 8px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.form-panel { padding: 32px; border-radius: 8px; }
.form-panel:hover { border-color: var(--text-primary); background: var(--bg-hover); }

.brutal-input { width: 100%; border: 1px solid var(--border); padding: 16px; font-size: 14px; font-weight: 600; background: transparent; color: var(--text-primary); outline: none; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 8px; font-family: inherit; }
.brutal-input:focus { border-color: var(--text-primary); background: var(--bg-hover);}
.brutal-textarea { width: 100%; border: 1px solid var(--border); padding: 16px; font-size: 14px; font-weight: 600; background: transparent; color: var(--text-primary); outline: none; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 8px; resize: none; font-family: inherit; }
.brutal-textarea:focus { border-color: var(--text-primary); background: var(--bg-hover);}

.brutal-tag { padding: 8px 16px; border: 1px solid var(--border); background: transparent; font-size: 11px; font-weight: 700; color: var(--text-muted); transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 8px; display: inline-flex; align-items: center; gap: 6px; }
.tech-icon { font-size: 13px; flex-shrink: 0; }
.clickable-tag { cursor: pointer; }
.clickable-tag:hover { background: var(--bg-hover); border-color: var(--text-primary); color: var(--text-primary); }
.clickable-tag.active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }
.inline-tag { display: inline-block; }
.border-muted { border-color: var(--border); }

.goal-card { padding: 24px; text-align: left; background: transparent; border: 1px solid var(--border); cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); display: flex; flex-direction: column; gap: 8px; border-radius: 8px; font-family: inherit; }
.goal-card:hover { border-color: var(--text-primary); background: var(--bg-hover);}
.goal-card.active { border-color: var(--text-primary); background: transparent; }
.goal-label { font-size: 14px; font-weight: 800; color: var(--text-primary); }
.goal-desc { font-size: 12px; color: var(--text-muted); font-weight: 600; }

.info-box { background: transparent; border: 1px dashed var(--border); padding: 24px; text-align: left; border-radius: 8px; }
.question-icon { width: 32px; height: 32px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 16px; flex-shrink: 0; border-radius: 50%; }
.info-title { font-size: 14px; font-weight: 800; color: var(--text-primary); margin-bottom: 8px; }
.icon-ai      { color: var(--clr-icon-ai); }
.icon-mission { color: #FBBF24; }
.icon-ref     { color: #86EFAC; }
.icon-clock   { color: #FB923C; }
.icon-doc     { color: #60A5FA; }
.text-mission { color: #FBBF24; }
.text-ref     { color: #86EFAC; }
.text-accent  { color: var(--clr-primary); }
.info-text { font-size: 12px; color: var(--text-muted); line-height: 1.6; font-weight: 600; }

.btn-primary-block { width: 100%; display: flex; justify-content: center; align-items: center; gap: 8px; background: transparent; color: var(--text-primary); border: 1px solid var(--text-primary); padding: 16px; font-size: 14px; font-weight: 800; font-family: inherit; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 8px; letter-spacing: 0.1em; }
.btn-primary-block:hover:not(:disabled) { background: var(--text-primary); color: var(--bg-base); }
.btn-primary-block:disabled { background: transparent; border-color: var(--border); color: var(--text-faint); cursor: not-allowed; }
.btn-text { background: transparent; border: none; font-size: 11px; font-weight: 700; color: var(--text-muted); display: flex; align-items: center; gap: 4px; padding: 0; cursor: pointer; transition: color 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.btn-text:hover { color: var(--text-primary); }

/* Modal and Quiz styles */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.8); backdrop-filter: blur(8px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 24px; }
.modal-large { max-width: 900px; max-height: 90vh; width: 100%; padding: 0; display: flex; flex-direction: column; animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both; background: var(--bg-surface); border: 1px solid var(--border); border-radius: 8px;}
@keyframes fadeUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }

.modal-header { padding: 24px 32px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: flex-start; background: transparent; }
.modal-header h3 { font-size: 20px; font-weight: 800; margin: 0; letter-spacing: 0.05em; color: var(--text-primary); }
.btn-close { background: transparent; border: none; font-size: 20px; color: var(--text-muted); cursor: pointer; padding: 4px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.btn-close:hover { color: var(--text-primary); }

.modal-body { flex: 1; overflow-y: auto; background: transparent; }
.modal-footer { padding: 24px 32px; border-top: 1px solid var(--border); background: transparent; }

.progress-bar-wrap { width: 100%; }
.lbl-light { font-size: 12px; color: var(--text-muted); font-weight: 600; }
.lbl-bold { font-size: 12px; font-weight: 800; color: var(--text-primary); }
.track-bg { height: 4px; background: transparent; border: 1px solid var(--border); width: 100%; border-radius: 4px; overflow: hidden; }
.track-fill { height: 100%; background: var(--text-primary); transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1); }

.quiz-q { font-size: 18px; font-weight: 800; line-height: 1.6; margin: 0 0 24px 0; color: var(--text-primary); letter-spacing: 0.05em; }
.options-list { display: flex; flex-direction: column; gap: 12px; }
.option-btn { background: var(--bg-surface); border: 1px solid var(--border); padding: 24px; cursor: pointer; text-align: left; display: flex; align-items: flex-start; gap: 16px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 8px; font-family: inherit; }
.option-btn:hover { border-color: var(--text-primary); background: var(--bg-hover); transform: translateX(4px); }
.option-btn.selected { border-color: var(--text-primary); background: var(--bg-surface); box-shadow: inset 4px 0 0 var(--text-primary); }
.radio-circle { width: 18px; height: 18px; border: 1px solid var(--border); border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-top: 2px; }
.option-btn.selected .radio-circle { border-color: var(--text-primary); }
.radio-inner { width: 8px; height: 8px; background: var(--text-primary); border-radius: 50%; }
.opt-label { font-size: 14px; font-weight: 800; color: var(--text-primary); margin-bottom: 4px; }
.opt-desc { font-size: 12px; font-weight: 600; color: var(--text-muted); }

/* Stats box */
.stat-box { background: transparent; border: 1px solid var(--border); padding: 24px; display: flex; flex-direction: column; gap: 12px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 8px; }
.stat-box:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.light-box { background: transparent; border-style: dashed; }
.stat-lbl { font-size: 11px; color: var(--text-muted); font-weight: 700; text-transform: uppercase; letter-spacing: 0.1em; }
.stat-val { font-size: 14px; font-weight: 800; color: var(--text-primary); }
.stat-list { font-size: 12px; font-weight: 600; color: var(--text-primary); line-height: 1.8; display: flex; flex-direction: column; gap: 6px; }

@media (max-width: 768px) {
  .grid-2, .grid-3 { grid-template-columns: 1fr; }
}
</style>
