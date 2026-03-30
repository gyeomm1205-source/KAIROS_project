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
      <router-link to="/calendar" class="nav-item" active-class="active" :class="{ pulse: calendarPulse }">
        <i class="fas fa-calendar-alt" /> CALENDAR
      </router-link>
      <router-link to="/recommend" class="nav-item" active-class="active">
        <i class="fas fa-compass" /> RECOMMEND
      </router-link>
      <router-link to="/history" class="nav-item" active-class="active">
        <i class="fas fa-history" /> HISTORY
      </router-link>
      <!-- <router-link to="/mypage" class="nav-item" active-class="active">
        <i class="fas fa-user-circle" /> MY PAGE
      </router-link> -->
    </nav>

    <div class="sync-section">
      <div class="sync-title">MANUAL SYNC</div>
      
      <button class="btn-sync btn-sync-github" :disabled="isGithubSyncing" @click="handleSync('github')">
        <i v-if="!isGithubSyncing" class="fab fa-github" />
        <i v-else class="fas fa-spinner fa-spin" />
        GITHUB SYNC
      </button>

      <button class="btn-sync btn-sync-velog" :disabled="isVelogSyncing" @click="handleSync('velog')">
        <span v-if="!isVelogSyncing" class="velog-icon">
          <svg width="16" height="16" viewBox="0 0 36 36" xmlns="http://www.w3.org/2000/svg">
            <rect width="36" height="36" rx="7" fill="#20C997"/>
            <path d="M8 11L15 26H21L28 11H23L18 22L13 11H8Z" fill="white"/>
          </svg>
        </span>
        <i v-else class="fas fa-spinner fa-spin" />
        VELOG SYNC
      </button>
    </div>

    <div class="sidebar-footer">
      <div class="user-profile" @click="router.push('/mypage')">
        <div class="user-avatar">
          <img v-if="authStore.displayProfileImage" :src="authStore.displayProfileImage" alt="avatar" class="avatar-img"/>
          <i v-else class="fas fa-user-circle" />
        </div>
        <div class="user-details">
          <span class="user-name">{{ authStore.displayName || '사용자' }}</span>
          <span class="user-email">{{ authStore.displayEmail || '로그인이 필요합니다' }}</span>
        </div>
      </div>
      <button class="btn-logout" @click.stop="showLogoutModal = true" title="LOGOUT">
        <i class="fas fa-power-off" />
      </button>
    </div>

    <!-- LOGOUT MODAL -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showLogoutModal" class="modal-overlay" @click.self="showLogoutModal = false">
          <div class="modal-content base-panel">
            <div class="modal-header" style="margin-bottom: 32px;">
              <div class="flex-column">
                <h3 class="modal-title">로그아웃 하시겠습니까?</h3>
              </div>
              <button @click="showLogoutModal = false" class="btn-close-modal">
                 <i class="fas fa-times"></i>
              </button>
            </div>
            <div class="modal-actions">
              <button class="btn-outline-small" @click="showLogoutModal = false">취소</button>
              <button class="btn-primary-small btn-danger" @click="handleLogout">로그아웃</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <Transition name="toast">
      <div v-if="showToast" class="sync-toast">
        <i class="fas fa-check-circle" /> {{ toastMsg }}
      </div>
    </Transition>
  </aside>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { syncActivities } from '@/api/aiApi'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const calendarPulse = ref(false)

watch(() => route.path, (newPath, oldPath) => {
  if (newPath === '/calendar' && oldPath !== '/calendar') {
    setTimeout(() => {
      calendarPulse.value = true
      setTimeout(() => { calendarPulse.value = false }, 600)
    }, 750)
  }
})

const isGithubSyncing = ref(false)
const isVelogSyncing = ref(false)
const showToast = ref(false)
const showLogoutModal = ref(false)
const toastMsg = ref('')

const handleSync = async (type) => {
  if (type === 'github') isGithubSyncing.value = true
  else isVelogSyncing.value = true

  try {
    const { data } = await syncActivities(type)
    toastMsg.value = `${type === 'github' ? 'GitHub' : 'Velog'} 동기화 완료 (${data.newCount}건 추가)`
    showToast.value = true
    setTimeout(() => showToast.value = false, 3000)
  } catch (e) {
    console.error('활동 동기화 실패:', e)
  } finally {
    if (type === 'github') isGithubSyncing.value = false
    else isVelogSyncing.value = false
  }
}

const handleLogout = () => {
  showLogoutModal.value = false
  router.push('/')
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
.nav-item.active { background: var(--clr-primary); color: var(--bg-base); border-color: var(--clr-primary); }
@keyframes navPulse {
  0%   { transform: scale(1); box-shadow: 0 0 0 0 var(--clr-primary); }
  30%  { transform: scale(1.06); box-shadow: 0 0 0 6px transparent; }
  65%  { transform: scale(0.97); }
  100% { transform: scale(1); box-shadow: 0 0 0 0 transparent; }
}
.nav-item.pulse { animation: navPulse 0.55s cubic-bezier(0.34, 1.56, 0.64, 1); }
.nav-item i { font-size: 16px; width: 20px; text-align: center; }
.nav-item:not(.active) .fa-calendar-alt { color: var(--clr-icon-calendar); }
.nav-item:not(.active) .fa-compass { color: var(--clr-icon-ai); }
.nav-item:not(.active) .fa-history { color: var(--clr-icon-history); }

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
.btn-sync-github i { color: var(--clr-icon-github); }
.btn-sync-github:hover:not(:disabled) { border-color: var(--clr-icon-github); color: var(--clr-icon-github); background: var(--bg-hover); }

.btn-sync-velog .velog-icon { display: flex; align-items: center; }
.btn-sync-velog:hover:not(:disabled) { border-color: var(--clr-icon-velog); color: var(--clr-icon-velog); background: var(--bg-hover); }

.btn-sync .fa-spinner { color: var(--clr-icon-sync); }
.btn-sync:disabled { opacity: 0.5; cursor: not-allowed; }

/* 푸터 & 유저 섹션 */
.sidebar-footer { 
  padding: 16px; 
  border-top: 1px solid var(--border); 
  display: flex; align-items: center; gap: 8px; 
  background: var(--bg-base);
}
.user-profile { 
  flex: 1; display: flex; align-items: center; gap: 12px; 
  padding: 8px 12px; border: 1px solid var(--border); border-radius: 40px;
  background: var(--bg-base); cursor: pointer; transition: all 0.2s;
  overflow: hidden;
}
.user-profile:hover { border-color: var(--text-primary); background: var(--bg-hover); }
.user-avatar { width: 32px; height: 32px; border-radius: 50%; overflow: hidden; display: flex; align-items: center; justify-content: center; background: var(--bg-surface); font-size: 20px; color: var(--text-muted); }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.user-details { display: flex; flex-direction: column; overflow: hidden; }
.user-name { font-size: 11px; font-weight: 800; color: var(--text-primary); white-space: nowrap; text-overflow: ellipsis; }
.user-email { font-size: 9px; color: var(--text-faint); white-space: nowrap; text-overflow: ellipsis; }

.btn-logout { 
  width: 40px; height: 40px; border-radius: 50%; 
  display: flex; align-items: center; justify-content: center; 
  border: 1px solid var(--border); background: var(--bg-base); 
  color: var(--text-muted); cursor: pointer; transition: all 0.2s; 
}
.btn-logout:hover { border-color: var(--clr-danger); color: var(--clr-danger); background: var(--clr-danger-subtle); }

.btn-logout:hover { border-color: var(--clr-danger); color: var(--clr-danger); background: var(--clr-danger-subtle); }

/* 모달 공통 스타일에 맞춤 (Global 혹은 Local 선언) */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.8); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(2px); padding: 20px; }
.modal-content { width: 100%; max-width: 400px; padding: 32px; background: var(--bg-surface); border: 1px solid var(--border); box-shadow: 0 20px 40px rgba(0,0,0,0.3); border-radius: 16px; }
.modal-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.btn-close-modal { background: transparent; border: none; color: var(--text-muted); cursor: pointer; padding: 4px; font-size: 16px; transition: color 0.2s; margin-top: -4px; margin-right: -4px; }
.btn-close-modal:hover { color: var(--text-primary); }
.modal-title { font-size: 15px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary); margin-bottom: 6px; }
.modal-subtitle { font-size: 11px; color: var(--text-muted); font-weight: 600; margin-bottom: 0; }
.modal-body { margin-bottom: 24px; }
.text-sm { font-size: 12px; line-height: 1.6; color: var(--text-secondary); }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; }

.btn-primary-small { padding: 10px 20px; border: 1px solid var(--text-primary); background: var(--text-primary); color: var(--bg-base); font-weight: 800; font-size: 12px; cursor: pointer; transition: all 0.2s; border-radius: 8px; }
.btn-primary-small:hover { background: transparent; color: var(--text-primary); }
.btn-primary-small.btn-danger { background: var(--clr-danger); border-color: var(--clr-danger); color: white; }
.btn-primary-small.btn-danger:hover { background: transparent; color: var(--clr-danger); }
.btn-outline-small { padding: 10px 20px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); font-weight: 700; font-size: 12px; cursor: pointer; transition: all 0.2s; border-radius: 8px; }
.btn-outline-small:hover { border-color: var(--text-primary); background: var(--bg-hover); }

/* 애니메이션 및 기타 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

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