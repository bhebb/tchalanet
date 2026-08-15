import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

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
        { provide: TranslateService, useValue: { instant: translateInstant } },
      ],
    });
    formBuilder = TestBed.inject(FormBuilder);
    component = TestBed.runInInjectionContext(() => new MaryajOfferPanelComponent());
    const testForm = form();
    (component as unknown as { form: () => typeof testForm }).form = () => testForm;
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

  it('keeps active and paused offer statuses business-facing', () => {
    expect(component.statusLabel('ACTIVE')).toBe('Aktif');
    expect(component.statusLabel('PAUSED')).toBe('An poz');
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

  it('reads zero, one, and multiple tier rules for summary display', () => {
    setEffect({ quantityTiers: [] });
    expect(component.effectQuantityTiers()).toEqual([]);

    setEffect({ quantityTiers: [{ minPaidAmount: 100, maxPaidAmount: 199, quantity: 1 }] });
    expect(component.effectQuantityTiers()).toHaveLength(1);
    const firstTier = component.effectQuantityTiers().at(0);
    if (!firstTier) throw new Error('Expected one tier');
    expect(component.tierRuleLabel(firstTier)).toBe('100 HTG – 199 HTG → 1 Maryaj gratis');

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

  function translateInstant(key: string, params?: Record<string, unknown>): string {
    const translations: Record<string, string> = {
      'admin.maryajGratis.amount.htg': '{{amount}} HTG',
      'admin.maryajGratis.offer.status.active': 'Aktif',
      'admin.maryajGratis.offer.status.paused': 'An poz',
      'admin.maryajGratis.offer.tiers.openRange': '{{min}} +',
      'admin.maryajGratis.offer.tiers.range': '{{min}} – {{max}}',
      'admin.maryajGratis.offer.tiers.rule': '{{range}} → {{quantity}} Maryaj gratis',
    };
    const template = translations[key] ?? key;
    return Object.entries(params ?? {}).reduce(
      (value, [name, replacement]) => value.replace(`{{${name}}}`, String(replacement)),
      template,
    );
  }
});
