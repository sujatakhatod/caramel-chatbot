import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The dev server runs on 5173, which matches chat.allowed-origin in the backend's application.yml.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },
});
