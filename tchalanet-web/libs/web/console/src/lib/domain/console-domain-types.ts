export const CONSOLE_TENANT_STATUSES = [
  'DRAFT',
  'ACTIVE',
  'SUSPENDED',
  'REJECTED',
  'ARCHIVED',
] as const;

export type ConsoleTenantStatus = typeof CONSOLE_TENANT_STATUSES[number];

export const CONSOLE_TENANT_TYPES = [
  'BORLETTE',
  'RESEAU',
  'AMBULANT',
] as const;

export type ConsoleTenantType = typeof CONSOLE_TENANT_TYPES[number];

export const CONSOLE_TENANT_PROVISIONING_PROFILES = [
  'MINIMAL',
  'DEFAULT_HAITI_LOTTERY',
  'DEMO',
] as const;

export type ConsoleTenantProvisioningProfile = typeof CONSOLE_TENANT_PROVISIONING_PROFILES[number];

export const CONSOLE_READINESS_STATUSES = [
  'READY',
  'PARTIAL',
  'TODO',
  'INCOMPLETE',
  'BLOCKED',
  'MISSING',
  'UNKNOWN',
] as const;

export type ConsoleReadinessStatus = typeof CONSOLE_READINESS_STATUSES[number];

export const CONSOLE_SUBSCRIPTION_STATUSES = [
  'ACTIVE',
  'TRIAL',
  'SUSPENDED',
  'CANCELED',
  'EXPIRED',
] as const;

export type ConsoleSubscriptionStatus = typeof CONSOLE_SUBSCRIPTION_STATUSES[number];

export const CONSOLE_SELLER_TERMINAL_STATUSES = [
  'PENDING',
  'ACTIVE',
  'INACTIVE',
  'BLOCKED',
  'DISABLED',
] as const;

export type ConsoleSellerTerminalStatus = typeof CONSOLE_SELLER_TERMINAL_STATUSES[number];

export const CONSOLE_DRAW_STATUSES = [
  'SCHEDULED',
  'OPEN',
  'LOCKED',
  'CLOSED',
  'PENDING_RESULTS',
  'RESULTED',
  'RESULTS_APPLIED',
  'SETTLED',
  'CANCELED',
  'CANCELLED',
  'ARCHIVED',
] as const;

export type ConsoleDrawStatus = typeof CONSOLE_DRAW_STATUSES[number];

export const CONSOLE_DRAW_SALES_STATUSES = [
  'UPCOMING',
  'OPEN',
  'LOCKED',
  'CLOSED',
  'CANCELLED',
] as const;

export type ConsoleDrawSalesStatus = typeof CONSOLE_DRAW_SALES_STATUSES[number];

export const CONSOLE_DRAW_RESULT_STATUSES = [
  'NOT_DUE',
  'EXPECTED',
  'MISSING',
  'PROVISIONAL',
  'CONFIRMED',
  'OVERRIDDEN',
  'ERROR',
  'SOURCE_ERROR',
  'PENDING',
  'APPLIED',
  'CORRECTED',
  'VOIDED',
  'MANUAL',
  'REJECTED',
] as const;

export type ConsoleDrawResultStatus = typeof CONSOLE_DRAW_RESULT_STATUSES[number];

export const CONSOLE_DRAW_RESULT_QUALITIES = [
  'COMPLETE',
  'SUSPECT',
  'INVALID',
  'OFFICIAL',
  'MANUAL',
  'ESTIMATED',
  'UNKNOWN',
] as const;

export type ConsoleDrawResultQuality = typeof CONSOLE_DRAW_RESULT_QUALITIES[number];

export const CONSOLE_DRAW_RESULT_SOURCES = [
  'SYSTEM',
  'AUTO',
  'EXTERNAL',
  'US_LOTTERY',
  'NY_OPEN_DATA',
  'FL_APIM',
  'MANUAL',
  'ADMIN_OVERRIDE',
] as const;

export type ConsoleDrawResultSource = typeof CONSOLE_DRAW_RESULT_SOURCES[number];

export const CONSOLE_DRAW_RESULT_MODES = [
  'AUTO',
  'MANUAL',
] as const;

export type ConsoleDrawResultMode = typeof CONSOLE_DRAW_RESULT_MODES[number];

export const CONSOLE_DRAW_PUBLICATION_STATUSES = [
  'NOT_PUBLISHED',
  'PUBLISHED',
  'PROVISIONAL',
] as const;

export type ConsoleDrawPublicationStatus = typeof CONSOLE_DRAW_PUBLICATION_STATUSES[number];

export type ConsoleDrawStatusFilter =
  | 'all'
  | 'PAST'
  | ConsoleDrawStatus
  | ConsoleDrawResultStatus;

export type ConsoleDrawResultSaveMode = 'provisional' | 'confirmed';

export type ConsoleDrawLifecycleDisplayAction =
  | 'open'
  | 'close'
  | 'cancel'
  | 'lock'
  | 'unlock'
  | 'settle'
  | 'archive'
  | 'reschedule'
  | 'correct';

export const CONSOLE_DRAW_RESULT_ACQUISITION_MODES = [
  'AUTO',
  'MANUAL',
  'UNCONFIGURED',
] as const;

export type ConsoleDrawResultAcquisitionMode = typeof CONSOLE_DRAW_RESULT_ACQUISITION_MODES[number];

export const CONSOLE_DRAW_RESULT_SOURCE_STATUSES = [
  'OK',
  'PENDING',
  'ERROR',
  'DISABLED',
  'UNCONFIGURED',
] as const;

export type ConsoleDrawResultSourceStatus = typeof CONSOLE_DRAW_RESULT_SOURCE_STATUSES[number];

export const CONSOLE_DRAW_RESULT_SOURCE_KINDS = [
  'API',
  'RSS',
  'BATCH',
  'MANUAL',
] as const;

export type ConsoleDrawResultSourceKind = typeof CONSOLE_DRAW_RESULT_SOURCE_KINDS[number];

export const CONSOLE_TENANT_GAME_STATUSES = [
  'ACTIVE',
  'INACTIVE',
  'NEEDS_CONFIG',
  'UNAVAILABLE',
] as const;

export type ConsoleTenantGameStatus = typeof CONSOLE_TENANT_GAME_STATUSES[number];

export const CONSOLE_CATALOG_STATUSES = [
  'AVAILABLE',
  'DISABLED',
  'COMING_SOON',
] as const;

export type ConsoleCatalogStatus = typeof CONSOLE_CATALOG_STATUSES[number];

export const CONSOLE_LIMIT_TARGET_TYPES = [
  'TENANT',
  'AGENT',
  'OUTLET',
  'TERMINAL',
  'SELLER_TERMINAL',
  'GAME',
  'ZONE',
  'RANGE',
  'DRAW_CHANNEL',
] as const;

export type ConsoleLimitTargetType = typeof CONSOLE_LIMIT_TARGET_TYPES[number];

export const CONSOLE_LIMIT_BREACH_OUTCOMES = [
  'ALLOW',
  'WARN',
  'REQUIRE_APPROVAL',
  'BLOCK',
] as const;

export type ConsoleLimitBreachOutcome = typeof CONSOLE_LIMIT_BREACH_OUTCOMES[number];

export const CONSOLE_LIMIT_RULE_KEYS = [
  'MAX_STAKE_PER_LINE',
  'MAX_LINES_PER_TICKET',
  'MAX_STAKE_PER_TICKET',
  'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW',
  'MAX_STAKE_PER_BET_TYPE_PER_TICKET',
  'MAX_STAKE_PER_SELECTION_PER_TICKET',
  'MAX_SALES_COUNT_PER_SELECTION_PER_DRAW',
  'MAX_SALES_COUNT_PER_TICKET',
  'BLOCK_SELECTION_PER_DRAW',
  'BLOCK_BET_TYPE',
  'MAX_TICKET_COUNT_PER_AGENT_PER_WINDOW',
  'MAX_STAKE_PER_AGENT_PER_DRAW',
  'MAX_STAKE_PER_OUTLET_PER_DRAW',
] as const;

export type ConsoleLimitRuleKey = typeof CONSOLE_LIMIT_RULE_KEYS[number];

export const CONSOLE_CONTACT_REQUEST_INTENTS = [
  'REQUEST_DEMO',
  'BECOME_OPERATOR',
  'SUPPORT',
  'PARTNERSHIP',
  'OTHER',
] as const;

export type ConsoleContactRequestIntent = typeof CONSOLE_CONTACT_REQUEST_INTENTS[number];

export const CONSOLE_CONTACT_REQUEST_STATUSES = [
  'RECEIVED',
  'CONTACTED',
  'QUALIFIED',
  'CLOSED',
  'SPAM',
] as const;

export type ConsoleContactRequestStatus = typeof CONSOLE_CONTACT_REQUEST_STATUSES[number];

export const CONSOLE_NOTIFICATION_STATUSES = [
  'PUBLISHED',
  'EXPIRED',
  'CANCELLED',
  'PURGED',
] as const;

export type ConsoleNotificationStatus = typeof CONSOLE_NOTIFICATION_STATUSES[number];

export const CONSOLE_NOTIFICATION_AUDIENCE_TYPES = [
  'SPECIFIC_ACTORS',
  'PLATFORM_ADMINS',
  'ALL_APP_USERS',
  'TENANT_ADMINS',
  'TENANT_APP_USERS',
  'TENANT_SELLER_TERMINALS',
] as const;

export type ConsoleNotificationAudienceType = typeof CONSOLE_NOTIFICATION_AUDIENCE_TYPES[number];

export const CONSOLE_NOTIFICATION_SEVERITIES = [
  'INFO',
  'WARNING',
  'ERROR',
  'CRITICAL',
] as const;

export type ConsoleNotificationSeverity = typeof CONSOLE_NOTIFICATION_SEVERITIES[number];

export const CONSOLE_NOTIFICATION_KINDS = [
  'INFO',
  'WARNING',
  'ACTION_REQUIRED',
  'SYSTEM_ERROR',
] as const;

export type ConsoleNotificationKind = typeof CONSOLE_NOTIFICATION_KINDS[number];

export const CONSOLE_NOTIFICATION_CATEGORIES = [
  'PAGE_MODEL',
  'TENANT_CONFIG',
  'USER',
  'OUTLET',
  'TERMINAL',
  'SESSION',
  'SALES',
  'DRAW',
  'RESULT',
  'PAYOUT',
  'BATCH',
  'SYSTEM',
  'SECURITY',
] as const;

export type ConsoleNotificationCategory = typeof CONSOLE_NOTIFICATION_CATEGORIES[number];

export const CONSOLE_NOTIFICATION_CHANNELS = [
  'EMAIL',
  'SLACK',
  'SMS',
  'WHATSAPP',
  'PUSH',
  'WEB',
  'IN_APP',
] as const;

export type ConsoleNotificationChannel = typeof CONSOLE_NOTIFICATION_CHANNELS[number];

export const CONSOLE_NOTIFICATION_ACTOR_TYPES = [
  'APP_USER',
  'SELLER_TERMINAL',
] as const;

export type ConsoleNotificationActorType = typeof CONSOLE_NOTIFICATION_ACTOR_TYPES[number];

export const CONSOLE_PUBLIC_CONTENT_SOURCE_TYPES = [
  'INTERNAL',
  'EXTERNAL_RSS',
] as const;

export type ConsolePublicContentSourceType = typeof CONSOLE_PUBLIC_CONTENT_SOURCE_TYPES[number];

export const CONSOLE_PUBLIC_CONTENT_STATUSES = [
  'DRAFT',
  'PUBLISHED',
  'ARCHIVED',
] as const;

export type ConsolePublicContentStatus = typeof CONSOLE_PUBLIC_CONTENT_STATUSES[number];

export const CONSOLE_PUBLIC_CONTENT_SURFACES = [
  'PUBLIC_HOME',
  'TENANT_ADMIN_DASHBOARD',
  'PLATFORM_ADMIN_DASHBOARD',
  'POS_DASHBOARD',
] as const;

export type ConsolePublicContentSurface = typeof CONSOLE_PUBLIC_CONTENT_SURFACES[number];

export const CONSOLE_COMMUNICATION_CHANNELS = [
  'SLACK',
  'SLACK_INTERNAL',
  'SLACK_TENANT_WEBHOOK',
  'EMAIL',
  'SMS',
  'WHATSAPP',
  'PUSH',
] as const;

export type ConsoleCommunicationChannel = typeof CONSOLE_COMMUNICATION_CHANNELS[number];

export const CONSOLE_DELIVERY_STATUSES = [
  'PENDING',
  'DISPATCHING',
  'SENT',
  'FAILED',
  'SKIPPED',
  'CANCELLED',
] as const;

export type ConsoleDeliveryStatus = typeof CONSOLE_DELIVERY_STATUSES[number];

export type ConsolePageModelStatus = ConsolePublicContentStatus;
