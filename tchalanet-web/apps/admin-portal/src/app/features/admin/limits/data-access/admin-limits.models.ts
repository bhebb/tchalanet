export type TargetType =
  | 'TENANT' | 'AGENT' | 'OUTLET' | 'TERMINAL'
  | 'SELLER_TERMINAL' | 'GAME' | 'ZONE' | 'RANGE' | 'DRAW_CHANNEL';

export type BreachOutcome = 'ALLOW' | 'WARN' | 'REQUIRE_APPROVAL' | 'BLOCK';

export type RuleKey =
  | 'MAX_STAKE_PER_LINE'
  | 'MAX_LINES_PER_TICKET'
  | 'MAX_STAKE_PER_TICKET'
  | 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW'
  | 'MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW'
  | 'MAX_STAKE_PER_BET_TYPE_PER_TICKET'
  | 'MAX_STAKE_PER_SELECTION_PER_TICKET'
  | 'MAX_POTENTIAL_PAYOUT_PER_TICKET'
  | 'MAX_POTENTIAL_PAYOUT_PER_LINE'
  | 'MAX_SALES_COUNT_PER_SELECTION_PER_DRAW'
  | 'MAX_SALES_COUNT_PER_TICKET'
  | 'BLOCK_SELECTION_PER_DRAW'
  | 'BLOCK_BET_TYPE'
  | 'MAX_TICKET_COUNT_PER_AGENT_PER_WINDOW'
  | 'MAX_STAKE_PER_AGENT_PER_DRAW'
  | 'MAX_STAKE_PER_OUTLET_PER_DRAW';

export interface LimitRuleSpec {
  ruleKey: RuleKey;
  label: string;
  description: string;
  defaultOutcome: BreachOutcome;
  category: string;
  stateless: boolean;
  paramsTemplate: unknown;
}

export interface LimitAssignmentItem {
  id: { value: string };
  ruleKey: RuleKey;
  enabled: boolean;
  onBreach: BreachOutcome;
  params: unknown;
  startsAt: string | null;
  endsAt: string | null;
}

export interface ListLimitAssignmentsView {
  limitScopeRef: unknown;
  items: LimitAssignmentItem[];
}

export interface UpsertLimitAssignmentRequest {
  ruleKey: RuleKey;
  targetType: TargetType;
  targetId?: string | null;
  enabled: boolean;
  onBreach: BreachOutcome;
  params: unknown;
  startsAt?: string | null;
  endsAt?: string | null;
}

export interface RuleRow {
  spec: LimitRuleSpec;
  assignment: LimitAssignmentItem | null;
}

export interface TenantAdminPoliciesOverviewView {
  summary: LimitOverviewSummary;
  scopeCards: LimitOverviewCard[];
  actionLinks: LimitOverviewActionLink[];
  alerts: LimitOverviewAlert[];
  globalRules: LimitOverviewGlobalRule[];
}

export interface LimitOverviewSummary {
  activeRules: number;
  globalRules: number;
  sellerOverrides: number;
  channelOverrides: number;
  numberRules: number;
  warnings: number;
}

export interface LimitOverviewCard {
  id: string;
  icon: string;
  title: string;
  description: string;
  metric: string;
  status: 'OK' | 'À configurer' | 'À surveiller' | string;
  route: string;
  cta: string;
}

export interface LimitOverviewAlert {
  severity: 'info' | 'warning' | 'error' | string;
  message: string;
}

export interface LimitOverviewActionLink {
  id: string;
  icon: string;
  label: string;
  description: string;
  route: string;
}

export interface LimitOverviewGlobalRule {
  spec: LimitRuleSpec;
  assignment: LimitAssignmentItem | null;
}

// ── Param schema detection ────────────────────────────────────────────────────

export type ParamSchema =
  | 'NONE'
  | 'CENTS'
  | 'COUNT'
  | 'SELECTION'
  | 'BET_TYPE'
  | 'WINDOW_COUNT'
  | 'CENTS_BET_TYPE';

const RULE_PARAM_SCHEMAS: Partial<Record<RuleKey, ParamSchema>> = {
  MAX_STAKE_PER_LINE: 'CENTS',
  MAX_LINES_PER_TICKET: 'COUNT',
  MAX_STAKE_PER_TICKET: 'CENTS',
  MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW: 'CENTS',
  MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW: 'CENTS',
  MAX_STAKE_PER_BET_TYPE_PER_TICKET: 'CENTS_BET_TYPE',
  MAX_STAKE_PER_SELECTION_PER_TICKET: 'CENTS',
  MAX_POTENTIAL_PAYOUT_PER_TICKET: 'CENTS',
  MAX_POTENTIAL_PAYOUT_PER_LINE: 'CENTS',
  MAX_SALES_COUNT_PER_SELECTION_PER_DRAW: 'COUNT',
  MAX_SALES_COUNT_PER_TICKET: 'COUNT',
  BLOCK_SELECTION_PER_DRAW: 'SELECTION',
  BLOCK_BET_TYPE: 'BET_TYPE',
  MAX_TICKET_COUNT_PER_AGENT_PER_WINDOW: 'WINDOW_COUNT',
  MAX_STAKE_PER_AGENT_PER_DRAW: 'CENTS',
  MAX_STAKE_PER_OUTLET_PER_DRAW: 'CENTS',
};

const CATEGORY_LABELS: Record<string, string> = {
  TICKET: 'Ticket',
  PAYOUT_RISK: 'Risque de paiement',
  BLOCKING: 'Blocage',
  EXPOSURE: 'Exposition',
  SELLER: 'Vendeur',
  DRAW: 'Tirage',
};

export function detectParamSchema(spec: LimitRuleSpec): ParamSchema {
  const known = RULE_PARAM_SCHEMAS[spec.ruleKey];
  if (known) return known;
  if (spec.stateless) return 'NONE';
  const t = spec.paramsTemplate as Record<string, unknown> | null;
  if (!t || Object.keys(t).length === 0) return 'NONE';
  const keys = Object.keys(t).map(k => k.toLowerCase());
  const hasCents = keys.some(k => k.includes('cents'));
  const hasCount = keys.some(k => k.includes('count'));
  const hasWindow = keys.some(k => k.includes('window') || k.includes('minutes'));
  const hasBetType = keys.some(k => k.includes('bettype') || k.includes('bet_type'));
  const hasSelection = keys.some(k => k.includes('selection'));
  if (hasCents && hasBetType) return 'CENTS_BET_TYPE';
  if (hasCount && hasWindow) return 'WINDOW_COUNT';
  if (hasCents) return 'CENTS';
  if (hasCount) return 'COUNT';
  if (hasSelection) return 'SELECTION';
  if (hasBetType) return 'BET_TYPE';
  return 'NONE';
}

export function buildParams(
  schema: ParamSchema,
  template: unknown,
  values: {
    valueCentsHtg: number;
    maxCount: number;
    windowMinutes: number;
    betTypeCode: string;
    selectionIds: string[];
  },
): unknown {
  const t = (template as Record<string, unknown>) ?? {};
  const keys = Object.keys(t);
  const centsKey = keys.find(k => k.toLowerCase().includes('cents')) ?? 'valueCents';
  const countKey = keys.find(k => k.toLowerCase().includes('count')) ?? 'maxCount';
  const windowKey = keys.find(k => k.toLowerCase().includes('window') || k.toLowerCase().includes('minutes')) ?? 'windowMinutes';
  const betTypeKey = keys.find(k => k.toLowerCase().includes('bettype') || k.toLowerCase().includes('bet_type')) ?? 'betTypeCode';
  const selectionKey = keys.find(k => k.toLowerCase().includes('selection')) ?? 'selections';

  switch (schema) {
    case 'CENTS':
      return { [centsKey]: Math.round(values.valueCentsHtg * 100) };
    case 'COUNT':
      return { [countKey]: values.maxCount };
    case 'SELECTION':
      return { [selectionKey]: values.selectionIds };
    case 'BET_TYPE':
      return { [betTypeKey]: values.betTypeCode };
    case 'WINDOW_COUNT':
      return { [countKey]: values.maxCount, [windowKey]: values.windowMinutes };
    case 'CENTS_BET_TYPE':
      return { [centsKey]: Math.round(values.valueCentsHtg * 100), [betTypeKey]: values.betTypeCode };
    default:
      return {};
  }
}

export function extractParamValues(
  schema: ParamSchema,
  template: unknown,
  params: unknown,
): {
  valueCentsHtg: number;
  maxCount: number;
  windowMinutes: number;
  betTypeCode: string;
  selectionIds: string[];
} {
  const t = (template as Record<string, unknown>) ?? {};
  const p = (params as Record<string, unknown>) ?? {};
  const tkeys = Object.keys(t);
  const centsKey = tkeys.find(k => k.toLowerCase().includes('cents')) ?? 'valueCents';
  const countKey = tkeys.find(k => k.toLowerCase().includes('count')) ?? 'maxCount';
  const windowKey = tkeys.find(k => k.toLowerCase().includes('window') || k.toLowerCase().includes('minutes')) ?? 'windowMinutes';
  const betTypeKey = tkeys.find(k => k.toLowerCase().includes('bettype') || k.toLowerCase().includes('bet_type')) ?? 'betTypeCode';
  const selectionKey = tkeys.find(k => k.toLowerCase().includes('selection')) ?? 'selections';

  const rawCents = (p[centsKey] ?? t[centsKey] ?? 0) as number;
  const rawSelections = p[selectionKey] ?? t[selectionKey];
  return {
    valueCentsHtg: schema === 'CENTS' || schema === 'CENTS_BET_TYPE' ? rawCents / 100 : 0,
    maxCount: (p[countKey] ?? t[countKey] ?? 0) as number,
    windowMinutes: (p[windowKey] ?? t[windowKey] ?? 0) as number,
    betTypeCode: String(p[betTypeKey] ?? t[betTypeKey] ?? ''),
    selectionIds: Array.isArray(rawSelections) ? rawSelections.map(String) : [],
  };
}

export function formatLimitCategory(category: string): string {
  return CATEGORY_LABELS[category] ?? category
    .split('_')
    .map(p => p.charAt(0).toUpperCase() + p.slice(1).toLowerCase())
    .join(' ');
}

export function formatLimitParams(spec: LimitRuleSpec, params: unknown): string {
  const schema = detectParamSchema(spec);
  if (schema === 'NONE') return 'Aucun paramètre';
  const values = extractParamValues(schema, spec.paramsTemplate, params);
  const parts: string[] = [];

  if (schema === 'CENTS' || schema === 'CENTS_BET_TYPE') {
    parts.push(`${amountLabel(spec.ruleKey)} : ${formatHtg(values.valueCentsHtg)}`);
  }
  if (schema === 'COUNT' || schema === 'WINDOW_COUNT') {
    parts.push(`${countLabel(spec.ruleKey)} : ${formatInteger(values.maxCount)}`);
  }
  if (schema === 'WINDOW_COUNT') {
    parts.push(`Fenêtre : ${formatInteger(values.windowMinutes)} min`);
  }
  if (schema === 'BET_TYPE' || schema === 'CENTS_BET_TYPE') {
    parts.push(`Type de pari : ${values.betTypeCode || 'Non défini'}`);
  }
  if (schema === 'SELECTION') {
    parts.push(`Numéros : ${values.selectionIds.length ? values.selectionIds.join(', ') : 'Non définis'}`);
  }

  return parts.length ? parts.join(' · ') : 'Aucun paramètre';
}

export function formatLimitSentence(row: RuleRow): string {
  const assignment = row.assignment;
  const outcome = assignment ? outcomeVerb(assignment.onBreach) : 'Configurer';
  const params = assignment?.params ? extractParamValues(detectParamSchema(row.spec), row.spec.paramsTemplate, assignment.params) : null;
  const amount = params ? formatHtg(params.valueCentsHtg) : null;
  const count = params ? formatInteger(params.maxCount) : null;

  switch (row.spec.ruleKey) {
    case 'MAX_STAKE_PER_LINE':
      return `${outcome} si une ligne dépasse ${amount ?? 'le montant défini'}.`;
    case 'MAX_LINES_PER_TICKET':
      return `${outcome} si un ticket dépasse ${count ?? 'le nombre défini'} ligne(s).`;
    case 'MAX_STAKE_PER_TICKET':
      return `${outcome} si le ticket dépasse ${amount ?? 'le montant défini'}.`;
    case 'MAX_POTENTIAL_PAYOUT_PER_TICKET':
      return `${outcome} si le gain potentiel du ticket dépasse ${amount ?? 'le plafond défini'}.`;
    case 'MAX_POTENTIAL_PAYOUT_PER_LINE':
      return `${outcome} si le gain potentiel d'une ligne dépasse ${amount ?? 'le plafond défini'}.`;
    case 'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW':
      return `${outcome} si la mise cumulée sur un numéro dépasse ${amount ?? 'le plafond défini'} pour un tirage.`;
    case 'MAX_POTENTIAL_PAYOUT_EXPOSURE_PER_SELECTION_PER_DRAW':
      return `${outcome} si le gain potentiel cumulé sur un numéro dépasse ${amount ?? 'le plafond défini'} pour un tirage.`;
    case 'MAX_SALES_COUNT_PER_SELECTION_PER_DRAW':
      return `${outcome} si un numéro dépasse ${count ?? 'le nombre défini'} vente(s) pour un tirage.`;
    case 'BLOCK_SELECTION_PER_DRAW':
      return `${outcome} les numéros sélectionnés pour un tirage.`;
    case 'BLOCK_BET_TYPE':
      return `${outcome} le type de pari configuré.`;
    default:
      return `${outcome} selon la règle « ${row.spec.label || row.spec.ruleKey} ».`;
  }
}

function amountLabel(ruleKey: RuleKey): string {
  if (ruleKey.includes('PAYOUT')) return 'Gain potentiel max';
  if (ruleKey.includes('EXPOSURE')) return 'Mise totale max';
  return 'Montant max';
}

function outcomeVerb(outcome: BreachOutcome): string {
  if (outcome === 'BLOCK') return 'Bloquer';
  if (outcome === 'WARN') return 'Avertir';
  if (outcome === 'REQUIRE_APPROVAL') return 'Demander validation';
  return 'Autoriser';
}

function countLabel(ruleKey: RuleKey): string {
  if (ruleKey.includes('LINES')) return 'Lignes max';
  if (ruleKey.includes('TICKET_COUNT')) return 'Tickets max';
  if (ruleKey.includes('SALES_COUNT')) return 'Ventes max';
  return 'Nombre max';
}

function formatHtg(value: number): string {
  return `${formatInteger(value)} HTG`;
}

function formatInteger(value: number): string {
  return new Intl.NumberFormat('fr-HT', { maximumFractionDigits: 0 }).format(value);
}
