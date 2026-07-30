/// <reference types="node" />

import '@angular/compiler';

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { ProblemDetail } from '@tch/api';
import { describe, expect, it } from 'vitest';

import { adminSubscriptionErrorView } from './admin-subscription.page';

describe('AdminSubscriptionPage error presentation', () => {
  it('keeps subscription load failures page-owned', () => {
    const fixture = contractFixture<ProblemDetail>('problem-details/validation-failed.json');

    const view = adminSubscriptionErrorView(
      fixture,
      'admin.subscription.load',
      testTranslate,
      'page',
    );

    expect(view).toMatchObject({
      severity: 'warn',
      title: 'Configuration à corriger',
      message: 'Corrigez les champs indiqués puis réessayez.',
      surface: 'page',
    });
  });

  it('keeps subscription action failures section-owned', () => {
    const fixture = contractFixture<ProblemDetail>('problem-details/validation-failed.json');

    const view = adminSubscriptionErrorView(
      fixture,
      'admin.subscription.actions',
      testTranslate,
      'section',
    );

    expect(view).toMatchObject({
      severity: 'warn',
      title: 'Configuration à corriger',
      message: 'Corrigez les champs indiqués puis réessayez.',
      surface: 'section',
    });
  });
});

function contractFixture<T>(relativePath: string): T {
  return JSON.parse(
    readFileSync(
      resolve(
        process.cwd(),
        '../tchalanet-server/testing/contracts/error-contract/v1',
        relativePath,
      ),
      'utf8',
    ),
  ) as T;
}

function testTranslate(key: string): string {
  const translations: Record<string, string> = {
    'common.errors.codes.validation.failed.title': 'Configuration à corriger',
    'common.errors.codes.validation.failed.message': 'Corrigez les champs indiqués puis réessayez.',
    'common.errors.fallback.message': 'Réessayez.',
  };
  return translations[key] ?? key;
}
