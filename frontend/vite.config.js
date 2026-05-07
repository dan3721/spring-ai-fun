import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const target = process.env.VITE_PROXY_TARGET || "http://backend:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    host: "0.0.0.0",
    port: 5173,
    watch: {
      usePolling: true
    },
    proxy: {
      "/api": {
        target,
        changeOrigin: true
      }
    }
  }
});
