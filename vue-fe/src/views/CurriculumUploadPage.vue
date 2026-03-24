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
.modal-root { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f9f9f9; font-family: 'Escoredream', sans-serif; padding: 20px; }
.modal-card { width: 100%; max-width: 440px; background: #fff; border-radius: 16px; padding: 48px 32px; box-shadow: 0 4px 20px rgba(0,0,0,0.06); text-align: center; border: 1px solid #eaeaea; }
.check-icon { width: 56px; height: 56px; background: #eee; color: #555; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24px; margin: 0 auto 24px; }
h2 { font-size: 18px; font-weight: 800; color: #111; margin-bottom: 12px; }
p { font-size: 13px; color: #666; margin-bottom: 32px; }
.btn-primary { width: 100%; background: #111; color: #fff; border: none; padding: 14px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; font-family: inherit; transition: background 0.2s; }
.btn-primary:hover { background: #333; }
</style>