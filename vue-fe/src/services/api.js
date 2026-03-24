import axios from 'axios';
import { getAuthToken } from './authToken';

const springBaseURL = import.meta.env.VITE_SPRING_BASE_URL || '';
const fastApiBaseURL = import.meta.env.VITE_FASTAPI_BASE_URL || '';

export const springApi = axios.create({
  baseURL: springBaseURL,
  timeout: 5000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// GitHub 연동은 백엔드가 FastAPI 수집 트리거까지 포함하므로 timeout을 넉넉하게 설정
export const springApiSlow = axios.create({
  baseURL: springBaseURL,
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const fastApi = axios.create({
  baseURL: fastApiBaseURL,
  timeout: 5000,
});

function attachBearerToken(config) {
  // 호출부에서 Authorization을 직접 지정한 경우(예: linkGithub의 onboardingToken)는 덮어쓰지 않음
  if (config.headers.Authorization) {
    return config;
  }
  const token = getAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}

springApi.interceptors.request.use(attachBearerToken);
springApiSlow.interceptors.request.use(attachBearerToken);
fastApi.interceptors.request.use(attachBearerToken);
