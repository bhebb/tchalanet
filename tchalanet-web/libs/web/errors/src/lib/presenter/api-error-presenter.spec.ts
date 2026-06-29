import '@angular/compiler';

import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { describe, expect, it } from 'vitest';

import { presentApiError, presentFieldErrorsFromApiError } from './api-error-presenter';

describe('api error presenter', () => {
  it('normalizes unknown HTTP failures into translated view models with diagnostics', () => {
    const result = presentApiError(
      new HttpErrorResponse({
        status: 503,
        statusText: 'Service Unavailable',
        url: '/api/v1/admin/draws',
        headers: new HttpHeaders({ 'X-Trace-Id': 'trace-1' }),
        error: {
          title: 'SQL timeout',
          status: 503,
          code: 'platform.draws.unavailable',
        },
      }),
      key => ({
        'common.errors.codes.platform.draws.unavailable.title': 'Tirages indisponibles',
        'common.errors.codes.platform.draws.unavailable.message': 'Reessayez plus tard.',
      })[key] ?? key,
      { source: 'admin.draws', surface: 'section' },
    );

    expect(result.error.surface).toBe('section');
    expect(result.viewModel).toMatchObject({
      title: 'Tirages indisponibles',
      message: 'Reessayez plus tard.',
      code: 'platform.draws.unavailable',
      traceId: 'trace-1',
      source: 'admin.draws',
    });
  });

  it('presents ProblemDetail field violations as field view models', () => {
    const fields = presentFieldErrorsFromApiError(
      {
        title: 'Validation failed',
        status: 400,
        code: 'validation.failed',
        traceId: 'trace-2',
        violations: [{ field: 'email', code: 'identity.email.invalid', message: 'raw' }],
      },
      key => ({
        'common.errors.codes.identity.email.invalid.title': 'Email invalide',
        'common.errors.codes.identity.email.invalid.message': 'Entrez une adresse courriel valide.',
      })[key] ?? key,
      'profile.form',
    );

    expect(fields).toHaveLength(1);
    expect(fields[0].error.surface).toBe('field');
    expect(fields[0].viewModel).toMatchObject({
      title: 'Email invalide',
      message: 'Entrez une adresse courriel valide.',
      field: 'email',
      traceId: 'trace-2',
    });
  });
});
