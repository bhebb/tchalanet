import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, ParamMap, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { PageModelApi, PageModelComponent, PageRuntimeResponse } from '@tch/page-model';
import {
  RuntimeSettingsStore,
  TENANT_DASHBOARD_SETTINGS_WIDGET_KEY,
  TenantDashboardSettingsInput,
} from '@tch/shared-config';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { Subject, catchError, combineLatest, map, of, startWith, switchMap, tap } from 'rxjs';

interface DashboardQuery {
  readonly period?: string;
  readonly performancePage?: number;
}

interface DashboardQuickAction {
  readonly id: string;
  readonly labelKey: string;
  readonly icon: string;
  readonly route: string;
  readonly queryParams?: Record<string, string>;
}

type DashboardState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly response: PageRuntimeResponse };

@Component({
  selector: 'tch-admin-dashboard-page',
  imports: [RouterLink, PageModelComponent, TchErrorPanel, TchLoading, TranslatePipe],
  templateUrl: './admin-dashboard.page.html',
  styleUrl: './admin-dashboard.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboardPage {
  private readonly pageModelApi = inject(PageModelApi);
  private readonly runtimeSettings = inject(RuntimeSettingsStore);
  private readonly route = inject(ActivatedRoute);
  private readonly reloads = new Subject<void>();

  protected readonly quickActions: readonly DashboardQuickAction[] = [
    {
      id: 'block-number',
      labelKey: 'dashboard.admin.quick_actions.block_number',
      icon: 'pin',
      route: '/app/admin/limits/number',
    },
    {
      id: 'draws',
      labelKey: 'dashboard.admin.quick_actions.draws',
      icon: 'event',
      route: '/app/admin/draws',
      queryParams: { status: 'OPEN' },
    },
    {
      id: 'sellers',
      labelKey: 'dashboard.admin.quick_actions.sellers',
      icon: 'groups',
      route: '/app/admin/seller-terminals',
    },
    {
      id: 'sell-ticket',
      labelKey: 'dashboard.admin.quick_actions.sell_ticket',
      icon: 'point_of_sale',
      route: '/app/admin/tickets/sell',
    },
    {
      id: 'verify-ticket',
      labelKey: 'dashboard.admin.quick_actions.verify_ticket',
      icon: 'fact_check',
      route: '/app/admin/tickets/verify',
    },
    {
      id: 'reports',
      labelKey: 'dashboard.admin.quick_actions.reports',
      icon: 'analytics',
      route: '/app/admin/reports/daily',
    },
  ];

  protected readonly state = toSignal(
    combineLatest([
      this.route.queryParamMap.pipe(map(readDashboardQuery)),
      this.reloads.pipe(startWith(undefined)),
    ]).pipe(
      map(([query]) => query),
      switchMap(query =>
        this.pageModelApi.getTenantPage(undefined, query.period, query.performancePage).pipe(
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

function readDashboardQuery(params: ParamMap): DashboardQuery {
  const performancePage = Number.parseInt(params.get('performancePage') ?? '', 10);
  return {
    period: params.get('period') ?? undefined,
    performancePage: Number.isFinite(performancePage) && performancePage >= 0
      ? performancePage
      : undefined,
  };
}
