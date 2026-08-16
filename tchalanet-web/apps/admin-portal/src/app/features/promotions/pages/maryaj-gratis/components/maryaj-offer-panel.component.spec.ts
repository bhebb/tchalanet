import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { FormBuilder } from '@angular/forms';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type {
  PromotionCampaignView,
  PromotionConfigItem,
} from '../../../data-access/admin-promotions-api.service';
import { MaryajOfferPanelComponent } from './maryaj-offer-panel.component';

describe(MaryajOfferPanelComponent.name, () => {
  let formBuilder: FormBuilder;
  let component: MaryajOfferPanelComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        FormBuilder,
        provideNoopAnimations(),
        provideTranslateService({
          fallbackLang: 'ht',
          lang: 'ht',
        }),
      ],
    });
    formBuilder = TestBed.inject(FormBuilder);
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('ht', translations);
    translate.use('ht');
    component = TestBed.runInInjectionContext(() => new MaryajOfferPanelComponent());
    const testForm = form();
    (component as unknown as { form: () => typeof testForm }).form = () => testForm;
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('opens a single tier editor at a time', () => {
    expect(component.isTierOpen(0)).toBe(true);
    expect(component.isTierOpen(1)).toBe(false);

    component.openTier(1);

    expect(component.isTierOpen(0)).toBe(false);
    expect(component.isTierOpen(1)).toBe(true);
  });

  it('opens the newly added tier', () => {
    const emit = vi.spyOn(component.addQuantityTier, 'emit');

    component.addTier();

    expect(emit).toHaveBeenCalledOnce();
    expect(component.openTierIndex()).toBe(2);
  });

  it('maps an open-ended tier to a null upper amount', () => {
    component.setTierOpenEnded(0, true);

    expect(component.quantityTiers().at(0).get('maxPaidAmount')?.value).toBeNull();
    expect(component.isTierOpenEnded(0)).toBe(true);

    component.setTierOpenEnded(0, false);

    expect(component.quantityTiers().at(0).get('maxPaidAmount')?.value).toBe(100);
    expect(component.isTierOpenEnded(0)).toBe(false);
  });

  it('maps campaign statuses to shared console status tones', () => {
    expect(component.statusTone('ACTIVE')).toBe('success');
    expect(component.statusTone('PAUSED')).toBe('warning');
    expect(component.statusTone('DRAFT')).toBe('warning');
    expect(component.statusTone('ARCHIVED')).toBe('danger');
    expect(component.statusTone('INACTIVE')).toBe('neutral');
  });

  it('keeps offer statuses business-facing', () => {
    expect(component.statusLabel('ACTIVE')).toBe('Aktif');
    expect(component.statusLabel('PAUSED')).toBe('An poz');
    expect(component.statusLabel('DRAFT')).toBe('Bouyon');
    expect(component.statusLabel('INACTIVE')).toBe('Inaktif');
    expect(component.statusLabel('ARCHIVED')).toBe('Achive');
  });

  it('surfaces scheduled and ended campaign lifecycle states from dates', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-15T12:00:00Z'));

    expect(
      component.campaignStatusLabel(
        campaign({
          startsAt: '2026-08-20T00:00:00Z',
          endsAt: '2026-12-31T23:59:59Z',
        }),
      ),
    ).toBe('Pwograme');
    expect(
      component.campaignStatusTone(
        campaign({
          startsAt: '2026-08-20T00:00:00Z',
          endsAt: '2026-12-31T23:59:59Z',
        }),
      ),
    ).toBe('warning');

    expect(
      component.campaignStatusLabel(
        campaign({
          startsAt: '2026-01-01T00:00:00Z',
          endsAt: '2026-07-31T23:59:59Z',
        }),
      ),
    ).toBe('Fini');
    expect(
      component.campaignStatusTone(
        campaign({
          startsAt: '2026-01-01T00:00:00Z',
          endsAt: '2026-07-31T23:59:59Z',
        }),
      ),
    ).toBe('neutral');

    expect(
      component.campaignStatusLabel(
        campaign({
          startsAt: '2026-01-01T00:00:00Z',
          endsAt: '2036-01-01T23:59:59Z',
        }),
      ),
    ).toBe('Aktif');
  });

  it('treats no-end and long-running campaign dates as permanent', () => {
    expect(component.isPermanentCampaign(campaign({ endsAt: null }))).toBe(true);
    expect(
      component.isPermanentCampaign(
        campaign({
          startsAt: '2026-07-01T00:00:00Z',
          endsAt: '2036-07-01T23:59:59Z',
        }),
      ),
    ).toBe(true);
    expect(
      component.isPermanentCampaign(
        campaign({
          startsAt: '2026-07-01T00:00:00Z',
          endsAt: '2026-12-31T23:59:59Z',
        }),
      ),
    ).toBe(false);
  });

  it('reads zero tier rules for summary display', () => {
    setEffect({ quantityTiers: [] });
    expect(component.effectQuantityTiers()).toEqual([]);
  });

  it('reads one tier rule for summary display', () => {
    setEffect({ quantityTiers: [{ minPaidAmount: 100, maxPaidAmount: 199, quantity: 1 }] });

    expect(component.effectQuantityTiers()).toHaveLength(1);
    const firstTier = component.effectQuantityTiers().at(0);
    if (!firstTier) throw new Error('Expected one tier');
    expect(component.tierRuleLabel(firstTier)).toBe('100 HTG – 199 HTG → 1 Maryaj gratis');
  });

  it('reads multiple tier rules for summary display', () => {
    setEffect({
      quantityTiers: [
        { minPaidAmount: 100, maxPaidAmount: 199, quantity: 1 },
        { minPaidAmount: 200, maxPaidAmount: null, quantity: 2 },
      ],
    });

    expect(component.effectQuantityTiers()).toHaveLength(2);
    const secondTier = component.effectQuantityTiers().at(1);
    if (!secondTier) throw new Error('Expected a second tier');
    expect(component.tierRuleLabel(secondTier)).toBe('200 HTG + → 2 Maryaj gratis');
  });

  it('renders an incomplete offer with a compact setup action', () => {
    const fixture = TestBed.createComponent(MaryajOfferPanelComponent);
    fixture.componentRef.setInput('campaign', null);
    fixture.componentRef.setInput('effect', null);
    fixture.componentRef.setInput('form', form());
    fixture.componentRef.setInput('saving', false);
    fixture.componentRef.setInput('gameReady', true);
    fixture.componentRef.setInput('editing', false);

    fixture.detectChanges();

    const text = normalizedText(fixture.nativeElement.textContent as string);
    expect(text).toContain('Pou aktive');
    expect(text).toContain('Òf Maryaj gratis la poko aktive.');
    expect(text).toContain('Modifye òf la');
  });

  function form() {
    return formBuilder.group({
      quantityTiers: formBuilder.array([
        formBuilder.group({ minPaidAmount: 100, maxPaidAmount: 199, quantity: 1 }),
        formBuilder.group({ minPaidAmount: 200, maxPaidAmount: null, quantity: 2 }),
      ]),
    });
  }

  function setEffect(params: Record<string, unknown>): void {
    const effect: PromotionConfigItem = { type: 'FREE_GAME_LINE', params };
    (component as unknown as { effect: () => PromotionConfigItem }).effect = () => effect;
  }

  function campaign(overrides: Partial<PromotionCampaignView> = {}): PromotionCampaignView {
    return {
      id: { value: 'campaign-maryaj-gratis' },
      code: 'DEFAULT_MARYAJ_GRATIS',
      name: 'Maryaj gratis',
      status: 'ACTIVE',
      priority: 100,
      startsAt: '2026-07-01T00:00:00Z',
      endsAt: '2036-07-01T23:59:59Z',
      rules: [],
      ...overrides,
    };
  }

  function normalizedText(value: string): string {
    return value.replace(/\s+/g, ' ').trim();
  }
});

const translations = {
  common: {
    edit: 'Modifye',
    not_available: 'Pa disponib',
  },
  admin: {
    maryajGratis: {
      amount: {
        htg: '{{amount}} HTG',
      },
      offer: {
        description: 'Dat, palye ak kantite.',
        empty: {
          action: 'Modifye òf la',
          title: 'Òf Maryaj gratis la poko aktive.',
          copy: 'Konfigire òf la pou kliyan yo resevwa Maryaj gratis.',
        },
        status: {
          active: 'Aktif',
          paused: 'An poz',
          draft: 'Bouyon',
          inactive: 'Inaktif',
          archived: 'Achive',
          scheduled: 'Pwograme',
          ended: 'Fini',
          needsSetup: 'Pou aktive',
        },
        tiers: {
          openRange: '{{min}} +',
          range: '{{min}} – {{max}}',
          rule: '{{range}} → {{quantity}} Maryaj gratis',
        },
        title: 'Òf gratis la',
      },
    },
  },
};
