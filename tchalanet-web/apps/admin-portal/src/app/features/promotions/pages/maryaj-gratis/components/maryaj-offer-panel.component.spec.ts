import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { MaryajOfferPanelComponent } from './maryaj-offer-panel.component';

describe(MaryajOfferPanelComponent.name, () => {
  let formBuilder: FormBuilder;
  let component: MaryajOfferPanelComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        FormBuilder,
        { provide: TranslateService, useValue: { instant: (key: string) => key } },
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

  function form() {
    return formBuilder.group({
      quantityTiers: formBuilder.array([
        formBuilder.group({ minPaidAmount: 100, maxPaidAmount: 199, quantity: 1 }),
        formBuilder.group({ minPaidAmount: 200, maxPaidAmount: null, quantity: 2 }),
      ]),
    });
  }
});
