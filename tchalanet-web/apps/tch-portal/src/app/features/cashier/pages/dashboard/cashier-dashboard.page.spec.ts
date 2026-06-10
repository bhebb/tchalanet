import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { CashierDashboardService } from './cashier-dashboard.service';
import { CashierDashboardPage } from './cashier-dashboard.page';
import { CASHIER_DASHBOARD_MOCK } from './cashier-dashboard.mock';
import { CashierDashboardView } from './cashier-dashboard.model';

const BLOCKED_MOCK: CashierDashboardView = {
  ...CASHIER_DASHBOARD_MOCK,
  session: { ...CASHIER_DASHBOARD_MOCK.session, status: 'BLOCKED' },
};

describe('CashierDashboardPage', () => {
  function setup(mockData: CashierDashboardView = CASHIER_DASHBOARD_MOCK, shouldError = false) {
    const svc = {
      load: vi.fn().mockReturnValue(shouldError ? throwError(() => new Error('err')) : of(mockData)),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideTranslateService(),
        { provide: CashierDashboardService, useValue: svc },
      ],
    });
    const fixture = TestBed.createComponent(CashierDashboardPage);
    return { fixture, cmp: fixture.componentInstance, svc };
  }

  it('starts in loading state', () => {
    const { cmp } = setup();
    expect(cmp.state()).toBe('loading');
  });

  it('transitions to ready state after successful load', () => {
    const { fixture, cmp } = setup();
    fixture.detectChanges();
    expect(cmp.state()).toBe('ready');
  });

  it('sets vm with mock seller data', () => {
    const { fixture, cmp } = setup();
    fixture.detectChanges();
    expect(cmp.vm()?.seller?.displayName).toBe(CASHIER_DASHBOARD_MOCK.seller.displayName);
  });

  it('transitions to blocked state when session status is BLOCKED', () => {
    const { fixture, cmp } = setup(BLOCKED_MOCK);
    fixture.detectChanges();
    expect(cmp.state()).toBe('blocked');
  });

  it('transitions to error state on load failure', () => {
    const { fixture, cmp } = setup(CASHIER_DASHBOARD_MOCK, true);
    fixture.detectChanges();
    expect(cmp.state()).toBe('error');
  });

  it('calls service load on init', () => {
    const { fixture, svc } = setup();
    fixture.detectChanges();
    expect(svc.load).toHaveBeenCalledOnce();
  });
});
