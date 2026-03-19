<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content custom-scroll">
      <header class="page-header">
        <div class="header-title"><i class="fas fa-user-cog" /> PROFILE SETTINGS</div>
      </header>

      <div class="content-inner max-w-xl mx-auto">
        <button class="btn-back" @click="$router.push('/mypage')">
          <i class="fas fa-arrow-left" /> 마이페이지로 돌아가기
        </button>

        <div class="flex-col gap-lg pb-xl">
          <!-- Current Job -->
          <div class="base-panel p-md shadow-normal">
            <label class="form-label">현재 직업</label>
            <p class="form-desc">현재 상태에 맞는 추천 난이도와 활동 구성이 반영됩니다</p>
            <div class="grid-2 col-gap gap-sm mt-md">
              <button
                v-for="option in jobOptions"
                :key="option"
                class="choice-btn"
                :class="{ 'active': job === option }"
                @click="job = option"
              >
                {{ option }}
              </button>
            </div>
          </div>

          <!-- Desired Positions -->
          <div class="base-panel p-md shadow-normal">
            <label class="form-label">희망 포지션</label>
            <p class="form-desc">복수 선택이 가능하며, 추천 커리큘럼 방향 설정에 활용됩니다</p>
            <div class="flex-wrap gap-sm mt-md">
              <button
                v-for="pos in positionOptions"
                :key="pos"
                class="pill-btn"
                :class="{ 'active': positions.includes(pos) }"
                @click="togglePosition(pos)"
              >
                {{ pos }}
              </button>
            </div>
          </div>

          <!-- Tech Stacks -->
          <div class="base-panel p-md shadow-normal">
            <label class="form-label">기술 스택</label>
            <p class="form-desc">관심 기술을 입력하면 추천에 반영됩니다</p>
            
            <div class="flex-wrap gap-sm mt-md mb-md">
              <span v-for="tag in techStacks" :key="tag" class="tech-tag">
                {{ tag }}
                <button @click="removeTag(tag)"><i class="fas fa-times" /></button>
              </span>
            </div>

            <div class="flex-align gap-sm">
              <input
                v-model="newTag"
                @keydown.enter="addTag"
                placeholder="기술명 입력 후 Enter"
                class="text-input flex-1"
              />
              <button class="btn-primary-small" @click="addTag">
                <i class="fas fa-plus" /> 추가
              </button>
            </div>
          </div>

          <!-- Scope -->
          <div class="base-panel p-md shadow-normal">
            <label class="form-label">연동 정보 범위 선택</label>
            <p class="form-desc">추천과 분석에 포함할 활동 유형을 선택하세요</p>
            <div class="grid-2 col-gap gap-sm mt-md">
              <button
                v-for="s in scopeOptions"
                :key="s.value"
                class="scope-btn"
                :class="{ 'active': selectedScopes.includes(s.value) }"
                @click="toggleScope(s.value)"
              >
                <div class="flex-align gap-sm mb-xs">
                  <div class="checkbox" :class="{ 'checked': selectedScopes.includes(s.value) }">
                    <i v-if="selectedScopes.includes(s.value)" class="fas fa-check" />
                  </div>
                  <span class="scope-label">{{ s.label }}</span>
                </div>
                <p class="scope-desc">{{ s.desc }}</p>
              </button>
            </div>
          </div>

          <!-- Schedule Range -->
          <div class="base-panel p-md shadow-normal">
            <label class="form-label">일정 반영 범위</label>
            <p class="form-desc">
              커리큘럼 제작 시 개발/공부/취준 외 내용도 고려할까요?<br />
              예: 친구 결혼식, 가족 일정, 병원 예약처럼 이미 잡혀 있는 개인 일정
            </p>
            <div class="flex-align gap-sm mt-md">
              <button
                class="choice-btn flex-1"
                :class="{ 'active': scheduleInclusion === 'yes' }"
                @click="scheduleInclusion = 'yes'"
              >
                예
              </button>
              <button
                class="choice-btn flex-1"
                :class="{ 'active': scheduleInclusion === 'no' }"
                @click="scheduleInclusion = 'no'"
              >
                아니오
              </button>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex-align gap-md mt-md">
            <button class="btn-outline flex-1" @click="$router.push('/mypage')">취소</button>
            <button class="btn-primary flex-1" @click="$router.push('/mypage')">저장</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'

const router = useRouter()

const initialTechStacks = ["React", "TypeScript", "Next.js", "JavaScript", "Docker"]
const jobOptions = ["학생", "취업 준비생", "주니어 개발자", "시니어 개발자", "비전공 전환자", "기타"]
const positionOptions = ["백엔드", "프론트엔드", "AI", "디자인", "PM", "데이터 분석가", "잘 모르겠어요"]
const scopeOptions = [
  { value: "study", label: "학습", desc: "온라인 강의, 문서 읽기, 퀴즈 등" },
  { value: "dev", label: "개발", desc: "코드 작성, PR, 프로젝트 작업 등" },
  { value: "career", label: "취준", desc: "이력서, 면접 준비, 포트폴리오 등" },
]

const job = ref("주니어 개발자")
const positions = ref(["프론트엔드"])
const techStacks = ref(initialTechStacks)
const newTag = ref("")
const selectedScopes = ref(["study", "dev", "career"])
const scheduleInclusion = ref("yes")

const addTag = () => {
  const trimmed = newTag.value.trim()
  if (trimmed && !techStacks.value.includes(trimmed)) {
    techStacks.value.push(trimmed)
    newTag.value = ""
  }
}

const removeTag = (tag) => {
  techStacks.value = techStacks.value.filter((t) => t !== tag)
}

const toggleScope = (value) => {
  if (selectedScopes.value.includes(value)) {
    selectedScopes.value = selectedScopes.value.filter((v) => v !== value)
  } else {
    selectedScopes.value.push(value)
  }
}

const togglePosition = (value) => {
  if (positions.value.includes(value)) {
    positions.value = positions.value.filter((item) => item !== value)
  } else {
    positions.value.push(value)
  }
}
</script>

<style scoped>
.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow-y: auto; position: relative; }
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

.page-header { display: flex; align-items: center; padding: 20px 32px; border-bottom: 1px solid var(--border); background: var(--bg-surface); position: sticky; top: 0; z-index: 10; }
.header-title { font-size: 16px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary); display: flex; align-items: center; gap: 10px; }

.content-inner { padding: 48px 32px; width: 100%; }
.max-w-xl { max-width: 680px; }
.mx-auto { margin-left: auto; margin-right: auto; }

/* Utils */
.flex-align { display: flex; align-items: center; }
.flex-col { display: flex; flex-direction: column; }
.flex-wrap { display: flex; flex-wrap: wrap; }
.flex-1 { flex: 1; }
.grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); }
@media (max-width: 600px) { .grid-2 { grid-template-columns: 1fr; } }

.gap-sm { gap: 12px; }
.gap-md { gap: 16px; }
.gap-lg { gap: 24px; }

.mb-xs { margin-bottom: 6px; }
.mb-md { margin-bottom: 16px; }
.mt-sm { margin-top: 8px; }
.mt-md { margin-top: 16px; }
.p-md { padding: 24px; }
.pb-xl { padding-bottom: 64px; }

/* Panels & Shadows */
.base-panel { background: transparent; border: 1px solid var(--border); border-radius: 0; transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s; }
.shadow-normal { box-shadow: none; }

.btn-back { background: transparent; border: none; font-size: 13px; font-weight: 700; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px; padding: 0; margin-bottom: 24px; transition: color 0.3s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; }
.btn-back:hover { color: var(--text-primary); transform: translateX(-4px); }

/* Forms */
.form-label { display: block; font-size: 14px; font-weight: 800; color: var(--text-primary); margin-bottom: 4px; }
.form-desc { font-size: 12px; color: var(--text-muted); font-weight: 600; margin: 0; line-height: 1.5; }

.choice-btn { padding: 12px 16px; background: transparent; border: 1px solid var(--border); color: var(--text-muted); font-size: 13px; font-weight: 700; text-align: left; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; border-radius: 0; }
.choice-btn:hover { background: var(--bg-hover); border-color: var(--text-primary); color: var(--text-primary); }
.choice-btn.active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); font-weight: 800; }

.pill-btn { padding: 8px 16px; border: 1px solid var(--border); background: transparent; color: var(--text-muted); font-size: 13px; font-weight: 700; font-family: inherit; cursor: pointer; border-radius: 40px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); display: inline-flex; align-items: center; justify-content: center; }
.pill-btn:hover { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-hover); }
.pill-btn.active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); font-weight: 800; }

.tech-tag { display: inline-flex; align-items: center; gap: 8px; padding: 6px 12px; background: transparent; border: 1px solid var(--border); font-size: 12px; font-weight: 700; color: var(--text-primary); border-radius: 40px; }
.tech-tag button { background: transparent; border: none; color: var(--text-muted); padding: 0; cursor: pointer; font-size: 12px; line-height: 1; transition: color 0.3s; }
.tech-tag button:hover { color: var(--text-primary); }

.text-input { padding: 10px 16px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-size: 13px; font-weight: 700; outline: none; font-family: inherit; transition: border-color 0.3s cubic-bezier(0.16, 1, 0.3, 1); border-radius: 0; }
.text-input:focus { border-color: var(--text-primary); }

.scope-btn { padding: 16px; background: transparent; border: 1px solid var(--border); text-align: left; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; border-radius: 0; }
.scope-btn:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.scope-btn.active { border-color: var(--text-primary); background: transparent; }

.checkbox { width: 18px; height: 18px; border: 1px solid var(--border); background: transparent; display: flex; align-items: center; justify-content: center; border-radius: 0; transition: all 0.3s; font-size: 10px; color: var(--bg-base); }
.checkbox.checked { border-color: var(--text-primary); background: var(--text-primary); }

.scope-label { font-size: 14px; font-weight: 800; color: var(--text-primary); }
.scope-desc { font-size: 12px; font-weight: 600; color: var(--text-muted); margin: 0; padding-left: 26px; line-height: 1.4; }

.btn-primary-small { background: transparent; color: var(--text-primary); border: 1px solid var(--text-primary); padding: 10px 16px; border-radius: 40px; font-size: 13px; font-weight: 800; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; }
.btn-primary-small:hover { background: var(--text-primary); color: var(--bg-base); }

.btn-primary { background: transparent; color: var(--text-primary); border: 1px solid var(--text-primary); border-radius: 40px; padding: 16px; font-size: 14px; font-weight: 800; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; letter-spacing: 0.1em; text-align: center; }
.btn-primary:hover { background: var(--text-primary); color: var(--bg-base); }

.btn-outline { background: transparent; color: var(--text-primary); border: 1px solid var(--border); border-radius: 40px; padding: 16px; font-size: 14px; font-weight: 800; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; letter-spacing: 0.1em; text-align: center; }
.btn-outline:hover { background: var(--bg-hover); border-color: var(--text-primary); color: var(--text-primary); }
</style>
