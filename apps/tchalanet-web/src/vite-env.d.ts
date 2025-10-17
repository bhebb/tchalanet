/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE: string
  readonly VITE_AUTH_URL: string
  readonly VITE_AUTH_CLIENT_ID: string
  readonly VITE_APP_VERSION: string
  readonly VITE_ERROR_VERSION: string
  readonly VITE_API_VERSION: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
