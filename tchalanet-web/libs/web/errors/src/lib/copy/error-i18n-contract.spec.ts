/// <reference types="node" />

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

const LOCALES = ['ht', 'fr', 'en'] as const;

const VISIBLE_ERROR_KEYS = [
  'common.errors.categories.access_denied.title',
  'common.errors.categories.access_denied.message',
  'common.errors.categories.validation.title',
  'common.errors.categories.validation.message',
  'common.errors.categories.business_rule.title',
  'common.errors.categories.business_rule.message',
  'common.errors.categories.service_unavailable.title',
  'common.errors.categories.service_unavailable.message',
  'common.errors.codes.access.denied.title',
  'common.errors.codes.access.denied.message',
  'common.errors.codes.validation.failed.title',
  'common.errors.codes.validation.failed.message',
  'common.errors.codes.idempotency.missing.title',
  'common.errors.codes.idempotency.missing.message',
  'common.errors.codes.idempotency.payload_mismatch.title',
  'common.errors.codes.idempotency.payload_mismatch.message',
  'common.errors.codes.idempotency.in_progress.title',
  'common.errors.codes.idempotency.in_progress.message',
  'common.errors.codes.idempotency.completed_no_response.title',
  'common.errors.codes.idempotency.completed_no_response.message',
  'common.errors.codes.sales.draw_closed.title',
  'common.errors.codes.sales.draw_closed.message',
] as const;

describe('visible web error i18n contract', () => {
  it('ships public/admin/POS priority error copy in every locale', () => {
    for (const locale of LOCALES) {
      const translations = flatten(errorsBundle(locale));

      for (const key of VISIBLE_ERROR_KEYS) {
        expect(translations[key], `${locale} missing ${key}`).toEqual(expect.any(String));
        expect(translations[key], `${locale} empty ${key}`).not.toHaveLength(0);
      }
    }
  });
});

function errorsBundle(locale: string): Record<string, unknown> {
  return JSON.parse(
    readFileSync(
      resolve(process.cwd(), `libs/shared-assets/public/assets/i18n/${locale}/errors.json`),
      'utf8',
    ),
  ) as Record<string, unknown>;
}

function flatten(tree: Record<string, unknown>): Record<string, string> {
  const out: Record<string, string> = {};

  const visit = (prefix: string, value: unknown): void => {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      for (const [key, nested] of Object.entries(value)) {
        visit(prefix ? `${prefix}.${key}` : key, nested);
      }
      return;
    }

    if (typeof value === 'string') {
      out[prefix] = value;
    }
  };

  visit('', tree);
  return out;
}
