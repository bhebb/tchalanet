import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { beforeEach, describe, expect, it } from 'vitest';

import { ConsoleDrawsTableComponent } from './console-draws-table.component';
import { ConsoleDrawRow } from './console-draws-table.models';
import { ConsoleRowAction } from '../draw-results/console-draw-results-table.models';

function createComponent(): ConsoleDrawsTableComponent {
  TestBed.configureTestingModule({ providers: [provideTranslateService()] });
  const fixture = TestBed.createComponent(ConsoleDrawsTableComponent);
  fixture.componentRef.setInput('rows', []);
  return fixture.componentInstance;
}

function row(overrides: Partial<ConsoleDrawRow> = {}): ConsoleDrawRow {
  return {
    id: 'draw-1',
    title: 'NY-SOIR',
    identity: {},
    statusLabel: 'Ouvert',
    statusTone: 'success',
    ...overrides,
  };
}

function action(id: string, variant: 'button' | 'icon' = 'icon'): ConsoleRowAction {
  return { id, label: id, variant };
}

describe('ConsoleDrawsTableComponent — primary/secondary split', () => {
  let component: ConsoleDrawsTableComponent;

  beforeEach(() => {
    component = createComponent();
  });

  it('treats the first action as the primary action', () => {
    const primary = action('viewDetails', 'button');
    expect(component.primaryAction(row({ actions: [primary, action('lock'), action('cancel')] }))).toBe(primary);
  });

  it('puts every action after the first into the secondary list', () => {
    const [primary, lock, cancel] = [action('viewDetails', 'button'), action('lock'), action('cancel')];
    expect(component.secondaryActions(row({ actions: [primary, lock, cancel] }))).toEqual([lock, cancel]);
  });

  it('returns no primary action and an empty secondary list when there are no actions', () => {
    expect(component.primaryAction(row({ actions: undefined }))).toBeNull();
    expect(component.secondaryActions(row({ actions: undefined }))).toEqual([]);
  });

  it('treats a single action as primary-only, with nothing left for the menu', () => {
    const primary = action('viewDetails', 'button');
    expect(component.primaryAction(row({ actions: [primary] }))).toBe(primary);
    expect(component.secondaryActions(row({ actions: [primary] }))).toEqual([]);
  });
});

describe('ConsoleDrawsTableComponent — local time', () => {
  let component: ConsoleDrawsTableComponent;

  beforeEach(() => {
    component = createComponent();
  });

  it('never surfaces the provider time, only the local one', () => {
    const label = component.localLabel(row({
      identity: {
        localTimeLabel: '19:00',
        localTimezoneLabel: 'America/Port-au-Prince',
        providerTimeLabel: '19:00',
        providerTimezoneLabel: 'America/New_York',
      },
    }));
    expect(label).not.toContain('America/New_York');
    expect(label).toBe('19:00 · America/Port-au-Prince');
  });
});
