import '@angular/compiler';
import { createEnvironmentInjector, runInInjectionContext } from '@angular/core';
import { of } from 'rxjs';
import { describe, expect, it } from 'vitest';

import { TchSearchOption, TchSearchSelect } from './search-select';

describe(TchSearchSelect.name, () => {
  it('clears the selected value', () => {
    const injector = createEnvironmentInjector([]);
    const component = runInInjectionContext(injector, () => new TchSearchSelect());
    const option: TchSearchOption = { id: 'tenant-1', title: 'Tenant A' };

    component.writeValue(option);
    expect(component.selected()).toEqual(option);

    component.clear();
    expect(component.selected()).toBeNull();
    expect(component.query.value).toBe('');
  });

  it('stores selected option and clears result options', () => {
    const injector = createEnvironmentInjector([]);
    const component = runInInjectionContext(injector, () => new TchSearchSelect());
    const option: TchSearchOption = { id: 'admin-1', title: 'Admin A' };

    component.options.set([option]);
    component.select(option);

    expect(component.selected()).toEqual(option);
    expect(component.query.value).toBe('Admin A');
    expect(component.options()).toEqual([]);
  });

  it('accepts a server-side search function', () => {
    const injector = createEnvironmentInjector([]);
    const component = runInInjectionContext(injector, () => new TchSearchSelect());

    expect(component.searchFn()).toBeTruthy();
    expect(of([{ id: 'x', title: 'Result' }])).toBeTruthy();
  });
});
