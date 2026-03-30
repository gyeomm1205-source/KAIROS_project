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
.modal-root { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f5f5f5; font-family: 'Escoredream', sans-serif; padding: 20px; }
.modal-card { width: 100%; max-width: 400px; background: #fff; border-radius: 12px; padding: 36px 32px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); text-align: center; }
h2 { font-size: 18px; font-weight: 800; margin-bottom: 24px; color: #111; }
textarea { width: 100%; padding: 14px; border: 1px solid #ddd; border-radius: 8px; font-size: 13px; font-family: inherit; resize: none; margin-bottom: 12px; box-sizing: border-box; outline: none; transition: border-color 0.2s; }
textarea:focus { border-color: #111; }
.hint { font-size: 11px; color: #666; margin-bottom: 28px; text-align: left; }
.btn-group { display: flex; flex-direction: column; gap: 10px; }
button { width: 100%; padding: 14px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; font-family: inherit; transition: opacity 0.2s; }
.btn-primary { background: #111; color: #fff; border: none; }
.btn-secondary { background: #fff; color: #333; border: 1px solid #ddd; }
button:hover { opacity: 0.8; }
</style>