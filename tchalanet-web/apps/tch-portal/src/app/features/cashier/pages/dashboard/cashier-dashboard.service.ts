import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { CASHIER_DASHBOARD_MOCK } from './cashier-dashboard.mock';
import { CashierDashboardView } from './cashier-dashboard.model';

@Injectable({ providedIn: 'root' })
export class CashierDashboardService {
  load(): Observable<CashierDashboardView> {
    // Backend endpoint not yet available — return local mock data.
    return of(CASHIER_DASHBOARD_MOCK);
  }
}
