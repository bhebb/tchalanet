import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { PlatformDashboardService } from './platform-dashboard.service';
import { PlatformDashboardPage } from './platform-dashboard.page';
import { PLATFORM_DASHBOARD_MOCK } from './platform-dashboard.mock';

describe('PlatformDashboardPage', () => {
  function setup(mockData = PLATFORM_DASHBOARD_MOCK, shouldError = false) {
    const svc = {
      load: vi.fn().mockReturnValue(shouldError ? throwError(() => new Error('err')) : of(mockData)),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideTranslateService(),
        { provide: PlatformDashboardService, useValue: svc },
      ],
    });
    const fixture = TestBed.createComponent(PlatformDashboardPage);
    return { fixture, cmp: fixture.componentInstance, svc };
  }

  it('starts in loading state', () => {
    const { cmp } = setup();
    expect(cmp.state()).toBe('loading');
  });

  it('transitions to ready state after load', () => {
    const { fixture, cmp } = setup();
    fixture.detectChanges();
    expect(cmp.state()).toBe('ready');
  });

  it('sets vm with mock data after load', () => {
    const { fixture, cmp } = setup();
    fixture.detectChanges();
    expect(cmp.vm()?.kpis?.activeTenants).toBe(PLATFORM_DASHBOARD_MOCK.kpis.activeTenants);
  });

  it('transitions to error state on load failure', () => {
    const { fixture, cmp } = setup(PLATFORM_DASHBOARD_MOCK, true);
    fixture.detectChanges();
    expect(cmp.state()).toBe('error');
  });

  it('calls service load on init', () => {
    const { fixture, svc } = setup();
    fixture.detectChanges();
    expect(svc.load).toHaveBeenCalledOnce();
  });
});
