import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { PLATFORM_DASHBOARD_MOCK } from './platform-dashboard.mock';
import { PlatformDashboardView } from './platform-dashboard.model';

@Injectable({ providedIn: 'root' })
export class PlatformDashboardService {
  load(): Observable<PlatformDashboardView> {
    // Backend endpoint not yet available — return local mock data.
    return of(PLATFORM_DASHBOARD_MOCK);
  }
}
