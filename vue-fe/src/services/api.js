import axios from 'axios';
import { getAuthToken } from './authToken';

const springBaseURL = import.meta.env.VITE_SPRING_BASE_URL || 'http://localhost:8080';
const fastApiBaseURL = import.meta.env.VITE_FASTAPI_BASE_URL || 'http://localhost:8000';

export const springApi = axios.create({
  baseURL: springBaseURL,
  timeout: 5000,
});

export const fastApi = axios.create({
  baseURL: fastApiBaseURL,
  timeout: 5000,
});

function attachBearerToken(config) {
  const token = getAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}

springApi.interceptors.request.use(attachBearerToken);
fastApi.interceptors.request.use(attachBearerToken);
