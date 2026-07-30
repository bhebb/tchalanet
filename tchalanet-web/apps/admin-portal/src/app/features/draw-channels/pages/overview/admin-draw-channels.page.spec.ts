/// <reference types="node" />

import '@angular/compiler';

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { ProblemDetail } from '@tch/api';
import { describe, expect, it } from 'vitest';

import { adminDrawChannelsErrorView } from './admin-draw-channels.page';

describe('AdminDrawChannelsPage error presentation', () => {
  it('normalizes provider loading failures as page errors', () => {
    const fixture = contractFixture<ProblemDetail>('problem-details/validation-failed.json');

    const view = adminDrawChannelsErrorView(fixture, testTranslate);

    expect(view).toMatchObject({
      severity: 'warn',
      title: 'Configuration à corriger',
      message: 'Corrigez les champs indiqués puis réessayez.',
      surface: 'page',
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
