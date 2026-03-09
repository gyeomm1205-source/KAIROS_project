import { createRouter, createWebHistory } from 'vue-router';
import TestView from '../views/TestView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: TestView,
      alias: ['/test'],
    },
  ],
});

export default router;
