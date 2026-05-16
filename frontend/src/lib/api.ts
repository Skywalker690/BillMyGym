import axios from "axios";

const apiBaseUrl = "http://localhost:8000/api";

const api = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000,
  headers: {
    Accept: "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("gymflow_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && typeof body === "object" && "success" in body && "data" in body) {
      (response as any).apiMessage = body.message;
      response.data = body.data;
    }
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("gymflow_token");
      localStorage.removeItem("gymflow_user");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export default api;
