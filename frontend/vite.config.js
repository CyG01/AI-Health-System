import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import viteCompression from 'vite-plugin-compression'

// 预构建依赖列表，稳定依赖单独缓存
const optimizeDepsInclude = [
  'vue',
  'vue-router',
  'pinia',
  'axios',
  'element-plus',
  '@element-plus/icons-vue'
]

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
      imports: ['vue', 'vue-router', 'pinia'],
      dts: true,
      // 自动导入缓存，避免重复解析
      cache: true
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'sass' })],
      dts: true,
      // 仅解析 src/components 下的组件
      dirs: ['src/components'],
      deep: false
    }),
    viteCompression({
      algorithm: 'gzip',
      ext: '.gz',
      threshold: 10240,
      deleteOriginFile: false,
      // 过滤较大文件
      filter: (file) => file.endsWith('.js') || file.endsWith('.css')
    }),
    viteCompression({
      algorithm: 'brotliCompress',
      ext: '.br',
      threshold: 10240,
      deleteOriginFile: false,
      filter: (file) => file.endsWith('.js') || file.endsWith('.css')
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
    // 减少解析次数
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
  },
  server: {
    port: 5173,
    // 预热提高首次响应速度
    warmup: {
      clientFiles: ['./index.html', './src/**/*.vue']
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 配置连接超时减少等待
        timeout: 30000,
        proxyTimeout: 30000
      },
      '/avatars': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern'
      }
    },
    // css 代码分割
    devSourcemap: false
  },
  // 依赖预构建优化
  optimizeDeps: {
    include: optimizeDepsInclude,
    // 强制预构建
    force: false
  },
  build: {
    target: 'es2020',
    minify: 'terser',
    sourcemap: false,
    // 清空输出目录
    emptyOutDir: true,
    // 提高构建速度
    write: true,
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
        pure_funcs: ['console.log', 'console.info', 'console.debug']
      },
      format: {
        comments: false
      }
    },
    rollupOptions: {
      output: {
        chunkFileNames: 'assets/js/[name]-[hash].js',
        entryFileNames: 'assets/js/[name]-[hash].js',
        assetFileNames: 'assets/[ext]/[name]-[hash].[ext]',
        // 更精细的代码分割
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-element-plus': ['element-plus', '@element-plus/icons-vue'],
          'vendor-echarts': ['echarts'],
          'vendor-axios': ['axios']
        }
      }
    },
    chunkSizeWarningLimit: 1500,
    // 不报告压缩大小加速构建
    reportCompressedSize: false
  }
})