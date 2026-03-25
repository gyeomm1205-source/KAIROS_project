<template>
  <div class="setup-root">
    
    <div class="global-stepper-wrap">
      <div class="page-stepper">
        <div class="step done">1. 계정 연동</div>
        <div class="step active">2. 사전 설문</div>
        <div class="step">3. 데이터 분석</div>
        <div class="step">4. 결과 확인</div>
      </div>
    </div>

    <div class="setup-card custom-scroll">
      
      <div class="header-top">
        <button @click="$router.push('/onboarding/connect')" class="btn-back">
          <i class="fas fa-arrow-left" /> BACK
        </button>
      </div>

      <div class="setup-header">
        <h2>맞춤 분석을 위한 기본 정보</h2>
        <p>입력한 정보는 학습 추천에만 사용됩니다.</p>
      </div>

      <div class="form-section">
        <label class="section-title">연동 정보 범위 선택</label>
        <p class="section-desc">추천과 분석에 포함할 활동 유형을 선택하세요.</p>
        <div class="scope-grid">
          <button
            v-for="opt in scopeOptions" :key="opt.value"
            class="scope-box"
            :class="{ active: selectedScopes.includes(opt.value) }"
            @click="toggleScope(opt.value)"
          >
            <div class="box-header">
              <div class="checkbox-square">
                <i v-if="selectedScopes.includes(opt.value)" class="fas fa-check" />
              </div>
              <span class="box-title">{{ opt.label }}</span>
            </div>
            <p class="box-desc">{{ opt.desc }}</p>
          </button>
        </div>
      </div>

      <div class="form-section">
        <label class="section-title">일정 반영 범위</label>
        <p class="section-desc">
          커리큘럼 제작 시 개발/공부/취준 외 내용도 고려할까요?<br>
          (예: 친구 결혼식, 가족 일정 등 이미 잡혀 있는 개인 일정)
        </p>
        <div class="flex-row">
          <button 
            class="choice-btn flex-1"
            :class="{ active: scheduleInclusion === 'yes' }"
            @click="scheduleInclusion = 'yes'"
          >
            예
          </button>
          <button 
            class="choice-btn flex-1"
            :class="{ active: scheduleInclusion === 'no' }"
            @click="scheduleInclusion = 'no'"
          >
            아니오
          </button>
        </div>
      </div>

      <div class="form-section">
        <label class="section-title">현재 직업 <span class="required-mark">*</span></label>
        <p class="section-desc">하나를 선택해주세요.</p>
        <div class="job-list">
          <label v-for="j in jobOptions" :key="j" class="radio-label">
            <input type="radio" name="job" :value="j" v-model="job" class="hidden-radio" />
            <div class="radio-custom">
              <div class="radio-dot" v-if="job === j"></div>
            </div>
            <span>{{ j }}</span>
          </label>
        </div>
      </div>

      <div class="form-section">
        <label class="section-title">기술 스택 <span class="required-mark">*</span></label>
        <p class="section-desc">사용 중인 기술을 추가하세요.</p>
        <div class="input-row">
          <input
            v-model="techInput"
            type="text"
            class="text-input"
            placeholder="기술 스택 입력 후 Enter"
            @keydown.enter.prevent="addTech(techInput)"
          />
          <button class="btn-add" @click="addTech(techInput)"><i class="fas fa-plus" /></button>
        </div>
        
        <div class="pill-group active-pills">
          <span v-for="t in techs" :key="t" class="tag active">
            {{ t }}
            <button @click="removeTech(t)"><i class="fas fa-times" /></button>
          </span>
        </div>
        
        <div class="pill-group">
          <button 
            v-for="t in availableSuggestedTechs" :key="t" 
            class="tag dashed"
            @click="addTech(t)"
          >
            + {{ t }}
          </button>
        </div>
      </div>

      <div class="form-section mb-8">
        <label class="section-title">희망 포지션</label>
        <p class="section-desc">복수 선택 가능합니다.</p>
        <div class="pill-group">
          <button
            v-for="p in positionOptions" :key="p"
            class="tag"
            :class="{ active: positions.includes(p) }"
            @click="togglePosition(p)"
          >
            {{ p }}
          </button>
        </div>
      </div>

      <button 
        class="btn-primary" 
        :disabled="!canProceed"
        @click="submitSurvey"
      >
        NEXT STEP
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOnboardingMeta, submitSurvey as apiSubmitSurvey } from '@/api/onboardingApi'
import { useAuthStore } from '@/stores/useAuthStore'

const router = useRouter()
const authStore = useAuthStore()

const SCOPE_TO_CATEGORY = { study: 'THEORY', dev: 'PRACTICE', career: 'EMPLOYMENT' }
const JOB_TO_POSITION = {
  '학생': 'STUDENT',
  '취업 준비생': 'JOB_SEEKER',
  '주니어 개발자': 'JUNIOR',
  '시니어 개발자': 'SENIOR',
  '기타': 'OTHER',
}

const scopeOptions = [
  { value: "study", label: "학습", desc: "온라인 강의, 문서 읽기, 퀴즈 등" },
  { value: "dev", label: "개발", desc: "코드 작성, PR, 프로젝트 작업 등" },
  { value: "career", label: "취준", desc: "이력서, 면접 준비, 포트폴리오 등" },
]
const jobOptions = ["학생", "취업 준비생", "주니어 개발자", "시니어 개발자", "기타"]

// BE에서 가져올 목록 (onMounted에서 채워짐)
const metaPositions = ref([])
const metaTechStacks = ref([])
const positionOptions = computed(() => metaPositions.value.map(p => p.positionName))
const suggestedTechs = computed(() => metaTechStacks.value.map(t => t.techName))

const selectedScopes = ref(["study", "dev", "career"])
const scheduleInclusion = ref("")
const job = ref("")
const techs = ref([])
const techInput = ref("")
const positions = ref([])
const isSubmitting = ref(false)

onMounted(async () => {
  try {
    const { data } = await getOnboardingMeta()
    metaPositions.value = data.devPositions || []
    metaTechStacks.value = data.techStacks || []
  } catch (e) {
    console.error('onboarding meta 조회 실패:', e)
  }
})

const toggleScope = (val) => {
  if (selectedScopes.value.includes(val)) selectedScopes.value = selectedScopes.value.filter(x => x !== val)
  else selectedScopes.value.push(val)
}

const togglePosition = (val) => {
  if (positions.value.includes(val)) positions.value = positions.value.filter(x => x !== val)
  else positions.value.push(val)
}

const addTech = (t) => {
  const val = t.trim()
  if (val && !techs.value.includes(val)) techs.value.push(val)
  techInput.value = ""
}

const removeTech = (t) => {
  techs.value = techs.value.filter(x => x !== t)
}

const availableSuggestedTechs = computed(() => {
  return suggestedTechs.value.filter(t => !techs.value.includes(t))
})

const canProceed = computed(() => {
  return scheduleInclusion.value !== "" && job.value !== "" && techs.value.length > 0
})

const submitSurvey = async () => {
  if (!canProceed.value || isSubmitting.value) return
  isSubmitting.value = true

  const techStackIds = techs.value
    .map(name => metaTechStacks.value.find(t => t.techName === name)?.techStackId)
    .filter(Boolean)

  const desiredPositionIds = positions.value
    .map(name => metaPositions.value.find(p => p.positionName === name)?.devPositionId)
    .filter(Boolean)

  const payload = {
    position: JOB_TO_POSITION[job.value] || 'OTHER',
    desiredPositionIds,
    techStackIds,
    curriculumCategories: selectedScopes.value.map(s => SCOPE_TO_CATEGORY[s]).filter(Boolean),
    considerPersonalSchedule: scheduleInclusion.value === 'yes',
    githubTaskId: authStore.githubTaskId || '',
    velogTaskId: authStore.velogTaskId || '',
  }

  try {
    const { data } = await apiSubmitSurvey(payload)
    router.push({ path: '/onboarding/loading', query: { taskId: data.taskId } })
  } catch (e) {
    console.error('설문 제출 실패:', e)
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.setup-root {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg-base); padding: 72px 24px 24px;
  font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; position: relative;
}
.setup-card {
  width: 100%; max-width: 540px; max-height: calc(100vh - 100px);
  background: var(--bg-surface); border: 1px solid var(--border);
  padding: 40px; animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both; overflow-y: auto;
}
@keyframes fadeUp { from { opacity:0; transform:translateY(12px); } to { opacity:1; transform:translateY(0); } }

.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

/* ── Stepper ── */
.global-stepper-wrap { position: fixed; top: 16px; left: 50%; transform: translateX(-50%); width: 100%; max-width: 640px; padding: 0 24px; z-index: 100; }
.page-stepper { display: flex; gap: 0; width: 100%; border: 1px solid var(--border); overflow: hidden; }
.page-stepper .step {
  flex: 1; text-align: center; padding: 10px 4px;
  background: var(--bg-surface); color: var(--text-muted);
  font-size: 12px; font-weight: 700; font-family: inherit;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); white-space: nowrap;
  border-right: 1px solid var(--border);
}
.page-stepper .step:last-child { border-right: none; }
.page-stepper .step.active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); }
.page-stepper .step.done { color: var(--text-primary); background: transparent; }
@media (max-width: 640px) { .page-stepper .step { font-size: 10px; padding: 8px 2px; } }

/* ── Back button ── */
.header-top { display: flex; justify-content: flex-start; align-items: center; margin-bottom: 20px; }
.btn-back {
  background: transparent; border: 1px solid var(--border);
  font-weight: 800; font-size: 11px; color: var(--text-muted);
  cursor: pointer; transition: all 0.2s; letter-spacing: 0.1em;
  padding: 8px 14px; display: inline-flex; align-items: center; gap: 8px;
}
.btn-back:hover { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-hover); }

/* ── Header ── */
.setup-header { border-bottom: 1px solid var(--border); padding-bottom: 16px; margin-bottom: 28px; }
.setup-header h2 { font-size: 20px; font-weight: 900; color: var(--text-primary); margin-bottom: 6px; }
.setup-header p { font-size: 13px; font-weight: 600; color: var(--text-muted); }

/* ── Form sections ── */
.form-section { margin-bottom: 28px; }
.section-title { display: block; font-size: 14px; font-weight: 900; color: var(--text-primary); margin-bottom: 4px; letter-spacing: 0.03em; }
.section-desc { font-size: 12px; font-weight: 600; color: var(--text-muted); margin-bottom: 14px; line-height: 1.5; }
.required-mark { color: var(--text-muted); font-weight: 900; margin-left: 2px; }

/* ── Scope boxes ── */
.scope-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.scope-box {
  text-align: left; padding: 16px; border: 1px solid var(--border);
  background: transparent; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  display: flex; flex-direction: column; gap: 8px;
}
.scope-box:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.scope-box.active { border-color: var(--text-primary); background: var(--text-primary); }
.box-header { display: flex; align-items: center; gap: 10px; }
.checkbox-square {
  width: 16px; height: 16px; border: 1px solid var(--border);
  display: flex; align-items: center; justify-content: center;
  font-size: 9px; background: transparent; color: var(--text-primary); flex-shrink: 0;
}
.scope-box.active .checkbox-square { border-color: var(--bg-base); background: transparent; color: var(--bg-base); }
.box-title { font-size: 13px; font-weight: 900; color: var(--text-primary); }
.scope-box.active .box-title { color: var(--bg-base); }
.box-desc { font-size: 11px; font-weight: 600; color: var(--text-muted); padding-left: 26px; }
.scope-box.active .box-desc { color: var(--bg-base); opacity: 0.7; }

/* ── Choice buttons ── */
.flex-row { display: flex; gap: 10px; }
.choice-btn {
  padding: 14px; border: 1px solid var(--border); background: transparent;
  color: var(--text-primary); font-size: 14px; font-weight: 800;
  cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: inherit;
}
.choice-btn:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.choice-btn.active { border-color: var(--text-primary); background: var(--text-primary); color: var(--bg-base); }

/* ── Radio ── */
.job-list { display: flex; flex-direction: column; gap: 10px; }
.radio-label { display: flex; align-items: center; gap: 12px; cursor: pointer; font-size: 14px; font-weight: 700; color: var(--text-primary); }
.hidden-radio { display: none; }
.radio-custom {
  width: 16px; height: 16px; border: 1px solid var(--text-primary);
  border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.radio-dot { width: 8px; height: 8px; background: var(--text-primary); border-radius: 50%; }

/* ── Input row ── */
.input-row { display: flex; gap: 8px; margin-bottom: 16px; }
.text-input {
  flex: 1; padding: 12px 16px; border: 1px solid var(--border);
  background: transparent; color: var(--text-primary);
  font-size: 14px; font-weight: 700; outline: none;
  transition: border-color 0.2s; font-family: inherit;
}
.text-input:focus { border-color: var(--text-primary); }
.btn-add {
  width: 44px; border: 1px solid var(--text-primary);
  background: var(--text-primary); color: var(--bg-base);
  cursor: pointer; transition: all 0.2s; font-size: 14px;
}
.btn-add:hover { background: transparent; color: var(--text-primary); }

/* ── Tags / Pills ── */
.pill-group { display: flex; flex-wrap: wrap; gap: 8px; }
.active-pills { margin-bottom: 12px; }
.tag {
  padding: 6px 14px; border: 1px solid var(--border);
  background: transparent; font-size: 12px; font-weight: 800;
  color: var(--text-muted); cursor: pointer; transition: all 0.2s;
  display: inline-flex; align-items: center; gap: 8px; font-family: inherit;
}
.tag:hover { border-color: var(--text-primary); color: var(--text-primary); }
.tag.dashed { border-style: dashed; }
.tag.active {
  border-style: solid; border-color: var(--text-primary);
  background: var(--text-primary); color: var(--bg-base);
}
.tag.active button {
  background: transparent; border: none; color: var(--bg-base);
  cursor: pointer; padding: 0; font-size: 11px; opacity: 0.7; transition: opacity 0.15s;
}
.tag.active button:hover { opacity: 1; }

/* ── Primary button ── */
.btn-primary {
  width: 100%; padding: 16px; border: 1px solid var(--text-primary);
  background: var(--text-primary); color: var(--bg-base);
  font-weight: 900; font-size: 14px; letter-spacing: 0.1em;
  cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  font-family: inherit;
}
.btn-primary:disabled {
  background: transparent; border-color: var(--border);
  color: var(--text-faint); cursor: not-allowed;
}
.btn-primary:not(:disabled):hover { background: transparent; color: var(--text-primary); }
</style>