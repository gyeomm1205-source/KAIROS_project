<template>
  <div class="login-root">
    <div class="login-card">
      <div class="login-logo">
        <svg width="32" height="32" viewBox="0 0 28 28" fill="none">
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" fill="currentColor" opacity="0.15"/>
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linejoin="round"/>
          <circle cx="14" cy="14" r="2" fill="currentColor"/>
          <path d="M8 5h12l-4 4H12L8 5z" fill="currentColor" opacity="0.5"/>
        </svg>
        <span class="logo-text">KAIROS</span>
      </div>

      <h2 class="login-title">WELCOME BACK.</h2>
      <p class="login-sub">계정에 로그인하여 학습을 이어가세요</p>

      <div class="form-group">
        <label class="form-label">EMAIL</label>
        <input
          v-model="email"
          type="email"
          class="form-input"
          placeholder="example@email.com"
        />
      </div>

      <div class="form-group">
        <label class="form-label">PASSWORD</label>
        <div class="input-wrap">
          <input
            v-model="password"
            :type="showPw ? 'text' : 'password'"
            class="form-input"
            placeholder="비밀번호를 입력하세요"
          />
          <button class="pw-toggle" @click="showPw = !showPw">
            <i :class="showPw ? 'fas fa-eye-slash' : 'fas fa-eye'" />
          </button>
        </div>

        <div class="auth-footer">
          신규 유저이신가요? 
          <span class="text-accent-1 link-hover" @click="$router.push('/signup')">[회원가입]</span>
        </div>

      </div>

      <button class="btn-primary" @click="handleLogin">LOGIN</button>

      <div class="divider"><span>OR</span></div>

      <button class="btn-google" @click="handleLogin">
        <div class="google-icon">G</div>
        CONTINUE WITH GOOGLE
      </button>

      <p class="signup-link">
        계정이 없으신가요?
        <span @click="$router.push('/signup')">CREATE ACCOUNT</span>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/useThemeStore'

const router = useRouter()
const themeStore = useThemeStore()
const email = ref('')
const password = ref('')
const showPw = ref(false)

function handleLogin() {
  router.push('/calendar')
}

onMounted(() => {
  // 로그인 페이지 접속 시 기본적으로 다크모드가 감성이 좋아 다크모드로 세팅 권장 (선택사항)
  // if (!themeStore.isDark) themeStore.isDark = true;
})
</script>

<style scoped>
.login-root {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-base);
  font-family: 'Space Grotesk', 'Escoredream', system-ui, sans-serif;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: transparent;
  border: 1px solid var(--border);
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  gap: 0;
  animation: fadeUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) both;
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

.login-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 32px;
  color: var(--text-primary);
}
.logo-text {
  font-weight: 800;
  font-size: 20px;
  letter-spacing: 0.16em;
  color: var(--text-primary);
}

.login-title {
  font-size: 24px;
  font-weight: 900;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: 0.05em;
}
.login-sub {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 32px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}
.form-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--text-primary);
}
.input-wrap {
  position: relative;
}
.form-input {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-primary);
  font-size: 14px;
  font-family: inherit;
  transition: border-color 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-sizing: border-box;
}
.form-input:focus {
  outline: none;
  border-color: var(--text-primary);
}
.pw-toggle {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-muted);
  font-size: 14px;
}
.pw-toggle:hover { color: var(--text-primary); }

.btn-primary {
  width: 100%;
  padding: 16px;
  border: 1px solid var(--text-primary);
  background: var(--text-primary);
  color: var(--bg-base);
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.1em;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  margin-top: 8px;
}
.btn-primary:hover { 
  background: transparent; 
  color: var(--text-primary); 
}

.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 24px 0;
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.1em;
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border);
}

.btn-google {
  width: 100%;
  padding: 14px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.05em;
  cursor: pointer;
  font-family: inherit;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.btn-google:hover { 
  background: var(--bg-hover); 
  border-color: var(--text-primary);
}
.google-icon {
  width: 20px;
  height: 20px;
  border: 1px solid var(--border);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
  transition: border-color 0.3s;
}
.btn-google:hover .google-icon {
  border-color: var(--text-primary);
}
.terminal-input::placeholder { color: var(--k-text-muted); font-family: 'Mulmaru', sans-serif; font-size: 13px; }
.terminal-input:focus { outline: none; border-color: var(--k-acc-1-bg); background: var(--k-key-bg); color: var(--k-text); box-shadow: inset 0 2px 4px rgba(0,0,0,0.2), 0 0 10px rgba(209, 154, 102, 0.2); }

.signup-link {
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 24px;
  letter-spacing: 0.05em;
}
.signup-link span {
  color: var(--text-primary);
  font-weight: 700;
  cursor: pointer;
  margin-left: 6px;
}
.signup-link span:hover { text-decoration: underline; }
</style>