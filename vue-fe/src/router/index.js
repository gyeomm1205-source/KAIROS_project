import { createRouter, createWebHistory } from 'vue-router';
import TestView from '../views/TestView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/test' },
    { path: '/test', component: TestView },
  ],
});

export default router;
