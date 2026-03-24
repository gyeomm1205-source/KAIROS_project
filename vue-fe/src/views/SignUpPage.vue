<template>
  <div class="auth-root">
    <div class="auth-card">
      <div class="auth-logo-wrap">
        <div class="auth-avatar"><i class="fas fa-user" /></div>
        <h2>회원가입</h2>
        <p>계정 정보를 확인하고 서비스를 연동해주세요</p>
      </div>

      <!-- 구글 계정 정보 -->
      <div class="auth-section">
        <div class="section-label">
          <div class="google-badge">G</div>
          <span>구글 계정 정보</span>
        </div>
        <div class="field-row">
          <label>이름</label>
          <div class="field-display">홍길동</div>
        </div>
        <div class="field-row">
          <label>이메일</label>
          <div class="field-display">hong.gildong@gmail.com</div>
        </div>
      </div>

      <!-- 서비스 연동 -->
      <div class="auth-section auth-section--bordered">
        <div class="section-plain-label">서비스 연동</div>
        <button class="link-btn">
          <div class="link-btn-left">
            <div class="link-icon"><i class="fas fa-link" /></div>
            <span>Velog 연동하기</span>
          </div>
          <i class="fas fa-chevron-right" />
        </button>
        <button class="link-btn">
          <div class="link-btn-left">
            <div class="link-icon"><i class="fab fa-github" /></div>
            <span>GitHub 연동하기</span>
          </div>
          <i class="fas fa-chevron-right" />
        </button>
        <p class="link-hint">연동은 선택사항이며, 나중에 설정에서 추가할 수 있습니다</p>
      </div>

      <button class="btn-primary" @click="$router.push('/setup')">회원가입 완료</button>

      <p class="terms-text">
        회원가입을 진행하면<br>
        <a href="#">이용약관</a> 및 <a href="#">개인정보처리방침</a>에 동의하는 것으로 간주됩니다
      </p>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.auth-root {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: var(--bg-base); padding: 24px;
  font-family: 'Escoredream', system-ui, sans-serif;
}
.auth-card {
  width: 100%; max-width: 440px;
  background: var(--bg-surface); border: 1px solid var(--border);
  border-radius: 20px; padding: 40px 36px;
  animation: fadeUp 0.3s ease both;
}
@keyframes fadeUp { from { opacity:0; transform:translateY(10px); } to { opacity:1; transform:translateY(0); } }

.auth-logo-wrap { text-align: center; margin-bottom: 32px; }
.auth-avatar {
  width: 56px; height: 56px; border-radius: 50%; margin: 0 auto 16px;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; color: #fff;
}
.auth-logo-wrap h2 { font-size: 24px; font-weight: 800; color: var(--text-primary); }
.auth-logo-wrap p  { font-size: 13px; color: var(--text-muted); margin-top: 6px; }

.auth-section { margin-bottom: 22px; }
.auth-section--bordered { padding-top: 20px; border-top: 1px solid var(--border); }

.section-label {
  display: flex; align-items: center; gap: 10px; margin-bottom: 14px;
  font-weight: 600; font-size: 14px; color: var(--text-secondary);
}
.google-badge {
  width: 26px; height: 26px; border-radius: 50%; background: var(--bg-elevated);
  border: 1px solid var(--border); display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 800; color: var(--text-muted);
}
.section-plain-label { font-weight: 600; font-size: 14px; color: var(--text-secondary); margin-bottom: 12px; }

.field-row { margin-bottom: 12px; }
.field-row label { font-size: 12px; font-weight: 600; color: var(--text-muted); display: block; margin-bottom: 6px; }
.field-display {
  background: var(--bg-elevated); border: 1px solid var(--border);
  border-radius: 10px; padding: 12px 16px; font-size: 14px;
  color: var(--text-faint); text-align: center;
}

.link-btn {
  width: 100%; display: flex; align-items: center; justify-content: space-between;
  border: 1px solid var(--border); padding: 13px 16px; border-radius: 12px;
  background: var(--bg-elevated); cursor: pointer; margin-bottom: 10px;
  color: var(--text-secondary); transition: background 0.15s;
}
.link-btn:hover { background: var(--bg-hover); }
.link-btn-left { display: flex; align-items: center; gap: 12px; }
.link-icon {
  width: 34px; height: 34px; background: var(--bg-base); border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted); font-size: 14px;
}
.link-btn span { font-size: 14px; font-weight: 500; }
.link-btn .fa-chevron-right { font-size: 11px; color: var(--text-faint); }
.link-hint { text-align: center; font-size: 11px; color: var(--text-faint); margin-top: 4px; }

.btn-primary {
  width: 100%; padding: 14px; border-radius: 12px; border: none; cursor: pointer;
  background: linear-gradient(135deg, #818cf8, #38bdf8);
  color: #fff; font-weight: 700; font-size: 15px; margin-top: 4px;
  transition: opacity 0.15s; font-family: 'Escoredream', sans-serif;
}
.btn-primary:hover { opacity: 0.87; }

.terms-text {
  text-align: center; font-size: 11px; color: var(--text-faint);
  margin-top: 16px; line-height: 1.9;
}
.terms-text a { color: var(--text-muted); text-decoration: underline; cursor: pointer; }
</style>
