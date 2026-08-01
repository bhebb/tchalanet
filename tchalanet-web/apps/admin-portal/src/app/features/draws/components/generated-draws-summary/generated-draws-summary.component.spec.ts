import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { GeneratedDrawsSummaryComponent } from './generated-draws-summary.component';

describe('GeneratedDrawsSummaryComponent', () => {
  it('renders the exact backend summary values', () => {
    const fixture = TestBed.createComponent(GeneratedDrawsSummaryComponent);

    fixture.componentRef.setInput('summary', {
      todayCount: 28,
      salesOpenCount: 16,
      expectedCount: 20,
      confirmedCount: 20,
    });

    expect(fixture.componentInstance.todayValue()).toBe('28');
    expect(fixture.componentInstance.salesOpenValue()).toBe('16');
    expect(fixture.componentInstance.expectedValue()).toBe('20');
    expect(fixture.componentInstance.confirmedValue()).toBe('20');
  });

  it('uses a neutral placeholder while the summary is loading', () => {
    const fixture = TestBed.createComponent(GeneratedDrawsSummaryComponent);

    expect(fixture.componentInstance.todayValue()).toBe('—');
    expect(fixture.componentInstance.salesOpenValue()).toBe('—');
  });
});
