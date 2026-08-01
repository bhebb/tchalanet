import { TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';

import { WidgetConfig } from '@tch/page-model';

import { PeriodSelectorWidget } from './period-selector.widget';

describe('PeriodSelectorWidget', () => {
  it('has one selected period even when the current URL also contains other query params', () => {
    TestBed.configureTestingModule({
      imports: [PeriodSelectorWidget],
      providers: [provideTranslateService()],
    });
    const fixture = TestBed.createComponent(PeriodSelectorWidget);
    fixture.componentRef.setInput('config', {
      type: 'PeriodSelectorWidget',
      props: { options: ['TODAY', 'YESTERDAY'] },
    } satisfies WidgetConfig);
    fixture.componentRef.setInput('dynamic', {
      period: 'YESTERDAY',
      fromDate: '2026-07-31',
      toDate: '2026-07-31',
    });

    expect(fixture.componentInstance.selectedPeriod()).toBe('YESTERDAY');
    expect(fixture.componentInstance.options()).toEqual(['TODAY', 'YESTERDAY']);
  });
});
