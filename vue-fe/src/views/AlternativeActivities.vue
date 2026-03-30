<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content custom-scroll">
      <header class="page-header">
        <div class="header-title"><i class="fas fa-random" /> ALTERNATIVE ACTIVITIES</div>
      </header>

      <div class="content-inner max-w-xl mx-auto">
        <button class="btn-back" @click="$router.go(-1)">
          <i class="fas fa-arrow-left" /> 뒤로
        </button>

        <h2 class="form-title">다른 방식으로 시작해볼까요?</h2>
        <p class="form-sub">커리큘럼 외에 다른 학습 활동도 추천합니다</p>

        <div class="grid-2 col-gap gap-y">
          <!-- Quiz Card -->
          <div class="brutal-panel card-panel">
            <div class="icon-box-muted"><i class="fas fa-brain" /></div>
            <h3 class="panel-title-sm">퀴즈 추천</h3>
            <p class="panel-desc-sm">최근 블로그, GitHub, 캘린더 활동에서 다룬 주제를 기반으로 복습 퀴즈를 생성합니다. 학습한 내용을 점검하고 기억을 강화하세요.</p>

            <div class="info-list mb-md">
              <div class="info-item"><i class="fas fa-clock" /><span>예상 소요 시간: 약 15분</span></div>
              <div class="info-item"><i class="fas fa-tag" /><span>주제: React 상태관리, TypeScript 제네릭</span></div>
            </div>

            <div class="tag-group-small mb-md flex-wrap">
              <span class="brutal-tag inline-tag">상태관리</span>
              <span class="brutal-tag inline-tag">React</span>
              <span class="brutal-tag inline-tag">TypeScript</span>
            </div>

            <button class="btn-primary-block mt-auto" @click="$router.push('/activity')">
              퀴즈 시작하기 <i class="fas fa-arrow-right" />
            </button>
          </div>

          <!-- Reference Card -->
          <div class="brutal-panel card-panel">
            <div class="icon-box-muted"><i class="fas fa-file-search" /></div>
            <h3 class="panel-title-sm">레퍼런스 추천</h3>
            <p class="panel-desc-sm">다음 블로그 글 작성이나 프로젝트를 위해 참고할 만한 문서를 추천합니다. 현재 학습 맥락과 연결된 자료를 선별했습니다.</p>

            <div class="info-list mb-md">
              <div class="info-item"><i class="fas fa-clock" /><span>예상 읽기 시간: 약 20분</span></div>
              <div class="info-item"><i class="fas fa-tag" /><span>주제: React Server Components, Streaming</span></div>
            </div>

            <div class="warning-box mb-lg">
              <i class="fas fa-exclamation-triangle warning-icon" />
              <p>추천 문서의 최신성은 보장되지 않을 수 있습니다. 공식 문서를 함께 확인해주세요.</p>
            </div>

            <button class="btn-outline-block mt-auto" @click="$router.push('/activity')">
              레퍼런스 보기 <i class="fas fa-arrow-right" />
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'

const router = useRouter()
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
.max-w-xl { max-width: 800px; }
.mx-auto { margin-left: auto; margin-right: auto; }

/* Typography */
.form-title { font-size: 24px; font-weight: 900; color: var(--text-primary); margin-bottom: 8px; }
.form-sub { font-size: 14px; color: var(--text-muted); font-weight: 600; margin-bottom: 40px; }

/* Utils */
.grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); }
.col-gap { gap: 24px; }
.gap-y { gap: 24px; }
.mb-md { margin-bottom: 16px; }
.mb-lg { margin-bottom: 24px; }
.mt-auto { margin-top: auto; }
.flex-wrap { display: flex; flex-wrap: wrap; }

/* Buttons */
.btn-back { background: transparent; border: none; font-size: 13px; font-weight: 700; color: var(--text-muted); cursor: pointer; display: flex; align-items: center; gap: 6px; margin-bottom: 24px; padding: 0; transition: color 0.1s; }
.btn-back:hover { color: var(--text-primary); }

.btn-primary-block { width: 100%; display: flex; justify-content: center; align-items: center; gap: 8px; background: var(--text-primary); color: var(--bg-base); border: 2px solid var(--text-primary); padding: 14px; font-size: 14px; font-weight: 900; box-shadow: 4px 4px 0 #6b7280; font-family: inherit; cursor: pointer; transition: all 0.1s; }
.btn-primary-block:hover { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; background: transparent; color: var(--text-primary); }

.btn-outline-block { width: 100%; display: flex; justify-content: center; align-items: center; gap: 8px; background: transparent; color: var(--text-primary); border: 2px solid var(--text-primary); padding: 14px; font-size: 14px; font-weight: 900; box-shadow: 4px 4px 0 #6b7280; font-family: inherit; cursor: pointer; transition: all 0.1s; }
.btn-outline-block:hover { transform: translate(-2px, -2px); box-shadow: 6px 6px 0 #6b7280; background: var(--bg-surface); }

/* Panels */
.brutal-panel { background: var(--bg-base); border: 2px solid var(--text-primary); padding: 24px; box-shadow: 6px 6px 0 #6b7280; display: flex; flex-direction: column; transition: transform 0.1s, box-shadow 0.1s; border-radius: 0; }
.card-panel:hover { transform: translate(-2px, -2px); box-shadow: 8px 8px 0 #6b7280; }

.icon-box-muted { width: 48px; height: 48px; background: var(--bg-surface); display: flex; align-items: center; justify-content: center; font-size: 20px; color: var(--text-primary); border: 2px solid var(--border); margin-bottom: 20px; }
.panel-title-sm { font-size: 18px; font-weight: 900; color: var(--text-primary); margin-bottom: 8px; }
.panel-desc-sm { font-size: 13px; color: var(--text-muted); line-height: 1.6; font-weight: 600; margin-bottom: 24px; min-height: 48px; }

/* Lists & Tags */
.info-list { display: flex; flex-direction: column; gap: 8px; }
.info-item { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text-muted); font-weight: 600; }
.info-item i { width: 14px; text-align: center; }

.tag-group-small { display: flex; gap: 8px; }
.brutal-tag { padding: 4px 8px; border: 1px solid var(--border); background: var(--bg-surface); font-size: 11px; font-weight: 700; color: var(--text-muted); }
.inline-tag { display: inline-block; }

/* Warning Box */
.warning-box { background: var(--bg-surface); border: 1px dashed var(--border); padding: 12px; display: flex; align-items: flex-start; gap: 8px; }
.warning-icon { color: var(--text-muted); margin-top: 2px; font-size: 14px; flex-shrink: 0; }
.warning-box p { font-size: 11px; color: var(--text-muted); line-height: 1.5; font-weight: 600; margin: 0; }

@media (max-width: 768px) {
  .grid-2 { grid-template-columns: 1fr; }
}
</style>
