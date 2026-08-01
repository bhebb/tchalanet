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
  it('uses the API sellerTerminalId string shape', () => {
    const row = { ...baseRow, sellerTerminalId: 'terminal-uuid' } satisfies SellerTerminalCommissionRow;

    expect(sellerTerminalCommissionId(row)).toBe('terminal-uuid');
  });

  it('keeps compatibility with the legacy sellerTerminalId object shape', () => {
    const row = {
      ...baseRow,
      sellerTerminalId: { value: 'terminal-uuid' },
    } satisfies SellerTerminalCommissionRow;

    expect(sellerTerminalCommissionId(row)).toBe('terminal-uuid');
  });

  it('keeps compatibility with the previous frontend id field', () => {
    const row = { ...baseRow, id: { value: 'terminal-uuid' } } satisfies SellerTerminalCommissionRow;

    expect(sellerTerminalCommissionId(row)).toBe('terminal-uuid');
  });
});
