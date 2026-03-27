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
            <div class="title-section-row">
              <p>최근 7일간 활동을 기반으로 맞춤 추천을 제공합니다. 확인할 활동을 선택하세요.</p>
              <div class="title-action-group">
                <button class="btn-outline-small" @click="openCurriculumModal">
                  <i class="fas fa-book-open" /> 커리큘럼 추천받기
                </button>
                <button class="btn-outline-small" @click="$router.push('/recommend/new-learning')">
                  <i class="fas fa-plus" /> 학습 맥락 직접 추가
                </button>
              </div>
            </div>
          </div>

          <div class="recommend-grid">
            <button
              v-for="activity in store.recentActivities"
              :key="activity.id"
              class="base-panel activity-card"
              @click="selectActivity(activity.id)"
            >
              <div class="activity-top">
                <div class="activity-icon-wrap" :class="'type-' + activity.type">
                  <i :class="activityTypeConfig[activity.type].icon" />
                </div>
                <div class="activity-info">
                  <div class="activity-header">
                    <span class="activity-title">{{ activity.title }}</span>
                    <span v-if="activity.status === 'in-progress'" class="pill-tag active-tag">진행 중</span>
                    <span v-if="activity.status === 'done'" class="pill-tag done-tag">완료</span>
                  </div>
                  <div class="activity-meta">
                    <span class="activity-date"><i class="fas fa-calendar-alt" /> {{ activity.date }}</span>
                    <div class="tag-group-small">
                      <span v-for="tag in activity.tags" :key="tag" class="small-tag">
                        <i v-if="hasTechIcon(tag)" :class="getTechIcon(tag)" class="tech-icon" />{{ tag }}
                      </span>
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

          <section class="base-panel highlight-panel main-recommendation mb-gap">
            <div class="panel-header-flex">
              <div class="flex-align">
                <div class="icon-box-white"><i class="fas fa-lightbulb" /></div>
                <div>
                  <div class="panel-title-group">
                    <h3 class="panel-title">{{ selectedActivityCard?.title || '추천 학습' }}</h3>
                  </div>
                  <span class="panel-sub">{{ recommendationSubLabel }}</span>
                </div>
              </div>
              <div class="flex-align gap-sm">
                <span class="pill-tag badge-dark">AI 추천</span>
              </div>
            </div>

            <p class="panel-desc large-desc">
              {{ recommendationSummary }}
            </p>

            <div class="flow-box">
              <div class="flow-title">{{ recommendationFlowTitle }}</div>
              <div ref="flowTrackRef" class="flow-track scroll-x">
                <template v-for="(flow, index) in recommendationFlow" :key="index">
                  <div class="flow-item">
                    <span
                      v-if="flow.status === 'next'"
                      class="flow-node dashed-node"
                      :data-flow-current="index === activeFlowIndex ? 'true' : null"
                    >{{ flow.label }}</span>
                    <span
                      v-else
                      class="flow-node"
                      :class="'status-' + flow.status"
                      :data-flow-current="index === activeFlowIndex ? 'true' : null"
                    >{{ flow.label }}</span>
                    <span v-if="index < recommendationFlow.length - 1" class="flow-line"></span>
                  </div>
                </template>
              </div>
            </div>

            <div class="flex-footer">
              <div class="info-group">
                <span class="info-text"><i class="fas fa-clock" /> {{ recommendationTimeLabel }}</span>
                <span class="info-text"><i class="fas fa-chart-line" /> {{ recommendationStatusLabel }}</span>
              </div>
              <div class="tag-group">
                <span class="pill-tag tag-white" v-for="tag in recommendationTags" :key="tag">
                  <i v-if="hasTechIcon(tag)" :class="getTechIcon(tag)" class="tech-icon" />{{ tag }}
                </span>
              </div>
              <button class="btn-reason ml-auto" @click="showReasonModal = true">
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
                  class="base-panel mission-card"
                  :class="{ 'active': activeMission === mission.key }"
                >
                  <div class="mission-icon"><i :class="mission.icon" /></div>
                  <div class="mission-content">
                    <div class="mission-header">
                      <span class="mission-title">{{ mission.title }}</span>
                      <span v-if="mission.badge" class="pill-tag small-badge">{{ mission.badge }}</span>
                    </div>
                    <p class="mission-desc">{{ mission.desc }}</p>
                    <div class="mission-footer mt-md">
                      <div class="info-group">
                        <span class="info-text"><i class="fas fa-clock" /> {{ mission.time }}</span>
                        <span class="pill-tag outline-badge ml-sm">{{ mission.tag }}</span>
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
              <div class="base-panel panel-sticky shadow-heavy p-0">
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
                      <p>{{ quizIntroDescription }}</p>
                    </div>
                    <div class="grid-2 col-gap mb-md">
                      <div class="stat-box">
                        <span class="stat-lbl">예상 소요 시간</span>
                        <span class="stat-val">{{ quizExpectedMinutesLabel }}</span>
                      </div>
                      <div class="stat-box">
                        <span class="stat-lbl">문항 수</span>
                        <span class="stat-val">총 {{ store.quizQuestions.length }}문제</span>
                      </div>
                    </div>
                    <button class="btn-primary-block" :disabled="store.isLoading" @click="handleQuizStart">
                      {{ store.isLoading ? '퀴즈 생성 중...' : '퀴즈 시작하기' }}
                    </button>
                  </template>

                  <template v-else-if="quizStep === 'question'">
                    <div class="progress-bar-wrap mb-md">
                      <div class="flex-between mb-xs">
                        <span class="lbl-light">문제 {{ currentQuizIndex + 1 }} / {{ store.quizQuestions.length }}</span>
                        <span class="lbl-bold">진행 중</span>
                      </div>
                      <div class="track-bg"><div class="track-fill" :style="{ width: ((currentQuizIndex + 1) / store.quizQuestions.length) * 100 + '%' }"></div></div>
                    </div>

                    <div class="base-panel mb-md p-md">
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
                    <button class="btn-primary-block" :disabled="selectedOption === null || store.isSubmitting" @click="handleQuizSubmit">
                      {{ store.isSubmitting ? '제출 중...' : (currentQuizIndex < store.quizQuestions.length - 1 ? '다음 문제' : '결과 확인') }}
                    </button>
                  </template>

                  <template v-else-if="quizStep === 'result'">
                    <div class="info-box center-txt mb-md p-lg">
                      <div class="score-circle">{{ store.quizTotalScore !== null ? store.quizTotalScore + '점' : quizCorrectCount + '/' + store.quizQuestions.length }}</div>
                      <h3 class="panel-title mb-sm mt-md">퀴즈를 완료했어요!</h3>
                      <p>현재 이해 수준을 반영해서 다음 추천 흐름을 조정할 수 있습니다.</p>
                    </div>
                  </template>
                </div>

                <!-- 벨로그 래퍼런스 패널 -->
                <div v-if="activeMission === 'velog'" class="panel-body p-lg pt-0">
                  <div class="info-box mb-md">
                    <div class="info-title"><i class="fas fa-pen-nib" /> velog 글 작성을 위한 레퍼런스 추천</div>
                    <p>{{ referenceContextDescription }}</p>
                  </div>

                  <div class="ref-list space-y">
                    <div v-for="ref in store.references" :key="ref.title" class="base-panel p-md option-btn">
                      <div class="ref-card-top mb-sm">
                        <div class="flex-align gap-sm ref-title-row">
                          <i class="fas fa-file-alt text-muted ref-icon" />
                          <span class="font-bold ref-title">{{ ref.title }}</span>
                        </div>
                        <span class="ref-type-badge">{{ ref.type }}</span>
                      </div>
                      <div v-if="referenceContextLabel" class="ref-context-chip">
                        <i class="fas fa-crosshairs" /> {{ referenceContextLabel }} 기준 추천
                      </div>
                      <p class="text-muted text-sm ref-desc mb-sm">{{ ref.reason }}</p>
                      <div class="ref-card-bottom">
                        <div class="flex-align gap-md text-muted text-xs">
                          <span>{{ ref.freshness }}</span>
                          <span><i class="fas fa-shield-alt" /> 안전한 링크</span>
                        </div>
                        <button class="btn-text-muted" @click="openReference(ref.url)"><i class="fas fa-external-link-alt" /> 보기</button>
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
      <div class="base-modal shadow-heavy">
        <div class="modal-header">
          <h3><i class="fas fa-magic" /> 추천 근거 확인</h3>
          <button class="btn-close" @click="showReasonModal = false"><i class="fas fa-times"/></button>
        </div>
        <div class="reason-content custom-scroll">
          <div class="reason-item shadow-normal" v-if="reasonStatusSummary || recommendationTags.length">
            <div class="reason-item-header"><i class="fas fa-compass" /><h4>커리큘럼 포인트</h4></div>
            <div class="reason-meta-grid">
              <div v-if="reasonStatusSummary" class="reason-meta-box">
                <span class="reason-meta-label">현재 위치</span>
                <p>{{ reasonStatusSummary }}</p>
              </div>
              <div v-if="recommendationTags.length" class="reason-meta-box">
                <span class="reason-meta-label">핵심 기술</span>
                <div class="reason-tag-list">
                  <span class="pill-tag tag-white" v-for="tag in recommendationTags" :key="tag">
                    <i v-if="hasTechIcon(tag)" :class="getTechIcon(tag)" class="tech-icon" />{{ tag }}
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div class="reason-item shadow-normal" v-if="curriculumReasonSummary">
            <div class="reason-item-header"><i class="fas fa-lightbulb" /><h4>커리큘럼 요약</h4></div>
            <p>{{ curriculumReasonSummary }}</p>
          </div>
          <div class="reason-item shadow-normal" v-if="curriculumReasonUserContext">
            <div class="reason-item-header"><i class="fas fa-user-circle" /><h4>사용자 맥락</h4></div>
            <p>{{ curriculumReasonUserContext }}</p>
          </div>
          <div class="reason-item shadow-normal" v-if="curriculumReasonAiInterpretation">
            <div class="reason-item-header"><i class="fas fa-brain" /><h4>AI 해석</h4></div>
            <p>{{ curriculumReasonAiInterpretation }}</p>
          </div>
          <div class="reason-item shadow-normal" v-if="curriculumReasonRationale">
            <div class="reason-item-header"><i class="fas fa-bullseye" /><h4>커리큘럼 구성 근거</h4></div>
            <p>{{ curriculumReasonRationale }}</p>
          </div>
          <div class="reason-item shadow-normal" v-else-if="reasonSummary">
            <div class="reason-item-header"><i class="fas fa-lightbulb" /><h4>추천 요약</h4></div>
            <p>{{ reasonSummary }}</p>
          </div>
          <div class="reason-item shadow-normal" v-if="!curriculumReasonRationale && reasonDetail">
            <div class="reason-item-header"><i class="fas fa-bullseye" /><h4>추천 상세</h4></div>
            <p>{{ reasonDetail }}</p>
          </div>
          <div class="reason-item shadow-normal" v-if="!curriculumReasonAiInterpretation && currentStatusDetail">
            <div class="reason-item-header"><i class="fas fa-chart-line" /><h4>현재 학습 상태</h4></div>
            <p>{{ currentStatusDetail }}</p>
          </div>
        </div>
        <div class="modal-actions mt-md"><button class="btn-primary" @click="showReasonModal = false">확인</button></div>
      </div>
    </div>

    <!-- 모달: 커리큘럼 추천 (2개 옵션 나란히) -->
    <div v-if="showCurriculumModal" class="modal-overlay" @click.self="showCurriculumModal = false">
      <div class="base-modal curriculum-dual-modal shadow-heavy">
        <div class="modal-header">
          <h3><i class="fas fa-book-open" /> 추천 커리큘럼</h3>
          <button class="btn-close" @click="showCurriculumModal = false"><i class="fas fa-times"/></button>
        </div>
        <div v-if="isCurriculumModalLoading" class="curriculum-modal-state">
          <i class="fas fa-spinner fa-spin" />
          <p>현재 상태를 바탕으로 약점 보완형과 강점 강화형 커리큘럼을 만들고 있어요.</p>
        </div>
        <div v-else-if="curriculumModalError" class="curriculum-modal-state error-state">
          <i class="fas fa-exclamation-circle" />
          <p>{{ curriculumModalError }}</p>
        </div>
        <div v-else class="curriculum-dual-body">
          <!-- 옵션 A -->
          <div v-if="curriculumOptionA" class="curriculum-option">
            <div class="curriculum-option-header">
              <span class="option-badge badge-a">옵션 A</span>
              <h4 class="option-title">{{ curriculumOptionATitle }}</h4>
              <p class="option-subtitle">{{ curriculumOptionASubtitle }}</p>
              <span class="option-meta"><i class="fas fa-clock"/> {{ curriculumOptionAMeta }}</span>
            </div>
            <div class="curriculum-items">
              <div v-for="(item, i) in curriculumOptionA.nodes || []" :key="`a-${i}`" class="reason-item shadow-normal">
                <div class="flex-between mb-xs">
                  <span class="font-bold text-sm">{{ i + 1 }}일차</span>
                  <span class="text-muted text-xs"><i class="fas fa-clock"/> {{ formatNodeDuration(item.expectedMinutes) }}</span>
                </div>
                <h4 class="mb-xs">{{ item.title }}</h4>
                <p class="text-muted text-sm">{{ item.description }}</p>
              </div>
            </div>
            <div class="curriculum-option-footer">
              <button class="btn-primary w-full" :disabled="isCurriculumConfirming" @click="selectRecommendedCurriculum('WEAKNESS')">
                {{ isCurriculumConfirming ? '저장 중...' : '이 커리큘럼 선택' }}
              </button>
            </div>
          </div>

          <div class="curriculum-divider"></div>

          <div v-if="curriculumOptionB" class="curriculum-option">
            <div class="curriculum-option-header">
              <span class="option-badge badge-b">옵션 B</span>
              <h4 class="option-title">{{ curriculumOptionBTitle }}</h4>
              <p class="option-subtitle">{{ curriculumOptionBSubtitle }}</p>
              <span class="option-meta"><i class="fas fa-clock"/> {{ curriculumOptionBMeta }}</span>
            </div>
            <div class="curriculum-items">
              <div v-for="(item, i) in curriculumOptionB.nodes || []" :key="`b-${i}`" class="reason-item shadow-normal">
                <div class="flex-between mb-xs">
                  <span class="font-bold text-sm">{{ i + 1 }}일차</span>
                  <span class="text-muted text-xs"><i class="fas fa-clock"/> {{ formatNodeDuration(item.expectedMinutes) }}</span>
                </div>
                <h4 class="mb-xs">{{ item.title }}</h4>
                <p class="text-muted text-sm">{{ item.description }}</p>
              </div>
            </div>
            <div class="curriculum-option-footer">
              <button class="btn-primary w-full" :disabled="isCurriculumConfirming" @click="selectRecommendedCurriculum('STRENGTH')">
                {{ isCurriculumConfirming ? '저장 중...' : '이 커리큘럼 선택' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useRecommendStore } from '@/stores/useRecommendStore'
import AppSidebar from '@/components/AppSidebar.vue'
import { getTechIcon, hasTechIcon } from '@/utils/techIcons'
import { postCurriculumPreview, postCurriculaConfirm } from '@/api/aiApi'

const router = useRouter()
const store = useRecommendStore()

onMounted(() => {
  store.loadRecommendations()
})

const activityTypeConfig = {
  study: { icon: "fas fa-book-open" },
  dev:   { icon: "fas fa-code" },
  blog:  { icon: "fas fa-file-alt" },
  review:{ icon: "fas fa-clock" },
}

const selectedActivity = ref(null)
const showReasonModal = ref(false)
const showCurriculumModal = ref(false)
const flowTrackRef = ref(null)
const curriculumPreviewData = ref(null)
const isCurriculumModalLoading = ref(false)
const isCurriculumConfirming = ref(false)
const curriculumModalError = ref('')

const activeMission = ref(null)
const quizStep = ref("intro") // 'intro' | 'question' | 'result'
const currentQuizIndex = ref(0)
const selectedOption = ref(null)
const quizAnswers = ref([])

const selectedActivityCard = computed(() =>
  store.recentActivities.find(activity => activity.id === selectedActivity.value) || null
)

const detailData = computed(() => store.recommendationDetail || {})
const curriculumReasonData = computed(() => store.curriculumReason || {})
const recommendationReason = computed(() => detailData.value.recommendationReason || {})
const currentStatus = computed(() => detailData.value.currentStatus || {})
const primaryQuiz = computed(() => detailData.value.quizzes?.[0] || null)

const recommendationSubLabel = computed(() => {
  if (selectedActivityCard.value?.status === 'in-progress') return '맞춤 추천 · 현재 진행 중인 커리큘럼 기반'
  return '맞춤 추천 · 완료한 커리큘럼 회고 기반'
})

const curriculumNodes = computed(() => store.selectedCurriculumNodes || [])

const todayDateString = computed(() => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
})

const formatFlowNodeLabel = (node) => {
  if (!node?.scheduledDate) return node?.title || ''
  const [, month, day] = node.scheduledDate.split('-')
  return `${Number(month)}/${Number(day)} · ${node.title}`
}

const currentCurriculumIndex = computed(() => {
  if (!curriculumNodes.value.length) return -1

  const exactIndex = curriculumNodes.value.findIndex(node => node.scheduledDate === todayDateString.value)
  if (exactIndex !== -1) return exactIndex

  const upcomingIndex = curriculumNodes.value.findIndex(node => node.scheduledDate > todayDateString.value)
  if (upcomingIndex !== -1) return upcomingIndex

  return curriculumNodes.value.length - 1
})

const recommendationSummary = computed(() => {
  if (curriculumNodes.value.length) {
    const currentNode = curriculumNodes.value[currentCurriculumIndex.value] || curriculumNodes.value[0]
    const currentDay = currentCurriculumIndex.value >= 0 ? currentCurriculumIndex.value + 1 : 1
    const totalDays = curriculumNodes.value.length

    if (todayDateString.value > (curriculumNodes.value[curriculumNodes.value.length - 1]?.scheduledDate || '')) {
      return `이 커리큘럼은 총 ${totalDays}일 흐름으로 완료된 상태예요. 진행했던 학습 노드를 다시 확인하고 퀴즈나 레퍼런스로 복습을 이어갈 수 있습니다.`
    }

    return `현재 커리큘럼의 ${currentDay}일차 위치는 "${currentNode?.title || '학습 노드'}"입니다. 날짜 순서대로 이어지는 전체 흐름 안에서 지금 학습할 지점을 바로 확인할 수 있어요.`
  }

  return recommendationReason.value.summary
    || currentStatus.value.summary
    || '현재 커리큘럼 흐름을 불러오는 중입니다.'
})

const recommendationFlowTitle = computed(() =>
  curriculumNodes.value.length ? '추천 학습 흐름' : '현재 핵심 기술'
)

const recommendationFlow = computed(() => {
  if (curriculumNodes.value.length) {
    const currentIndex = currentCurriculumIndex.value

    return curriculumNodes.value.map((node, index) => {
      let status = 'next'

      if (todayDateString.value > (curriculumNodes.value[curriculumNodes.value.length - 1]?.scheduledDate || '')) {
        status = 'done'
      } else if (currentIndex === -1) {
        status = index === 0 ? 'in-progress' : 'next'
      } else if (index < currentIndex) {
        status = 'done'
      } else if (index === currentIndex) {
        status = 'in-progress'
      }

      return {
        label: formatFlowNodeLabel(node),
        status,
      }
    })
  }

  const flow = []

  if (currentStatus.value.topSkills?.length) {
    currentStatus.value.topSkills.forEach((skill, index) => {
      flow.push({ label: skill, status: index === 0 ? 'in-progress' : 'done' })
    })
  }

  return flow
})

const activeFlowIndex = computed(() => {
  if (curriculumNodes.value.length) {
    return currentCurriculumIndex.value >= 0 ? currentCurriculumIndex.value : 0
  }

  const inProgressIndex = recommendationFlow.value.findIndex(flow => flow.status === 'in-progress')
  return inProgressIndex >= 0 ? inProgressIndex : 0
})

const recommendationTimeLabel = computed(() => {
  if (primaryQuiz.value?.expectedMinutes) return `추천 퀴즈 ${primaryQuiz.value.expectedMinutes}분`
  if (store.references.length) return `추천 자료 ${store.references.length}개`
  return '추천 정보 준비 중'
})

const recommendationStatusLabel = computed(() => {
  if (curriculumNodes.value.length) {
    if (todayDateString.value > (curriculumNodes.value[curriculumNodes.value.length - 1]?.scheduledDate || '')) {
      return '전체 흐름 완료'
    }
    return `현재 ${currentCurriculumIndex.value + 1}일차 진행 위치`
  }

  if (currentStatus.value.summary) return currentStatus.value.summary
  return selectedActivityCard.value?.status === 'done' ? '완료한 흐름 기반' : '진행 중인 흐름 기반'
})

const recommendationTags = computed(() =>
  selectedActivityCard.value?.tags?.length
    ? selectedActivityCard.value.tags
    : (currentStatus.value.topSkills || [])
)

const currentCurriculumNodeLabel = computed(() => {
  if (!curriculumNodes.value.length) return ''
  const node = curriculumNodes.value[currentCurriculumIndex.value] || curriculumNodes.value[0]
  if (!node) return ''
  return `${currentCurriculumIndex.value + 1}일차 ${node.title}`
})

const referenceContextLabel = computed(() => currentCurriculumNodeLabel.value)
const referenceContextDescription = computed(() => {
  if (referenceContextLabel.value) {
    return `글을 대신 써주는 것이 아니라, 현재 학습 위치인 ${referenceContextLabel.value}에 맞는 참고 자료를 추천해서 글 구조를 잡는 데 도움을 줍니다.`
  }
  return '글을 대신 써주는 것이 아니라, 지금 학습 흐름에 맞는 참고 자료를 추천해서 글 구조를 잡는 데 도움을 줍니다.'
})

const reasonStatusSummary = computed(() =>
  recommendationStatusLabel.value || currentStatus.value.summary || ''
)
const curriculumReasonSummary = computed(() => curriculumReasonData.value.summaryLine || '')
const curriculumReasonUserContext = computed(() => curriculumReasonData.value.userContext || '')
const curriculumReasonAiInterpretation = computed(() => curriculumReasonData.value.aiInterpretation || '')
const curriculumReasonRationale = computed(() => curriculumReasonData.value.curriculumRationale || '')
const reasonSummary = computed(() => recommendationReason.value.summary || '')
const reasonDetail = computed(() => recommendationReason.value.detail || '')
const currentStatusDetail = computed(() => currentStatus.value.detail || '')

const curriculumOptionA = computed(() => {
  const data = curriculumPreviewData.value
  if (!data) return null
  return data.optionA || (data.nodes?.length ? {
    optionType: 'WEAKNESS',
    optionLabel: '약점 보완형',
    recommendationReason: data.recommendationReason,
    techStacks: data.techStacks || [],
    nodes: data.nodes || [],
  } : null)
})

const curriculumOptionB = computed(() => curriculumPreviewData.value?.optionB || null)

const buildCurriculumMeta = (option) => {
  const nodes = option?.nodes || []
  if (!nodes.length) return '일정 정보 없음'
  const totalMinutes = nodes.reduce((sum, node) => sum + (node.expectedMinutes || 0), 0)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  const durationLabel = hours > 0
    ? `약 ${hours}시간${minutes ? ` ${minutes}분` : ''}`
    : `약 ${minutes}분`
  return `${nodes.length}일 · ${durationLabel}`
}

const curriculumOptionATitle = computed(() =>
  curriculumOptionA.value?.recommendationReason?.summaryLine || '약점 보완형 커리큘럼'
)
const curriculumOptionASubtitle = computed(() =>
  curriculumOptionA.value?.optionLabel || '약점 보완형'
)
const curriculumOptionAMeta = computed(() => buildCurriculumMeta(curriculumOptionA.value))

const curriculumOptionBTitle = computed(() =>
  curriculumOptionB.value?.recommendationReason?.summaryLine || '강점 강화형 커리큘럼'
)
const curriculumOptionBSubtitle = computed(() =>
  curriculumOptionB.value?.optionLabel || '강점 강화형'
)
const curriculumOptionBMeta = computed(() => buildCurriculumMeta(curriculumOptionB.value))

const quizIntroDescription = computed(() =>
  primaryQuiz.value?.description
  || '현재 추천 흐름을 바탕으로 핵심 개념을 점검할 수 있는 퀴즈입니다.'
)

const quizExpectedMinutesLabel = computed(() =>
  primaryQuiz.value?.expectedMinutes ? `약 ${primaryQuiz.value.expectedMinutes}분` : '약 15분'
)

const formatNodeDuration = (minutes) => {
  const value = Number(minutes || 0)
  const hours = Math.floor(value / 60)
  const remain = value % 60
  if (hours > 0 && remain > 0) return `${hours}시간 ${remain}분`
  if (hours > 0) return `${hours}시간`
  return `${remain}분`
}

const selectActivity = async (activityId) => {
  selectedActivity.value = activityId
  const activity = store.recentActivities.find(item => item.id === activityId)

  await Promise.all([
    store.loadRecommendationDetail(activityId),
    store.loadCurriculumReason(activityId),
    store.loadCurriculumNodes(activityId, activity?.startDate, activity?.endDate),
  ])
}

const scrollFlowToCurrent = async () => {
  await nextTick()

  const container = flowTrackRef.value
  if (!container) return

  const currentNode = container.querySelector('[data-flow-current="true"]')
  if (!currentNode) {
    container.scrollLeft = 0
    return
  }

  const targetLeft =
    currentNode.offsetLeft - container.clientWidth / 2 + currentNode.clientWidth / 2

  container.scrollTo({
    left: Math.max(0, targetLeft),
    behavior: 'smooth',
  })
}

watch(
  () => [selectedActivity.value, recommendationFlow.value.length, activeFlowIndex.value],
  () => {
    scrollFlowToCurrent()
  }
)

const handleBack = () => {
  selectedActivity.value = null
  store.curriculumReason = null
  store.selectedCurriculumNodes = []
  curriculumPreviewData.value = null
  curriculumModalError.value = ''
  closeMission()
}

const openCurriculumModal = async () => {
  showCurriculumModal.value = true
  isCurriculumModalLoading.value = true
  curriculumModalError.value = ''

  try {
    const { data } = await postCurriculumPreview({})
    curriculumPreviewData.value = data
  } catch (e) {
    console.error('추천 커리큘럼 미리보기 생성 실패:', e)
    curriculumPreviewData.value = null
    curriculumModalError.value = '커리큘럼 추천안을 불러오지 못했어요. 잠시 후 다시 시도해주세요.'
  } finally {
    isCurriculumModalLoading.value = false
  }
}

const selectRecommendedCurriculum = async (optionType) => {
  const previewKey = curriculumPreviewData.value?.curriculumPreviewKey
  if (!previewKey) return

  isCurriculumConfirming.value = true
  try {
    await postCurriculaConfirm({
      curriculumPreviewKey: previewKey,
      selectedOptionType: optionType,
    })
    showCurriculumModal.value = false
    router.push('/calendar')
  } catch (e) {
    console.error('추천 커리큘럼 저장 실패:', e)
    curriculumModalError.value = '선택한 커리큘럼 저장에 실패했어요. 다시 시도해주세요.'
  } finally {
    isCurriculumConfirming.value = false
  }
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

const handleQuizStart = async () => {
  const curriculumId = selectedActivity.value
  await store.startQuizSession(curriculumId)
  quizStep.value = 'question'
  currentQuizIndex.value = 0
  selectedOption.value = null
  quizAnswers.value = []
}

const handleQuizSubmit = async () => {
  if (selectedOption.value === null) return

  // 인덱스 → 텍스트 변환하여 BE에 제출
  const question = store.quizQuestions[currentQuizIndex.value]
  question._selectedOption = selectedOption.value
  quizAnswers.value.push(selectedOption.value)

  await store.submitQuizAnswer(currentQuizIndex.value)

  if (currentQuizIndex.value < store.quizQuestions.length - 1) {
    currentQuizIndex.value++
    selectedOption.value = null
  } else {
    await store.completeQuizSession()
    quizStep.value = 'result'
  }
}

const openReference = (url) => {
  if (!url) return
  window.open(url, '_blank', 'noopener,noreferrer')
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

.page-header { 
  display: flex; align-items: center; 
  height: 64px; min-height: 64px; flex-shrink: 0;
  padding: 0 24px; border-bottom: 1px solid var(--border); 
  background: var(--bg-surface); position: sticky; top: 0; z-index: 10; 
  box-sizing: border-box; 
}
.header-title { 
  font-size: 16px; font-weight: 800; letter-spacing: 0.1em; color: var(--text-primary); 
  display: flex; align-items: center; gap: 12px; 
}
.header-title i { font-size: 18px; width: 24px; text-align: center; }
.header-title .fa-compass { color: var(--clr-icon-ai); }

.content-inner { max-width: 1000px; margin: 0 auto; padding: 48px 32px; width: 100%; }
.title-section { margin-bottom: 32px; }
.title-section h2 { font-size: 24px; font-weight: 800; color: var(--text-primary); margin-bottom: 8px; letter-spacing: 0.05em; }
.title-section p { font-size: 14px; font-weight: 600; color: var(--text-muted); }
.title-section-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.title-action-group { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }

/* Buttons & Tags */
.btn-back { background: transparent; border: none; font-size: 14px; font-weight: 700; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px; margin-bottom: 32px; padding: 0; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.btn-back:hover { color: var(--text-primary); transform: translateX(-4px); }

.base-panel { background: var(--bg-surface); border: 1px solid var(--border); padding: 32px; display: flex; flex-direction: column; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 8px; }
.base-panel:hover { border-color: var(--text-primary); background: var(--bg-hover); }

.pill-tag { padding: 4px 10px; border: 1px solid var(--border); background: transparent; font-size: 11px; font-weight: 700; border-radius: 4px; }
.badge-dark { background: var(--clr-icon-ai); color: var(--bg-base); border-color: var(--clr-icon-ai); }
.tag-white { border-color: var(--border); color: var(--text-primary); background: transparent; }

/* Common Utilities */
.flex-align { display: flex; align-items: center; gap: 12px; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.gap-sm { gap: 8px; }
.gap-md { gap: 12px; }
.mb-xs { margin-bottom: 8px; }
.mb-sm { margin-bottom: 12px; }
.mb-md { margin-bottom: 20px; }
.mb-lg { margin-bottom: 32px; }
.mb-gap { margin-bottom: 32px; }
.ml-auto { margin-left: auto; }
.ml-lg { margin-left: 36px; }
.mt-md { margin-top: 20px; }
.p-md { padding: 18px; }
.p-lg { padding: 28px; }
.p-0 { padding: 0 !important; }
.pt-0 { padding-top: 0 !important; }
.border-b { border-bottom: 1px solid var(--border); }
.space-y > * + * { margin-top: 16px; }
.font-bold { font-weight: 700; }
.text-sm { font-size: 13px; }
.text-xs { font-size: 11px; }
.text-muted { color: var(--text-muted); }
.center-txt { text-align: center; }
.shadow-heavy { box-shadow: none; border: 1px solid var(--border); }
.shadow-heavy:hover { border-color: var(--text-primary); }
.shadow-normal { box-shadow: none; border: 1px solid var(--border); }
.shadow-normal:hover { border-color: var(--text-primary); background: var(--bg-hover); transform: translateX(2px); }

/* Buttons Set */
button { font-family: 'Space Grotesk', 'Pretendard', sans-serif; cursor: pointer; border-radius: 8px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); letter-spacing: 0.05em;}
.btn-primary { background: var(--clr-primary); color: var(--bg-base); border: 1px solid var(--clr-primary); padding: 14px 24px; font-weight: 700; font-size: 13px;}
.btn-primary:hover { background: transparent; color: var(--clr-primary); }

.btn-primary-small { background: var(--clr-primary); color: var(--bg-base); border: 1px solid var(--clr-primary); padding: 8px 16px; font-size: 12px; font-weight: 700; display: inline-flex; align-items: center; gap: 6px; border-radius: 4px; }
.btn-primary-small:hover { background: transparent; color: var(--clr-primary); }
.btn-dark { background: transparent; color: var(--clr-primary); border-color: var(--clr-primary); }

.btn-primary-small-invert { background: transparent; color: var(--text-primary); border: 1px solid var(--text-primary); padding: 8px 16px; font-size: 12px; font-weight: 700; display: inline-flex; align-items: center; gap: 6px; border-radius: 4px;}
.btn-primary-small-invert:hover { background: var(--bg-hover); }
.btn-reason { background: var(--clr-primary); color: var(--bg-base); border: 1px solid var(--clr-primary); padding: 10px 18px; font-size: 13px; font-weight: 700; display: inline-flex; align-items: center; gap: 8px; border-radius: 8px; }
.btn-reason:hover { background: transparent; color: var(--clr-primary); }

.btn-primary-block { width: 100%; display: block; background: var(--clr-primary); color: var(--bg-base); border: 1px solid var(--clr-primary); padding: 16px; font-size: 14px; font-weight: 700; border-radius: 8px;}
.btn-primary-block:hover:not(:disabled) { background: transparent; color: var(--clr-primary); }
.btn-primary-block:disabled { background: transparent; border-color: var(--border); color: var(--text-faint); cursor: not-allowed; }

.btn-outline-small { background: transparent; color: var(--text-primary); border: 1px solid var(--border); padding: 8px 16px; font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; border-radius: 4px;}
.btn-outline-small:hover { border-color: var(--text-primary); background: var(--bg-hover); }

.btn-outline-invert { background: transparent; color: var(--text-primary); border: 1px solid var(--border); padding: 10px 18px; font-size: 13px; font-weight: 700; display: inline-flex; align-items: center; gap: 8px; border-radius: 8px;}
.btn-outline-invert.ml-auto { margin-left: auto; }
.btn-outline-invert:hover { border-color: var(--text-primary); background: var(--bg-hover); }

.btn-text { background: transparent; border: none; padding: 4px 8px; font-size: 12px; font-weight: 600; color: var(--text-muted); }
.btn-text:hover { color: var(--text-primary); }
.btn-text-muted { background: transparent; border: none; font-size: 12px; color: var(--text-muted); display: flex; align-items: center; gap: 4px; padding: 0; }
.btn-text-muted:hover { color: var(--text-primary); }

.btn-close { background: transparent; border: none; font-size: 20px; color: var(--text-muted); padding: 4px; line-height: 1; transition: color 0.3s; }
.btn-close:hover { color: var(--text-primary); }

/* Activity Card */
.recommend-grid { display: flex; flex-direction: column; gap: 16px; }
.activity-card { padding: 20px; cursor: pointer; background: var(--bg-surface); border: 1px solid var(--border); transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 8px;}
.activity-card:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.activity-card:hover .arrow-icon { color: var(--text-primary); transform: translateX(2px); }
.activity-top { display: flex; align-items: center; gap: 20px; }
.activity-icon-wrap { width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; font-size: 18px; border: 1px solid var(--border); flex-shrink: 0; border-radius: 4px; background: transparent; color: var(--text-primary);}
.type-study  { border-color: #86EFAC; color: #86EFAC; }
.type-dev    { border-color: var(--border); }
.type-blog   { border-color: #60A5FA; color: #60A5FA; }
.type-review { border-color: #FB923C; color: #FB923C; font-weight: 700; }

.activity-info { flex: 1; min-width: 0; }
.activity-header { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.activity-title { font-size: 15px; font-weight: 700; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; letter-spacing: 0.05em;}
.active-tag { background: transparent; color: var(--clr-warning); padding: 3px 8px; font-size: 9px; border-color: var(--clr-warning); }
.done-tag { background: transparent; color: var(--text-muted); padding: 3px 8px; font-size: 9px; border-color: var(--border); }
.activity-meta { display: flex; align-items: center; gap: 16px; }
.activity-date { font-size: 12px; color: var(--text-muted); font-weight: 500; display: flex; align-items: center; gap: 6px; }
.tag-group { display: flex; gap: 8px; flex-wrap: wrap; }
.tag-group-small { display: flex; gap: 8px; flex-wrap: wrap; }
.small-tag { padding: 3px 8px; background: transparent; border: 1px solid var(--border); font-size: 10px; color: var(--text-muted); font-weight: 500; border-radius: 4px;}
.arrow-icon { font-size: 18px; color: var(--border); transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }

/* Highlighted Main Recommendation */
.highlight-panel { background: var(--bg-surface); color: var(--text-primary); border: 1px solid var(--text-primary); padding: 36px; border-radius: 8px; }
.panel-header-flex { display: flex; justify-content: space-between; align-items: flex-start; }
.icon-box-white { width: 44px; height: 44px; background: transparent; color: var(--text-primary); display: flex; align-items: center; justify-content: center; font-size: 20px; border: 1px solid var(--border); border-radius: 4px;}
.icon-box-white .fa-lightbulb { color: var(--clr-icon-ai); }
.info-title .fa-brain { color: #F9A8D4; }
.info-text .fa-chart-line { color: var(--clr-icon-growth); }
.panel-title-group { display: flex; align-items: center; gap: 16px; margin-bottom: 8px; }
.panel-title { font-size: 19px; font-weight: 700; margin: 0; letter-spacing: 0.05em; }
.panel-sub { font-size: 12px; font-weight: 500; color: var(--text-muted); }
.large-desc { font-size: 14px; line-height: 1.6; color: var(--text-secondary); margin: 28px 0; font-weight: 500; }

.flow-box { background: transparent; padding: 20px; border: 1px dashed var(--border); margin-bottom: 28px; border-radius: 8px; }
.flow-title { font-size: 11px; font-weight: 700; margin-bottom: 14px; color: var(--text-muted); letter-spacing: 0.1em; text-transform: uppercase;}
.flow-track { display: flex; align-items: center; overflow-x: auto; padding-bottom: 8px; }
.scroll-x::-webkit-scrollbar { height: 4px; }
.scroll-x::-webkit-scrollbar-thumb { background: var(--border); }
.flow-item { display: flex; align-items: center; flex-shrink: 0; }
.flow-node { padding: 5px 12px; font-size: 11px; font-weight: 600; border: 1px solid var(--border); background: transparent; border-radius: 4px;}
.status-done { background: transparent; color: var(--text-muted); border-color: var(--border); text-decoration: line-through;}
.status-in-progress { border-color: var(--text-primary); color: var(--text-primary); font-weight: 700;}
.dashed-node { border-style: dashed; padding: 5px 12px; font-weight: 700; color: var(--text-primary);}
.flow-line { width: 20px; height: 1px; background: var(--border); margin: 0 6px; }

.flex-footer { display: flex; align-items: center; gap: 24px; flex-wrap: wrap; }
.info-group { display: flex; align-items: center; gap: 16px; }
.info-text { font-size: 12px; font-weight: 500; display: flex; align-items: center; gap: 6px; color: var(--text-muted);}
.highlight-panel .info-text { color: var(--text-muted); }

/* Dashboard Split Layout */
.layout-split { display: grid; grid-template-columns: 1fr; gap: 32px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.layout-split.has-active { grid-template-columns: 1.4fr 1.6fr; }
@media (max-width: 900px) { .layout-split.has-active { grid-template-columns: 1fr; } }

.missions-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.missions-grid.compact { grid-template-columns: 1fr; }

.mission-card { padding: 28px; flex-direction: column; gap: 20px; background: var(--bg-surface); border: 1px solid var(--border); }
.mission-card.active { border-color: var(--text-primary); background: var(--bg-surface); }
.mission-icon { width: 44px; height: 44px; background: transparent; border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 18px; color: var(--text-primary); flex-shrink: 0; border-radius: 4px;}
.mission-card.active .mission-icon { border-color: var(--text-primary); }
.mission-icon:has(.fa-brain) { border-color: #F9A8D4; color: #F9A8D4; }
.mission-icon:has(.fa-pen-nib) { border-color: var(--clr-icon-velog); color: var(--clr-icon-velog); }
.mission-content { flex: 1; min-width: 0; }
.mission-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.mission-title { font-size: 15px; font-weight: 700; letter-spacing: 0.05em;}
.small-badge { font-size: 9px; padding: 3px 8px; border-radius: 4px;}
.mission-desc { font-size: 12px; color: var(--text-muted); line-height: 1.6; margin-bottom: 20px; font-weight: 500; }
.mission-footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-top: auto; }
.outline-badge { border-color: var(--border); color: var(--text-muted); background: transparent; padding: 3px 8px; font-size: 9px; font-weight: 600; border-radius: 4px; }

/* Ref card layout */
.ref-card-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.ref-title-row { flex: 1; min-width: 0; align-items: flex-start; }
.ref-icon { flex-shrink: 0; margin-top: 2px; }
.ref-title { font-size: 13px; line-height: 1.4; word-break: keep-all; }
.ref-type-badge { flex-shrink: 0; padding: 2px 7px; border: 1px solid var(--border); border-radius: 4px; font-size: 9px; font-weight: 700; color: var(--text-muted); white-space: nowrap; margin-top: 2px; }
.ref-context-chip { display: inline-flex; align-items: center; gap: 6px; padding: 4px 8px; margin-left: 20px; margin-bottom: 10px; border: 1px dashed var(--border); border-radius: 999px; font-size: 10px; font-weight: 700; color: var(--text-muted); }
.ref-desc { padding-left: 20px; line-height: 1.6; }
.ref-card-bottom { display: flex; align-items: center; justify-content: space-between; padding-left: 20px; }

/* Active Panel (Sticky) */
.active-panel { position: sticky; top: 84px; }
.panel-sticky { min-height: 480px; background: var(--bg-surface); height: auto; max-height: calc(100vh - 120px); display: flex; flex-direction: column; overflow-y: auto; border-radius: 8px; }
.panel-title-lg { font-size: 18px; font-weight: 700; margin-bottom: 6px; letter-spacing: 0.05em;}
.panel-sub { font-size: 12px; color: var(--text-muted); font-weight: 500; }
.info-box { background: var(--bg-hover); border: 1px dashed var(--border); padding: 20px; border-radius: 8px;}
.info-title { font-size: 13px; font-weight: 700; margin-bottom: 10px; display: flex; align-items: center; gap: 8px; color: var(--text-primary);}
.info-box p { font-size: 12px; color: var(--text-muted); line-height: 1.6; font-weight: 500;}

.grid-2 { display: grid; grid-template-columns: 1fr 1fr; }
.col-gap { gap: 16px; }
.stat-box { background: var(--bg-surface); border: 1px solid var(--border); padding: 16px; display: flex; flex-direction: column; gap: 6px; border-radius: 8px; }
.stat-lbl { font-size: 10px; color: var(--text-muted); font-weight: 600; text-transform: uppercase; letter-spacing: 0.1em;}
.stat-val { font-size: 15px; font-weight: 700; color: var(--text-primary); }

.lbl-light { font-size: 11px; color: var(--text-muted); font-weight: 500; }
.lbl-bold { font-size: 11px; font-weight: 700; color: var(--text-primary); }
.track-bg { height: 4px; background: var(--bg-hover); border: none; width: 100%; border-radius: 2px; overflow: hidden; }
.track-fill { height: 100%; background: var(--clr-primary); transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1); }

.quiz-q { font-size: 16px; font-weight: 700; line-height: 1.6; margin-bottom: 24px; margin-top: 0; color: var(--text-primary); letter-spacing: 0.05em;}
.options-list { display: flex; flex-direction: column; gap: 12px; }
.option-btn { background: var(--bg-surface); border: 1px solid var(--border); padding: 16px; cursor: pointer; text-align: left; display: flex; align-items: flex-start; gap: 14px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 8px; }
.option-btn:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.option-btn.selected { border-color: var(--text-primary); background: var(--bg-hover); font-weight: 700; }
.radio-circle { width: 16px; height: 16px; border: 1px solid var(--border); border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-top: 2px; }
.option-btn.selected .radio-circle { border-color: var(--text-primary); }
.radio-inner { width: 8px; height: 8px; background: var(--text-primary); border-radius: 50%; }
.option-btn span { font-size: 13px; line-height: 1.5; color: var(--text-primary); font-weight: 500; }
.option-btn.selected span { color: var(--text-primary); }

.score-circle { width: 72px; height: 72px; border: 1px solid var(--text-primary); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 22px; font-weight: 700; margin: 0 auto; margin-bottom: 28px; color: var(--text-primary); }

/* Modal specific */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.8); backdrop-filter: blur(8px); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 24px; }
.base-modal { width: 100%; max-width: 600px; max-height: 85vh; display: flex; flex-direction: column; background: var(--bg-surface); border: 1px solid var(--border); padding: 40px; animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both; border-radius: 8px; overflow-y: auto; }
.reason-meta-grid { display: grid; grid-template-columns: 1fr; gap: 12px; }
.reason-meta-box { border: 1px solid var(--border); border-radius: 8px; padding: 14px; background: var(--bg-hover); }
.reason-meta-label { display: block; margin-bottom: 8px; font-size: 10px; font-weight: 700; color: var(--text-muted); letter-spacing: 0.08em; text-transform: uppercase; }
.reason-tag-list { display: flex; flex-wrap: wrap; gap: 8px; }

/* Dual curriculum modal */
.curriculum-dual-modal { max-width: 960px; overflow: hidden; }
.curriculum-dual-modal .reason-content { display: none; }
.curriculum-dual-body { display: grid; grid-template-columns: 1fr auto 1fr; gap: 0; height: 480px; }
.curriculum-modal-state { min-height: 320px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; color: var(--text-muted); text-align: center; font-weight: 600; }
.curriculum-modal-state i { font-size: 24px; color: var(--text-primary); }
.curriculum-modal-state.error-state i { color: var(--clr-danger, #ff6b6b); }
.curriculum-option { display: flex; flex-direction: column; padding: 4px; min-height: 0; }
.curriculum-option-header { flex-shrink: 0; margin-bottom: 16px; }
.option-badge { display: inline-block; padding: 3px 10px; font-size: 10px; font-weight: 800; border-radius: 4px; letter-spacing: 0.1em; margin-bottom: 8px; }
.badge-a { background: var(--clr-primary); color: var(--bg-base); border: 1px solid var(--clr-primary); }
.badge-b { background: transparent; color: var(--clr-icon-ai); border: 1px solid var(--clr-icon-ai); }
.option-title { font-size: 16px; font-weight: 800; color: var(--text-primary); margin: 4px 0; letter-spacing: 0.05em; }.option-meta { font-size: 12px; color: var(--text-muted); font-weight: 600; display: flex; align-items: center; gap: 6px; }
.curriculum-items { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; min-height: 0; padding-right: 4px; -ms-overflow-style: none; scrollbar-width: none; }
.curriculum-items::-webkit-scrollbar { display: none; }
.curriculum-option-footer { flex-shrink: 0; border-top: 1px solid var(--border); margin-top: auto; padding-top: 16px; }
.curriculum-divider { width: 1px; background: var(--border); margin: 0 24px; }
.w-full { width: 100%; justify-content: center; }
@keyframes fadeUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 32px; border-bottom: 1px solid var(--border); padding-bottom: 24px; }
.modal-header h3 { font-size: 20px; font-weight: 800; display: flex; align-items: center; gap: 12px; margin: 0; letter-spacing: 0.05em; color: var(--text-primary);}
.reason-content { flex: 1; display: flex; flex-direction: column; gap: 24px; margin-bottom: 16px; padding-right: 8px; }
.reason-item { background: transparent; border: 1px solid var(--border); padding: 24px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 8px;}
.reason-item:hover { border-color: var(--text-primary); background: var(--bg-hover);}
.reason-item-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.reason-item-header i { font-size: 18px; color: var(--text-primary);}
.reason-item-header h4 { font-size: 16px; font-weight: 800; margin: 0; letter-spacing: 0.05em;}
.reason-item p { font-size: 14px; font-weight: 600; color: var(--text-muted); line-height: 1.6; margin: 0; }
.reason-item code { background: transparent; color: var(--text-primary); border: 1px solid var(--border); padding: 2px 8px; font-size: 12px; font-family: 'Space Grotesk', monospace; font-weight: 800; border-radius: 4px;}
</style>
