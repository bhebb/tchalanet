import { describe, expect, it } from 'vitest';

import {
  sellerTerminalCommissionId,
  SellerTerminalCommissionRow,
} from './admin-commission-api.service';

const baseRow = {
  terminalCode: 'POS-001',
  displayName: 'Bhebb',
  status: 'ACTIVE',
  commissionRate: 10,
  rateSource: 'DEFAULT',
} as const;

describe('sellerTerminalCommissionId', () => {
  it('uses the API string id shape', () => {
    const row = { ...baseRow, id: 'terminal-uuid' } satisfies SellerTerminalCommissionRow;

    expect(sellerTerminalCommissionId(row)).toBe('terminal-uuid');
  });

  it('keeps compatibility with the legacy typed-id object shape', () => {
    const row = { ...baseRow, id: { value: 'terminal-uuid' } } satisfies SellerTerminalCommissionRow;

    expect(sellerTerminalCommissionId(row)).toBe('terminal-uuid');
  });
});
