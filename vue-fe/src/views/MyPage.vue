<template>
  <div class="app-layout">
    <AppSidebar />

    <main class="main-content custom-scroll">
      <header class="page-header">
        <div class="header-title"><i class="fas fa-user-circle" /> MY PAGE</div>
      </header>

      <div class="content-inner max-w-xl mx-auto">
        <!-- Profile Summary -->
        <div class="base-panel p-lg mb-md shadow-normal flex-align gap-lg">
          <div class="avatar-circle">{{ profile.initials }}</div>
          <div class="flex-col min-w-0">
            <h2 class="profile-name">{{ profile.nickname }}</h2>
            <p class="profile-email">{{ profile.email }}</p>
            <div class="flex-align gap-md mt-sm">
              <span class="profile-meta">{{ profile.position }}</span>
            </div>
          </div>
        </div>

        <!-- Settings Menu Cards -->
        <div class="flex-col gap-sm mb-lg">
          <!-- Profile Settings -->
          <button class="menu-card group shadow-normal" @click="$router.push('/mypage/profile')">
            <div class="menu-icon-box">
              <i class="fas fa-user" />
            </div>
            <div class="menu-text-wrap">
              <h3 class="menu-title">프로필 설정</h3>
              <p class="menu-desc">연동 정보 범위, 일정 반영 범위, 현재 직업과 기술 스택을 수정합니다</p>
            </div>
            <i class="fas fa-arrow-right menu-arrow" />
          </button>

          <!-- Account Management -->
          <button class="menu-card group shadow-normal" @click="$router.push('/mypage/accounts')">
            <div class="menu-icon-box">
              <i class="fas fa-link" />
            </div>
            <div class="menu-text-wrap">
              <h3 class="menu-title">외부 계정 관리</h3>
              <p class="menu-desc">GitHub, Velog 연동 상태와 마지막 동기화 시각을 확인합니다</p>
            </div>
            <i class="fas fa-arrow-right menu-arrow" />
          </button>
        </div>

        <!-- Additional Info -->
        <div class="info-note base-panel shadow-none">
          <div class="flex-start gap-md">
            <i class="fas fa-shield-alt text-muted mt-xs text-sm shrink-0" />
            <p class="text-sm text-muted font-bold lh-lg m-0">
              연동된 계정 데이터는 학습 분석과 추천에만 활용되며, 외부에 공유되지 않습니다.
            </p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import { useThemeStore } from '@/stores/useThemeStore'
import { getUserProfile } from '@/api/aiApi'

const router = useRouter()
const themeStore = useThemeStore()

const profile = reactive({
  nickname: '사용자',
  email: '',
  position: '',
  initials: 'U',
})

onMounted(async () => {
  try {
    const { data } = await getUserProfile()
    profile.nickname = data.nickname || '사용자'
    profile.email = data.email || ''
    profile.position = data.position || ''
    profile.initials = (data.nickname || 'U').slice(0, 2).toUpperCase()
    if (data.darkModeEnabled !== undefined) {
      themeStore.setFromServer(data.darkModeEnabled)
    }
  } catch (e) {
    console.error('프로필 조회 실패 (Mock 유지):', e)
  }
})
</script>

<style scoped>
.app-layout { display: flex; width: 100%; height: 100vh; overflow: hidden; background: var(--bg-base); font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif; }
.main-content { flex: 1; display: flex; flex-direction: column; overflow-y: auto; position: relative; }
.custom-scroll { -ms-overflow-style: none; scrollbar-width: none; }
.custom-scroll::-webkit-scrollbar { display: none; }

.page-header { display: flex; align-items: center; padding: 20px 32px; border-bottom: 2px solid var(--text-primary); background: var(--bg-surface); position: sticky; top: 0; z-index: 10; }
.header-title { font-size: 16px; font-weight: 900; letter-spacing: 0.1em; color: var(--text-primary); display: flex; align-items: center; gap: 10px; }

.content-inner { padding: 48px 32px; width: 100%; }
.max-w-xl { max-width: 600px; }
.mx-auto { margin-left: auto; margin-right: auto; }

/* Utils */
.flex-align { display: flex; align-items: center; }
.flex-col { display: flex; flex-direction: column; }
.flex-start { display: flex; align-items: flex-start; }
.gap-sm { gap: 12px; }
.gap-md { gap: 16px; }
.gap-lg { gap: 24px; }
.mb-md { margin-bottom: 16px; }
.mb-lg { margin-bottom: 24px; }
.mt-sm { margin-top: 8px; }
.mt-xs { margin-top: 4px; }
.p-lg { padding: 32px; }
.m-0 { margin: 0; }
.min-w-0 { min-width: 0; }
.shrink-0 { flex-shrink: 0; }

.text-muted { color: var(--text-muted); }
.text-sm { font-size: 13px; }
.font-bold { font-weight: 800; }
.lh-lg { line-height: 1.6; }

/* Panels & Shadows */
.base-panel { 
  background: transparent; 
  border: 1px solid var(--border); 
  border-radius: 0; 
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.3s; 
}
.shadow-normal { box-shadow: none; }
.shadow-none { box-shadow: none; }

/* Profile Area */
.avatar-circle { width: 64px; height: 64px; border: 2px solid var(--text-primary); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 900; background: var(--bg-base); color: var(--text-primary); }
.profile-name { font-size: 20px; font-weight: 900; margin: 0 0 4px 0; color: var(--text-primary); }
.profile-email { font-size: 13px; font-weight: 700; margin: 0; color: var(--text-muted); }
.profile-meta { font-size: 12px; font-weight: 800; color: var(--text-muted); }

/* Menu Cards */
.menu-card { 
  display: flex; align-items: center; gap: 20px; text-align: left; 
  padding: 24px; background: transparent; 
  border: 1px solid var(--border); cursor: pointer; 
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); 
  font-family: inherit; margin-bottom: 16px; 
}
.menu-card:hover { 
  background: var(--bg-hover); border-color: var(--text-primary); 
}
.menu-icon-box { 
  width: 48px; height: 48px; background: transparent; 
  border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; 
  font-size: 20px; color: var(--text-primary); transition: all 0.3s; 
}
.menu-card:hover .menu-icon-box { border-color: var(--text-primary); }
.menu-text-wrap { flex: 1; min-width: 0; }
.menu-title { font-size: 16px; font-weight: 900; margin: 0 0 4px 0; color: var(--text-primary); }
.menu-desc { font-size: 13px; font-weight: 700; margin: 0; color: var(--text-muted); }
.menu-arrow { font-size: 18px; color: var(--text-muted); transition: color 0.3s; }
.menu-card:hover .menu-arrow { color: var(--text-primary); opacity: 1; transform: translateX(4px); }

/* Additional Info */
.info-note { 
  background: transparent; padding: 20px; 
  border: 1px dashed var(--border); 
}
</style>