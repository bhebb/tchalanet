/// <reference types="node" />

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { ApiResponse, WebAppError, webAppErrorFromNotice } from '@tch/api';
import { describe, expect, it } from 'vitest';

import { posSaleNoticeView } from './pos-terminal-sale.page';

describe('PosTerminalSalePage notice presentation', () => {
  it('renders shared business warning fixtures through client-owned copy', () => {
    const fixture = contractApiResponseFixture('api-responses/success-with-warning.json');
    const error = webAppErrorFromNotice(
      fixture.notices[0],
      fixture.trace,
      'admin.sellerTerminal.pos.preparation',
      'section',
    );

    const view = posSaleNoticeView(error, testTranslate);

    expect(view).toEqual({
      type: 'warning',
      title: 'Action indisponible',
      message: 'Vérifiez les paramètres puis réessayez.',
      scope: null,
    });
    expect(view.message).not.toBe(fixture.notices[0].message);
  });

  it('keeps sale issue warnings scoped to the affected ticket line', () => {
    const view = posSaleNoticeView(
      {
        id: 'issue-1',
        origin: 'backend',
        category: 'business_rule',
        severity: 'error',
        surface: 'section',
        placement: 'inline',
        title: '',
        message: '',
        code: 'sales.draw_closed',
        source: 'admin.sellerTerminal.pos.confirmPreparation',
        target: 'admin.sellerTerminal.pos.sale',
        field: 'lines.2',
        retryable: false,
        dedupeKey:
          'sales.draw_closed|na|admin.sellerTerminal.pos.confirmPreparation|section|admin.sellerTerminal.pos.sale|lines.2',
      } satisfies WebAppError,
      testTranslate,
    );

    expect(view).toEqual({
      type: 'error',
      title: 'Action indisponible',
      message: 'Vérifiez les paramètres puis réessayez.',
      scope: 'Ligne 3',
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

function testTranslate(key: string): string {
  const translations: Record<string, string> = {
    'common.errors.categories.business_rule.title': 'Action indisponible',
    'common.errors.categories.business_rule.message': 'Vérifiez les paramètres puis réessayez.',
    'common.errors.categories.service_unavailable.title': 'Service temporairement indisponible',
    'common.errors.categories.service_unavailable.message': 'Réessayez dans quelques instants.',
    'common.errors.fallback.title': 'Erreur',
    'common.errors.fallback.message': 'Réessayez.',
  };
  return translations[key] ?? key;
}
