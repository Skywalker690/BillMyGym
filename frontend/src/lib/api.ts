import axios from "axios";
import type {
  AxiosError,
  AxiosInstance,
  AxiosResponse,
  InternalAxiosRequestConfig,
  RawAxiosRequestHeaders,
} from "axios";
import { getApiRequestTimeoutMs, getBrowserApiBaseUrl } from "../../app.config";

type ApiEnvelope<T = unknown> = {
  success: boolean;
  message?: string | null;
  data?: T;
  timestamp?: string;
};

type ApiErrorBody = {
  success?: false;
  message?: string;
  errorCode?: string;
  errors?: Array<{ field?: string; message?: string }>;
  timestamp?: string;
};

type ApiErrorMeta = {
  status?: number;
  errorCode?: string;
  message: string;
  fieldErrors: ApiErrorBody["errors"];
};

export type ApiError = AxiosError<ApiErrorBody> & {
  api?: ApiErrorMeta;
};

declare module "axios" {
  interface AxiosResponse<T = any, D = any, H = {}> {
    apiMessage?: string | null;
  }
}

export const AUTH_TOKEN_KEY = "billmygym_token";
export const AUTH_USER_KEY = "billmygym_user";
export const LEGACY_AUTH_TOKEN_KEY = "gymflow_token";
export const LEGACY_AUTH_USER_KEY = "gymflow_user";

const BROWSER_REDIRECT_DELAY_MS = 0;

let pendingRedirectTo: string | null = null;

const api: AxiosInstance = axios.create({
  baseURL: resolveApiBaseUrl(),
  timeout: getApiRequestTimeoutMs(),
  headers: {
    Accept: "application/json",
  },
});

api.interceptors.request.use((config) => {
  const token = readAuthToken();

  if (token) {
    config.headers = {
      ...(config.headers ?? {}),
      Authorization: `Bearer ${token}`,
    } as InternalAxiosRequestConfig["headers"];
  }

  return config;
});

api.interceptors.response.use(
  (response) => normalizeApiResponse(response),
  (error: AxiosError<ApiErrorBody>) => {
    const normalizedError = normalizeApiError(error);
    handleAuthRedirect(normalizedError);
    return Promise.reject(normalizedError);
  },
);

export function readAuthToken() {
  return readStorage(AUTH_TOKEN_KEY) || readStorage(LEGACY_AUTH_TOKEN_KEY);
}

export function persistAuthToken(token: string) {
  writeStorage(AUTH_TOKEN_KEY, token);
  removeStorage(LEGACY_AUTH_TOKEN_KEY);
}

export function readStoredUser() {
  return readStorage(AUTH_USER_KEY) || readStorage(LEGACY_AUTH_USER_KEY);
}

export function persistStoredUser(serializedUser: string) {
  writeStorage(AUTH_USER_KEY, serializedUser);
  removeStorage(LEGACY_AUTH_USER_KEY);
}

export function clearStoredAuth() {
  removeStorage(AUTH_TOKEN_KEY);
  removeStorage(LEGACY_AUTH_TOKEN_KEY);
  clearStoredUser();
}

export function clearStoredUser() {
  removeStorage(AUTH_USER_KEY);
  removeStorage(LEGACY_AUTH_USER_KEY);
}

export function getApiErrorMessage(error: unknown, fallback = "Something went wrong. Please try again.") {
  if (axios.isAxiosError(error)) {
    const apiError = error as ApiError;
    return apiError.api?.message || apiError.response?.data?.message || apiError.message || fallback;
  }

  if (error instanceof Error) {
    return error.message || fallback;
  }

  return fallback;
}

function resolveApiBaseUrl() {
  return getBrowserApiBaseUrl(import.meta.env.MODE, import.meta.env.DEV, {
    hostname: typeof window !== "undefined" ? window.location.hostname : undefined,
    apiOriginOverride: import.meta.env.VITE_API_ORIGIN,
    apiTargetOverride: import.meta.env.VITE_API_TARGET,
  });
}

function normalizeApiResponse<T>(response: AxiosResponse<T | ApiEnvelope<T>>) {
  if (shouldSkipEnvelopeUnwrap(response)) {
    return response as AxiosResponse<T>;
  }

  const body = response.data;
  if (isApiEnvelope<T>(body)) {
    response.apiMessage = body.message ?? null;
    response.data = body.data as T;
  }

  return response as AxiosResponse<T>;
}

function normalizeApiError(error: AxiosError<ApiErrorBody>): ApiError {
  const apiError = error as ApiError;
  const body = apiError.response?.data;
  const message = body?.message || apiError.message || "Request failed";

  apiError.api = {
    status: apiError.response?.status,
    errorCode: body?.errorCode,
    message,
    fieldErrors: Array.isArray(body?.errors) ? body.errors : [],
  };

  return apiError;
}

function handleAuthRedirect(error: ApiError) {
  if (!isBrowser()) {
    return;
  }

  const status = error.api?.status;
  const errorCode = error.api?.errorCode;
  const message = error.api?.message.toLowerCase() ?? "";

  if (status === 401) {
    clearStoredAuth();
    scheduleRedirect("/login");
    return;
  }

  if (status === 403 && errorCode === "LEGAL_CONSENT_REQUIRED") {
    scheduleRedirect("/consent", (path) => path !== "/consent");
    return;
  }

  const subscriptionInactive =
    status === 402 ||
    (status === 403 && (errorCode === "SUBSCRIPTION_INACTIVE" || message.includes("subscription")));

  if (subscriptionInactive) {
    scheduleRedirect("/app/subscription/recovery", (path) => !path.startsWith("/app/subscription/recovery"));
  }
}

function scheduleRedirect(targetPath: string, shouldRedirect: (currentPath: string) => boolean = (path) => path !== targetPath) {
  if (!isBrowser() || pendingRedirectTo) {
    return;
  }

  const currentPath = window.location.pathname;
  if (!shouldRedirect(currentPath)) {
    return;
  }

  pendingRedirectTo = targetPath;
  window.setTimeout(() => {
    window.location.assign(targetPath);
  }, BROWSER_REDIRECT_DELAY_MS);
}

function shouldSkipEnvelopeUnwrap(response: AxiosResponse) {
  return response.config.responseType === "blob" || response.config.responseType === "arraybuffer";
}

function isApiEnvelope<T>(value: unknown): value is ApiEnvelope<T> {
  if (!isPlainObject(value)) {
    return false;
  }

  return typeof value.success === "boolean" && "data" in value;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function readStorage(key: string) {
  if (!isBrowser()) {
    return null;
  }

  try {
    return window.localStorage.getItem(key);
  } catch {
    return null;
  }
}

function writeStorage(key: string, value: string) {
  if (!isBrowser()) {
    return;
  }

  try {
    window.localStorage.setItem(key, value);
  } catch {
    // Authentication should not crash the app when storage is unavailable.
  }
}

function removeStorage(key: string) {
  if (!isBrowser()) {
    return;
  }

  try {
    window.localStorage.removeItem(key);
  } catch {
    // Best-effort cleanup for private browsing and restricted storage contexts.
  }
}

function isBrowser() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

export function withIdempotencyKey(headers: RawAxiosRequestHeaders = {}) {
  return {
    ...headers,
    "X-Idempotency-Key": crypto.randomUUID(),
  };
}

export default api;
