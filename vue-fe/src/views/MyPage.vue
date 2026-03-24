<template>
  <div class="mypage-layout">
    <AppSidebar />

    <main class="mypage-main">
      <!-- 헤더 -->
      <div class="mypage-header">
        <button class="btn-back" @click="router.back()">
          <i class="fas fa-arrow-left" />
        </button>
        <div class="mypage-header-title">
          <h1 class="page-title">마이페이지</h1>
          <p class="page-sub">프로필과 AI 설정을 관리하세요</p>
        </div>
      </div>

      <div class="mypage-content">

        <!-- ── A. 기본 정보 관리 ─────────────────────────── -->
        <section class="card">
          <div class="card-header">
            <div class="card-icon" style="background: rgba(99,102,241,0.15); color:#818cf8">
              <i class="fas fa-user" />
            </div>
            <div>
              <h2 class="card-title">기본 정보</h2>
              <p class="card-sub">닉네임을 수정할 수 있습니다</p>
            </div>
          </div>
          <div class="card-body">
            <div class="field-group">
              <label class="field-label">닉네임</label>
              <div class="input-row">
                <input
                  v-model="nickname"
                  type="text"
                  class="field-input"
                  placeholder="닉네임을 입력하세요"
                  maxlength="20"
                />
                <button class="btn-save" @click="saveNickname" :class="{ 'btn-saved': nicknameSaved }">
                  <i :class="nicknameSaved ? 'fas fa-check' : 'fas fa-save'" />
                  {{ nicknameSaved ? '저장됨' : '저장' }}
                </button>
              </div>
              <p class="field-hint">최대 20자 · 현재 {{ nickname.length }}자</p>
            </div>
          </div>
        </section>

        <!-- ── B. GitHub 연동 관리 ──────────────────────── -->
        <section class="card">
          <div class="card-header">
            <div class="card-icon" style="background: rgba(15,23,42,0.5); color:#e2e8f0">
              <i class="fab fa-github" />
            </div>
            <div>
              <h2 class="card-title">GitHub 연동</h2>
              <p class="card-sub">커밋 기록과 학습 일정을 연동합니다</p>
            </div>
          </div>
          <div class="card-body">
            <div class="github-status-row">
              <div class="github-info">
                <span class="status-badge" :class="github.connected ? 'badge--connected' : 'badge--disconnected'">
                  <span class="status-dot" />
                  {{ github.connected ? '연동됨' : '미연동' }}
                </span>
                <span v-if="github.connected" class="github-username">
                  <i class="fab fa-github" /> {{ github.username }}
                </span>
              </div>
              <button
                class="btn-github"
                :class="github.connected ? 'btn-github--disconnect' : 'btn-github--connect'"
                @click="toggleGithub"
              >
                <i :class="github.connected ? 'fas fa-unlink' : 'fas fa-link'" />
                {{ github.connected ? '연동 해제' : 'GitHub 연동하기' }}
              </button>
            </div>
            <div v-if="github.connected" class="github-stats">
              <div class="github-stat">
                <span class="stat-num">{{ github.repos }}</span>
                <span class="stat-label">레포지토리</span>
              </div>
              <div class="github-stat">
                <span class="stat-num">{{ github.commits }}</span>
                <span class="stat-label">이번 달 커밋</span>
              </div>
              <div class="github-stat">
                <span class="stat-num">{{ github.streak }}일</span>
                <span class="stat-label">연속 커밋</span>
              </div>
            </div>
          </div>
        </section>

        <!-- ── C. 기술 스택 관리 ────────────────────────── -->
        <section class="card">
          <div class="card-header">
            <div class="card-icon" style="background: rgba(16,185,129,0.15); color:#10b981">
              <i class="fas fa-code" />
            </div>
            <div>
              <h2 class="card-title">기술 스택</h2>
              <p class="card-sub">보유한 기술을 등록하세요</p>
            </div>
          </div>
          <div class="card-body">
            <!-- 검색/추가 입력 -->
            <div class="stack-input-row">
              <div class="stack-input-wrap">
                <i class="fas fa-search stack-search-icon" />
                <input
                  v-model="stackInput"
                  type="text"
                  class="field-input stack-input"
                  placeholder="기술 입력 후 Enter (예: Vue, Spring, Python...)"
                  @keydown.enter.prevent="addStack"
                />
              </div>
              <button class="btn-add-stack" @click="addStack">
                <i class="fas fa-plus" /> 추가
              </button>
            </div>

            <!-- 추천 태그 -->
            <div class="stack-suggestions">
              <span class="suggest-label">빠른 추가:</span>
              <button
                v-for="s in filteredSuggestions"
                :key="s"
                class="suggest-chip"
                @click="addStackDirect(s)"
              >+ {{ s }}</button>
            </div>

            <!-- 등록된 스택 -->
            <div v-if="techStack.length" class="stack-pills">
              <div
                v-for="tech in techStack"
                :key="tech.name"
                class="stack-pill"
                :style="{ background: tech.color + '22', borderColor: tech.color + '55', color: tech.color }"
              >
                <span class="pill-icon">{{ tech.icon }}</span>
                <span>{{ tech.name }}</span>
                <button class="pill-remove" @click="removeStack(tech.name)">
                  <i class="fas fa-times" />
                </button>
              </div>
            </div>
            <p v-else class="stack-empty">아직 등록된 기술 스택이 없습니다</p>
          </div>
        </section>

        <!-- ── D. AI 멘토 페르소나 ──────────────────────── -->
        <section class="card card--persona">
          <div class="card-header">
            <div class="card-icon" style="background: rgba(245,158,11,0.15); color:#f59e0b">
              <i class="fas fa-robot" />
            </div>
            <div>
              <h2 class="card-title">AI 멘토 페르소나</h2>
              <p class="card-sub">AI 에이전트가 참고하는 나의 목표와 스타일</p>
            </div>
            <span class="md-badge"><i class="fab fa-markdown" /> Markdown 지원</span>
          </div>
          <div class="card-body">
            <div class="persona-editor-wrap">
              <div class="persona-toolbar">
                <button class="toolbar-btn" @click="insertMd('**', '**')"><b>B</b></button>
                <button class="toolbar-btn" @click="insertMd('*', '*')"><i>I</i></button>
                <button class="toolbar-btn" @click="insertMd('\n## ', '')">H</button>
                <button class="toolbar-btn" @click="insertMd('\n- ', '')"><i class="fas fa-list-ul" /></button>
                <button class="toolbar-btn" @click="insertMd('`', '`')"><i class="fas fa-code" /></button>
                <div class="toolbar-sep" />
                <span class="char-count">{{ persona.length }}자</span>
              </div>
              <textarea
                ref="personaRef"
                v-model="persona"
                class="persona-textarea"
                rows="10"
                placeholder="예: AutoInCar 프로젝트의 SW/AI 리드 역할을 맡고 있으며, 컴퓨터 비전(OpenCV)과 AI 모델 최적화에 관심이 많습니다. 블로그 작성 시 전문적이고 간결한 어조를 선호합니다."
              />
            </div>
            <div class="persona-actions">
              <button class="btn-preview" @click="showPreview = !showPreview">
                <i :class="showPreview ? 'fas fa-edit' : 'fas fa-eye'" />
                {{ showPreview ? '편집' : '미리보기' }}
              </button>
              <button class="btn-save btn-save--persona" @click="savePersona" :class="{ 'btn-saved': personaSaved }">
                <i :class="personaSaved ? 'fas fa-check' : 'fas fa-save'" />
                {{ personaSaved ? '저장됨' : 'AI에 적용' }}
              </button>
            </div>
            <!-- 마크다운 미리보기 (간단) -->
            <div v-if="showPreview && persona" class="persona-preview" v-html="renderMarkdown(persona)" />
          </div>
        </section>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'

const router = useRouter()

// ── A. 기본 정보 ────────────────────────────────────────────
const nickname     = ref('김싸피')
const nicknameSaved = ref(false)
function saveNickname() {
  if (!nickname.value.trim()) return
  nicknameSaved.value = true
  setTimeout(() => { nicknameSaved.value = false }, 2000)
}

// ── B. GitHub 연동 ──────────────────────────────────────────
const github = ref({
  connected: true,
  username: 'kimssafy',
  repos: 24,
  commits: 87,
  streak: 12
})
function toggleGithub() {
  github.value.connected = !github.value.connected
}

// ── C. 기술 스택 ────────────────────────────────────────────
const TECH_META = {
  'Vue':        { color: '#42d392', icon: '💚' },
  'React':      { color: '#61dafb', icon: '⚛️' },
  'Spring':     { color: '#6db33f', icon: '🍃' },
  'Python':     { color: '#3776ab', icon: '🐍' },
  'TypeScript': { color: '#3178c6', icon: '🔷' },
  'Java':       { color: '#ed8b00', icon: '☕' },
  'Node.js':    { color: '#339933', icon: '🟢' },
  'MySQL':      { color: '#4479a1', icon: '🗄️' },
  'Docker':     { color: '#2496ed', icon: '🐳' },
  'Kubernetes': { color: '#326ce5', icon: '⚙️' },
  'AWS':        { color: '#ff9900', icon: '☁️' },
  'Git':        { color: '#f05032', icon: '🌿' },
  'C++':        { color: '#00599c', icon: '⚡' },
  'FastAPI':    { color: '#009688', icon: '🚀' },
  'Redis':      { color: '#dc382d', icon: '🔴' },
}
const SUGGESTIONS = Object.keys(TECH_META)

const techStack = ref([
  { name: 'Vue',    ...TECH_META['Vue'] },
  { name: 'Python', ...TECH_META['Python'] },
  { name: 'Spring', ...TECH_META['Spring'] },
])
const stackInput = ref('')

const filteredSuggestions = computed(() => {
  const added = new Set(techStack.value.map(t => t.name))
  const q = stackInput.value.toLowerCase()
  return SUGGESTIONS
    .filter(s => !added.has(s) && (q === '' || s.toLowerCase().includes(q)))
    .slice(0, 6)
})

function addStack() {
  const name = stackInput.value.trim()
  if (!name) return
  if (techStack.value.some(t => t.name.toLowerCase() === name.toLowerCase())) {
    stackInput.value = ''
    return
  }
  const meta = TECH_META[name] || { color: '#6b7280', icon: '🔧' }
  techStack.value.push({ name, ...meta })
  stackInput.value = ''
}

function addStackDirect(name) {
  if (techStack.value.some(t => t.name === name)) return
  const meta = TECH_META[name] || { color: '#6b7280', icon: '🔧' }
  techStack.value.push({ name, ...meta })
}

function removeStack(name) {
  techStack.value = techStack.value.filter(t => t.name !== name)
}

// ── D. AI 페르소나 ───────────────────────────────────────────
const persona     = ref('')
const personaSaved = ref(false)
const showPreview  = ref(false)
const personaRef   = ref(null)

function insertMd(before, after) {
  const el = personaRef.value
  if (!el) return
  const s = el.selectionStart, e = el.selectionEnd
  const sel = persona.value.substring(s, e)
  persona.value = persona.value.substring(0,s) + before + sel + after + persona.value.substring(e)
  nextTick(() => { el.selectionStart = s+before.length; el.selectionEnd = e+before.length; el.focus() })
}

function savePersona() {
  personaSaved.value = true
  setTimeout(() => { personaSaved.value = false }, 2000)
}

// 간단 마크다운 렌더링
function renderMarkdown(text) {
  return text
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/^## (.+)$/gm, '<h2>$1</h2>')
    .replace(/^### (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm,  '<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g,     '<em>$1</em>')
    .replace(/`(.+?)`/g,       '<code>$1</code>')
    .replace(/^- (.+)$/gm,     '<li>$1</li>')
    .replace(/\n/g, '<br>')
}

import { nextTick } from 'vue'
</script>

<style scoped>
/* ── 레이아웃 ──────────────────────────────────────────────── */
.mypage-layout {
  display: flex; width: 100%; height: 100vh; overflow: hidden;
  background: var(--bg-base); color: var(--text-primary);
  font-family: 'Escoredream', system-ui, sans-serif;
}
.mypage-main {
  flex: 1; display: flex; flex-direction: column;
  overflow-y: auto; overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: var(--scrollbar-thumb) transparent;
}

/* ── 헤더 ──────────────────────────────────────────────────── */
.mypage-header {
  display: flex; align-items: center; gap: 16px;
  padding: 24px 32px 0;
  flex-shrink: 0;
}
.btn-back {
  width: 36px; height: 36px; border-radius: 10px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  color: var(--text-muted); font-size: 14px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s; flex-shrink: 0;
}
.btn-back:hover { background: var(--bg-hover); color: var(--text-primary); }
.page-title {
  font-size: 22px; font-weight: 800;
  font-family: 'Escoredream', sans-serif;
  color: var(--text-primary); line-height: 1.2;
}
.page-sub { font-size: 12px; color: var(--text-faint); margin-top: 2px; }

/* ── 콘텐츠 그리드 ─────────────────────────────────────────── */
.mypage-content {
  padding: 24px 32px 40px;
  display: flex; flex-direction: column; gap: 20px;
}

/* ── 카드 공통 ─────────────────────────────────────────────── */
.card {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  transition: border-color 0.15s;
}
.card:hover { border-color: var(--border-mid); }
.card--persona { }

.card-header {
  display: flex; align-items: center; gap: 14px;
  padding: 18px 22px 14px;
  border-bottom: 1px solid var(--border);
}
.card-icon {
  width: 38px; height: 38px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; flex-shrink: 0;
}
.card-title {
  font-size: 15px; font-weight: 700;
  font-family: 'Escoredream', sans-serif;
  color: var(--text-primary);
}
.card-sub {
  font-size: 11px; color: var(--text-faint); margin-top: 2px;
}
.card-body { padding: 18px 22px 20px; }

/* ── A. 기본 정보 ──────────────────────────────────────────── */
.field-group { display: flex; flex-direction: column; gap: 8px; }
.field-label {
  font-size: 12px; font-weight: 700; color: var(--text-muted);
  letter-spacing: 0.04em;
}
.input-row { display: flex; gap: 10px; align-items: center; }
.field-input {
  flex: 1; background: var(--bg-elevated);
  border: 1px solid var(--border); border-radius: 9px;
  color: var(--text-primary); font-size: 14px;
  font-family: 'Escoredream', sans-serif;
  padding: 10px 14px; outline: none;
  transition: border-color 0.15s;
}
.field-input:focus { border-color: var(--accent); }
.field-input::placeholder { color: var(--text-faint); }
.field-hint { font-size: 11px; color: var(--text-faint); }

.btn-save {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 18px; border: none; border-radius: 9px;
  background: var(--accent); color: #fff;
  font-size: 13px; font-weight: 700;
  font-family: 'Escoredream', sans-serif;
  cursor: pointer; transition: all 0.18s; white-space: nowrap;
  flex-shrink: 0;
}
.btn-save:hover { opacity: 0.85; }
.btn-save.btn-saved { background: #10b981; }

/* ── B. GitHub ──────────────────────────────────────────────── */
.github-status-row {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  flex-wrap: wrap;
}
.github-info { display: flex; align-items: center; gap: 12px; }
.status-badge {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 12px; border-radius: 999px;
  font-size: 12px; font-weight: 700;
  font-family: 'Escoredream', sans-serif;
}
.badge--connected   { background: rgba(16,185,129,0.15); color: #10b981; border: 1px solid rgba(16,185,129,0.3); }
.badge--disconnected{ background: rgba(107,114,128,0.15); color: #6b7280; border: 1px solid rgba(107,114,128,0.3); }
.status-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: currentColor;
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0%,100% { opacity: 1; }
  50% { opacity: 0.4; }
}
.github-username {
  font-size: 13px; color: var(--text-muted);
  font-family: monospace;
}
.btn-github {
  display: flex; align-items: center; gap: 7px;
  padding: 9px 18px; border-radius: 9px;
  font-size: 13px; font-weight: 700; cursor: pointer;
  font-family: 'Escoredream', sans-serif; transition: all 0.15s;
  border: none;
}
.btn-github--connect    { background: #24292e; color: #fff; }
.btn-github--connect:hover { background: #1a1f24; }
.btn-github--disconnect { background: rgba(239,68,68,0.12); color: #ef4444; border: 1px solid rgba(239,68,68,0.3); }
.btn-github--disconnect:hover { background: rgba(239,68,68,0.2); }

.github-stats {
  display: flex; gap: 0; margin-top: 16px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: 10px; overflow: hidden;
}
.github-stat {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  padding: 12px 16px; border-right: 1px solid var(--border);
}
.github-stat:last-child { border-right: none; }
.stat-num {
  font-size: 20px; font-weight: 800;
  font-family: 'Escoredream', sans-serif; color: var(--text-primary);
}
.stat-label { font-size: 10px; color: var(--text-faint); margin-top: 2px; }

/* ── C. 기술 스택 ──────────────────────────────────────────── */
.stack-input-row { display: flex; gap: 10px; align-items: center; }
.stack-input-wrap { flex: 1; position: relative; }
.stack-search-icon {
  position: absolute; left: 12px; top: 50%;
  transform: translateY(-50%);
  color: var(--text-faint); font-size: 12px; pointer-events: none;
}
.stack-input { padding-left: 34px !important; }
.btn-add-stack {
  display: flex; align-items: center; gap: 6px;
  padding: 9px 16px; border: none; border-radius: 9px;
  background: rgba(59,130,246,0.15); color: #60a5fa;
  font-size: 13px; font-weight: 700; cursor: pointer;
  font-family: 'Escoredream', sans-serif; transition: all 0.15s;
  border: 1px solid rgba(59,130,246,0.3); white-space: nowrap;
}
.btn-add-stack:hover { background: rgba(59,130,246,0.25); }

.stack-suggestions {
  display: flex; flex-wrap: wrap; align-items: center; gap: 7px;
  margin-top: 10px;
}
.suggest-label { font-size: 11px; color: var(--text-faint); flex-shrink: 0; }
.suggest-chip {
  padding: 4px 10px; border: 1px dashed var(--border-mid);
  background: none; color: var(--text-muted); border-radius: 6px;
  font-size: 11px; cursor: pointer; transition: all 0.15s;
  font-family: 'Escoredream', sans-serif;
}
.suggest-chip:hover { border-color: var(--accent); color: var(--accent); background: rgba(59,130,246,0.08); }

.stack-pills {
  display: flex; flex-wrap: wrap; gap: 8px; margin-top: 14px;
}
.stack-pill {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 10px 6px 8px;
  border: 1px solid; border-radius: 8px;
  font-size: 12px; font-weight: 700;
  font-family: 'Escoredream', sans-serif;
  transition: opacity 0.15s;
}
.pill-icon { font-size: 14px; }
.pill-remove {
  background: none; border: none; cursor: pointer;
  color: currentColor; opacity: 0.5; padding: 0 0 0 4px;
  font-size: 10px; transition: opacity 0.15s; line-height: 1;
}
.pill-remove:hover { opacity: 1; }
.stack-empty { font-size: 13px; color: var(--text-faint); margin-top: 12px; }

/* ── D. AI 페르소나 ─────────────────────────────────────────── */
.md-badge {
  margin-left: auto; display: flex; align-items: center; gap: 5px;
  padding: 4px 10px; border-radius: 6px;
  background: rgba(245,158,11,0.1); color: #f59e0b;
  font-size: 11px; font-weight: 600; border: 1px solid rgba(245,158,11,0.25);
}
.persona-editor-wrap {
  background: var(--bg-elevated); border: 1px solid var(--border); border-radius: 10px;
  overflow: hidden;
}
.persona-toolbar {
  display: flex; align-items: center; gap: 4px;
  padding: 8px 12px; border-bottom: 1px solid var(--border);
  background: var(--bg-surface);
}
.toolbar-btn {
  width: 28px; height: 28px; border-radius: 6px;
  background: none; border: none; color: var(--text-muted);
  cursor: pointer; font-size: 13px;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.15s;
}
.toolbar-btn:hover { background: var(--bg-hover); color: var(--text-primary); }
.toolbar-sep { width: 1px; height: 18px; background: var(--border); margin: 0 4px; }
.char-count { font-size: 10px; color: var(--text-faint); margin-left: auto; font-family: monospace; }
.persona-textarea {
  width: 100%; background: transparent;
  border: none; outline: none; resize: none;
  color: var(--text-primary); font-size: 13px;
  font-family: 'Escoredream', monospace, sans-serif;
  padding: 14px 16px; line-height: 1.8;
  box-sizing: border-box;
}
.persona-textarea::placeholder { color: var(--text-faint); }
.persona-actions {
  display: flex; justify-content: flex-end; gap: 10px; margin-top: 12px;
}
.btn-preview {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 16px; border: 1px solid var(--border);
  background: var(--bg-elevated); color: var(--text-muted);
  border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer;
  transition: all 0.15s; font-family: 'Escoredream', sans-serif;
}
.btn-preview:hover { background: var(--bg-hover); color: var(--text-primary); }
.btn-save--persona { padding: 8px 20px; }

.persona-preview {
  margin-top: 14px; padding: 16px;
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: 10px; line-height: 1.8;
  font-size: 13px; color: var(--text-secondary);
}
.persona-preview :deep(h1) { font-size: 18px; font-weight: 800; margin: 8px 0 4px; color: var(--text-primary); }
.persona-preview :deep(h2) { font-size: 15px; font-weight: 700; margin: 6px 0 3px; color: var(--text-primary); }
.persona-preview :deep(h3) { font-size: 13px; font-weight: 700; margin: 4px 0 2px; }
.persona-preview :deep(strong) { color: var(--text-primary); font-weight: 700; }
.persona-preview :deep(em) { font-style: italic; opacity: 0.85; }
.persona-preview :deep(code) {
  background: var(--bg-surface); padding: 1px 6px;
  border-radius: 4px; font-family: monospace; font-size: 12px;
}
.persona-preview :deep(li) { margin-left: 16px; list-style: disc; }
</style>
