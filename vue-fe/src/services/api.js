import axios from 'axios';

const springBaseURL = import.meta.env.VITE_SPRING_BASE_URL || 'http://localhost:8080';

export const springApi = axios.create({
  baseURL: springBaseURL,
  timeout: 5000,
});
