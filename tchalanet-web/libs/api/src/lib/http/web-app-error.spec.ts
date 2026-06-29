import { describe, expect, it } from 'vitest';

import { ApiNotice, ProblemDetail } from '../contracts/api.types';
import {
  webAppErrorsFromProblemDetailFields,
  webAppErrorFromNotice,
  webAppErrorFromProblemDetail,
  webAppErrorFromServiceStatus,
} from './web-app-error';

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
  });

  it('normalizes non-blocking ApiNotice metadata without turning it into an HTTP error', () => {
    const notice: ApiNotice = {
      code: 'platform.identity.activation.error',
      message: 'Identity activation could not be completed.',
      domain: 'platform.identity',
      severity: 'WARN',
      meta: {
        source: 'identityActivation',
        service: 'keycloak',
        operation: 'completeFirstLogin',
        requestId: 'req-2',
        traceId: 'trace-2',
        errorId: 'err-2',
      },
    };

    const error = webAppErrorFromNotice(notice, undefined, '/api/bootstrap');

    expect(error.severity).toBe('warn');
    expect(error.surface).toBe('shell');
    expect(error.code).toBe('platform.identity.activation.error');
    expect(error.source).toBe('identityActivation');
    expect(error.requestId).toBe('req-2');
    expect(error.traceId).toBe('trace-2');
    expect(error.errorId).toBe('err-2');
  });

  it('keeps targeted section notices out of the shell surface', () => {
    const notice: ApiNotice = {
      code: 'dashboard.commissions.unavailable',
      message: 'Commissions are temporarily unavailable.',
      domain: 'dashboard',
      severity: 'WARN',
      meta: {
        surface: 'section',
        placement: 'top',
        target: 'dashboard.commissions',
        source: 'commissions',
      },
    };

    const error = webAppErrorFromNotice(notice, undefined, '/api/dashboard');

    expect(error.surface).toBe('section');
    expect(error.placement).toBe('top');
    expect(error.target).toBe('dashboard.commissions');
    expect(error.source).toBe('commissions');
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
  });
});
