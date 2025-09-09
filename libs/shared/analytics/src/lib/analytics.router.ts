import { inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { AnalyticsService } from './analytics.service';
import { filter } from 'rxjs/operators';

export function setupRouterPageViews() {
  const router = inject(Router);
  const analytics = inject(AnalyticsService);

  router.events
    .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
    .subscribe(e => analytics.pageView(e.urlAfterRedirects));
}
