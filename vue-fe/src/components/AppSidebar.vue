<template>
  <aside class="app-sidebar">
    <div class="sidebar-header" @click="$router.push('/calendar')">
      <svg width="24" height="24" viewBox="0 0 28 28" fill="none">
        <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="currentColor" stroke-width="2" fill="none" stroke-linejoin="round"/>
        <rect x="12" y="12" width="4" height="4" fill="currentColor"/>
      </svg>
      <span class="logo-text">KAIROS</span>
    </div>

    <nav class="sidebar-nav">
      <div class="nav-group">
        <button 
          class="nav-item nav-toggle" 
          :class="{ 'active': $route.path.startsWith('/calendar') || $route.path.startsWith('/study-calendar') || $route.path.startsWith('/prompt') }"
          @click="toggleCalendarMenu"
        >
          <div class="nav-toggle-left">
            <i class="fas fa-calendar-alt" /> CALENDAR
          </div>
          <i class="fas" :class="isCalendarMenuOpen ? 'fa-chevron-up' : 'fa-chevron-down'" />
        </button>
        
        <div v-show="isCalendarMenuOpen" class="nav-sub-menu">
          <router-link to="/calendar" class="nav-sub-item" active-class="active">
            <div class="bullet"></div> CALENDAR
          </router-link>
          <router-link to="/study-calendar" class="nav-sub-item" active-class="active">
            <div class="bullet"></div> STUDY CALENDAR
          </router-link>
          <router-link to="/prompt" class="nav-sub-item" active-class="active">
            <div class="bullet"></div> PROMPT
          </router-link>
        </div>
      </div>
      <router-link to="/recommend" class="nav-item" active-class="active">
        <i class="fas fa-compass" /> RECOMMEND
      </router-link>
      <router-link to="/history" class="nav-item" active-class="active">
        <i class="fas fa-history" /> HISTORY
      </router-link>
      <router-link to="/mypage" class="nav-item" active-class="active">
        <i class="fas fa-user-circle" /> MY PAGE
      </router-link>
    </nav>

    <div class="sync-section">
      <div class="sync-title">MANUAL SYNC</div>
      
      <button class="btn-sync" :disabled="isGithubSyncing" @click="handleSync('github')">
        <i v-if="!isGithubSyncing" class="fab fa-github" />
        <i v-else class="fas fa-spinner fa-spin" />
        GITHUB SYNC
      </button>
      
      <button class="btn-sync" :disabled="isVelogSyncing" @click="handleSync('velog')">
        <i v-if="!isVelogSyncing" class="fas fa-v" />
        <i v-else class="fas fa-spinner fa-spin" />
        VELOG SYNC
      </button>
    </div>

    <Transition name="toast">
      <div v-if="showToast" class="sync-toast">
        <i class="fas fa-check-circle" /> {{ toastMsg }}
      </div>
    </Transition>
  </aside>
</template>

<script setup>
import { ref } from 'vue'

const isGithubSyncing = ref(false)
const isVelogSyncing = ref(false)
const showToast = ref(false)
const toastMsg = ref('')

// 캘린더 드랍다운 토글 상태
const isCalendarMenuOpen = ref(false)
const toggleCalendarMenu = () => {
  isCalendarMenuOpen.value = !isCalendarMenuOpen.value
}

const handleSync = (type) => {
  if (type === 'github') isGithubSyncing.value = true
  else isVelogSyncing.value = true

  // 1.5초 후 동기화 완료 시뮬레이션
  setTimeout(() => {
    if (type === 'github') isGithubSyncing.value = false
    else isVelogSyncing.value = false
    
    toastMsg.value = `${type === 'github' ? 'GitHub' : 'Velog'} 데이터가 동기화되었습니다.`
    showToast.value = true
    setTimeout(() => showToast.value = false, 3000)
  }, 1500)
}
</script>

<style scoped>
.app-sidebar { width: 240px; height: 100vh; background: var(--bg-surface); border-right: 2px solid var(--text-primary); display: flex; flex-direction: column; flex-shrink: 0; font-family: 'Space Grotesk', 'Escoredream', sans-serif; position: relative; z-index: 100; }
.sidebar-header { padding: 24px; display: flex; align-items: center; gap: 12px; color: var(--text-primary); border-bottom: 2px solid var(--text-primary); cursor: pointer; transition: background 0.2s; }
.sidebar-header:hover { background: var(--bg-base); }
.logo-text { font-size: 20px; font-weight: 900; letter-spacing: 0.15em; }

.sidebar-nav { flex: 1; padding: 24px 16px; display: flex; flex-direction: column; gap: 8px; overflow-y: auto; -ms-overflow-style: none; scrollbar-width: none; }
.sidebar-nav::-webkit-scrollbar { display: none; }

.nav-item { display: flex; align-items: center; gap: 12px; padding: 14px 16px; font-size: 14px; font-weight: 900; color: var(--text-primary); text-decoration: none; border: 2px solid transparent; transition: all 0.1s; letter-spacing: 0.05em; cursor: pointer; background: transparent; width: 100%; text-align: left; font-family: inherit; }
.nav-item:hover { border-color: var(--text-primary); background: var(--bg-base); transform: translate(-2px, -2px); box-shadow: 4px 4px 0 #6b7280; }
.nav-item.active { background: var(--text-primary); color: var(--bg-base); border-color: var(--text-primary); box-shadow: 4px 4px 0 #6b7280; transform: translate(-2px, -2px); }
.nav-item i { font-size: 16px; width: 20px; text-align: center; }

.nav-toggle { justify-content: space-between; }
.nav-toggle-left { display: flex; align-items: center; gap: 12px; }

.nav-sub-menu { display: flex; flex-direction: column; gap: 8px; padding-left: 16px; margin-top: 8px; margin-bottom: 8px; border-left: 2px dashed var(--border); margin-left: 24px; animation: slideDown 0.2s ease-out; }
@keyframes slideDown { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }

.nav-sub-item { display: flex; align-items: center; gap: 8px; padding: 10px 12px; font-size: 13px; font-weight: 800; color: var(--text-muted); text-decoration: none; border: 2px solid transparent; transition: all 0.1s; letter-spacing: 0.05em; }
.nav-sub-item .bullet { width: 6px; height: 6px; background: var(--border); transition: all 0.1s; }
.nav-sub-item:hover { color: var(--text-primary); border-color: var(--text-primary); background: var(--bg-base); transform: translate(-2px, -2px); box-shadow: 4px 4px 0 #6b7280; }
.nav-sub-item:hover .bullet { background: var(--text-primary); }
.nav-sub-item.active { color: var(--bg-base); background: var(--text-primary); border-color: var(--text-primary); box-shadow: 4px 4px 0 #6b7280; transform: translate(-2px, -2px); }
.nav-sub-item.active .bullet { background: var(--bg-base); }

/* 수동 동기화 섹션 */
.sync-section { padding: 24px 16px; border-top: 2px solid var(--text-primary); background: var(--bg-base); display: flex; flex-direction: column; gap: 12px; }
.sync-title { font-size: 11px; font-weight: 900; color: var(--text-muted); letter-spacing: 0.1em; margin-bottom: 4px; padding-left: 4px; }
.btn-sync { display: flex; align-items: center; gap: 10px; padding: 12px; background: transparent; border: 2px solid var(--border); color: var(--text-primary); font-size: 12px; font-weight: 800; cursor: pointer; transition: all 0.1s; font-family: inherit; letter-spacing: 0.05em; }
.btn-sync:hover:not(:disabled) { border-color: var(--text-primary); background: var(--text-primary); color: var(--bg-base); transform: translate(-2px, -2px); box-shadow: 4px 4px 0 #6b7280; }
.btn-sync:disabled { opacity: 0.6; cursor: not-allowed; }

/* 토스트 메시지 */
.sync-toast { position: fixed; bottom: 24px; left: 264px; background: var(--text-primary); color: var(--bg-base); padding: 16px 24px; font-size: 13px; font-weight: 800; border: 2px solid var(--text-primary); box-shadow: 6px 6px 0 #6b7280; display: flex; align-items: center; gap: 10px; z-index: 1000; }
.toast-enter-active, .toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(20px); }
</style>