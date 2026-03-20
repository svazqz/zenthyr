import { defineConfig } from 'vite';
import angular from '@analogjs/vite-plugin-angular';

export default defineConfig({
  plugins: [angular()],
  root: 'src/app',
  server: {
    port: 5173
  }
});
