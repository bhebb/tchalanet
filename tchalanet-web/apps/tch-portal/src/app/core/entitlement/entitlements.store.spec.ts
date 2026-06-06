import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { RuntimeSettings } from '@tch/shared-config';
import { RuntimeSettingsStore } from '@tch/shared-config';
import { EntitlementsStore } from './entitlements.store';

describe('EntitlementsStore', () => {
  const settingsState = signal<RuntimeSettings>({ featureFlags: {}, values: {} });

  function setup() {
    TestBed.configureTestingModule({
      providers: [
        EntitlementsStore,
        { provide: RuntimeSettingsStore, useValue: { settings: settingsState.asReadonly() } },
      ],
    });
    return TestBed.inject(EntitlementsStore);
  }

  it('grants entitlements present and true under the entitlement.* namespace', () => {
    settingsState.set({
      featureFlags: {},
      values: { 'entitlement.payouts': true, 'entitlement.reports': false, 'feature.x': true },
    });
    const store = setup();

    expect(store.has('payouts')).toBe(true);
    expect(store.has('entitlement.payouts')).toBe(true);
    expect(store.has('reports')).toBe(false);
    expect(store.has('x')).toBe(false);
  });

  it('reacts to settings updates', () => {
    settingsState.set({ featureFlags: {}, values: {} });
    const store = setup();
    expect(store.has('payouts')).toBe(false);

    settingsState.set({ featureFlags: {}, values: { 'entitlement.payouts': true } });
    expect(store.has('payouts')).toBe(true);
  });
});
