import axios from 'axios'

/**
 * Axios 인스턴스 - Java 백엔드 기본 설정
 * baseURL은 .env 파일의 VITE_API_BASE_URL 로 관리하세요.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// ─── 노드 API ───
export const nodeApi = {
  /** 전체 노드 조회 */
  getAll:  ()         => api.get('/nodes'),
  /** 노드 생성 */
  create:  (data)     => api.post('/nodes', data),
  /** 노드 수정 */
  update:  (id, data) => api.put(`/nodes/${id}`, data),
  /** 노드 삭제 */
  delete:  (id)       => api.delete(`/nodes/${id}`)
}

// ─── 엣지(연결선) API ───
export const edgeApi = {
  getAll: ()     => api.get('/edges'),
  create: (data) => api.post('/edges', data),
  delete: (id)   => api.delete(`/edges/${id}`)
}

// ─── 브랜치 API ───
export const branchApi = {
  getAll:  ()         => api.get('/branches'),
  create:  (data)     => api.post('/branches', data),
  update:  (id, data) => api.put(`/branches/${id}`, data),
  delete:  (id)       => api.delete(`/branches/${id}`)
}
