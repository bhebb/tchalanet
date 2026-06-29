import { describe, expect, it } from 'vitest';

import { WebAppError } from '@tch/api';
import { resolveErrorFeedbackCopy } from './error-feedback-copy';

const baseError: WebAppError = {
  id: '1',
  origin: 'backend',
  category: 'service_unavailable',
  severity: 'warn',
  surface: 'shell',
  placement: 'top',
  title: 'platform.identity.activation.error',
  message: 'Raw backend message',
  code: 'platform.identity.activation.error',
  retryable: false,
  dedupeKey: 'key',
};

describe('resolveErrorFeedbackCopy', () => {
  it('prefers exact stable code translation', () => {
    const result = resolveErrorFeedbackCopy(baseError, key => ({
      'common.errors.codes.platform.identity.activation.error.title': 'Activation incomplete',
      'common.errors.codes.platform.identity.activation.error.message': 'Continue and contact support if needed.',
    })[key] ?? key);

    expect(result.title).toBe('Activation incomplete');
    expect(result.message).toBe('Continue and contact support if needed.');
  });

  it('falls back to category translation when code is unknown', () => {
    const result = resolveErrorFeedbackCopy(baseError, key => ({
      'common.errors.categories.service_unavailable.title': 'Service unavailable',
      'common.errors.categories.service_unavailable.message': 'Try again later.',
    })[key] ?? key);

    expect(result.title).toBe('Service unavailable');
    expect(result.message).toBe('Try again later.');
  });

  it('falls back to safe generic copy when no translation exists', () => {
    const result = resolveErrorFeedbackCopy(baseError, key => ({
      'common.errors.fallback.title': 'Problem detected',
      'common.errors.fallback.message': 'Copy the support reference.',
    })[key] ?? key);

    expect(result.title).toBe('Problem detected');
    expect(result.message).toBe('Copy the support reference.');
  });
});
