export type ThemeKind = 'material' | 'custom';
export type ThemeMode = 'light' | 'dark' | 'system';

export interface TchTheme {
  /** ID lisible et stable, ex: "violet", "carmine", "rose" */
  id: string;

  /** Nom marketing / affichage dans UI admin */
  label: string;

  /** Bloc CSS complet .tch-theme[data-preset="id"] {...} + .tch-theme.dark[data-preset="id"] {...} */
  css: string;
}

/** Overrides côté tenant : juste des vars CSS, pas tout le thème */
export interface TenantOverrides {
  /** map varName -> value
   *  --dark:--xxx = valeur spécifique dark
   *  ex:
   *   {
   *     "--tch-header-bg": "#1a1a1a",
   *     "--dark:--tch-header-bg": "#000",
   *     "--tch-font-family": "\"Inter\", system-ui, sans-serif"
   *   }
   */
  vars: Record<string, string>;

  /** URL pour charger la police custom si besoin */
  fontHref?: string;
}

export interface TenantThemePayload {
  presetId: string;
  mode?: 'light' | 'dark' | 'system';
  density?: 0 | -1 | -2;
  overrides?: TenantOverrides;
}
