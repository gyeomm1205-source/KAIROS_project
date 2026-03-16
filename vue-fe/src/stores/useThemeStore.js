// src/stores/useThemeStore.js
import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false)  // 기본: 라이트 모드로 변경됨

  const themeClass = computed(() => isDark.value ? 'theme-dark' : 'theme-light')

  // body에 클래스 동기화 → Teleport to="body" 모달에서 CSS 변수 사용 가능
  function syncBody(dark) {
    document.body.classList.toggle('is-dark',  dark)
    document.body.classList.toggle('is-light', !dark)
  }

  watch(isDark, syncBody, { immediate: true })

  function toggle() { isDark.value = !isDark.value }

  return { isDark, themeClass, toggle }
})