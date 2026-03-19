<template>
  <div class="modal-root">
    <div class="modal-card">
      <div class="check-icon"><i class="fas fa-check" /></div>
      <h2>커리큘럼이 캘린더에 업로드되었습니다.</h2>
      <p>다음 주 학습 계획을 확인해보세요.</p>
      
      <button class="btn-primary" @click="$router.push('/calendar')">캘린더로 이동</button>
    </div>
  </div>
</template>

<style scoped>
.modal-root { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--bg-base); font-family: 'Escoredream', sans-serif; padding: 20px; }
.modal-card { width: 100%; max-width: 440px; background: transparent; border: 1px solid var(--border); border-radius: 0; padding: 48px 40px; text-align: center; animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both; }
@keyframes fadeUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }

.check-icon { width: 64px; height: 64px; background: transparent; color: var(--text-primary); border: 1px solid var(--border); border-radius: 0; display: flex; align-items: center; justify-content: center; font-size: 24px; margin: 0 auto 32px; }
h2 { font-size: 20px; font-weight: 800; color: var(--text-primary); margin-bottom: 16px; letter-spacing: 0.05em; }
p { font-size: 14px; font-weight: 600; color: var(--text-muted); margin-bottom: 40px; }

.btn-primary { width: 100%; padding: 16px; border-radius: 40px; font-size: 14px; font-weight: 800; cursor: pointer; font-family: inherit; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); letter-spacing: 0.1em; background: transparent; color: var(--text-primary); border: 1px solid var(--text-primary); }
.btn-primary:hover { background: var(--text-primary); color: var(--bg-base); }
</style>