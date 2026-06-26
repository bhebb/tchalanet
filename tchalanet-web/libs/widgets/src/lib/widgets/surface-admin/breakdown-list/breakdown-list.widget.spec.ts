import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { WidgetConfig } from '@tch/page-model';

import { BreakdownListWidget } from './breakdown-list.widget';

describe('BreakdownListWidget', () => {
  function component(config: WidgetConfig, dynamic: unknown) {
    TestBed.configureTestingModule({
      imports: [BreakdownListWidget],
      providers: [provideTranslateService()],
    });
    const fixture = TestBed.createComponent(BreakdownListWidget);
    fixture.componentRef.setInput('config', config);
    fixture.componentRef.setInput('dynamic', dynamic);
    return fixture.componentInstance;
  }

  it('reads dynamic breakdown items, parses decimal values and filters zero rows', () => {
    const cmp = component(
      {
        type: 'BreakdownListWidget',
        props: {
          items: { source: 'dynamic', path: 'gameBreakdown' },
          labelPath: 'gameLabel',
          valuePath: 'grossSales',
          valueFormat: 'currency',
          currencyCode: 'HTG',
        },
      },
      {
        gameBreakdown: [
          { gameCode: 'BOLET', gameLabel: 'Borlette', grossSales: '1250.75' },
          { gameCode: 'LOTO', gameLabel: 'Loto', grossSales: 0 },
          { gameCode: 'MARYAJ', gameLabel: 'Maryaj', grossSales: 250 },
        ],
      },
    );

    expect(cmp.valueFormat()).toBe('currency');
    expect(cmp.currencyCode()).toBe('HTG');
    expect(cmp.items()).toEqual([
      { id: 'Borlette', label: 'Borlette', value: 1250.75 },
      { id: 'Maryaj', label: 'Maryaj', value: 250 },
    ]);
    expect(cmp.total()).toBe(1500.75);
    expect(cmp.percentage(cmp.items()[0])).toBe(83);
  });

  it('returns an empty item list when the dynamic path is missing', () => {
    const cmp = component(
      { type: 'BreakdownListWidget', props: { items: { source: 'dynamic', path: 'missing' } } },
      { items: [] },
    );

    expect(cmp.items()).toEqual([]);
    expect(cmp.total()).toBe(0);
  });
});
