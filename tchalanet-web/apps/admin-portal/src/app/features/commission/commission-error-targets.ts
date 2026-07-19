export const COMMISSION_FIELD_TARGETS = {
  rate: 'rate',
  'admin.commission.sellerRate.rate': 'rate',
} as const;

const COMMISSION_CODE_TARGETS: Readonly<Record<string, 'rate'>> = {
  'sellerterminal.commission_rate_invalid': 'rate',
};

/** Transitional mapping until all commission validation responses carry a field target. */
export function commissionFieldForCode(code: string | undefined): 'rate' | undefined {
  return code ? COMMISSION_CODE_TARGETS[code] : undefined;
}
