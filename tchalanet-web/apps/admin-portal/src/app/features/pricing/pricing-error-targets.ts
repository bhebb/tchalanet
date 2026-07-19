export type PricingFormField = 'odds' | 'fixedAmount' | 'payoutRuleType';

export const PRICING_FIELD_TARGETS: Readonly<Record<string, PricingFormField>> = {
  odds: 'odds',
  fixedAmount: 'fixedAmount',
  payoutRuleType: 'payoutRuleType',
  'admin.pricing.odds.odds': 'odds',
  'admin.pricing.odds.fixedAmount': 'fixedAmount',
  'admin.pricing.odds.payoutRuleType': 'payoutRuleType',
};

/**
 * Transitional ownership for code-first backend validation that has not yet emitted a field
 * violation. The dialog keeps this mapping local to the pricing feature rather than guessing from
 * generic error text.
 */
export function pricingFieldForCode(code: string | undefined): PricingFormField | undefined {
  switch (code) {
    case 'pricing.odds_invalid':
      return 'odds';
    case 'pricing.fixed_amount_invalid':
      return 'fixedAmount';
    case 'pricing.payout_rule_type_invalid':
      return 'payoutRuleType';
    default:
      return undefined;
  }
}
