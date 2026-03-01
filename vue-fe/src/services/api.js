import axios from 'axios';

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
