export const TCH_ASSET_BASE_PATH = '/assets' as const;

export const TCH_BRAND_ASSETS = {
  appIcon: tchAssetPath('brand/tchalanet-app-icon.svg'),
  logo: tchAssetPath('brand/tchalanet-logo.svg'),
  logoInverse: tchAssetPath('brand/tchalanet-logo-inverse.svg'),
  logoPrint: tchAssetPath('brand/tchalanet-logo-print.svg'),
  points: tchAssetPath('brand/tchalanet-points.svg'),
} as const;

export const TCH_FONT_ASSETS = {
  materialSymbolsOutlined: tchAssetPath('fonts/material-symbols-outlined.woff2'),
} as const;

export const TCH_I18N_ASSETS = {
  basePath: tchAssetPath('i18n'),
  bundlePath: (locale: string, bundle: string) => tchAssetPath(`i18n/${locale}/${bundle}.json`),
} as const;

export const TCH_PUBLIC_ASSETS = {
  checkTicketPreview: tchAssetPath('public/ticket-verification-preview.svg'),
  pagesPath: tchAssetPath('public/pages'),
  resultsHeroBalls: tchAssetPath('public/results-hero-balls.png'),
  rulesTchalaPreview: tchAssetPath('public/rules-tchala-preview.png'),
} as const;

export const TCH_SOCIAL_ASSETS = {
  facebook: tchAssetPath('svg/socials/facebook.svg'),
  instagram: tchAssetPath('svg/socials/instagram.svg'),
  linkedin: tchAssetPath('svg/socials/linkedin.svg'),
  x: tchAssetPath('svg/socials/x.svg'),
  youtube: tchAssetPath('svg/socials/youtube.svg'),
} as const;

export const TCH_LOCALE_FLAG_ASSETS = {
  en: tchAssetPath('svg/i18n/en.svg'),
  fr: tchAssetPath('svg/i18n/fr.svg'),
  ht: tchAssetPath('svg/i18n/ht.svg'),
} as const;

export const TCH_FALLBACK_ASSETS = {
  publicBootstrapFr: tchAssetPath('fallback/public-bootstrap-fallback.fr.json'),
} as const;

export const TCH_CONFIG_ASSETS = {
  pageDefault: tchAssetPath('config/page-default.json'),
  pagePrivateFallback: tchAssetPath('config/page-private-fallback.json'),
  test: tchAssetPath('config/test.json'),
} as const;

export const TCH_LOTTERY_ASSET_BASE_PATH = tchAssetPath('images/lottery');
export const TCH_LOTTERY_LOGO_ASSET_BASE_PATH = tchAssetPath('images/logo');

export function tchAssetPath(path: string): string {
  const cleanPath = path.replace(/^\/+/, '');

  if (!cleanPath) {
    return TCH_ASSET_BASE_PATH;
  }

  return `${TCH_ASSET_BASE_PATH}/${cleanPath}`;
}
