import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type {
  PromotionCampaignView,
  PromotionConfigItem,
} from '../../../../data-access/admin-promotions-api.service';
import { MaryajOfferCardComponent } from './maryaj-offer-card.component';

describe(MaryajOfferCardComponent.name, () => {
  let component: MaryajOfferCardComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideNoopAnimations(),
        provideTranslateService({ fallbackLang: 'ht', lang: 'ht' }),
      ],
    });
    const translate = TestBed.inject(TranslateService);
    translate.setTranslation('ht', translations);
    translate.use('ht');
    component = TestBed.runInInjectionContext(() => new MaryajOfferCardComponent());
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  // --- cardStatus ---

  it('is empty when no campaign', () => {
    expect(component.cardStatus()).toBe('empty');
  });

  it('is active for a current ACTIVE campaign', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-15T12:00:00Z'));
    setInput('campaign', campaign({ status: 'ACTIVE', startsAt: '2026-01-01T00:00:00Z', endsAt: '2036-01-01T00:00:00Z' }));
    expect(component.cardStatus()).toBe('active');
  });

  it('is scheduled when startsAt is in the future', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-15T12:00:00Z'));
    setInput('campaign', campaign({ status: 'ACTIVE', startsAt: '2026-09-01T00:00:00Z', endsAt: '2026-12-31T00:00:00Z' }));
    expect(component.cardStatus()).toBe('scheduled');
  });

  it('is ended when endsAt is in the past for a non-permanent campaign', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-15T12:00:00Z'));
    setInput('campaign', campaign({ status: 'ACTIVE', startsAt: '2026-01-01T00:00:00Z', endsAt: '2026-07-31T23:59:59Z' }));
    expect(component.cardStatus()).toBe('ended');
  });

  it('is paused for a PAUSED campaign', () => {
    setInput('campaign', campaign({ status: 'PAUSED' }));
    expect(component.cardStatus()).toBe('paused');
  });

  it('is inactive for INACTIVE and ARCHIVED campaigns', () => {
    setInput('campaign', campaign({ status: 'INACTIVE' }));
    expect(component.cardStatus()).toBe('inactive');
  });

  // --- statusTone ---

  it('maps active to success tone', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-15T12:00:00Z'));
    setInput('campaign', campaign({ status: 'ACTIVE', startsAt: '2026-01-01T00:00:00Z', endsAt: '2036-01-01T00:00:00Z' }));
    expect(component.statusTone()).toBe('success');
  });

  it('maps empty and paused to warning tone', () => {
    expect(component.statusTone()).toBe('warning');
    setInput('campaign', campaign({ status: 'PAUSED' }));
    expect(component.statusTone()).toBe('warning');
  });

  it('maps ended and inactive to neutral tone', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-15T12:00:00Z'));
    setInput('campaign', campaign({ status: 'ACTIVE', startsAt: '2026-01-01T00:00:00Z', endsAt: '2026-07-31T23:59:59Z' }));
    expect(component.statusTone()).toBe('neutral');
  });

  // --- isPermanent ---

  it('treats a null endsAt as permanent', () => {
    expect(component.isPermanent(campaign({ endsAt: null }))).toBe(true);
  });

  it('treats a long-running campaign as permanent', () => {
    expect(
      component.isPermanent(campaign({ startsAt: '2026-07-01T00:00:00Z', endsAt: '2036-07-01T23:59:59Z' })),
    ).toBe(true);
  });

  it('does not treat a short campaign as permanent', () => {
    expect(
      component.isPermanent(campaign({ startsAt: '2026-07-01T00:00:00Z', endsAt: '2026-12-31T23:59:59Z' })),
    ).toBe(false);
  });

  // --- effectQuantityTiers ---

  it('returns empty array for missing tiers', () => {
    setEffect({});
    expect(component.effectQuantityTiers()).toEqual([]);
  });

  it('parses a single bounded tier', () => {
    setEffect({ quantityTiers: [{ minPaidAmount: 100, maxPaidAmount: 199, quantity: 1 }] });
    expect(component.effectQuantityTiers()).toHaveLength(1);
    expect(component.effectQuantityTiers()[0]).toEqual({ minPaidAmount: 100, maxPaidAmount: 199, quantity: 1 });
  });

  it('parses an open-ended tier with null maxPaidAmount', () => {
    setEffect({ quantityTiers: [{ minPaidAmount: 200, maxPaidAmount: null, quantity: 2 }] });
    expect(component.effectQuantityTiers()[0]?.maxPaidAmount).toBeNull();
  });

  // --- tierRuleLabel ---

  it('formats a bounded tier rule', () => {
    const tier = { minPaidAmount: 100, maxPaidAmount: 199, quantity: 1 };
    expect(component.tierRuleLabel(tier)).toBe('100 HTG – 199 HTG → 1 Maryaj gratis');
  });

  it('formats an open-ended tier rule', () => {
    const tier = { minPaidAmount: 200, maxPaidAmount: null, quantity: 2 };
    expect(component.tierRuleLabel(tier)).toBe('200 HTG + → 2 Maryaj gratis');
  });

  // --- inlineWarningKey ---

  it('shows no warning for a normal active campaign', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-15T12:00:00Z'));
    setInput('campaign', campaign({ status: 'ACTIVE', startsAt: '2026-01-01T00:00:00Z', endsAt: '2036-01-01T00:00:00Z' }));
    expect(component.inlineWarningKey()).toBeNull();
  });

  it('shows ended warning when cardStatus is ended', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-15T12:00:00Z'));
    setInput('campaign', campaign({ status: 'ACTIVE', startsAt: '2026-01-01T00:00:00Z', endsAt: '2026-07-31T23:59:59Z' }));
    expect(component.inlineWarningKey()).toBe('admin.maryajGratis.offer.warning.ended');
  });

  it('shows noTiers warning when mode is TIERED but tiers list is empty', () => {
    setInput('campaign', campaign());
    setEffect({ quantityMode: 'TIERED_PAID_AMOUNT', quantityTiers: [] });
    expect(component.inlineWarningKey()).toBe('admin.maryajGratis.offer.warning.noTiers');
  });

  it('shows no warning when TIERED mode has tiers', () => {
    setInput('campaign', campaign());
    setEffect({ quantityMode: 'TIERED_PAID_AMOUNT', quantityTiers: [{ minPaidAmount: 100, maxPaidAmount: null, quantity: 1 }] });
    expect(component.inlineWarningKey()).toBeNull();
  });

  it('renders inline warning block when warning key is set', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-15T12:00:00Z'));

    const fixture = TestBed.createComponent(MaryajOfferCardComponent);
    fixture.componentRef.setInput('campaign', campaign({ status: 'ACTIVE', startsAt: '2026-01-01T00:00:00Z', endsAt: '2026-07-31T23:59:59Z' }));
    fixture.componentRef.setInput('effect', null);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="maryaj-offer-card-warning"]')).toBeTruthy();
  });

  it('renders actionError block when actionError input is set', () => {
    const fixture = TestBed.createComponent(MaryajOfferCardComponent);
    fixture.componentRef.setInput('campaign', campaign());
    fixture.componentRef.setInput('effect', null);
    fixture.componentRef.setInput('actionError', { title: 'Erè', message: 'Echèk koneksyon.' });
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="maryaj-offer-card-error"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="maryaj-offer-card-error"]')?.textContent).toContain('Erè');
  });

  // --- DOM rendering ---

  it('renders empty state when no campaign', () => {
    const fixture = TestBed.createComponent(MaryajOfferCardComponent);
    fixture.componentRef.setInput('campaign', null);
    fixture.componentRef.setInput('effect', null);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="maryaj-gratis-offer-card-empty"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="maryaj-gratis-offer-card-create"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="maryaj-gratis-offer-card-edit"]')).toBeFalsy();
  });

  it('renders edit and pause buttons for an ACTIVE campaign', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-15T12:00:00Z'));

    const fixture = TestBed.createComponent(MaryajOfferCardComponent);
    fixture.componentRef.setInput('campaign', campaign({ status: 'ACTIVE', startsAt: '2026-01-01T00:00:00Z', endsAt: '2036-01-01T00:00:00Z' }));
    fixture.componentRef.setInput('effect', null);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="maryaj-gratis-offer-card-edit"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="maryaj-gratis-offer-card-pause"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="maryaj-gratis-offer-card-activate"]')).toBeFalsy();
  });

  it('renders edit and activate buttons for a PAUSED campaign', () => {
    const fixture = TestBed.createComponent(MaryajOfferCardComponent);
    fixture.componentRef.setInput('campaign', campaign({ status: 'PAUSED' }));
    fixture.componentRef.setInput('effect', null);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="maryaj-gratis-offer-card-activate"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="maryaj-gratis-offer-card-pause"]')).toBeFalsy();
  });

  it('emits edit when Modifye is clicked', () => {
    const fixture = TestBed.createComponent(MaryajOfferCardComponent);
    fixture.componentRef.setInput('campaign', campaign());
    fixture.componentRef.setInput('effect', null);
    fixture.detectChanges();

    const spy = vi.spyOn(fixture.componentInstance.edit, 'emit');
    (fixture.nativeElement.querySelector('[data-testid="maryaj-gratis-offer-card-edit"]') as HTMLButtonElement).click();

    expect(spy).toHaveBeenCalledOnce();
  });

  it('emits pauseCampaign with campaign when Mete an poz is clicked', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-15T12:00:00Z'));

    const c = campaign({ status: 'ACTIVE', startsAt: '2026-01-01T00:00:00Z', endsAt: '2036-01-01T00:00:00Z' });
    const fixture = TestBed.createComponent(MaryajOfferCardComponent);
    fixture.componentRef.setInput('campaign', c);
    fixture.componentRef.setInput('effect', null);
    fixture.detectChanges();

    const spy = vi.spyOn(fixture.componentInstance.pauseCampaign, 'emit');
    (fixture.nativeElement.querySelector('[data-testid="maryaj-gratis-offer-card-pause"]') as HTMLButtonElement).click();

    expect(spy).toHaveBeenCalledWith(c);
  });

  it('emits createOffer when empty state action is clicked', () => {
    const fixture = TestBed.createComponent(MaryajOfferCardComponent);
    fixture.componentRef.setInput('campaign', null);
    fixture.componentRef.setInput('effect', null);
    fixture.detectChanges();

    const spy = vi.spyOn(fixture.componentInstance.createOffer, 'emit');
    (fixture.nativeElement.querySelector('[data-testid="maryaj-gratis-offer-card-create"]') as HTMLButtonElement).click();

    expect(spy).toHaveBeenCalledOnce();
  });

  // --- helpers ---

  function setInput<K extends keyof MaryajOfferCardComponent>(
    key: K,
    value: MaryajOfferCardComponent[K] extends () => infer V ? V : never,
  ): void {
    (component as unknown as Record<K, () => unknown>)[key] = () => value;
  }

  function setEffect(params: Record<string, unknown>): void {
    const effect: PromotionConfigItem = { type: 'FREE_GAME_LINE', params };
    setInput('effect', effect as never);
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
});

const translations = {
  common: {
    edit: 'Modifye',
    activate: 'Aktive',
    not_available: 'Pa disponib',
  },
  admin: {
    maryajGratis: {
      amount: {
        htg: '{{amount}} HTG',
      },
      offer: {
        empty: {
          action: 'Modifye òf la',
          title: 'Òf Maryaj gratis la poko aktive.',
          copy: 'Konfigire òf la pou kliyan yo resevwa Maryaj gratis.',
        },
        pause: 'Mete an poz',
        permanent: 'Pa gen fen',
        status: {
          active: 'Aktif',
          paused: 'An poz',
          inactive: 'Inaktif',
          scheduled: 'Pwograme',
          ended: 'Fini',
          needsSetup: 'Pou aktive',
        },
        tiers: {
          openRange: '{{min}} +',
          range: '{{min}} – {{max}}',
          rule: '{{range}} → {{quantity}} Maryaj gratis',
          title: 'Règ palye',
          empty: 'Okenn règ',
        },
        mode: {
          tieredShort: 'Pa palye',
          perAmountShort: 'Pa montan',
          fixedShort: 'Fiks',
        },
        fixed: {
          rule: '{{quantity}} Maryaj gratis pou chak vant',
        },
        perAmount: {
          rule: '{{quantity}} pou chak {{amount}} (maks {{max}})',
        },
        title: 'Òf gratis la',
        warning: {
          ended: 'Òf sa a fini.',
          noTiers: 'Pa gen règ palye.',
        },
      },
    },
  },
};
