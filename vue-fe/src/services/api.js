import axios from 'axios';
import { getAuthToken, setAuthToken, clearAuthToken } from './authToken';

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

// --- 401 토큰 만료 시 자동 갱신 ---
let isRefreshing = false;
let pendingRequests = [];

function onRefreshed(newToken) {
  pendingRequests.forEach(({ resolve }) => resolve(newToken));
  pendingRequests = [];
}

function onRefreshFailed(err) {
  pendingRequests.forEach(({ reject }) => reject(err));
  pendingRequests = [];
}

function handle401(error) {
  const originalRequest = error.config;

  if (originalRequest.url?.includes('/auth/reissue')) {
    clearAuthToken();
    // window.location.href = '/login'; // [임시 비활성화]
    return Promise.reject(error);
  }

  if (!isRefreshing) {
    isRefreshing = true;
    axios.post(`${springBaseURL}/api/v1/auth/reissue`, null, { withCredentials: true })
      .then(({ data }) => {
        setAuthToken(data.accessToken);
        isRefreshing = false;
        onRefreshed(data.accessToken);
      })
      .catch((err) => {
        isRefreshing = false;
        clearAuthToken();
        onRefreshFailed(err);
        // window.location.href = '/login'; // [임시 비활성화]
      });
  }

  return new Promise((resolve, reject) => {
    pendingRequests.push({
      resolve: (newToken) => {
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        resolve(springApi(originalRequest));
      },
      reject,
    });
  });
}

function responseErrorInterceptor(error) {
  if (error.response?.status === 401) {
    return handle401(error);
  }
  return Promise.reject(error);
}

springApi.interceptors.response.use(res => res, responseErrorInterceptor);
springApiSlow.interceptors.response.use(res => res, responseErrorInterceptor);
