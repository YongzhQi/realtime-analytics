import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Proxy API calls if you prefer; here we call absolute URLs directly.
// You can add a proxy if needed.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173
  }
})