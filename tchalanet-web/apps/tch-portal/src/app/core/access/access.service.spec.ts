import { TestBed } from '@angular/core/testing';

import { EntitlementsStore } from '../entitlement';
import { FeatureFlags } from '../feature';
import { AccessService } from './access.service';

describe('AccessService', () => {
  function setup(opts: { flags?: Record<string, boolean>; entitled?: string[] } = {}) {
    const flags = opts.flags ?? {};
    const entitled = new Set(opts.entitled ?? []);

    TestBed.configureTestingModule({
      providers: [
        AccessService,
        { provide: FeatureFlags, useValue: { isEnabled: (k: string, d = false) => flags[k] ?? d } },
        { provide: EntitlementsStore, useValue: { has: (k: string) => entitled.has(k) } },
      ],
    });
    return TestBed.inject(AccessService);
  }

  it('allows when both feature and entitlement pass', () => {
    const access = setup({ flags: { 'web.x': true }, entitled: ['payouts'] });
    expect(access.can({ feature: 'web.x', entitlement: 'payouts' })).toBe(true);
  });

  it('denies when the feature is off even if entitled', () => {
    const access = setup({ flags: { 'web.x': false }, entitled: ['payouts'] });
    expect(access.can({ feature: 'web.x', entitlement: 'payouts' })).toBe(false);
  });

  it('denies when the entitlement is missing even if the feature is on', () => {
    const access = setup({ flags: { 'web.x': true }, entitled: [] });
    expect(access.can({ feature: 'web.x', entitlement: 'payouts' })).toBe(false);
  });

  it('gates on feature alone when no entitlement is required', () => {
    const access = setup({ flags: { 'web.x': true } });
    expect(access.can({ feature: 'web.x' })).toBe(true);
  });

  it('gates on entitlement alone when no feature is required', () => {
    const access = setup({ entitled: ['payouts'] });
    expect(access.can({ entitlement: 'payouts' })).toBe(true);
    expect(access.can({ entitlement: 'missing' })).toBe(false);
  });

  it('honours the feature default when the flag is unresolved', () => {
    const access = setup();
    expect(access.can({ feature: 'web.x', featureDefault: true })).toBe(true);
    expect(access.can({ feature: 'web.x' })).toBe(false);
  });
});
