/// <reference types="node" />

import '@angular/compiler';

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { ApiResponse, ProblemDetail, webAppErrorFromNotice } from '@tch/api';
import { describe, expect, it } from 'vitest';

import {
  adminSetupPageError,
  adminSetupSectionError,
  isReadinessCardBlocking,
  setupPosPrintingStatus,
  setupPosPrintingStatusLabelKey,
  setupSettingsTarget,
  setupSettingsTargetFromReason,
} from './admin-complete-tenant-config.page';

describe('AdminCompleteTenantConfigPage error presentation', () => {
  it('renders page failures from client-owned ProblemDetail copy', () => {
    const fixture = contractFixture<ProblemDetail>('problem-details/validation-failed.json');

    const view = adminSetupPageError(fixture, testTranslate);

    expect(view).toEqual({
      severity: 'warn',
      title: 'Configuration à corriger',
      message: 'Corrigez les champs indiqués puis réessayez.',
    });
    expect(view.title).not.toBe(fixture.title);
    expect(view.message).not.toBe(fixture.detail);
  });

  it('renders setup section notices without server message prose', () => {
    const fixture = contractFixture<ApiResponse<unknown>>('api-responses/success-with-warning.json');
    const error = webAppErrorFromNotice(
      fixture.notices[0],
      fixture.trace,
      'admin.setup.overview',
      'section',
    );

    const view = adminSetupSectionError(error, testTranslate);

    expect(view).toEqual({
      target: 'setup.optional',
      severity: 'warn',
      title: 'Action requise',
      message: 'Complétez les éléments manquants quand vous êtes prêt.',
    });
    expect(view.message).not.toBe(fixture.notices[0].message);
  });
});

describe('AdminCompleteTenantConfigPage setup classification', () => {
  it('keeps operational setup out of readiness blockers', () => {
    expect(isReadinessCardBlocking('required')).toBe(true);
    expect(isReadinessCardBlocking('blocking')).toBe(true);
    expect(isReadinessCardBlocking('optional')).toBe(false);
    expect(isReadinessCardBlocking('operational')).toBe(false);
  });

  it('maps POS printing status from settings print issues', () => {
    expect(setupPosPrintingStatus(undefined)).toBe('UNKNOWN');
    expect(setupPosPrintingStatus({ issues: [] })).toBe('READY');
    expect(
      setupPosPrintingStatus({
        issues: [{ messageKey: 'settings.print.receipt_template_missing' }],
      }),
    ).toBe('MISSING');
    expect(
      setupPosPrintingStatus({
        issues: [{ messageKey: 'settings.locale.default_missing' }],
      }),
    ).toBe('READY');
  });

  it('uses simple operational status labels for POS printing', () => {
    expect(setupPosPrintingStatusLabelKey('READY')).toBe(
      'admin.setup.operationalStatus.configured',
    );
    expect(setupPosPrintingStatusLabelKey('MISSING')).toBe(
      'admin.setup.operationalStatus.notConfigured',
    );
    expect(setupPosPrintingStatusLabelKey('UNKNOWN')).toBe(
      'admin.setup.operationalStatus.recommended',
    );
  });

  it('keeps print issues operational while required settings corrections prefer sale blockers', () => {
    const settings = {
      status: 'MISSING',
      issues: [
        { messageKey: 'settings.print.paper_size_missing' },
        { messageKey: 'settings.defaults.currency_missing' },
      ],
    };

    expect(setupPosPrintingStatus(settings)).toBe('MISSING');
    expect(setupSettingsTarget(settings)).toEqual({
      route: '/app/admin/company/settings',
    });
  });

  it('routes print-only settings issues to the overview (POS Printing card handles them)', () => {
    const settings = { issues: [{ messageKey: 'settings.print.paper_size_missing' }] };
    expect(setupSettingsTarget(settings)).toEqual({ route: '/app/admin/company/settings' });
  });

  it('routes settings corrective actions to the matching settings section', () => {
    expect(setupSettingsTargetFromReason('settings.print.paper_size_missing')).toEqual({
      route: '/app/admin/company/settings/receipt',
    });
    expect(setupSettingsTargetFromReason('settings.send.sms_missing')).toEqual({
      route: '/app/admin/company/settings/delivery',
    });
    expect(setupSettingsTargetFromReason('settings.calendar.weekend_missing')).toEqual({
      route: '/app/admin/company/settings/calendar',
    });
    expect(setupSettingsTargetFromReason('settings.locale.default_missing')).toEqual({
      route: '/app/admin/business-profile',
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
    'common.errors.categories.business_rule.title': 'Action requise',
    'common.errors.categories.business_rule.message':
      'Complétez les éléments manquants quand vous êtes prêt.',
    'common.errors.fallback.message': 'Réessayez.',
  };
  return translations[key] ?? key;
}
