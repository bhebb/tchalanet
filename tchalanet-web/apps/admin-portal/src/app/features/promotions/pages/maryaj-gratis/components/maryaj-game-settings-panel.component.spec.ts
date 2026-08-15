import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { TenantGamePricingView } from '../../../../games-pricing/data-access/admin-games-pricing.models';
import { MaryajGameSettingsPanelComponent } from './maryaj-game-settings-panel.component';

describe(MaryajGameSettingsPanelComponent.name, () => {
  let component: MaryajGameSettingsPanelComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({
          fallbackLang: 'ht',
          lang: 'ht',
        }),
      ],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('ht', translations);
    translate.use('ht');
    component = TestBed.runInInjectionContext(() => new MaryajGameSettingsPanelComponent());
  });

  it('does not leak raw readiness translation keys for unknown statuses', () => {
    expect(component.readinessLabel('READY')).toBe('Pare');
    expect(component.readinessLabel('TODO')).toBe('Pou konfigire');
    expect(component.readinessLabel('BLOCKED')).toBe('Pa disponib');
    expect(component.readinessLabel('UNKNOWN')).toBe('UNKNOWN');
  });

  it('translates readiness reasons and keeps plain backend text as fallback', () => {
    expect(
      component.readinessReason('admin.gamesPricing.readiness.reason.missingStakeOrPricing'),
    ).toBe('Miz oswa barèm manke.');
    expect(component.readinessReason('Configure stake first')).toBe('Configure stake first');
    expect(component.readinessReason(null)).toBeNull();
  });

  it('maps readiness states to shared console status tones', () => {
    expect(component.readinessTone('READY')).toBe('success');
    expect(component.readinessTone('TODO')).toBe('warning');
    expect(component.readinessTone('BLOCKED')).toBe('danger');
    expect(component.readinessTone('UNKNOWN')).toBe('neutral');
  });

  it('keeps draw availability navigation on the dedicated matrix route', () => {
    expect(component.availabilityRoute).toBe('/app/admin/games/channel-matrix');
  });

  it('renders an incomplete Maryaj Gratis game as an exceptional setup state', () => {
    const fixture = TestBed.createComponent(MaryajGameSettingsPanelComponent);
    fixture.componentRef.setInput('game', maryajGame({ tenantStatus: 'NEEDS_CONFIG' }));

    fixture.detectChanges();

    const text = normalizedText(fixture.nativeElement.textContent as string);
    expect(text).toContain('Pou konfigire');
    expect(text).toContain('Miz oswa barèm manke.');
    expect(text).toContain('Modifye jwèt la');
  });

  it('renders a compact missing-game empty state', () => {
    const fixture = TestBed.createComponent(MaryajGameSettingsPanelComponent);
    fixture.componentRef.setInput('game', null);

    fixture.detectChanges();

    const text = normalizedText(fixture.nativeElement.textContent as string);
    expect(text).toContain('Pa konfigire');
    expect(text).toContain('Jwèt Maryaj gratis la poko disponib.');
  });
});

function normalizedText(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

function maryajGame(overrides: Partial<TenantGamePricingView> = {}): TenantGamePricingView {
  return {
    gameCode: 'HT_MARYAJ_GRATIS',
    tenantGameId: 'tenant-game-1',
    gameName: 'Maryaj gratis',
    catalogStatus: 'AVAILABLE',
    tenantStatus: 'ACTIVE',
    visibleInPos: true,
    pricingProfileLabel: null,
    odds: [],
    oddsGroups: [],
    limits: {
      minStake: 1,
      maxStake: 10_000_000,
      maxPerDraw: null,
      currency: 'HTG',
    },
    readiness: {
      status: 'TODO',
      label: 'Pou konfigire',
      reason: 'admin.gamesPricing.readiness.reason.missingStakeOrPricing',
    },
    ...overrides,
  };
}

const translations = {
  common: {
    not_available: 'Pa disponib',
  },
  admin: {
    gamesPricing: {
      card: {
        availability: 'Disponibilite',
        availabilitySummary: 'Jere sou chak tiraj',
        configure: 'Konfigire',
        payoutOptions: 'Opsyon gany yo',
        readiness: 'Disponib pou vann',
        stakes: 'Miz',
      },
      readiness: {
        reason: {
          missingStakeOrPricing: 'Miz oswa barèm manke.',
        },
      },
    },
    maryajGratis: {
      game: {
        configure: 'Modifye jwèt la',
        empty: {
          title: 'Jwèt Maryaj gratis la poko disponib.',
          copy: 'Konfigire jwèt la pou aktive òf gratis la.',
        },
        notConfigured: 'Pa konfigire',
        readiness: {
          READY: 'Pare',
          TODO: 'Pou konfigire',
          BLOCKED: 'Pa disponib',
        },
        subtitle: 'Menm lojik ak Maryaj.',
        title: 'Jwèt la',
      },
    },
  },
};
