/// <reference types="vite/client" />

interface ImportMetaEnv {
  // API
  readonly VITE_API_BASE?: string
  readonly VITE_API_BASE_URL?: string
  readonly VITE_APP_URL?: string
  readonly VITE_API_VERSION?: string
  readonly VITE_APP_VERSION?: string
  readonly VITE_ERROR_VERSION?: string

  // Auth
  readonly VITE_AUTH_URL?: string
  readonly VITE_AUTH_CLIENT_ID?: string
  // En dev on peut pointer vers un target complet (avec port) pour la proxy Vite
  readonly VITE_AUTH_TARGET?: string

  // Feature Flags
  // 'memory' pour flags locaux, 'unleash' pour utilisation d'Unleash
  readonly VITE_FEATURE_KIND?: 'memory' | 'unleash'
  readonly VITE_FEATURE_URL: string
  readonly VITE_FEATURE_CLIENT_KEY: string
  readonly VITE_FEATURE_APP_NAME: string
  readonly VITE_FEATURE_ENVIRONMENT: string
  readonly VITE_FEATURE_REFRESH: string
  readonly VITE_FEATURE_DEFAULT_VALUE: string

  // Analytics
  readonly VITE_ANALYTICS_PROVIDER: string
  readonly VITE_GA_MEASUREMENT_ID: string
  readonly VITE_ANALYTICS_AUTO_TRACK: string

  // Other
  readonly VITE_TENANT: string
  readonly VITE_LANG: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
