import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";
import path from "path";
import { defineConfig, loadEnv } from "vite";
import { getActiveApiOrigin, getApiPathPrefix } from "./app.config";

const rootDir = fileURLToPath(new URL(".", import.meta.url));

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".", "");

  return {
    plugins: [react(), tailwindcss()],
    define: {
      "process.env.GEMINI_API_KEY": JSON.stringify(env.GEMINI_API_KEY),
    },
    resolve: {
      alias: {
        "@": path.resolve(rootDir, "./src"),
      },
    },
    server: {
      hmr: process.env.DISABLE_HMR !== "true",
      watch: process.env.DISABLE_HMR === "true" ? null : {},
      proxy: {
        [getApiPathPrefix()]: {
          target: getActiveApiOrigin(mode, {
            apiOriginOverride: env.VITE_API_ORIGIN,
            apiTargetOverride: env.VITE_API_TARGET,
          }),
          changeOrigin: true,
        },
      },
    },
  };
});
