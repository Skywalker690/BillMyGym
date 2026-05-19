import axios from "axios";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() || "/api";
const AUTH_TOKEN_KEY = "billmygym_token";
const AUTH_USER_KEY = "billmygym_user";
const LEGACY_AUTH_TOKEN_KEY = "gymflow_token";
const LEGACY_AUTH_USER_KEY = "gymflow_user";

const api = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000,
  headers: {
    Accept: "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY) || localStorage.getItem(LEGACY_AUTH_TOKEN_KEY);
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
    const status = error.response?.status;
    const errorCode = error.response?.data?.errorCode;
    const message = String(error.response?.data?.message || "");
    const subscriptionInactive =
      status === 402 ||
      (status === 403 && (errorCode === "SUBSCRIPTION_INACTIVE" || message.toLowerCase().includes("subscription")));

    if (subscriptionInactive && !window.location.pathname.startsWith("/app/subscription/recovery")) {
      window.location.href = "/app/subscription/recovery";
    }
    if (status === 401) {
      localStorage.removeItem(AUTH_TOKEN_KEY);
      localStorage.removeItem(AUTH_USER_KEY);
      localStorage.removeItem(LEGACY_AUTH_TOKEN_KEY);
      localStorage.removeItem(LEGACY_AUTH_USER_KEY);
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export default api;
