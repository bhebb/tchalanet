import { TranslateService } from '@ngx-translate/core';

import { AdminStatusTone } from '@tch/ui/console';

export const DOMAIN_LABEL_KEYS: Record<string, string> = {
  tenant_identity: 'platform.tenantProvisioning.domain.tenantIdentity',
  pagemodels: 'platform.tenantProvisioning.domain.pagemodels',
  theme: 'platform.tenantProvisioning.domain.theme',
  settings: 'platform.tenantProvisioning.domain.settings',
  i18n: 'platform.tenantProvisioning.domain.i18n',
  games: 'platform.tenantProvisioning.domain.games',
  pricing: 'platform.tenantProvisioning.domain.pricing',
  draw_channels: 'platform.tenantProvisioning.domain.drawChannels',
  promotions_templates: 'platform.tenantProvisioning.domain.promotionsTemplates',
  limits_templates: 'platform.tenantProvisioning.domain.limitsTemplates',
  demo_users: 'platform.tenantProvisioning.domain.demoUsers',
  demo_seller_terminals: 'platform.tenantProvisioning.domain.demoSellerTerminals',
  subscription: 'platform.tenantProvisioning.domain.subscription',
};

export const STATUS_LABEL_KEYS: Record<string, string> = {
  CREATED: 'platform.tenantProvisioning.status.created',
  SEEDED_VIA_LISTENER: 'platform.tenantProvisioning.status.seeded',
  DEFAULT: 'platform.tenantProvisioning.status.default',
  DEFAULT_LOTTERY: 'platform.tenantProvisioning.status.defaultLottery',
  DEFAULT_HAITI: 'platform.tenantProvisioning.status.defaultHaiti',
  TEMPLATES_AVAILABLE: 'platform.tenantProvisioning.status.templatesAvailable',
  PLAN_APPLIED: 'platform.tenantProvisioning.status.planApplied',
  NONE: 'platform.tenantProvisioning.status.none',
};

export const STEP_LABEL_KEYS: Record<string, string> = {
  CREATE_INITIAL_ADMIN: 'platform.tenantProvisioning.step.createInitialAdmin',
  CONFIGURE_GAMES: 'platform.tenantProvisioning.step.configureGames',
  CONFIGURE_DRAW_CHANNELS: 'platform.tenantProvisioning.step.configureDrawChannels',
  CREATE_SELLER_TERMINAL: 'platform.tenantProvisioning.step.createSellerTerminal',
  CONFIGURE_SELLER_RULES: 'platform.tenantProvisioning.step.configureSellerRules',
  CONFIGURE_LIMITS: 'platform.tenantProvisioning.step.configureLimits',
  CONFIGURE_ODDS: 'platform.tenantProvisioning.step.configureOdds',
  VERIFY_DEMO_SETUP: 'platform.tenantProvisioning.step.verifyDemoSetup',
};

export const STEP_ICONS: Record<string, string> = {
  CREATE_INITIAL_ADMIN: 'admin_panel_settings',
  CONFIGURE_GAMES: 'casino',
  CONFIGURE_DRAW_CHANNELS: 'schedule',
  CREATE_SELLER_TERMINAL: 'point_of_sale',
  CONFIGURE_SELLER_RULES: 'tune',
  CONFIGURE_LIMITS: 'speed',
  CONFIGURE_ODDS: 'percent',
  VERIFY_DEMO_SETUP: 'checklist',
};

const WARNING_LABEL_KEYS: Record<string, string> = {
  INITIAL_ADMIN_EMAIL_MISSING: 'platform.tenantProvisioning.warning.initialAdminMissing',
  EXISTING_USER_ATTACHED: 'platform.tenantProvisioning.warning.existingUserAttached',
  TEMPORARY_CREDENTIAL_NOT_RETURNED: 'platform.tenantProvisioning.warning.temporaryCredentialNotReturned',
  TEMPORARY_PASSWORD_ISSUED: 'platform.tenantProvisioning.warning.temporaryPasswordIssued',
  NO_PLAN_SELECTED: 'platform.tenantProvisioning.warning.noPlanSelected',
};

export function domainTone(status: string): AdminStatusTone {
  const normalized = status?.toUpperCase();
  if (['OK', 'DONE', 'READY', 'SUCCESS', 'ACTIVE'].includes(normalized)) {
    return 'success';
  }
  if (['WARNING', 'PARTIAL', 'INCOMPLETE'].includes(normalized)) {
    return 'warning';
  }
  if (['ERROR', 'FAILED', 'MISSING', 'BLOCKED'].includes(normalized)) {
    return 'danger';
  }
  if (['PENDING', 'EXPECTED', 'PREVU', 'PRÉVU'].includes(normalized)) {
    return 'info';
  }
  return 'neutral';
}

export function translatedLabel(translate: TranslateService, keyOrValue: string): string {
  return keyOrValue.startsWith('platform.')
    ? translate.instant(keyOrValue)
    : keyOrValue;
}

export function warningLabel(translate: TranslateService, code: string): string {
  const key = WARNING_LABEL_KEYS[code];
  return key
    ? translate.instant(key)
    : translate.instant('platform.tenantProvisioning.warning.fallback');
}
