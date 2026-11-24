import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// In development, every request to /api is forwarded to the Spring Boot backend.
// The browser then sees one address (localhost:5173), so cookies and CSRF just work.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": { target: "http://localhost:8081", changeOrigin: false },
    },
  },
});
