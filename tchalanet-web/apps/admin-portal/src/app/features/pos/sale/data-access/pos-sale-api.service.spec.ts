/// <reference types="node" />

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { ApiResponse } from '@tch/api';
import { describe, expect, it } from 'vitest';

import { webAppErrorFromPosNotice, webAppErrorFromSaleIssue } from './pos-sale-api.service';

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

  it('maps shared warning fixtures for POS preparation without server prose', () => {
    const fixture = contractApiResponseFixture('api-responses/success-with-warning.json');
    const error = webAppErrorFromPosNotice(
      fixture.notices[0],
      fixture.trace,
      'admin.sellerTerminal.pos.preparation',
    );

    expect(error).toMatchObject({
      code: 'tenant.setup.optional_step_missing',
      surface: 'section',
      target: 'setup.optional',
      source: 'tenantSetup',
      severity: 'warn',
      category: 'business_rule',
      title: 'Action indisponible',
      requestId: 'req_contract_warning',
      traceId: 'trace-contract-warning',
      errorId: 'err_contract_warning',
      params: { completedSteps: 3, totalSteps: 5 },
    });
    expect(error.message).not.toBe(fixture.notices[0].message);
  });

  it('maps shared partial degradation fixtures for POS confirm/sell sections', () => {
    const fixture = contractApiResponseFixture('api-responses/partial-section-degradation.json');
    const error = webAppErrorFromPosNotice(
      fixture.notices[0],
      fixture.trace,
      'admin.sellerTerminal.pos.confirmPreparation',
    );

    expect(fixture.status).toBe('PARTIAL');
    expect(error).toMatchObject({
      code: 'admin.dashboard.provider_results_unavailable',
      surface: 'section',
      target: 'admin_dashboard.provider_results',
      source: 'providerResults',
      severity: 'warn',
      category: 'service_unavailable',
      requestId: 'req_contract_partial',
      traceId: 'trace-contract-partial',
      errorId: 'err_contract_partial',
    });
  });
});

function contractApiResponseFixture(relativePath: string): ApiResponse<unknown> {
  return JSON.parse(
    readFileSync(
      resolve(
        process.cwd(),
        '../tchalanet-server/testing/contracts/error-contract/v1',
        relativePath,
      ),
      'utf8',
    ),
  ) as ApiResponse<unknown>;
}
