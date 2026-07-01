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

// ── Param schema detection ────────────────────────────────────────────────────

export type ParamSchema =
  | 'NONE'
  | 'CENTS'
  | 'COUNT'
  | 'SELECTION'
  | 'BET_TYPE'
  | 'WINDOW_COUNT'
  | 'CENTS_BET_TYPE';

export function detectParamSchema(spec: LimitRuleSpec): ParamSchema {
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
    selectionId: string;
  },
): unknown {
  const t = (template as Record<string, unknown>) ?? {};
  const keys = Object.keys(t);
  const centsKey = keys.find(k => k.toLowerCase().includes('cents')) ?? 'valueCents';
  const countKey = keys.find(k => k.toLowerCase().includes('count')) ?? 'maxCount';
  const windowKey = keys.find(k => k.toLowerCase().includes('window') || k.toLowerCase().includes('minutes')) ?? 'windowMinutes';
  const betTypeKey = keys.find(k => k.toLowerCase().includes('bettype') || k.toLowerCase().includes('bet_type')) ?? 'betTypeCode';
  const selectionKey = keys.find(k => k.toLowerCase().includes('selection')) ?? 'selectionId';

  switch (schema) {
    case 'CENTS':
      return { [centsKey]: Math.round(values.valueCentsHtg * 100) };
    case 'COUNT':
      return { [countKey]: values.maxCount };
    case 'SELECTION':
      return { [selectionKey]: values.selectionId };
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
  selectionId: string;
} {
  const t = (template as Record<string, unknown>) ?? {};
  const p = (params as Record<string, unknown>) ?? {};
  const tkeys = Object.keys(t);
  const centsKey = tkeys.find(k => k.toLowerCase().includes('cents')) ?? 'valueCents';
  const countKey = tkeys.find(k => k.toLowerCase().includes('count')) ?? 'maxCount';
  const windowKey = tkeys.find(k => k.toLowerCase().includes('window') || k.toLowerCase().includes('minutes')) ?? 'windowMinutes';
  const betTypeKey = tkeys.find(k => k.toLowerCase().includes('bettype') || k.toLowerCase().includes('bet_type')) ?? 'betTypeCode';
  const selectionKey = tkeys.find(k => k.toLowerCase().includes('selection')) ?? 'selectionId';

  const rawCents = (p[centsKey] ?? t[centsKey] ?? 0) as number;
  return {
    valueCentsHtg: schema === 'CENTS' || schema === 'CENTS_BET_TYPE' ? rawCents / 100 : 0,
    maxCount: (p[countKey] ?? t[countKey] ?? 0) as number,
    windowMinutes: (p[windowKey] ?? t[windowKey] ?? 0) as number,
    betTypeCode: String(p[betTypeKey] ?? t[betTypeKey] ?? ''),
    selectionId: String(p[selectionKey] ?? t[selectionKey] ?? ''),
  };
}
