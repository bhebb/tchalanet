import '@angular/compiler';
import { createEnvironmentInjector, runInInjectionContext } from '@angular/core';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { AdminListSurface } from './admin-list-surface';

describe(AdminListSurface.name, () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('toggles the collapsible filters panel', () => {
    const injector = createEnvironmentInjector([]);
    const component = runInInjectionContext(injector, () => new AdminListSurface());
    const states: boolean[] = [];
    component.filtersExpandedChange.subscribe(value => states.push(value));

    component.ngOnInit();
    component.toggleFilters();

    expect(component.filtersOpen()).toBe(true);
    expect(states).toEqual([true]);
  });

  it('emits search only after the minimum length or when cleared', () => {
    vi.useFakeTimers();
    const injector = createEnvironmentInjector([]);
    const component = runInInjectionContext(injector, () => new AdminListSurface());
    const searches: string[] = [];
    component.searchChange.subscribe(value => searches.push(value));

    component.ngOnInit();
    component.onSearchInput(inputEvent('ab'));
    vi.advanceTimersByTime(350);
    expect(searches).toEqual([]);

    component.onSearchInput(inputEvent('abc'));
    vi.advanceTimersByTime(350);
    expect(searches).toEqual(['abc']);

    component.onSearchInput(inputEvent(''));
    vi.advanceTimersByTime(350);
    expect(searches).toEqual(['abc', '']);
  });

  it('emits selected status filters', () => {
    const injector = createEnvironmentInjector([]);
    const component = runInInjectionContext(injector, () => new AdminListSurface());
    const statuses: string[] = [];
    component.statusChange.subscribe(value => statuses.push(value));

    component.statusChange.emit('ACTIVE');

    expect(statuses).toEqual(['ACTIVE']);
  });
});

function inputEvent(value: string): Event {
  return { target: { value } } as unknown as Event;
}
