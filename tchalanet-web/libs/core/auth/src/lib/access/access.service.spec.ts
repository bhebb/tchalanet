import { TestBed } from '@angular/core/testing';

import { AuthSessionService } from '../auth-session.service';
import { EntitlementsStore } from '../entitlement';
import { FeatureFlags } from '@tch/shared-config';
import { AccessService } from './access.service';

describe('AccessService', () => {
  function setup(opts: {
    flags?: Record<string, boolean>;
    entitled?: string[];
    roles?: string[];
    permissions?: string[];
  } = {}) {
    const flags = opts.flags ?? {};
    const entitled = new Set(opts.entitled ?? []);
    const roles = new Set(opts.roles ?? []);
    const permissions = new Set(opts.permissions ?? []);

    TestBed.configureTestingModule({
      providers: [
        AccessService,
        { provide: FeatureFlags, useValue: { isEnabled: (k: string, d = false) => flags[k] ?? d } },
        { provide: EntitlementsStore, useValue: { has: (k: string) => entitled.has(k) } },
        {
          provide: AuthSessionService,
          useValue: {
            hasRole: (role: string) => roles.has(role),
            hasPermission: (permission: string) => permissions.has(permission.toLowerCase()),
          },
        },
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

  it('gates on role and permission requirements', () => {
    const access = setup({
      roles: ['SUPER_ADMIN'],
      permissions: ['draw-results.manual'],
    });

    expect(access.can({ role: 'SUPER_ADMIN' })).toBe(true);
    expect(access.can({ role: 'TENANT_ADMIN' })).toBe(false);
    expect(access.can({ permission: 'draw-results.manual' })).toBe(true);
    expect(access.can({ permission: 'draw-results.override' })).toBe(false);
  });

  it('allows any requirement when an array is provided', () => {
    const access = setup({ roles: ['SUPER_ADMIN'] });

    expect(access.can([{ permission: 'missing' }, { role: 'SUPER_ADMIN' }])).toBe(true);
    expect(access.can([{ permission: 'missing' }, { role: 'TENANT_ADMIN' }])).toBe(false);
  });
});
