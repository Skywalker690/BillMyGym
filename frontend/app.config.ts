export type ApiTarget = "local" | "prod";
export type AppMode = "development" | "production" | "test" | string;

type ApiTargetConfig = {
  origin: string;
};

type ApiRuntimeOptions = {
  hostname?: string;
  apiOriginOverride?: string;
  apiTargetOverride?: ApiTarget | string | null;
};

export const APP_CONFIG = {
  api: {
    // Leave null for automatic selection:
    // localhost/127.0.0.1 -> local, deployed domains -> prod.
    targetOverride: null as ApiTarget | null,
    defaultTargetByMode: {
      development: "local",
      production: "prod",
      test: "local",
    } satisfies Record<string, ApiTarget>,
    pathPrefix: "/api",
    requestTimeoutMs: 15_000,
    targets: {
      local: {
        origin: "http://localhost:8080",
      },
      prod: {
        origin: "https://api.billmygym.tech"
      },
    } satisfies Record<ApiTarget, ApiTargetConfig>,
  },
} as const;

export function getActiveApiTarget(mode: AppMode, options: ApiRuntimeOptions = {}) {
  const explicitTarget = normalizeApiTarget(options.apiTargetOverride);
  if (explicitTarget) {
    return explicitTarget;
  }

  if (APP_CONFIG.api.targetOverride) {
    return APP_CONFIG.api.targetOverride;
  }

  if (isLocalHostname(options.hostname)) {
    return "local";
  }

  return APP_CONFIG.api.defaultTargetByMode[mode] ?? "local";
}

export function getApiPathPrefix() {
  return normalizePathPrefix(APP_CONFIG.api.pathPrefix);
}

export function getApiRequestTimeoutMs() {
  return APP_CONFIG.api.requestTimeoutMs;
}

export function getActiveApiOrigin(mode: AppMode, options: ApiRuntimeOptions = {}) {
  if (options.apiOriginOverride?.trim()) {
    return trimTrailingSlash(options.apiOriginOverride);
  }

  const target = APP_CONFIG.api.targets[getActiveApiTarget(mode, options)];
  return trimTrailingSlash(target.origin);
}

export function getBrowserApiBaseUrl(mode: AppMode, isDevServer: boolean, options: ApiRuntimeOptions = {}) {
  const pathPrefix = getApiPathPrefix();

  if (isDevServer && !options.apiOriginOverride?.trim()) {
    return pathPrefix;
  }

  return `${getActiveApiOrigin(mode, options)}${pathPrefix}`;
}

function normalizePathPrefix(pathPrefix: string) {
  const trimmed = pathPrefix.trim();
  if (!trimmed) {
    return "";
  }

  const withLeadingSlash = trimmed.startsWith("/") ? trimmed : `/${trimmed}`;
  return trimTrailingSlash(withLeadingSlash);
}

function trimTrailingSlash(value: string) {
  return value.trim().replace(/\/+$/, "");
}

function normalizeApiTarget(value: ApiTarget | string | null | undefined) {
  return value === "local" || value === "prod" ? value : null;
}

function isLocalHostname(hostname: string | undefined) {
  if (!hostname) {
    return false;
  }

  const normalized = hostname.trim().toLowerCase();
  return normalized === "localhost" || normalized === "127.0.0.1" || normalized === "::1";
}
