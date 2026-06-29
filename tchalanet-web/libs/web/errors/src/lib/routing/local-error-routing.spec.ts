import '@angular/compiler';

import { FormControl, FormGroup } from '@angular/forms';
import { WebAppError } from '@tch/api';
import { describe, expect, it } from 'vitest';

import { applyServerFieldErrors, clearServerFieldErrors, errorsForTarget } from './local-error-routing';

describe('local error routing', () => {
  it('selects errors for an owned section target', () => {
    const errors = [
      makeError({ surface: 'section', target: 'dashboard.commissions' }),
      makeError({ surface: 'section', target: 'dashboard.draw' }),
    ];

    expect(errorsForTarget(errors, 'section', 'dashboard.commissions')).toHaveLength(1);
  });

  it('applies server field errors to matching controls', () => {
    const form = new FormGroup({
      email: new FormControl('bad-value'),
    });

    const remaining = applyServerFieldErrors(form, [
      makeError({ surface: 'field', field: 'email', target: 'profile.email', message: 'Email invalide' }),
    ]);

    expect(remaining).toEqual([]);
    expect(form.controls.email.errors?.['server']).toMatchObject({ message: 'Email invalide' });
  });

  it('returns field errors that do not map to a control', () => {
    const form = new FormGroup({
      email: new FormControl('bad-value'),
    });

    const remaining = applyServerFieldErrors(form, [
      makeError({ surface: 'field', field: 'phone', target: 'profile.phone' }),
    ]);

    expect(remaining).toHaveLength(1);
  });

  it('clears only server field errors', () => {
    const form = new FormGroup({
      email: new FormControl(''),
    });
    form.controls.email.setErrors({ required: true, server: makeError({ surface: 'field', field: 'email' }) });

    clearServerFieldErrors(form);

    expect(form.controls.email.errors).toEqual({ required: true });
  });
});

function makeError(overrides: Partial<WebAppError> = {}): WebAppError {
  return {
    id: 'id',
    origin: 'backend',
    category: 'validation',
    severity: 'error',
    surface: 'field',
    placement: 'inline',
    title: 'Erreur',
    message: 'Message',
    retryable: false,
    dedupeKey: 'key',
    ...overrides,
  };
}
