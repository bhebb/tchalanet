import { describe, expect, it } from 'vitest';

import { PRICING_FIELD_TARGETS, pricingFieldForCode } from './pricing-error-targets';

describe('pricing error targets', () => {
  it('maps known business validation codes to their pricing control', () => {
    expect(pricingFieldForCode('pricing.odds_invalid')).toBe('odds');
    expect(pricingFieldForCode('pricing.fixed_amount_invalid')).toBe('fixedAmount');
    expect(pricingFieldForCode('pricing.payout_rule_type_invalid')).toBe('payoutRuleType');
  });

  it('keeps unknown business failures for the form summary or panel', () => {
    expect(pricingFieldForCode('pricing.tenant_default_not_configured')).toBeUndefined();
  });

  it('maps explicit backend field targets without inferring from message text', () => {
    expect(PRICING_FIELD_TARGETS['admin.pricing.odds.fixedAmount']).toBe('fixedAmount');
  });
});
