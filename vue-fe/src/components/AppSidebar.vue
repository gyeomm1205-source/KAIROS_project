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
      <router-link to="/calendar" class="nav-item" active-class="active">
        <i class="fas fa-calendar-alt" /> CALENDAR
      </router-link>
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
.app-sidebar { 
  width: 260px; height: 100vh; 
  background: var(--bg-base); 
  border-right: 1px solid var(--border); 
  display: flex; flex-direction: column; flex-shrink: 0; 
  font-family: 'Escoredream', sans-serif; 
  position: relative; z-index: 100; 
}
.sidebar-header { 
  height: 64px; padding: 0 24px; 
  display: flex; align-items: center; gap: 12px; 
  color: var(--text-primary); 
  border-bottom: 1px solid var(--border); 
  cursor: pointer; transition: background 0.3s; 
}
.sidebar-header:hover { background: var(--bg-hover); }
.logo-text { font-size: 20px; font-weight: 900; letter-spacing: 0.15em; }

.sidebar-nav { 
  flex: 1; padding: 32px 16px; 
  display: flex; flex-direction: column; gap: 4px; 
  overflow-y: auto; -ms-overflow-style: none; scrollbar-width: none; 
}
.sidebar-nav::-webkit-scrollbar { display: none; }

.nav-item { 
  display: flex; align-items: center; gap: 16px; 
  padding: 16px; 
  font-size: 13px; font-weight: 700; color: var(--text-muted); 
  text-decoration: none; border: 1px solid transparent; 
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); 
  letter-spacing: 0.1em; cursor: pointer; background: transparent; 
  width: 100%; text-align: left; font-family: inherit; border-radius: 8px;
}
.nav-item:hover { color: var(--text-primary); background: var(--bg-hover); }
.nav-item.active { background: var(--text-primary); color: var(--bg-base); border-color: var(--border); }
.nav-item i { font-size: 16px; width: 20px; text-align: center; }

.nav-toggle { justify-content: space-between; }
.nav-toggle-left { display: flex; align-items: center; gap: 16px; }

.nav-sub-menu { 
  display: flex; flex-direction: column; gap: 4px; 
  padding-left: 16px; margin: 4px 0 12px 24px; 
  border-left: 1px dashed var(--border-mid); 
  animation: slideDown 0.2s ease-out; 
}
@keyframes slideDown { from { opacity: 0; transform: translateY(-5px); } to { opacity: 1; transform: translateY(0); } }

.nav-sub-item { 
  display: flex; align-items: center; gap: 12px; 
  padding: 12px; font-size: 12px; font-weight: 700; 
  color: var(--text-faint); text-decoration: none; 
  transition: all 0.2s; letter-spacing: 0.05em; border-radius: 6px;
}
.nav-sub-item .bullet { width: 4px; height: 4px; border-radius: 50%; background: var(--border-mid); transition: all 0.2s; }
.nav-sub-item:hover { color: var(--text-primary); background: var(--bg-hover); }
.nav-sub-item:hover .bullet { background: var(--text-primary); }
.nav-sub-item.active { color: var(--text-primary); background: var(--bg-hover); }
.nav-sub-item.active .bullet { background: var(--text-primary); box-shadow: 0 0 4px var(--text-primary); }

/* 수동 동기화 섹션 */
.sync-section { 
  padding: 24px 16px; 
  border-top: 1px solid var(--border); 
  background: var(--bg-base); 
  display: flex; flex-direction: column; gap: 12px; 
}
.sync-title { font-size: 10px; font-weight: 900; color: var(--text-faint); letter-spacing: 0.15em; margin-bottom: 4px; padding-left: 4px; }
.btn-sync { 
  display: flex; align-items: center; gap: 12px; 
  padding: 14px; background: transparent; 
  border: 1px solid var(--border); border-radius: 40px;
  color: var(--text-muted); font-size: 12px; font-weight: 700; 
  cursor: pointer; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); 
  font-family: inherit; letter-spacing: 0.1em; 
}
.btn-sync:hover:not(:disabled) { border-color: var(--text-primary); color: var(--text-primary); background: var(--bg-hover); }
.btn-sync:disabled { opacity: 0.5; cursor: not-allowed; }

/* 토스트 메시지 */
.sync-toast { 
  position: fixed; bottom: 24px; left: 284px; 
  background: var(--text-primary); color: var(--bg-base); 
  padding: 16px 24px; font-size: 13px; font-weight: 700; 
  border: 1px solid var(--border); display: flex; align-items: center; gap: 12px; 
  z-index: 1000; border-radius: 40px;
}
.toast-enter-active, .toast-leave-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(20px) scale(0.95); }
</style>