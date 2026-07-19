import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { PageModelApi, PageModelComponent, PageRuntimeResponse } from '@tch/page-model';
import {
  RuntimeSettingsStore,
  TENANT_DASHBOARD_SETTINGS_WIDGET_KEY,
  TenantDashboardSettingsInput,
} from '@tch/shared-config';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { Subject, catchError, map, of, startWith, switchMap, tap } from 'rxjs';

type DashboardState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly response: PageRuntimeResponse };

@Component({
  selector: 'tch-admin-dashboard-page',
  imports: [PageModelComponent, TchErrorPanel, TchLoading, TranslatePipe],
  templateUrl: './admin-dashboard.page.html',
  styleUrl: './admin-dashboard.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardPage {
  private readonly pageModelApi = inject(PageModelApi);
  private readonly runtimeSettings = inject(RuntimeSettingsStore);
  private readonly reloads = new Subject<void>();

  protected readonly state = toSignal(
    this.reloads.pipe(
      startWith(undefined),
      switchMap(() =>
        this.pageModelApi.getTenantPage().pipe(
          tap(response => {
            this.runtimeSettings.applyTenantDashboardSettings(tenantSettingsFrom(response));
          }),
          map(response => ({ status: 'ready', response }) as DashboardState),
          catchError(() =>
            this.pageModelApi.getPrivateFallbackPage().pipe(
              map(response => ({ status: 'ready', response }) as DashboardState),
              catchError(() => of({ status: 'error' } as DashboardState)),
            ),
          ),
          startWith({ status: 'loading' } as DashboardState),
        ),
      ),
    ),
    { initialValue: { status: 'loading' } as DashboardState },
  );

  protected reload(): void {
    this.reloads.next();
  }
}

function tenantSettingsFrom(response: PageRuntimeResponse): TenantDashboardSettingsInput | null {
  const value = response.dynamic.widgets[TENANT_DASHBOARD_SETTINGS_WIDGET_KEY];
  if (!isTenantDashboardSettings(value)) {
    return null;
  }
  return value;
}

function isTenantDashboardSettings(value: unknown): value is TenantDashboardSettingsInput {
  return typeof value === 'object' && value !== null;
}
