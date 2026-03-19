<template>
  <div class="modal-root">
    <div class="modal-card">
      <h2>분석 결과가 정확하지 않나요?</h2>
      <textarea
        placeholder="어떤 부분이 잘못 분석되었는지 입력해주세요."
        rows="5"
      ></textarea>
      <p class="hint">입력해주신 내용을 바탕으로 다시 분석합니다.</p>
      
      <div class="btn-group">
        <button class="btn-primary">다시 분석하기</button>
        <button class="btn-secondary" @click="$router.push('/analysis')">이전으로</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-root { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--bg-base); font-family: 'Escoredream', sans-serif; padding: 20px; }
.modal-card { width: 100%; max-width: 440px; background: transparent; border: 1px solid var(--border); border-radius: 0; padding: 48px 40px; text-align: center; animation: fadeUp 0.4s cubic-bezier(0.16, 1, 0.3, 1) both; }
@keyframes fadeUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }

h2 { font-size: 20px; font-weight: 800; margin-bottom: 24px; color: var(--text-primary); letter-spacing: 0.05em; }
textarea { width: 100%; padding: 16px; border: 1px solid var(--border); background: transparent; color: var(--text-primary); border-radius: 0; font-size: 14px; font-weight: 700; font-family: inherit; resize: none; margin-bottom: 12px; box-sizing: border-box; outline: none; transition: border-color 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
textarea:focus { border-color: var(--text-primary); background: var(--bg-hover);}

.hint { font-size: 12px; color: var(--text-muted); font-weight: 600; margin-bottom: 32px; text-align: left; }
.btn-group { display: flex; flex-direction: column; gap: 12px; }

button { width: 100%; padding: 16px; border-radius: 40px; font-size: 14px; font-weight: 800; cursor: pointer; font-family: inherit; transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); letter-spacing: 0.1em; }
.btn-primary { background: transparent; color: var(--text-primary); border: 1px solid var(--text-primary); }
.btn-primary:hover { background: var(--text-primary); color: var(--bg-base); }

.btn-secondary { background: transparent; color: var(--text-primary); border: 1px solid var(--border); }
.btn-secondary:hover { background: var(--bg-hover); border-color: var(--text-primary); }
</style>