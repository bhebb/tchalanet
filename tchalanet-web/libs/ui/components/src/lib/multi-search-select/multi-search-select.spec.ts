import '@angular/compiler';
import { createEnvironmentInjector, runInInjectionContext } from '@angular/core';
import { describe, expect, it } from 'vitest';

import { TchSearchOption } from '../search-select/search-select';
import { TchMultiSearchSelect } from './multi-search-select';

describe(TchMultiSearchSelect.name, () => {
  it('adds and removes selected chips', () => {
    const injector = createEnvironmentInjector([]);
    const component = runInInjectionContext(injector, () => new TchMultiSearchSelect());
    const option: TchSearchOption = { id: 'seller-1', title: 'Seller A' };

    component.add(option);
    expect(component.selected()).toEqual([option]);

    component.remove(option);
    expect(component.selected()).toEqual([]);
  });

  it('blocks duplicate selections', () => {
    const injector = createEnvironmentInjector([]);
    const component = runInInjectionContext(injector, () => new TchMultiSearchSelect());
    const option: TchSearchOption = { id: 'admin-1', title: 'Admin A' };

    component.add(option);
    component.add(option);

    expect(component.selected()).toEqual([option]);
  });

  it('does not add disabled options', () => {
    const injector = createEnvironmentInjector([]);
    const component = runInInjectionContext(injector, () => new TchMultiSearchSelect());

    component.add({ id: 'blocked-1', title: 'Blocked', disabled: true });

    expect(component.selected()).toEqual([]);
  });
});
