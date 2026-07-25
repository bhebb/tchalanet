import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { ApiNotice, ApiResponse, ProblemDetail } from '../contracts/api.types';
import {
  webAppErrorsFromProblemDetailFields,
  webAppErrorFromNotice,
  webAppErrorFromProblemDetail,
  webAppErrorFromServiceStatus,
} from './web-app-error';

const CONTRACT_FIXTURES_ROOT = resolve(
  __dirname,
  '../../../../../../tchalanet-server/testing/contracts/error-contract/v1',
);

describe('web app error normalization', () => {
  it('normalizes coded ProblemDetail with trace fields', () => {
    const problem: ProblemDetail = {
      title: 'Forbidden',
      status: 403,
      detail: 'Access denied',
      code: 'access.denied',
      requestId: 'req-1',
      traceId: 'trace-1',
      spanId: 'span-1',
      errorId: 'err-1',
    };

    const error = webAppErrorFromProblemDetail(problem, '/api/private');

    expect(error.category).toBe('access_denied');
    expect(error.severity).toBe('warn');
    expect(error.code).toBe('access.denied');
    expect(error.requestId).toBe('req-1');
    expect(error.traceId).toBe('trace-1');
    expect(error.errorId).toBe('err-1');
    expect(error.dedupeKey).toContain('access.denied');
    expect(error.message).not.toBe('Access denied');
    expect(error).not.toHaveProperty('diagnostics');
  });

  it('uses an allowed legacy ProblemDetail detail as code when the code field is missing', () => {
    const problem: ProblemDetail = {
      title: 'Forbidden',
      status: 403,
      detail: 'limits.blocked',
      type: 'about:blank',
      requestId: 'req-limits',
    };

    const error = webAppErrorFromProblemDetail(problem, '/api/v1/tenant/sales/preparations');

    expect(error.code).toBe('limits.blocked');
    expect(error.category).toBe('access_denied');
    expect(error.dedupeKey).toContain('limits.blocked');
  });

  it('does not promote arbitrary ProblemDetail detail values to codes', () => {
    const problem: ProblemDetail = {
      title: 'Forbidden',
      status: 403,
      detail: 'provider.internal_failure',
      type: 'about:blank',
      requestId: 'req-provider',
    };

    const error = webAppErrorFromProblemDetail(problem, '/api/v1/tenant/sales/preparations');

    expect(error.code).toBeUndefined();
    expect(error.category).toBe('access_denied');
    expect(error.dedupeKey).not.toContain('provider.internal_failure');
  });

  it('prefers structured non-blocking ApiNotice fields over its legacy meta bridge', () => {
    const notice: ApiNotice = {
      code: 'platform.identity.activation.error',
      message: 'Identity activation could not be completed.',
      domain: 'platform.identity',
      severity: 'WARN',
      source: {
        source: 'identityActivation',
        service: 'identity-provider',
        operation: 'completeFirstLogin',
      },
      target: 'identity.activation',
      trace: { requestId: 'req-2', traceId: 'trace-2', spanId: 'span-2', errorId: 'err-2' },
      meta: {
        source: 'legacy-source',
        requestId: 'legacy-request',
        traceId: 'legacy-trace',
        errorId: 'legacy-error',
      },
    };

    const error = webAppErrorFromNotice(notice, undefined, '/api/bootstrap');

    expect(error.severity).toBe('warn');
    expect(error.surface).toBe('shell');
    expect(error.code).toBe('platform.identity.activation.error');
    expect(error.source).toBe('identityActivation');
    expect(error.requestId).toBe('req-2');
    expect(error.traceId).toBe('trace-2');
    expect(error.spanId).toBe('span-2');
    expect(error.errorId).toBe('err-2');
    expect(error.message).not.toBe(notice.message);
    expect(error).not.toHaveProperty('diagnostics');
  });

  it('keeps targeted section notices out of the shell surface', () => {
    const notice: ApiNotice = {
      code: 'dashboard.commissions.unavailable',
      message: 'Commissions are temporarily unavailable.',
      domain: 'dashboard',
      severity: 'WARN',
      source: { source: 'commissions' },
      target: 'dashboard.commissions',
      meta: {
        surface: 'section',
        placement: 'top',
        target: 'legacy-target',
        source: 'commissions',
      },
    };

    const error = webAppErrorFromNotice(notice, undefined, '/api/dashboard');

    expect(error.surface).toBe('section');
    expect(error.placement).toBe('top');
    expect(error.target).toBe('dashboard.commissions');
    expect(error.source).toBe('commissions');
  });

  it('keeps form failures local with a summary placement', () => {
    const problem: ProblemDetail = {
      title: 'Invalid rate',
      status: 422,
      code: 'tenant.settings_invalid',
    };

    const error = webAppErrorFromProblemDetail(problem, 'admin.commission.defaultRate', 'form');

    expect(error.surface).toBe('form');
    expect(error.placement).toBe('summary');
  });

  it('extracts server field violations from ProblemDetail', () => {
    const problem: ProblemDetail = {
      title: 'Validation failed',
      status: 400,
      code: 'validation.failed',
      violations: [
        {
          code: 'validation.failed',
          field: 'email',
          target: 'profile.email',
          message: 'Email is invalid',
        },
      ],
    };

    const errors = webAppErrorsFromProblemDetailFields(problem, '/api/profile');

    expect(errors).toHaveLength(1);
    expect(errors[0]).toMatchObject({
      surface: 'field',
      placement: 'inline',
      field: 'email',
      target: 'profile.email',
      category: 'validation',
    });
    expect(errors[0].message).not.toBe('Email is invalid');
    expect(errors[0]).not.toHaveProperty('diagnostics');
  });

  it('normalizes degraded service status with response trace fallback', () => {
    const error = webAppErrorFromServiceStatus(
      { service: 'uslottery', status: 'DEGRADED', message: 'Latest results unavailable' },
      { requestId: 'req-3', traceId: 'trace-3' },
      '/api/dashboard',
    );

    expect(error.severity).toBe('warn');
    expect(error.code).toBe('service.uslottery.degraded');
    expect(error.requestId).toBe('req-3');
    expect(error.traceId).toBe('trace-3');
    expect(error.message).not.toBe('Latest results unavailable');
    expect(error).not.toHaveProperty('diagnostics');
  });

  it('consumes shared ProblemDetail fixtures for public page and admin page surfaces', () => {
    const publicProblem = contractFixture<ProblemDetail>('problem-details/access-denied.json');
    const publicError = webAppErrorFromProblemDetail(publicProblem, 'public.ticket.verify', 'page');

    expect(publicError).toMatchObject({
      code: 'access.denied',
      category: 'access_denied',
      surface: 'page',
      placement: 'top',
      source: 'public.ticket.verify',
      retryPolicy: 'NEVER',
      retryable: false,
    });
    expect(publicError.message).not.toBe(publicProblem.detail);
    expect(publicError.title).not.toBe(publicProblem.title);
    expect(publicError.requestId).toBe('req_contract_denied');
    expect(publicError.errorId).toBe('err_contract_denied');

    const adminProblem = contractFixture<ProblemDetail>('problem-details/validation-failed.json');
    const adminPageError = webAppErrorFromProblemDetail(
      adminProblem,
      'admin.sellerterminal.form',
      'form',
    );
    const fieldErrors = webAppErrorsFromProblemDetailFields(
      adminProblem,
      'admin.sellerterminal.form',
    );

    expect(adminPageError).toMatchObject({
      code: 'validation.failed',
      category: 'validation',
      surface: 'form',
      placement: 'summary',
      retryPolicy: 'AFTER_USER_ACTION',
      retryable: true,
    });
    expect(fieldErrors.map(error => [error.code, error.field, error.target])).toEqual([
      ['validation.required', 'sellerName', 'sellerName'],
      ['validation.invalid_format', 'email', 'email'],
    ]);
    expect(fieldErrors.every(error => error.surface === 'field')).toBe(true);
    expect(JSON.stringify(fieldErrors)).not.toContain('Request could not be completed');
  });

  it('consumes shared ApiResponse fixtures without turning targeted notices into shell feedback', () => {
    const warning = contractFixture<ApiResponse<{ tenantCode: string; ready: boolean }>>(
      'api-responses/success-with-warning.json',
    );
    const warningError = webAppErrorFromNotice(
      warning.notices[0],
      undefined,
      'admin.setup',
      'page',
    );

    expect(warning.status).toBe('SUCCESS_WITH_WARNINGS');
    expect(warningError).toMatchObject({
      code: 'tenant.setup.optional_step_missing',
      category: 'business_rule',
      severity: 'warn',
      surface: 'page',
      target: 'setup.optional',
      source: 'tenantSetup',
      requestId: 'req_contract_warning',
      errorId: 'err_contract_warning',
    });
    expect(warningError.params).toEqual({ completedSteps: 3, totalSteps: 5 });
    expect(warningError.message).not.toBe(warning.notices[0].message);

    const partial = contractFixture<ApiResponse<{ dashboard: Record<string, boolean> }>>(
      'api-responses/partial-section-degradation.json',
    );
    const partialError = webAppErrorFromNotice(
      partial.notices[0],
      undefined,
      'admin.dashboard',
      'section',
    );

    expect(partial.status).toBe('PARTIAL');
    expect(partialError).toMatchObject({
      code: 'admin.dashboard.provider_results_unavailable',
      category: 'service_unavailable',
      severity: 'warn',
      surface: 'section',
      placement: 'top',
      target: 'admin_dashboard.provider_results',
      source: 'providerResults',
      requestId: 'req_contract_partial',
      errorId: 'err_contract_partial',
    });
    expect(JSON.stringify(partialError)).not.toContain('uslottery');
    expect(partial.services?.[0]).toMatchObject({ service: 'uslottery', status: 'DEGRADED' });
  });
});

function contractFixture<T>(relativePath: string): T {
  return JSON.parse(readFileSync(resolve(CONTRACT_FIXTURES_ROOT, relativePath), 'utf8')) as T;
}
