<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content">
      <div class="page-header">
        <span class="page-title">레퍼런스 추천</span>
        <div class="header-right">
          <button class="icon-btn"><i class="fas fa-bell" /></button>
          <RouterLink to="/mypage" class="avatar-btn">K</RouterLink>
        </div>
      </div>

      <div class="rec-body fade-in">

        <!-- AI 어시스턴트 -->
        <div class="section-card">
          <div class="ai-header">
            <div class="ai-icon"><i class="fas fa-robot" /></div>
            <span>AI 학습 어시스턴트</span>
          </div>
          <div class="chat-bubbles">
            <div class="bubble bubble--ai">
              현재 학습 진행 상황을 분석하고 있습니다...
              <div class="typing-dots"><span /><span /><span /></div>
            </div>
            <div class="bubble bubble--msg">수학 기초 개념에서 약간의 부족함을 발견했습니다.</div>
            <div class="bubble bubble--msg">개인 맞춤형 학습 경로를 생성하고 있습니다...</div>
          </div>
        </div>

        <!-- 추천 그리드 -->
        <div class="rec-grid">
          <div class="section-card">
            <div class="rec-section-title"><i class="fas fa-graduation-cap" /> 학습 추천</div>
            <div v-for="c in courses" :key="c.title" class="rec-item">
              <h4>{{ c.title }}</h4>
              <p>{{ c.desc }}</p>
              <div class="rec-item-footer">
                <span class="rec-time"><i class="fas fa-clock" /> {{ c.time }}</span>
                <span class="rec-score">추천도 {{ c.score }}%</span>
              </div>
            </div>
          </div>
          <div class="section-card">
            <div class="rec-section-title"><i class="fas fa-book-open" /> 레퍼런스 추천</div>
            <div v-for="r in references" :key="r.title" class="rec-item">
              <h4>{{ r.title }}</h4>
              <p>{{ r.desc }}</p>
              <div class="rec-item-footer">
                <span class="ref-type"><i :class="r.icon" /> {{ r.type }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 액션 버튼 -->
        <div class="action-row">
          <button class="btn-action btn-action--primary">
            <i class="fas fa-graduation-cap" /> 학습 추천
          </button>
          <button class="btn-action btn-action--secondary">
            <i class="fas fa-book-open" /> 레퍼런스 추천
          </button>
          <button class="btn-action btn-action--secondary">
            <i class="fas fa-plus" /> 새로운 공부 시작
          </button>
        </div>

        <!-- ── 커리큘럼 업로드 섹션 ── -->
        <div class="section-card curriculum-card">
          <div class="curriculum-header">
            <div class="curriculum-icon-wrap">
              <i class="fas fa-file-upload" />
            </div>
            <div class="curriculum-header-text">
              <div class="curriculum-title">커리큘럼 업로드</div>
              <div class="curriculum-sub">PDF, 이미지, 텍스트 파일을 업로드하면 AI가 분석해 캘린더에 자동 등록합니다</div>
            </div>
          </div>

          <!-- 드롭존 -->
          <div
            class="upload-dropzone"
            :class="{ 'dropzone--over': isDragOver, 'dropzone--done': uploadedFile }"
            @dragover.prevent="isDragOver = true"
            @dragleave.prevent="isDragOver = false"
            @drop.prevent="handleDrop"
            @click="fileInputRef.click()"
          >
            <input
              ref="fileInputRef"
              type="file"
              accept=".pdf,.png,.jpg,.jpeg,.txt,.md"
              style="display:none"
              @change="handleFileChange"
            />

            <template v-if="!uploadedFile">
              <div class="dropzone-icon" :class="{ 'dropzone-icon--over': isDragOver }">
                <i class="fas fa-cloud-upload-alt" />
              </div>
              <div class="dropzone-label">클릭하거나 파일을 여기에 드래그하세요</div>
              <div class="dropzone-hint">PDF · 이미지 · TXT · Markdown 지원 · 최대 20MB</div>
            </template>

            <template v-else>
              <div class="dropzone-file-row">
                <div class="dropzone-file-icon">
                  <i :class="fileIcon" />
                </div>
                <div class="dropzone-file-info">
                  <div class="dropzone-filename">{{ uploadedFile.name }}</div>
                  <div class="dropzone-filesize">{{ fileSize }}</div>
                </div>
                <button class="dropzone-remove" @click.stop="clearFile">
                  <i class="fas fa-times" />
                </button>
              </div>
            </template>
          </div>

          <!-- 업로드 진행바 (분석 중일 때) -->
          <Transition name="progress-slide">
            <div v-if="uploading" class="upload-progress-wrap">
              <div class="upload-progress-label">
                <i class="fas fa-magic" /> AI가 커리큘럼을 분석하고 있어요...
              </div>
              <div class="upload-progress-bar">
                <div class="upload-progress-fill" :style="{ width: uploadProgress + '%' }" />
              </div>
              <div class="upload-progress-pct">{{ uploadProgress }}%</div>
            </div>
          </Transition>

          <button
            class="btn-upload-submit"
            :disabled="!uploadedFile || uploading"
            @click="submitUpload"
          >
            <i :class="uploading ? 'fas fa-spinner fa-spin' : 'fas fa-magic'" />
            {{ uploading ? 'AI가 분석 중...' : '커리큘럼 분석 및 캘린더 등록' }}
          </button>
        </div>

      </div>
    </main>

    <!-- ── 퀴즈 완료 후 캘린더 등록 모달 (이미지 1 스타일) ── -->
    <Teleport to="body">
      <Transition name="modal-fade">
        <div v-if="showCalModal" class="modal-overlay" @click.self="showCalModal = false">
          <div class="modal-box">
            <div class="modal-check-icon">
              <i class="fas fa-check" />
            </div>
            <h3 class="modal-title">커리큘럼이 캘린더에<br>업로드되었습니다.</h3>
            <p class="modal-sub">다음 주 학습 계획을 확인해보세요.</p>
            <RouterLink to="/calendar" class="modal-confirm-btn" @click="showCalModal = false">
              캘린더로 이동
            </RouterLink>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import AppSidebar from '@/components/AppSidebar.vue'
import { useCalendarStore } from '@/stores/useCalendarStore'

const calendarStore = useCalendarStore()

// ── 추천 데이터 ──
const courses = [
  { title: '기초 대수학',    desc: '현재 진도에 맞는 기초 개념 강화', time: '2시간',   score: 95 },
  { title: '문제 해결 전략', desc: '체계적인 문제 접근법 학습',        time: '1.5시간', score: 88 },
]
const references = [
  { title: 'Khan Academy - 대수학', desc: '기초부터 차근차근 설명하는 영상 강의', type: '영상 자료', icon: 'fas fa-play-circle' },
  { title: '수학의 정석 기초편',   desc: '체계적인 문제 풀이와 개념 정리',       type: '교재',    icon: 'fas fa-book' },
]

// ── 파일 업로드 ──
const fileInputRef  = ref(null)
const uploadedFile  = ref(null)
const isDragOver    = ref(false)
const uploading     = ref(false)
const uploadProgress = ref(0)
const showCalModal  = ref(false)

const fileSize = computed(() => {
  if (!uploadedFile.value) return ''
  const bytes = uploadedFile.value.size
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
})

const fileIcon = computed(() => {
  if (!uploadedFile.value) return 'fas fa-file'
  const name = uploadedFile.value.name.toLowerCase()
  if (name.endsWith('.pdf')) return 'fas fa-file-pdf'
  if (name.match(/\.(png|jpg|jpeg)$/)) return 'fas fa-file-image'
  return 'fas fa-file-alt'
})

function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (file) uploadedFile.value = file
}

function handleDrop(e) {
  isDragOver.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file) uploadedFile.value = file
}

function clearFile() {
  uploadedFile.value = null
  uploadProgress.value = 0
  if (fileInputRef.value) fileInputRef.value.value = ''
}

function submitUpload() {
  if (!uploadedFile.value || uploading.value) return
  uploading.value = true
  uploadProgress.value = 0

  // 진행 애니메이션
  const interval = setInterval(() => {
    uploadProgress.value += Math.floor(Math.random() * 18 + 8)
    if (uploadProgress.value >= 100) {
      uploadProgress.value = 100
      clearInterval(interval)
      setTimeout(finishUpload, 400)
    }
  }, 280)
}

function finishUpload() {
  // 다음 주 월~금에 일정 자동 등록 (샘플)
  const monday = getNextMonday()
  const subjects = [
    { title: `[커리큘럼] Day 1 - 개념 학습`, track: 'main' },
    { title: `[커리큘럼] Day 2 - 실습`,      track: 'algo' },
    { title: `[커리큘럼] Day 3 - 복습`,      track: 'main' },
    { title: `[커리큘럼] Day 4 - 심화`,      track: 'react' },
    { title: `[커리큘럼] Day 5 - 프로젝트`,  track: 'portfolio' },
  ]
  subjects.forEach((s, i) => {
    const d = new Date(monday)
    d.setDate(d.getDate() + i)
    calendarStore.createSchedule({
      day:   d.toISOString().slice(0, 10),
      track: s.track,
      text:  s.title,
      tooltip: { title: s.title, tags: ['#커리큘럼', '#자동등록'] },
    })
  })

  uploading.value = false
  uploadedFile.value = null
  uploadProgress.value = 0
  showCalModal.value = true
}

function getNextMonday() {
  const d = new Date()
  const day = d.getDay()
  const diff = day === 0 ? 1 : 8 - day
  d.setDate(d.getDate() + diff)
  return d
}
</script>

<style scoped>
.app-layout {
  display: flex; width: 100%; height: 100vh; overflow: hidden;
  background: var(--bg-base); color: var(--text-primary);
  font-family: 'Escoredream', system-ui, sans-serif;
}
.main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.page-header {
  height: 64px; background: var(--bg-surface); border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 28px; flex-shrink: 0;
}
.page-title { font-weight: 700; font-size: 15px; color: var(--text-primary); }
.header-right { display: flex; align-items: center; gap: 14px; }
.icon-btn { background: none; border: none; cursor: pointer; color: var(--text-muted); font-size: 16px; }
.avatar-btn {
  width: 32px; height: 32px; border-radius: 50%; text-decoration: none;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; color: #fff; font-weight: 700;
}

.rec-body {
  flex: 1; overflow-y: auto; padding: 28px 32px;
  display: flex; flex-direction: column; gap: 20px;
  scrollbar-width: thin; scrollbar-color: var(--scrollbar-thumb) transparent;
}
@keyframes fadeIn { from{opacity:0;transform:translateY(6px)} to{opacity:1;transform:translateY(0)} }
.fade-in { animation: fadeIn 0.35s ease both; }

.section-card {
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 16px; padding: 22px 24px;
}

/* AI 헤더 */
.ai-header { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; font-weight: 700; font-size: 14px; color: var(--text-primary); }
.ai-icon { width: 32px; height: 32px; border-radius: 9px; background: rgba(129,140,248,0.15); color: #818cf8; display: flex; align-items: center; justify-content: center; font-size: 14px; }

/* 버블 */
.chat-bubbles { display: flex; flex-direction: column; gap: 10px; }
.bubble { display: inline-block; padding: 12px 16px; border-radius: 12px; font-size: 13px; color: var(--text-secondary); max-width: 80%; line-height: 1.55; }
.bubble--ai { background: var(--bg-elevated); border: 1px solid var(--border); border-bottom-left-radius: 4px; }
.bubble--msg { background: var(--bg-surface); border: 1px solid var(--border); box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.typing-dots { display: flex; gap: 4px; margin-top: 8px; }
.typing-dots span { width: 6px; height: 6px; border-radius: 50%; background: var(--text-faint); }
.typing-dots span:nth-child(1) { animation: bounce 1.2s infinite; }
.typing-dots span:nth-child(2) { animation: bounce 1.2s infinite 0.15s; }
.typing-dots span:nth-child(3) { animation: bounce 1.2s infinite 0.3s; }
@keyframes bounce { 0%,80%,100%{transform:translateY(0)} 40%{transform:translateY(-5px)} }

/* 추천 그리드 */
.rec-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; }
.rec-section-title { display: flex; align-items: center; gap: 9px; font-weight: 700; font-size: 14px; color: var(--text-primary); margin-bottom: 16px; }
.rec-section-title i { color: #818cf8; }
.rec-item { background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 12px; padding: 16px 18px; margin-bottom: 12px; cursor: pointer; transition: all 0.15s; }
.rec-item:last-child { margin-bottom: 0; }
.rec-item:hover { background: var(--bg-hover); border-color: rgba(129,140,248,0.3); }
.rec-item h4 { font-size: 14px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.rec-item p  { font-size: 12px; color: var(--text-muted); margin-bottom: 12px; line-height: 1.5; }
.rec-item-footer { display: flex; justify-content: space-between; align-items: center; }
.rec-time { font-size: 11px; color: var(--text-faint); display: flex; align-items: center; gap: 5px; }
.rec-score { font-size: 11px; font-weight: 700; background: rgba(129,140,248,0.15); color: #818cf8; padding: 3px 9px; border-radius: 6px; }
.ref-type { font-size: 11px; color: var(--text-faint); display: flex; align-items: center; gap: 5px; }

/* 액션 버튼 */
.action-row { display: flex; gap: 12px; flex-wrap: wrap; }
.btn-action { flex: 1; min-width: 140px; padding: 14px; border-radius: 12px; font-size: 14px; font-weight: 600; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; transition: all 0.15s; font-family: 'Escoredream', sans-serif; }
.btn-action--primary { background: linear-gradient(135deg, #818cf8, #38bdf8); color: #fff; border: none; }
.btn-action--primary:hover { opacity: 0.87; }
.btn-action--secondary { background: var(--bg-surface); border: 1px solid var(--border); color: var(--text-muted); }
.btn-action--secondary:hover { background: var(--bg-hover); color: var(--text-primary); }

/* ── 커리큘럼 업로드 카드 ── */
.curriculum-card { display: flex; flex-direction: column; gap: 18px; }

.curriculum-header { display: flex; align-items: center; gap: 16px; }
.curriculum-icon-wrap {
  width: 48px; height: 48px; border-radius: 13px; flex-shrink: 0;
  background: rgba(129,140,248,0.12); border: 1px solid rgba(129,140,248,0.2);
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; color: #818cf8;
}
.curriculum-title { font-size: 15px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px; }
.curriculum-sub   { font-size: 12px; color: var(--text-muted); line-height: 1.5; }

/* 드롭존 */
.upload-dropzone {
  border: 2px dashed var(--border-mid);
  border-radius: 14px;
  padding: 36px 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--bg-elevated);
  min-height: 130px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.upload-dropzone:hover {
  border-color: rgba(129,140,248,0.5);
  background: rgba(129,140,248,0.04);
}
.dropzone--over {
  border-color: #818cf8 !important;
  background: rgba(129,140,248,0.08) !important;
  transform: scale(1.01);
}
.dropzone--done {
  border-style: solid;
  border-color: rgba(16,185,129,0.4);
  background: rgba(16,185,129,0.04) !important;
  padding: 20px 24px;
}

.dropzone-icon {
  font-size: 32px; color: var(--text-faint);
  transition: color 0.2s, transform 0.2s;
}
.dropzone-icon--over { color: #818cf8; transform: translateY(-3px); }
.dropzone-label { font-size: 14px; font-weight: 600; color: var(--text-secondary); }
.dropzone-hint  { font-size: 11px; color: var(--text-faint); }

/* 파일 선택 완료 상태 */
.dropzone-file-row {
  display: flex; align-items: center; gap: 14px; width: 100%;
  text-align: left;
}
.dropzone-file-icon {
  width: 44px; height: 44px; border-radius: 11px; flex-shrink: 0;
  background: rgba(16,185,129,0.12);
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; color: #10b981;
}
.dropzone-file-info { flex: 1; min-width: 0; }
.dropzone-filename { font-size: 14px; font-weight: 700; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dropzone-filesize { font-size: 11px; color: var(--text-faint); margin-top: 3px; }
.dropzone-remove {
  width: 32px; height: 32px; border-radius: 8px; flex-shrink: 0;
  background: var(--bg-hover); border: 1px solid var(--border);
  color: var(--text-faint); cursor: pointer; font-size: 12px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.dropzone-remove:hover { background: rgba(239,68,68,0.1); border-color: rgba(239,68,68,0.3); color: #ef4444; }

/* 진행 바 */
.upload-progress-wrap { width: 100%; }
.upload-progress-label { font-size: 12px; font-weight: 600; color: #818cf8; margin-bottom: 8px; display: flex; align-items: center; gap: 7px; }
.upload-progress-bar { width: 100%; height: 6px; background: var(--bg-elevated); border-radius: 3px; overflow: hidden; margin-bottom: 5px; }
.upload-progress-fill { height: 100%; background: linear-gradient(90deg, #818cf8, #38bdf8); border-radius: 3px; transition: width 0.25s ease; }
.upload-progress-pct { font-size: 11px; color: var(--text-faint); text-align: right; }

.progress-slide-enter-active, .progress-slide-leave-active { transition: all 0.25s ease; }
.progress-slide-enter-from, .progress-slide-leave-to { opacity: 0; transform: translateY(-6px); }

/* 업로드 제출 버튼 */
.btn-upload-submit {
  width: 100%; padding: 14px; border-radius: 12px; border: none; cursor: pointer;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  color: #fff; font-weight: 700; font-size: 15px;
  display: flex; align-items: center; justify-content: center; gap: 9px;
  transition: opacity 0.15s; font-family: 'Escoredream', sans-serif;
}
.btn-upload-submit:hover:not(:disabled) { opacity: 0.87; }
.btn-upload-submit:disabled { opacity: 0.4; cursor: not-allowed; }

/* ── 완료 모달 (이미지 1 & 2 디자인 통합) ── */
.modal-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center;
  padding: 24px;
}
.modal-box {
  width: 100%; max-width: 400px;
  background: var(--modal-bg, #fff);
  border: 1px solid var(--modal-border, #e5e7eb);
  border-radius: 22px;
  padding: 48px 36px 40px;
  box-shadow: 0 24px 60px rgba(0,0,0,0.18);
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  text-align: center;
  animation: popIn 0.25s cubic-bezier(0.34,1.56,0.64,1) both;
}
@keyframes popIn { from{opacity:0;transform:scale(0.9)} to{opacity:1;transform:scale(1)} }

/* 체크 아이콘 - 이미지처럼 큰 검정 원 */
.modal-check-icon {
  width: 68px; height: 68px; border-radius: 50%;
  background: #1a1a1a;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; color: #fff;
  margin-bottom: 8px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.25);
}
.is-light .modal-check-icon { background: #111; }

.modal-title {
  font-size: 20px; font-weight: 800;
  color: var(--modal-text, #111);
  line-height: 1.4; margin-bottom: 2px;
}
.modal-sub {
  font-size: 13px;
  color: var(--modal-sub, #6b7280);
  margin-bottom: 10px;
}

/* 이동 버튼 - 이미지처럼 검정 풀버튼 */
.modal-confirm-btn {
  display: block; width: 100%; padding: 15px;
  border-radius: 12px;
  background: #1a1a1a;
  color: #fff;
  text-align: center;
  font-size: 16px; font-weight: 700;
  text-decoration: none;
  transition: opacity 0.15s;
  font-family: 'Escoredream', sans-serif;
  margin-top: 4px;
}
.modal-confirm-btn:hover { opacity: 0.85; }

/* 모달 페이드 */
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
</style>
