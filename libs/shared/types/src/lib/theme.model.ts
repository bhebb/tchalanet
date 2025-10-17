export type ThemeKind = 'material' | 'custom';
export type ThemeMode = 'light' | 'dark' | 'system';

export interface TchPalette {
  primary: string; // #134D9F
  onPrimary: string; // #fff
  surface: string; // #fff
  onSurface: string; // #111
  surfaceContainer?: string;

  accent?: string; // #D84C51
  tertiary?: string; // fallback = accent
  outline?: string; // rgba(0,0,0,.16)
  shape?: { cornerRadius?: number };
  mode?: ThemeMode; // mode intrinsèque de la variante
}

export interface ThemeTokens {
  headerBg?: string;
  headerFg?: string;
  // étends au besoin : footerBg, chipRadius, etc.
}

export interface TchTheme {
  id: string; // 'tchalanet', 'indigo-pink'
  label: string; // nom affiché
  kind?: ThemeKind; // 'material' | 'custom' (facultatif)
  mode: ThemeMode; // mode de ce preset (light/dark)
  density?: 0 | -1 | -2;

  matClass?: string; // classe Material (ex: 'mat-indigo-pink')
  palette: TchPalette;
  tokens?: ThemeTokens;

  // optionnel: meta
  tenantId?: string;
  version?: string;
  source?: 'base' | 'tenant' | 'system';

  /** Vars prêtes en runtime (complément/override des palettes/tokens) */
  cssVars?: Record<string, string>;
}

export interface BackendThemePayload {
  id: string;            // ex: "tchalanet" ou un id tenant "tenant-42-v3"
  mode?: ThemeMode;
  primaryColor?: string;       // optionnel: override UI
  accentColor?: string;        // optionnel: override UI
  density?: 0|-1|-2;           // optionnel
  // futur-proof:
  tertiaryColor?: string;
  outline?: string;
  tokens?: Record<string,string>;
  cssVars?: Record<string,string>; // overrides bruts de vars CSS si besoin
}
