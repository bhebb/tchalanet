import { describe, expect, it } from 'vitest';

import { commissionFieldForCode } from './commission-error-targets';

describe('commissionFieldForCode', () => {
  it('routes the seller commission validation code to the rate control', () => {
    expect(commissionFieldForCode('sellerterminal.commission_rate_invalid')).toBe('rate');
  });

  it('does not treat unrelated errors as field validation', () => {
    expect(commissionFieldForCode('server.unexpected')).toBeUndefined();
  });
});
