import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { ADMIN_DASHBOARD_MOCK } from './admin-dashboard.mock';
import { AdminDashboardView } from './admin-dashboard.model';

@Injectable({ providedIn: 'root' })
export class AdminDashboardService {
  load(): Observable<AdminDashboardView> {
    // Backend endpoint not yet available — return local mock data.
    return of(ADMIN_DASHBOARD_MOCK);
  }
}
