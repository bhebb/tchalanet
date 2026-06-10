import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';

import { AdminDashboardService } from './admin-dashboard.service';
import { AdminDashboardPage } from './admin-dashboard.page';
import { ADMIN_DASHBOARD_MOCK } from './admin-dashboard.mock';

describe('AdminDashboardPage', () => {
  function setup(mockData = ADMIN_DASHBOARD_MOCK, shouldError = false) {
    const svc = {
      load: vi.fn().mockReturnValue(shouldError ? throwError(() => new Error('err')) : of(mockData)),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideTranslateService(),
        { provide: AdminDashboardService, useValue: svc },
      ],
    });
    const fixture = TestBed.createComponent(AdminDashboardPage);
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

  it('sets vm with mock kpi data', () => {
    const { fixture, cmp } = setup();
    fixture.detectChanges();
    expect(cmp.vm()?.kpis?.sellers).toBe(ADMIN_DASHBOARD_MOCK.kpis.sellers);
  });

  it('transitions to error state on load failure', () => {
    const { fixture, cmp } = setup(ADMIN_DASHBOARD_MOCK, true);
    fixture.detectChanges();
    expect(cmp.state()).toBe('error');
  });

  it('maps severity to tone correctly', () => {
    const { cmp } = setup();
    expect(cmp.severityTone('ERROR')).toBe('danger');
    expect(cmp.severityTone('WARN')).toBe('warning');
    expect(cmp.severityTone('INFO')).toBe('info');
  });
});
