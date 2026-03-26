<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content custom-scroll">
      <header class="page-header">
        <div class="header-title"><i class="fas fa-magic" /> CURRICULUM RESULT</div>
      </header>

      <div class="content-inner max-w-xl mx-auto">
        <button class="btn-back" @click="$router.push('/recommend')">
          <i class="fas fa-arrow-left" /> 뒤로
        </button>

        <!-- Top: Recommendation reason -->
        <div class="brutal-panel card-panel mb-lg">
          <div class="flex-align mb-md gap-md items-start">
            <div class="icon-box-muted"><i class="fas fa-lightbulb" /></div>
            <div>
              <h2 class="form-title mb-xs">Docker 기초 커리큘럼</h2>
              <span class="text-xs text-muted font-bold">사전 퀴즈 결과 기반 · 맞춤 생성됨</span>
            </div>
          </div>

          <div class="bg-surface border-muted p-md mb-md">
            <h3 class="panel-title-sm mb-xs">추천 이유</h3>
            <p class="panel-desc-sm">사전 퀴즈에서 Docker 기본 개념에 대한 이해도가 초급 수준으로 확인되었습니다. 현재 Node.js 기반 프로젝트를 활발히 진행 중이므로, Docker를 활용한 컨테이너화와 배포를 단계적으로 학습하는 커리큘럼을 구성했습니다.</p>
          </div>

          <div class="bg-surface border-muted p-md mb-md">
            <h3 class="panel-title-sm mb-xs">학습 방향</h3>
            <div class="flex-align flex-wrap gap-sm">
              <template v-for="(s, i) in ['개념 이해', 'Dockerfile', 'Compose', '실전 적용', '정리']" :key="s">
                <div class="flex-align">
                  <span class="brutal-tag inline-tag">{{ s }}</span>
                  <div v-if="i < 4" class="h-line mx-sm" />
                </div>
              </template>
            </div>
          </div>

          <div class="flex-align gap-lg mt-md flex-wrap">
            <div class="info-item"><i class="fas fa-clock icon-clock" /><span>총 소요: 약 3시간 50분 (5일)</span></div>
            <div class="flex-align gap-sm flex-wrap">
              <span v-for="t in ['Docker', 'DevOps', 'Node.js']" :key="t" class="brutal-tag inline-tag">
                <i v-if="hasTechIcon(t)" :class="getTechIcon(t)" class="tech-icon" />{{ t }}
              </span>
            </div>
          </div>
        </div>

        <!-- 2-column: Missions + References -->
        <div class="grid-2 col-gap gap-y mb-lg">
          <!-- Left: Action Missions -->
          <div>
            <h3 class="flex-align gap-sm font-bold text-md mb-md">
              <i class="fas fa-brain icon-brain" /> 액션 미션
            </h3>
            <div class="flex-col gap-sm">
              <div v-for="(m, i) in missions" :key="i" class="brutal-panel p-md shadow-normal">
                <div class="flex-start gap-md">
                  <div class="day-badge">{{ m.day }}</div>
                  <div class="flex-1 min-w-0">
                    <div class="flex-align gap-sm mb-xs">
                      <span class="font-bold text-sm">{{ m.title }}</span>
                      <span class="brutal-tag inline-tag text-[10px]">{{ m.tag }}</span>
                    </div>
                    <p class="text-xs text-muted mb-sm font-bold line-clamp-2">{{ m.desc }}</p>
                    <div class="info-item"><i class="fas fa-clock icon-clock" /><span>{{ m.time }}</span></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Right: References -->
          <div class="flex-col">
            <h3 class="flex-align gap-sm font-bold text-md mb-md">
              <i class="fas fa-book-open icon-ref" /> 기초 레퍼런스
            </h3>
            <div class="flex-col gap-sm">
              <div v-for="(r, i) in references" :key="i" class="brutal-panel p-md shadow-normal">
                <div class="flex-between mb-sm items-start">
                  <div class="flex-align gap-sm">
                    <i class="fas fa-file-text text-muted shrink-0" />
                    <span class="font-bold text-sm">{{ r.title }}</span>
                  </div>
                  <span class="brutal-tag inline-tag text-[10px] ml-sm shrink-0">{{ r.type }}</span>
                </div>
                <p class="text-xs text-muted pl-lg mb-sm font-bold">{{ r.reason }}</p>
                <div class="flex-between pl-lg">
                  <div class="flex-align gap-md text-xs text-muted font-bold">
                    <span>{{ r.freshness }}</span>
                    <span><i class="fas fa-shield-alt" /> 안전한 링크</span>
                  </div>
                  <button class="btn-text"><i class="fas fa-external-link-alt" /> 보기</button>
                </div>
              </div>
            </div>

            <!-- Study tips -->
            <div class="mt-md bg-surface border-muted p-md">
              <h4 class="panel-title-sm mb-xs">학습 팁</h4>
              <ul class="tips-list">
                <li v-for="(tip, i) in tips" :key="i">
                  <span class="dot text-muted">•</span>
                  {{ tip }}
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- Bottom CTA -->
        <div class="brutal-panel p-lg flex-between flex-wrap gap-md mt-auto shadow-heavy">
          <div>
            <div class="font-bold text-md mb-xs text-primary">이 커리큘럼을 시작하시겠어요?</div>
            <div class="text-xs text-muted font-bold">캘린더에 5일 일정이 자동으로 추가됩니다</div>
          </div>
          <div class="flex-align gap-md flex-wrap">
            <button class="btn-outline" @click="$router.push('/recommend')">돌아가기</button>
            <button class="btn-primary" @click="confirmAndGoCalendar">
              <i class="fas fa-calendar-plus" /> 캘린더에 추가하기
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { postCurriculaConfirm } from '@/api/aiApi'
import { useCalendarStore } from '@/stores/useCalendarStore'
import AppSidebar from '@/components/AppSidebar.vue'
import { getTechIcon, hasTechIcon } from '@/utils/techIcons'

const router = useRouter()
const store = useCalendarStore()

const missions = ref([
  { day: "1일차", title: "Docker 핵심 개념 이해", desc: "컨테이너, 이미지, Dockerfile의 기본 개념을 학습합니다", time: "40분", tag: "개념" },
  { day: "2일차", title: "Dockerfile 작성 실습", desc: "간단한 Node.js 앱을 Docker로 컨테이너화하는 실습", time: "50분", tag: "실습" },
  { day: "3일차", title: "Docker Compose 학습", desc: "멀티 컨테이너 환경 구성과 docker-compose.yml 작성", time: "45분", tag: "심화" },
  { day: "4일차", title: "프로젝트에 Docker 적용", desc: "기존 개인 프로젝트를 Docker 환경으로 전환", time: "60분", tag: "실전" },
  { day: "5일차", title: "학습 정리 + 복습 퀴즈", desc: "Docker 학습 내용을 블로그로 정리하고 퀴즈로 점검", time: "35분", tag: "정리" },
])

const references = ref([
  { title: "Docker 공식 Getting Started", reason: "Docker의 기본 워크플로우를 실습 중심으로 설명", type: "공식 문서", freshness: "2026.01 업데이트" },
  { title: "Docker for Node.js", reason: "현재 기술 스택(Node.js)에 맞는 Docker 활용법", type: "튜토리얼", freshness: "2025.12 작성" },
  { title: "Docker Compose 실전 (한국어)", reason: "멀티 컨테이너 환경 구성을 실전 예제로 설명", type: "기술 블로그", freshness: "2026.02 작성" },
])

const tips = ref([
  "각 미션은 하루에 하나씩 진행하는 것을 권장합니다",
  "실습 미션은 레퍼런스를 먼저 읽고 시작하면 효과적입니다",
  "마지막 날 블로그 정리로 학습 내용을 정착시키세요",
])

const tags = ['개념', '실습', '심화', '실전', '정리']

onMounted(() => {
  const data = store.curriculumResult
  if (!data) return

  if (data.nodes && data.nodes.length > 0) {
    missions.value = data.nodes.map((n, i) => ({
      day: `${i + 1}일차`,
      title: n.title,
      desc: n.description || '',
      time: `${n.expectedMinutes}분`,
      tag: tags[i % tags.length]
    }))
  }
})

const confirmAndGoCalendar = async () => {
  try {
    if (store.curriculumResult) {
      await postCurriculaConfirm({ curriculumPreviewKey: store.curriculumPreviewKey || 'curriculumPreview:1:temp' })
    }
  } catch (e) {
    console.error('curricula/confirm 호출 실패:', e)
  }
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
.mx-auto { margin-left: auto; margin-right: auto; }

/* Typography */
.form-title { font-size: 20px; font-weight: 800; color: var(--text-primary); margin: 0; }
.panel-title-sm { font-size: 14px; font-weight: 800; color: var(--text-primary); margin: 0; }
.panel-desc-sm { font-size: 13px; color: var(--text-muted); line-height: 1.6; font-weight: 600; margin: 0; }

/* Utils */
.flex-align { display: flex; align-items: center; }
.flex-start { display: flex; align-items: flex-start; }
.flex-between { display: flex; align-items: center; justify-content: space-between; }
.flex-col { display: flex; flex-direction: column; }
.flex-wrap { display: flex; flex-wrap: wrap; }
.grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); }
.col-gap { gap: 24px; }
.gap-y { gap: 24px; }
.gap-xs { gap: 4px; }
.gap-sm { gap: 8px; }
.gap-md { gap: 16px; }
.gap-lg { gap: 24px; }
.mb-xs { margin-bottom: 8px; }
.mb-sm { margin-bottom: 12px; }
.mb-md { margin-bottom: 16px; }
.mb-lg { margin-bottom: 24px; }
.mt-md { margin-top: 16px; }
.mt-auto { margin-top: auto; }
.mx-sm { margin-left: 8px; margin-right: 8px; }
.ml-sm { margin-left: 8px; }
.p-md { padding: 16px; }
.p-lg { padding: 24px; }
.pl-lg { padding-left: 28px; }

.font-bold { font-weight: 800; }
.text-xs { font-size: 11px; }
.text-sm { font-size: 13px; }
.text-md { font-size: 15px; }
.text-muted { color: var(--text-muted); }
.text-primary { color: var(--text-primary); }
.text-\[10px\] { font-size: 10px; }
.items-start { align-items: flex-start; }
.shrink-0 { flex-shrink: 0; }
.min-w-0 { min-width: 0; }
.flex-1 { flex: 1; }

.bg-surface { background: transparent; }
.border-muted { border: 1px solid var(--border); border-radius: 8px; }
.line-clamp-2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

/* Box shadows & Panels */
.brutal-panel { background: transparent; border: 1px solid var(--border); display: flex; flex-direction: column; border-radius: 8px; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.card-panel { padding: 32px; }
.card-panel:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.shadow-normal { box-shadow: none; }
.shadow-normal:hover { border-color: var(--text-primary); background: var(--bg-hover); transform: translateX(4px); }
.shadow-heavy { box-shadow: none; border-top: 1px solid var(--border); border-bottom: none; border-left: none; border-right: none;}
.shadow-heavy:hover { border-color: var(--text-primary); }

.icon-box-muted { width: 48px; height: 48px; background: transparent; display: flex; align-items: center; justify-content: center; font-size: 20px; color: var(--clr-icon-ai); border: 1px solid var(--clr-icon-ai); border-radius: 8px; flex-shrink: 0; }
.icon-brain { color: #F9A8D4; }
.icon-ref   { color: #86EFAC; }
.icon-clock { color: #FB923C; }
.day-badge { width: 44px; height: 44px; background: transparent; border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 800; color: var(--text-primary); flex-shrink: 0; border-radius: 50%;}
.h-line { width: 16px; height: 1px; background: var(--border); }

/* Buttons & Tags */
.btn-back { background: transparent; border: none; font-size: 13px; font-weight: 700; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px; margin-bottom: 24px; padding: 0; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.btn-back:hover { color: var(--text-primary); transform: translateX(-4px); }

.brutal-tag { padding: 4px 10px; border: 1px solid var(--border); background: transparent; font-size: 11px; font-weight: 700; color: var(--text-primary); border-radius: 40px;}
.inline-tag { display: inline-flex; align-items: center; gap: 4px;}
.tech-icon { font-size: 13px; flex-shrink: 0; }

.btn-primary { background: transparent; color: var(--text-primary); border: 1px solid var(--text-primary); padding: 14px 24px; font-size: 13px; font-weight: 800; font-family: inherit; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); display: inline-flex; align-items: center; gap: 8px; border-radius: 40px; letter-spacing: 0.05em;}
.btn-primary:hover { background: var(--text-primary); color: var(--bg-base); }
.btn-outline { background: transparent; color: var(--text-primary); border: 1px solid var(--border); padding: 14px 24px; font-size: 13px; font-weight: 800; font-family: inherit; cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); display: inline-flex; align-items: center; gap: 8px; border-radius: 40px; letter-spacing: 0.05em;}
.btn-outline:hover { background: var(--bg-hover); border-color: var(--text-primary); }
.btn-text { background: transparent; border: none; font-size: 11px; font-weight: 800; color: var(--text-muted); display: flex; align-items: center; gap: 4px; padding: 0; cursor: pointer; transition: color 0.3s; }
.btn-text:hover { color: var(--text-primary); }

.info-item { display: flex; align-items: center; gap: 6px; font-size: 11px; color: var(--text-muted); font-weight: 700; }
.info-item i { width: 14px; text-align: center; }

.tips-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px; }
.tips-list li { font-size: 12px; color: var(--text-muted); font-weight: 600; display: flex; align-items: flex-start; gap: 6px; line-height: 1.5; }
.dot { font-size: 14px; line-height: 1; margin-top: 2px; }

@media (max-width: 768px) {
  .grid-2 { grid-template-columns: 1fr; }
}
</style>
