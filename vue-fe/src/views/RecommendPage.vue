<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content custom-scroll">
      <header class="page-header">
        <div class="header-title"><i class="fas fa-compass" /> RECOMMENDATIONS</div>
      </header>

      <div class="content-inner">
        <!-- 첫 진입 시 (활동 목록을 고르지 않은 상태) -->
        <template v-if="!selectedActivity">
          <div class="title-section">
            <h2>추천</h2>
            <p>최근 7일간 활동을 기반으로 맞춤 추천을 제공합니다. 확인할 활동을 선택하세요.</p>
          </div>

          <div class="recommend-grid">
            <button
              v-for="activity in store.recentActivities"
              :key="activity.id"
              class="brutal-panel activity-card"
              @click="selectedActivity = activity.id"
            >
              <div class="activity-top">
                <div class="activity-icon-wrap" :class="'type-' + activity.type">
                  <i :class="activityTypeConfig[activity.type].icon" />
                </div>
                <div class="activity-info">
                  <div class="activity-header">
                    <span class="activity-title">{{ activity.title }}</span>
                    <span v-if="activity.status === 'in-progress'" class="brutal-tag active-tag">진행 중</span>
                    <span v-if="activity.status === 'done'" class="brutal-tag done-tag">완료</span>
                  </div>
                  <div class="activity-meta">
                    <span class="activity-date"><i class="fas fa-calendar-alt" /> {{ activity.date }}</span>
                    <div class="tag-group-small">
                      <span v-for="tag in activity.tags" :key="tag" class="small-tag">{{ tag }}</span>
                    </div>
                  </div>
                </div>
                <i class="fas fa-arrow-right arrow-icon" />
              </div>
            </button>
          </div>
        </template>

        <!-- 특정 활동(맥락)을 선택하여 들어온 후 -->
        <template v-else>
          <button class="btn-back" @click="handleBack">
            <i class="fas fa-arrow-left" /> 활동 목록으로 돌아가기
          </button>

          <section class="brutal-panel highlight-panel main-recommendation mb-gap">
            <div class="panel-header-flex">
              <div class="flex-align">
                <div class="icon-box-white"><i class="fas fa-lightbulb" /></div>
                <div>
                  <div class="panel-title-group">
                    <h3 class="panel-title">React Server Components 심화 학습</h3>
                    <button class="btn-primary-small-invert" @click="showCurriculumModal = true">
                      <i class="fas fa-book-open" /> 커리큘럼 추천받기
                    </button>
                  </div>
                  <span class="panel-sub">맞춤 추천 · 최근 학습 흐름 기반</span>
                </div>
              </div>
              <div class="flex-align gap-sm">
                <span class="brutal-tag badge-dark">AI 추천</span>
                <button class="btn-outline-small" @click="$router.push('/recommend/new-learning')">
                  <i class="fas fa-plus" /> 신규 학습 시작
                </button>
              </div>
            </div>

            <p class="panel-desc large-desc">
              최근 React 상태관리 학습을 완료하고, Next.js App Router 기반 프로젝트를 진행하고 있습니다. 
              현재 기술 흐름에서 자연스럽게 이어지는 React Server Components를 다음 학습 주제로 추천합니다.
            </p>

            <div class="flow-box">
              <div class="flow-title">최근 학습 흐름</div>
              <div class="flow-track scroll-x">
                <template v-for="(flow, index) in store.recentFlow" :key="index">
                  <div class="flow-item">
                    <span class="flow-node" :class="'status-' + flow.status">{{ flow.label }}</span>
                    <span v-if="index < store.recentFlow.length - 1" class="flow-line"></span>
                  </div>
                </template>
                <div class="flow-item">
                  <span class="flow-line"></span>
                  <span class="flow-node dashed-node">RSC 학습</span>
                </div>
              </div>
            </div>

            <div class="flex-footer">
              <div class="info-group">
                <span class="info-text"><i class="fas fa-clock" /> 예상 소요: 4시간 40분 (5일)</span>
                <span class="info-text"><i class="fas fa-chart-line" /> 난이도: 중급</span>
              </div>
              <div class="tag-group">
                <span class="brutal-tag tag-white" v-for="tag in ['React', 'SSR', 'Next.js']" :key="tag">
                  <i class="fas fa-tag" style="font-size: 10px;" /> {{ tag }}
                </span>
              </div>
              <button class="btn-outline-invert ml-auto" @click="showReasonModal = true">
                <i class="fas fa-info-circle" /> 추천 근거 보기
              </button>
            </div>
          </section>

          <!-- 미션 및 활성 패널 구역 -->
          <div class="layout-split" :class="{ 'has-active': activeMission }">
            <div class="mission-list">
              <div class="flex-between mb-sm" v-if="activeMission">
                <span></span>
                <button class="btn-text" @click="closeMission">전체 보기</button>
              </div>
              
              <div class="missions-grid" :class="{ 'compact': activeMission }">
                <div 
                  v-for="mission in store.missions" 
                  :key="mission.key"
                  class="brutal-panel mission-card"
                  :class="{ 'active': activeMission === mission.key }"
                >
                  <div class="mission-icon"><i :class="mission.icon" /></div>
                  <div class="mission-content">
                    <div class="mission-header">
                      <span class="mission-title">{{ mission.title }}</span>
                      <span v-if="mission.badge" class="brutal-tag small-badge">{{ mission.badge }}</span>
                    </div>
                    <p class="mission-desc">{{ mission.desc }}</p>
                    <div class="mission-footer mt-md">
                      <div class="info-group">
                        <span class="info-text"><i class="fas fa-clock" /> {{ mission.time }}</span>
                        <span class="brutal-tag outline-badge ml-sm">{{ mission.tag }}</span>
                      </div>
                      <button 
                        class="btn-primary-small" 
                        :class="{ 'btn-dark': activeMission === mission.key }" 
                        @click="openMission(mission.key)"
                      >
                        시작하기 <i class="fas fa-arrow-right" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="activeMission" class="active-panel">
              <div class="brutal-panel panel-sticky shadow-heavy p-0">
                <div class="panel-header-flex mb-md p-lg border-b">
                  <div>
                    <h3 class="panel-title-lg">{{ activeMission === 'quiz' ? '복습 퀴즈' : 'velog 글 작성하기' }}</h3>
                    <p class="panel-sub" v-if="activeMission === 'quiz'">퀴즈 제안과 실제 퀴즈 흐름을 확인합니다.</p>
                    <p class="panel-sub mt-sm" v-if="activeMission === 'velog'">글 작성을 위한 레퍼런스와 뼈대를 제안합니다.</p>
                  </div>
                  <button class="btn-close" @click="closeMission"><i class="fas fa-times" /></button>
                </div>

                <!-- 퀴즈 패널 내용 -->
                <div v-if="activeMission === 'quiz'" class="panel-body p-lg pt-0">
                  <template v-if="quizStep === 'intro'">
                    <div class="info-box mb-md">
                      <div class="info-title"><i class="fas fa-brain" /> 이전 학습 맥락 기반 복습 퀴즈</div>
                      <p>최근 학습한 React Server Components와 렌더링 흐름을 다시 떠올릴 수 있게 핵심 개념 위주로 구성했습니다.</p>
                    </div>
                    <div class="grid-2 col-gap mb-md">
                      <div class="stat-box">
                        <span class="stat-lbl">예상 소요 시간</span>
                        <span class="stat-val">약 15분</span>
                      </div>
                      <div class="stat-box">
                        <span class="stat-lbl">문항 수</span>
                        <span class="stat-val">총 2문제</span>
                      </div>
                    </div>
                    <button class="btn-primary-block" @click="quizStep = 'question'">퀴즈 시작하기</button>
                  </template>

                  <template v-else-if="quizStep === 'question'">
                    <div class="progress-bar-wrap mb-md">
                      <div class="flex-between mb-xs">
                        <span class="lbl-light">문제 {{ currentQuizIndex + 1 }} / {{ store.quizQuestions.length }}</span>
                        <span class="lbl-bold">진행 중</span>
                      </div>
                      <div class="track-bg"><div class="track-fill" :style="{ width: ((currentQuizIndex + 1) / store.quizQuestions.length) * 100 + '%' }"></div></div>
                    </div>

                    <div class="brutal-panel mb-md p-md">
                      <h4 class="quiz-q">{{ store.quizQuestions[currentQuizIndex].question }}</h4>
                      <div class="options-list">
                        <button 
                          v-for="(opt, idx) in store.quizQuestions[currentQuizIndex].options" 
                          :key="idx"
                          class="option-btn"
                          :class="{ 'selected': selectedOption === idx }"
                          @click="selectedOption = idx"
                        >
                          <div class="radio-circle">
                            <div class="radio-inner" v-if="selectedOption === idx"></div>
                          </div>
                          <span>{{ opt }}</span>
                        </button>
                      </div>
                    </div>
                    <button class="btn-primary-block" :disabled="selectedOption === null" @click="handleQuizSubmit">
                      {{ currentQuizIndex < store.quizQuestions.length - 1 ? '다음 문제' : '결과 확인' }}
                    </button>
                  </template>

                  <template v-else-if="quizStep === 'result'">
                    <div class="info-box center-txt mb-md p-lg">
                      <div class="score-circle">{{ quizCorrectCount }}/{{ store.quizQuestions.length }}</div>
                      <h3 class="panel-title mb-sm mt-md">퀴즈를 완료했어요!</h3>
                      <p>현재 이해 수준을 반영해서 다음 추천 흐름을 조정할 수 있습니다.</p>
                    </div>
                  </template>
                </div>

                <!-- 벨로그 래퍼런스 패널 -->
                <div v-if="activeMission === 'velog'" class="panel-body p-lg pt-0">
                  <div class="info-box mb-md">
                    <div class="info-title"><i class="fas fa-pen-nib" /> velog 글 작성을 위한 레퍼런스 추천</div>
                    <p>글을 대신 써주는 것이 아니라, 지금 학습 흐름에 맞는 참고 자료를 추천해서 글 구조를 잡는 데 도움을 줍니다.</p>
                  </div>

                  <div class="ref-list space-y">
                    <div v-for="ref in store.references" :key="ref.title" class="brutal-panel p-md option-btn">
                      <div class="flex-between mb-sm">
                        <div class="flex-align gap-sm"><i class="fas fa-file-alt text-muted" /><span class="font-bold">{{ ref.title }}</span></div>
                        <span class="brutal-tag outline-badge">{{ ref.type }}</span>
                      </div>
                      <p class="text-muted text-sm ml-lg mb-sm">{{ ref.reason }}</p>
                      <div class="flex-between ml-lg mt-md">
                        <div class="flex-align gap-md text-muted text-xs">
                          <span>{{ ref.freshness }}</span>
                          <span><i class="fas fa-shield-alt" /> 안전한 링크</span>
                        </div>
                        <button class="btn-text-muted"><i class="fas fa-external-link-alt" /> 보기</button>
                      </div>
                    </div>
                  </div>
                </div>

              </div>
            </div>
          </div>
        </template>
      </div>
    </main>

    <!-- 모달: 추천 이유 -->
    <div v-if="showReasonModal" class="modal-overlay" @click.self="showReasonModal = false">
      <div class="brutal-modal shadow-heavy">
        <div class="modal-header">
          <h3><i class="fas fa-magic" /> 추천 근거 확인</h3>
          <button class="btn-close" @click="showReasonModal = false"><i class="fas fa-times"/></button>
        </div>
        <div class="reason-content custom-scroll">
          <div class="reason-item shadow-normal">
            <div class="reason-item-header"><i class="fab fa-github" /><h4>GitHub 활동 분석</h4></div>
            <p>최근 2주간 React의 <code>useMemo</code>와 <code>useCallback</code>을 활용한 렌더링 최적화 커밋이 15건 이상 발생했습니다. 프론트엔드 최적화의 기초를 충분히 다진 것으로 판단되어, 다음 단계인 SSR(서버 사이드 렌더링) 및 Next.js 도입을 추천합니다.</p>
          </div>
          <div class="reason-item shadow-normal">
            <div class="reason-item-header"><i class="fas fa-bullseye" /><h4>설문 및 목표</h4></div>
            <p>희망 포지션을 <strong>'프론트엔드 개발자'</strong>로 설정하셨으며, 최근 시장에서 Next.js App Router 경험이 필수적 요구사항으로 떠오르고 있습니다.</p>
          </div>
        </div>
        <div class="modal-actions mt-md"><button class="btn-primary" @click="showReasonModal = false">확인</button></div>
      </div>
    </div>

    <!-- 모달: 커리큘럼 추천 -->
    <div v-if="showCurriculumModal" class="modal-overlay" @click.self="showCurriculumModal = false">
      <div class="brutal-modal shadow-heavy">
        <div class="modal-header">
          <h3><i class="fas fa-book-open" /> 추천 커리큘럼</h3>
          <button class="btn-close" @click="showCurriculumModal = false"><i class="fas fa-times"/></button>
        </div>
        <div class="reason-content custom-scroll">
          <div v-for="i in 3" :key="i" class="reason-item shadow-normal">
            <div class="flex-between mb-xs">
              <span class="font-bold text-sm">{{ i }}일차</span>
              <span class="text-muted text-xs"><i class="fas fa-clock"/> 1시간 30분</span>
            </div>
            <h4 class="mb-xs">RSC의 핵심 개념 {{ i }}</h4>
            <p class="text-muted text-sm">Server Component와 Client Component의 경계선을 이해합니다.</p>
          </div>
        </div>
        <div class="modal-actions mt-md"><button class="btn-primary" @click="showCurriculumModal = false">확인</button></div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useRecommendStore } from '@/stores/useRecommendStore'
import AppSidebar from '@/components/AppSidebar.vue'

const router = useRouter()
const store = useRecommendStore()

const activityTypeConfig = {
  study: { icon: "fas fa-book-open" },
  dev:   { icon: "fas fa-code" },
  blog:  { icon: "fas fa-file-alt" },
  review:{ icon: "fas fa-clock" },
}

const selectedActivity = ref(null)
const showReasonModal = ref(false)
const showCurriculumModal = ref(false)

const activeMission = ref(null)
const quizStep = ref("intro") // 'intro' | 'question' | 'result'
const currentQuizIndex = ref(0)
const selectedOption = ref(null)
const quizAnswers = ref([])

const handleBack = () => {
  selectedActivity.value = null
  closeMission()
}

const openMission = (missionKey) => {
  activeMission.value = missionKey
  quizStep.value = "intro"
  currentQuizIndex.value = 0
  selectedOption.value = null
  quizAnswers.value = []
}

const closeMission = () => {
  activeMission.value = null
  quizStep.value = "intro"
  currentQuizIndex.value = 0
  selectedOption.value = null
  quizAnswers.value = []
}

const handleQuizSubmit = () => {
  if (selectedOption.value === null) return
  quizAnswers.value.push(selectedOption.value)
  if (currentQuizIndex.value < store.quizQuestions.length - 1) {
    currentQuizIndex.value++
    selectedOption.value = null
  } else {
    quizStep.value = 'result'
  }
}

const quizCorrectCount = computed(() => {
  return quizAnswers.value.filter((ans, idx) => ans === store.quizQuestions[idx].correct).length
})

</script>

<style scoped>
.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow-y: auto; position: relative; }
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

.page-header { display: flex; align-items: center; padding: 20px 32px; border-bottom: 2px solid var(--text-primary); background: var(--bg-surface); position: sticky; top: 0; z-index: 10; }
.header-title { font-size: 16px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary); display: flex; align-items: center; gap: 10px; }

.content-inner { max-width: 1000px; margin: 0 auto; padding: 48px 32px; width: 100%; }
.title-section { margin-bottom: 24px; }
.title-section h2 { font-size: 24px; font-weight: 900; color: var(--text-primary); margin-bottom: 8px; }
.title-section p { font-size: 14px; font-weight: 600; color: var(--text-muted); }

/* Buttons & Tags */
.btn-back { background: transparent; border: none; font-size: 14px; font-weight: 700; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px; margin-bottom: 24px; padding: 0; transition: color 0.1s; }
.btn-back:hover { color: var(--text-primary); }

.brutal-panel { background: var(--bg-base); border: 2px solid var(--text-primary); padding: 24px; box-shadow: 4px 4px 0 #6b7280; display: flex; flex-direction: column; transition: transform 0.1s, box-shadow 0.1s; border-radius: 0; }
.brutal-panel:hover { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; }

.brutal-tag { padding: 4px 8px; border: 2px solid var(--text-primary); background: transparent; font-size: 11px; font-weight: 800; border-radius: 0; }
.badge-dark { background: var(--text-primary); color: var(--bg-base); }
.tag-white { border-color: var(--bg-base); color: var(--bg-base); }

/* Common Utilities */
.flex-align { display: flex; align-items: center; gap: 12px; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.gap-sm { gap: 8px; }
.gap-md { gap: 12px; }
.mb-xs { margin-bottom: 8px; }
.mb-sm { margin-bottom: 12px; }
.mb-md { margin-bottom: 20px; }
.mb-lg { margin-bottom: 32px; }
.mb-gap { margin-bottom: 24px; }
.ml-auto { margin-left: auto; }
.ml-lg { margin-left: 36px; }
.mt-md { margin-top: 20px; }
.p-md { padding: 20px; }
.p-lg { padding: 32px; }
.p-0 { padding: 0 !important; }
.pt-0 { padding-top: 0 !important; }
.border-b { border-bottom: 2px solid var(--border); }
.space-y > * + * { margin-top: 12px; }
.font-bold { font-weight: 800; }
.text-sm { font-size: 13px; }
.text-xs { font-size: 11px; }
.text-muted { color: var(--text-muted); }
.center-txt { text-align: center; }
.shadow-heavy { box-shadow: 8px 8px 0 #6b7280; }
.shadow-heavy:hover { box-shadow: 10px 10px 0 #6b7280; transform: translate(-2px, -2px); }
.shadow-normal { box-shadow: 4px 4px 0 #6b7280; }
.shadow-normal:hover { box-shadow: 6px 6px 0 #6b7280; transform: translate(-2px, -2px); }

/* Buttons Set */
button { font-family: 'Space Grotesk', 'Pretendard', sans-serif; cursor: pointer; border-radius: 0; transition: all 0.1s; }
.btn-primary { background: var(--text-primary); color: var(--bg-base); border: 2px solid var(--text-primary); padding: 12px 24px; font-weight: 900; box-shadow: 4px 4px 0 #6b7280; }
.btn-primary:hover { background: transparent; color: var(--text-primary); transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; }

.btn-primary-small { background: transparent; color: var(--text-primary); border: 2px solid var(--text-primary); padding: 8px 16px; font-size: 12px; font-weight: 800; }
.btn-primary-small:hover { background: var(--text-primary); color: var(--bg-base); box-shadow: 2px 2px 0 #6b7280; transform: translate(-2px, -2px); }
.btn-dark { background: var(--text-primary); color: var(--bg-base); }

.btn-primary-small-invert { background: var(--text-primary); color: var(--bg-base); border: 2px solid var(--bg-base); padding: 6px 14px; font-size: 12px; font-weight: 800; display: inline-flex; align-items: center; gap: 6px; }
.btn-primary-small-invert:hover { background: var(--bg-base); color: var(--text-primary); }

.btn-primary-block { width: 100%; display: block; background: var(--text-primary); color: var(--bg-base); border: 2px solid var(--text-primary); padding: 14px; font-size: 14px; font-weight: 900; box-shadow: 4px 4px 0 #6b7280; }
.btn-primary-block:hover:not(:disabled) { background: transparent; color: var(--text-primary); box-shadow: 6px 6px 0 #6b7280; transform: translate(-2px, -2px); }
.btn-primary-block:disabled { background: var(--bg-hover); border-color: var(--border); color: var(--text-muted); cursor: not-allowed; box-shadow: none; transform: none; }

.btn-outline-small { background: transparent; color: var(--text-primary); border: 2px solid var(--border); padding: 6px 12px; font-size: 12px; font-weight: 700; display: inline-flex; align-items: center; gap: 6px; }
.btn-outline-small:hover { border-color: var(--text-primary); background: var(--bg-surface); }

.btn-outline-invert { background: transparent; color: var(--bg-base); border: 2px solid var(--border); padding: 10px 16px; font-size: 13px; font-weight: 800; display: inline-flex; align-items: center; gap: 8px; }
.btn-outline-invert.ml-auto { margin-left: auto; }
.btn-outline-invert:hover { border-color: var(--bg-base); background: rgba(255,255,255,0.1); }

.btn-text { background: transparent; border: 1px solid transparent; padding: 4px 8px; font-size: 12px; font-weight: 700; color: var(--text-muted); }
.btn-text:hover { color: var(--text-primary); border-color: var(--border); }
.btn-text-muted { background: transparent; border: none; font-size: 12px; color: var(--text-muted); display: flex; align-items: center; gap: 4px; padding: 0; }
.btn-text-muted:hover { color: var(--text-primary); }

.btn-close { background: transparent; border: none; font-size: 18px; color: var(--text-muted); padding: 4px; line-height: 1; }
.btn-close:hover { color: var(--text-primary); transform: scale(1.1); }

/* Activity Card */
.recommend-grid { display: flex; flex-direction: column; gap: 16px; }
.activity-card { padding: 16px; cursor: pointer;  background: var(--bg-base); }
.activity-card:hover .arrow-icon { color: var(--text-primary); transform: translateX(2px); }
.activity-top { display: flex; align-items: center; gap: 16px; }
.activity-icon-wrap { width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; font-size: 18px; border: 2px solid var(--text-primary); flex-shrink: 0; }
.type-study { background: #e5e7eb; color: var(--text-primary); }
.type-dev { background: #d1d5db; color: var(--text-primary); }
.type-blog { background: #f3f4f6; color: var(--text-primary); }
.type-review { background: #9ca3af; color: var(--bg-base); }

.activity-info { flex: 1; min-width: 0; }
.activity-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.activity-title { font-size: 15px; font-weight: 800; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.active-tag { background: var(--text-primary); color: var(--bg-base); padding: 2px 6px; font-size: 10px; border-color: var(--text-primary); }
.done-tag { background: var(--bg-surface); color: var(--text-muted); padding: 2px 6px; font-size: 10px; border-color: var(--border); }
.activity-meta { display: flex; align-items: center; gap: 16px; }
.activity-date { font-size: 12px; color: var(--text-muted); font-weight: 600; display: flex; align-items: center; gap: 6px; }
.tag-group-small { display: flex; gap: 4px; }
.small-tag { padding: 2px 6px; background: var(--bg-surface); border: 1px solid var(--border); font-size: 10px; color: var(--text-muted); font-weight: 600; }
.arrow-icon { font-size: 18px; color: var(--border); transition: all 0.2s; }

/* Highlighted Main Recommendation */
.highlight-panel { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); padding: 32px; }
.panel-header-flex { display: flex; justify-content: space-between; align-items: flex-start; }
.icon-box-white { width: 44px; height: 44px; background: var(--bg-base); color: var(--text-primary); display: flex; align-items: center; justify-content: center; font-size: 20px; border: 2px solid var(--bg-base); }
.panel-title-group { display: flex; align-items: center; gap: 12px; margin-bottom: 4px; }
.panel-title { font-size: 18px; font-weight: 900; margin: 0; letter-spacing: 0.05em; }
.panel-sub { font-size: 13px; font-weight: 600; opacity: 0.8; }
.large-desc { font-size: 15px; line-height: 1.6; opacity: 0.9; margin: 24px 0; font-weight: 600; }

.flow-box { background: rgba(255,255,255,0.1); padding: 16px; border: 1px solid rgba(255,255,255,0.2); margin-bottom: 24px; }
.flow-title { font-size: 12px; font-weight: 800; margin-bottom: 12px; opacity: 0.8; }
.flow-track { display: flex; align-items: center; overflow-x: auto; padding-bottom: 4px; }
.scroll-x::-webkit-scrollbar { height: 4px; }
.scroll-x::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.3); }
.flow-item { display: flex; align-items: center; flex-shrink: 0; }
.flow-node { padding: 4px 8px; font-size: 11px; font-weight: 700; border: 1px solid var(--bg-base); background: transparent; }
.status-done { background: rgba(255,255,255,0.9); color: var(--text-primary); }
.status-in-progress { border-color: rgba(255,255,255,0.8); }
.dashed-node { border-style: dashed; padding: 4px 12px; }
.flow-line { width: 16px; height: 1px; background: rgba(255,255,255,0.4); margin: 0 4px; }

.flex-footer { display: flex; align-items: center; gap: 24px; flex-wrap: wrap; }
.info-group { display: flex; align-items: center; gap: 16px; }
.info-text { font-size: 13px; font-weight: 600; display: flex; align-items: center; gap: 6px; }
.highlight-panel .info-text { opacity: 0.8; }

/* Dashboard Split Layout */
.layout-split { display: grid; grid-template-columns: 1fr; gap: 24px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.layout-split.has-active { grid-template-columns: 1fr 1.2fr; }
@media (max-width: 900px) { .layout-split.has-active { grid-template-columns: 1fr; } }

.missions-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.missions-grid.compact { grid-template-columns: 1fr; }

.mission-card { padding: 20px; flex-direction: row; gap: 16px; background: var(--bg-surface); }
.mission-card.active { border-color: var(--text-primary); shadow: 4px 4px 0 #6b7280; background: var(--bg-base); }
.mission-icon { width: 44px; height: 44px; background: var(--bg-base); border: 2px solid var(--text-primary); display: flex; align-items: center; justify-content: center; font-size: 18px; color: var(--text-primary); flex-shrink: 0; }
.mission-card.active .mission-icon { background: var(--text-primary); color: var(--bg-base); }
.mission-content { flex: 1; min-width: 0; }
.mission-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.mission-title { font-size: 15px; font-weight: 800; }
.small-badge { font-size: 10px; padding: 2px 6px; }
.mission-desc { font-size: 12px; color: var(--text-muted); line-height: 1.5; margin-bottom: 16px; font-weight: 600; }
.mission-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 10px; }
.outline-badge { border-color: var(--border); color: var(--text-muted); background: var(--bg-surface); padding: 4px 10px; font-size: 11px; font-weight: 800; border-radius: 4px; }

/* Active Panel (Sticky) */
.active-panel { position: sticky; top: 100px; }
.panel-sticky { min-height: 480px; }
.panel-title-lg { font-size: 20px; font-weight: 900; margin-bottom: 4px; }
.info-box { background: var(--bg-surface); border: 1px solid var(--border); padding: 16px; }
.info-title { font-size: 14px; font-weight: 800; margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }
.info-box p { font-size: 12px; color: var(--text-muted); line-height: 1.6; }

.grid-2 { display: grid; grid-template-columns: 1fr 1fr; }
.col-gap { gap: 12px; }
.stat-box { background: var(--bg-base); border: 1px solid var(--border); padding: 16px; display: flex; flex-direction: column; gap: 4px; }
.stat-lbl { font-size: 11px; color: var(--text-muted); font-weight: 700; }
.stat-val { font-size: 16px; font-weight: 900; color: var(--text-primary); }

.lbl-light { font-size: 12px; color: var(--text-muted); font-weight: 600; }
.lbl-bold { font-size: 12px; font-weight: 800; color: var(--text-primary); }
.track-bg { height: 8px; background: var(--bg-surface); border: 1px solid var(--border); width: 100%; }
.track-fill { height: 100%; background: var(--text-primary); transition: width 0.3s; }

.quiz-q { font-size: 16px; font-weight: 800; line-height: 1.5; margin-bottom: 24px; margin-top: 0; }
.options-list { display: flex; flex-direction: column; gap: 12px; }
.option-btn { background: var(--bg-base); border: 2px solid var(--border); padding: 16px; cursor: pointer; text-align: left; display: flex; align-items: flex-start; gap: 12px; transition: all 0.1s; border-radius: 0; }
.option-btn:hover { border-color: var(--text-primary); background: var(--bg-surface); }
.option-btn.selected { border-color: var(--text-primary); background: var(--bg-surface); font-weight: bold; }
.radio-circle { width: 20px; height: 20px; border: 2px solid var(--border); border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-top: 2px; }
.option-btn.selected .radio-circle { border-color: var(--text-primary); }
.radio-inner { width: 10px; height: 10px; background: var(--text-primary); border-radius: 50%; }
.option-btn span { font-size: 13px; line-height: 1.5; color: var(--text-secondary); font-weight: 600; }
.option-btn.selected span { color: var(--text-primary); }

.score-circle { width: 72px; height: 72px; border: 4px solid var(--text-primary); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 900; margin: 0 auto; margin-bottom: 24px; }

/* Modal specific */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); backdrop-filter: blur(2px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 24px; }
.brutal-modal { width: 100%; max-width: 560px; max-height: 85vh; display: flex; flex-direction: column; background: var(--bg-base); border: 2px solid var(--text-primary); padding: 32px; animation: popUp 0.15s ease-out; border-radius: 0; }
@keyframes popUp { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; border-bottom: 2px solid var(--border); padding-bottom: 16px; }
.modal-header h3 { font-size: 18px; font-weight: 900; display: flex; align-items: center; gap: 8px; margin: 0; }
.reason-content { flex: 1; display: flex; flex-direction: column; gap: 16px; margin-bottom: 8px; padding-right: 8px; }
.reason-item { background: var(--bg-surface); border: 2px dashed var(--border); padding: 20px; transition: all 0.1s; }
.reason-item:hover { border-style: solid; border-color: var(--text-primary); }
.reason-item-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.reason-item-header i { font-size: 16px; }
.reason-item-header h4 { font-size: 15px; font-weight: 900; margin: 0; }
.reason-item p { font-size: 13px; font-weight: 600; color: var(--text-muted); line-height: 1.6; margin: 0; }
.reason-item code { background: var(--text-primary); color: var(--bg-base); padding: 2px 6px; font-size: 12px; font-family: monospace; font-weight: bold; }
</style>