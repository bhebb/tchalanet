import { describe, expect, it } from 'vitest';

import { webAppErrorFromSaleIssue } from './pos-sale-api.service';

describe('webAppErrorFromSaleIssue', () => {
  it('uses the stable issue code and never server-supplied prose', () => {
    const error = webAppErrorFromSaleIssue(
      {
        code: 'sales.draw_closed',
        severity: 'ERROR',
        message: 'Internal provider message',
        sellerInstruction: 'Raw operator prose',
        lineIndex: 2,
      },
      'admin.sellerTerminal.pos.confirmPreparation',
    );

    expect(error).toMatchObject({
      code: 'sales.draw_closed',
      surface: 'section',
      target: 'admin.sellerTerminal.pos.sale',
      field: 'lines.2',
      severity: 'error',
      title: '',
      message: '',
    });
  });
});
