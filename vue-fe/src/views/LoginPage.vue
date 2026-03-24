<template>
  <div class="login-root">
    <div class="login-card">
      <!-- 로고 -->
      <div class="login-logo">
        <svg width="32" height="32" viewBox="0 0 28 28" fill="none">
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" fill="url(#kgLogin)" opacity="0.15"/>
          <path d="M6 3h16v5l-6 6 6 6v5H6v-5l6-6-6-6V3z" stroke="url(#kgLogin)" stroke-width="1.5" fill="none" stroke-linejoin="round"/>
          <circle cx="14" cy="14" r="2" fill="url(#kgLogin)"/>
          <defs>
            <linearGradient id="kgLogin" x1="6" y1="3" x2="22" y2="25" gradientUnits="userSpaceOnUse">
              <stop stop-color="#818cf8"/><stop offset="1" stop-color="#38bdf8"/>
            </linearGradient>
          </defs>
        </svg>
        <span class="logo-text">KAIROS</span>
      </div>

      <h2 class="login-title">다시 오셨군요!</h2>
      <p class="login-sub">계정에 로그인하여 학습을 이어가세요</p>

      <!-- 이메일 -->
      <div class="form-group">
        <label class="form-label">이메일</label>
        <input
          v-model="email"
          type="email"
          class="form-input"
          placeholder="example@email.com"
        />
      </div>

      <!-- 비밀번호 -->
      <div class="form-group">
        <label class="form-label">비밀번호</label>
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
      </div>

      <!-- 로그인 버튼 -->
      <button class="btn-primary" @click="handleLogin">로그인</button>

      <div class="divider"><span>또는</span></div>

      <!-- Google 로그인 -->
      <button class="btn-google" @click="handleLogin">
        <div class="google-icon">G</div>
        Google로 로그인
      </button>

      <p class="signup-link">
        계정이 없으신가요?
        <span @click="$router.push('/signup')">회원가입</span>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const email = ref('')
const password = ref('')
const showPw = ref(false)

function handleLogin() {
  router.push('/calendar')
}
</script>

<style scoped>
.login-root {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-base);
  font-family: 'Escoredream', system-ui, sans-serif;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 40px 36px;
  display: flex;
  flex-direction: column;
  gap: 0;
  animation: fadeIn 0.35s ease both;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: translateY(0); }
}

.login-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
}
.logo-text {
  font-weight: 800;
  font-size: 18px;
  letter-spacing: 0.16em;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.login-sub {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 28px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.form-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.input-wrap {
  position: relative;
}
.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-elevated);
  color: var(--text-primary);
  font-size: 14px;
  font-family: 'Escoredream', sans-serif;
  transition: border-color 0.15s;
  box-sizing: border-box;
}
.form-input:focus {
  outline: none;
  border-color: #818cf8;
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

.btn-primary {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: #111;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  font-family: 'Escoredream', sans-serif;
  transition: opacity 0.15s;
  margin-top: 8px;
}
.btn-primary:hover { opacity: 0.85; }

.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 20px 0;
  color: var(--text-faint);
  font-size: 12px;
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
  padding: 13px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--bg-elevated);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: 'Escoredream', sans-serif;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transition: background 0.15s;
}
.btn-google:hover { background: var(--bg-hover); }
.google-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
  color: #fff;
}

.signup-link {
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 20px;
}
.signup-link span {
  color: #818cf8;
  font-weight: 600;
  cursor: pointer;
}
.signup-link span:hover { text-decoration: underline; }
</style>
