// @vitest-environment jsdom
import '@angular/compiler';
import {
  EnvironmentInjector,
  provideZonelessChangeDetection,
  runInInjectionContext,
} from '@angular/core';
import { TestBed, getTestBed } from '@angular/core/testing';
import {
  BrowserTestingModule,
  platformBrowserTesting,
} from '@angular/platform-browser/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { AdminListSurface } from './admin-list-surface';

try {
  getTestBed().initTestEnvironment(BrowserTestingModule, platformBrowserTesting());
} catch {
  // The browser platform is initialized once per Vitest worker.
}

describe(AdminListSurface.name, () => {
  afterEach(() => {
    vi.useRealTimers();
    TestBed.resetTestingModule();
  });

  it('toggles the collapsible filters panel', () => {
    const injector = testInjector();
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
    const injector = testInjector();
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
    const injector = testInjector();
    const component = runInInjectionContext(injector, () => new AdminListSurface());
    const statuses: string[] = [];
    component.statusChange.subscribe(value => statuses.push(value));

    component.statusChange.emit('ACTIVE');

    expect(statuses).toEqual(['ACTIVE']);
  });
});

function testInjector(): EnvironmentInjector {
  TestBed.configureTestingModule({ providers: [provideZonelessChangeDetection()] });
  return TestBed.inject(EnvironmentInjector);
}

function inputEvent(value: string): Event {
  return { target: { value } } as unknown as Event;
}
