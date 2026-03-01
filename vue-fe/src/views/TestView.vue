<script setup>
import { computed, reactive, ref } from 'vue';
import { fastApi, springApi } from '../services/api';

const logs = ref([]);

const createForm = reactive({
  authorId: 1,
  title: '',
  content: '',
});

const updateForm = reactive({
  id: '',
  title: '',
  content: '',
});

const postIdForGet = ref('');
const postIdForDelete = ref('');
const posts = ref([]);
const selectedPost = ref(null);

const springBase = computed(() => springApi.defaults.baseURL);
const fastApiBase = computed(() => fastApi.defaults.baseURL);

function normalizeError(error) {
  if (error?.response) {
    return {
      status: error.response.status,
      data: error.response.data,
    };
  }
  return {
    status: 'NETWORK',
    data: { message: error?.message || 'Unknown error' },
  };
}

function pushLog(action, payload, ok = true) {
  logs.value.unshift({
    at: new Date().toISOString(),
    action,
    ok,
    payload,
  });
}

async function runAction(action, fn) {
  try {
    const result = await fn();
    pushLog(action, result, true);
    return result;
  } catch (error) {
    const normalized = normalizeError(error);
    pushLog(action, normalized, false);
    throw error;
  }
}

async function pingSpring() {
  await runAction('spring:ping', async () => (await springApi.get('/api/test/ping')).data);
}

async function pingSpringFastapi() {
  await runAction('spring:ping:fastapi', async () => (await springApi.get('/api/test/ping/fastapi')).data);
}

async function pingFastapi() {
  await runAction('fastapi:ping', async () => (await fastApi.get('/api/test/ping')).data);
}

async function pingFastapiSpring() {
  await runAction('fastapi:ping:spring', async () => (await fastApi.get('/api/test/ping/spring')).data);
}

async function createPost() {
  const payload = {
    authorId: Number(createForm.authorId),
    title: createForm.title,
    content: createForm.content,
  };
  const created = await runAction('mockpost:create', async () => (await springApi.post('/api/test/mock-posts', payload)).data);
  createForm.title = '';
  createForm.content = '';
  await listPosts();
  return created;
}

async function listPosts() {
  const list = await runAction('mockpost:list', async () => (await springApi.get('/api/test/mock-posts')).data);
  posts.value = list;
  return list;
}

async function getPost() {
  if (!postIdForGet.value) {
    return;
  }
  const post = await runAction('mockpost:get', async () => (await springApi.get(`/api/test/mock-posts/${postIdForGet.value}`)).data);
  selectedPost.value = post;
}

async function updatePost() {
  if (!updateForm.id) {
    return;
  }
  const payload = {
    title: updateForm.title,
    content: updateForm.content,
  };
  await runAction('mockpost:update', async () => (await springApi.put(`/api/test/mock-posts/${updateForm.id}`, payload)).data);
  await listPosts();
}

async function deletePost() {
  if (!postIdForDelete.value) {
    return;
  }
  await runAction('mockpost:delete', async () => (await springApi.delete(`/api/test/mock-posts/${postIdForDelete.value}`)).status);
  postIdForDelete.value = '';
  await listPosts();
}
</script>

<template>
  <main class="page">
    <header class="hero">
      <p class="eyebrow">S14P21A506</p>
      <h1>Integration Test Console</h1>
      <p>Spring <code>{{ springBase }}</code> | FastAPI <code>{{ fastApiBase }}</code></p>
    </header>

    <section class="grid two">
      <article class="card">
        <h2>Ping-Pong</h2>
        <div class="actions">
          <button @click="pingSpring">Spring Ping</button>
          <button @click="pingSpringFastapi">Spring -> FastAPI</button>
          <button @click="pingFastapi">FastAPI Ping</button>
          <button @click="pingFastapiSpring">FastAPI -> Spring</button>
        </div>
      </article>

      <article class="card">
        <h2>MockPost Read</h2>
        <div class="field-row">
          <input v-model="postIdForGet" type="number" min="1" placeholder="Post ID" />
          <button @click="getPost">Get One</button>
          <button @click="listPosts">List All</button>
        </div>
        <pre v-if="selectedPost" class="json">{{ JSON.stringify(selectedPost, null, 2) }}</pre>
      </article>
    </section>

    <section class="grid two">
      <article class="card">
        <h2>Create MockPost</h2>
        <div class="form-grid">
          <label>Author ID<input v-model="createForm.authorId" type="number" min="1" /></label>
          <label>Title<input v-model="createForm.title" type="text" maxlength="120" /></label>
          <label>Content<textarea v-model="createForm.content" rows="5" maxlength="10000" /></label>
        </div>
        <button @click="createPost">Create</button>
      </article>

      <article class="card">
        <h2>Update/Delete MockPost</h2>
        <div class="form-grid">
          <label>Target ID<input v-model="updateForm.id" type="number" min="1" /></label>
          <label>Title<input v-model="updateForm.title" type="text" maxlength="120" /></label>
          <label>Content<textarea v-model="updateForm.content" rows="4" maxlength="10000" /></label>
        </div>
        <div class="field-row">
          <button @click="updatePost">Update</button>
          <input v-model="postIdForDelete" type="number" min="1" placeholder="Delete ID" />
          <button class="danger" @click="deletePost">Delete</button>
        </div>
      </article>
    </section>

    <section class="grid two">
      <article class="card">
        <h2>MockPost List</h2>
        <div class="list">
          <p v-if="posts.length === 0" class="muted">No posts loaded.</p>
          <article v-for="post in posts" :key="post.id" class="post-item">
            <h3>#{{ post.id }} {{ post.title }}</h3>
            <p>{{ post.content }}</p>
            <small>authorId={{ post.authorId }} | createdAt={{ post.createdAt }}</small>
          </article>
        </div>
      </article>

      <article class="card">
        <h2>Logs</h2>
        <div class="logs">
          <p v-if="logs.length === 0" class="muted">No logs yet.</p>
          <article v-for="(log, idx) in logs" :key="idx" class="log-item" :class="{ fail: !log.ok }">
            <header>
              <strong>{{ log.action }}</strong>
              <span>{{ log.at }}</span>
            </header>
            <pre class="json">{{ JSON.stringify(log.payload, null, 2) }}</pre>
          </article>
        </div>
      </article>
    </section>
  </main>
</template>

<style scoped>
.page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 18px 42px;
  display: grid;
  gap: 16px;
}

.hero {
  background: linear-gradient(120deg, rgba(8, 81, 156, 0.85), rgba(16, 185, 129, 0.8));
  border-radius: 18px;
  padding: 20px;
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.35);
}

.eyebrow {
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 12px;
  opacity: 0.85;
}

.grid {
  display: grid;
  gap: 16px;
}

.two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.card {
  background: rgba(7, 15, 31, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  padding: 16px;
  backdrop-filter: blur(8px);
}

.actions,
.field-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.form-grid {
  display: grid;
  gap: 8px;
  margin-bottom: 8px;
}

label {
  display: grid;
  gap: 6px;
  font-size: 14px;
}

input,
textarea {
  width: 100%;
  background: rgba(4, 11, 24, 0.8);
  color: #f2f4f7;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 9px;
  padding: 10px;
}

button {
  background: linear-gradient(120deg, #0ea5e9, #22c55e);
  border: none;
  color: #03131f;
  border-radius: 9px;
  padding: 10px 12px;
  font-weight: 700;
  cursor: pointer;
}

button.danger {
  background: linear-gradient(120deg, #f97316, #ef4444);
}

.logs,
.list {
  max-height: 370px;
  overflow: auto;
  display: grid;
  gap: 10px;
}

.log-item,
.post-item {
  background: rgba(3, 9, 19, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  padding: 10px;
}

.log-item.fail {
  border-color: rgba(248, 113, 113, 0.9);
}

.log-item header {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 12px;
  margin-bottom: 6px;
}

.json {
  background: rgba(2, 7, 16, 0.86);
  border-radius: 8px;
  padding: 10px;
  overflow: auto;
  font-size: 12px;
}

.muted {
  opacity: 0.72;
}

@media (max-width: 960px) {
  .two {
    grid-template-columns: 1fr;
  }
}
</style>
