import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  AdminListStatusOption,
  AdminListSurface,
  BadgeStatus,
  TchActionButton,
  TchConfirmDialog,
  TchConfirmDialogData,
  TchSectionError,
  TchStatusBadge,
} from '@tch/ui/components';
import {
  AdminEmptyStateComponent,
  AdminPageShellComponent,
  TchPaginationComponent,
} from '@tch/ui/console';
import {
  numberParam,
  resourceErrorVm,
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  tchMutation,
} from '@tch/web/async';
import { TchErrorViewModel } from '@tch/web/errors';
import { TchPage } from '@tch/api';

import {
  PlatformTenantsApi,
  TenantSummaryView,
} from '../../data-access/platform-tenants-api.service';
import { TenantStatus } from '../../data-access/platform-tenant-contracts';
import { StartTenantAdminAccessDialog } from '../../../shared/start-tenant-admin-access/start-tenant-admin-access-dialog';

const PAGE_SIZE = 20;
const DEFAULT_SORT = 'updatedAt,desc';
const STATUS_OPTIONS = ['DRAFT', 'ACTIVE', 'SUSPENDED', 'ARCHIVED'] as const;

type TenantAction = 'activate' | 'suspend' | 'reactivate' | 'archive';

interface TenantActionInput {
  readonly tenant: TenantSummaryView;
  readonly action: TenantAction;
}

@Component({
  selector: 'tch-platform-tenants-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    RouterLink,
    DatePipe,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminListSurface,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    TchPaginationComponent,
    TchSectionError,
    TchStatusBadge,
    TchActionButton,
    MatButtonModule,
    MatMenuModule,
    MatSortModule,
    MatTableModule,
  ],
  templateUrl: './platform-tenants.page.html',
  styleUrls: ['./platform-tenants.page.scss'],
})
export class PlatformTenantsPage {
  private readonly api = inject(PlatformTenantsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly displayedColumns = ['tenant', 'code', 'type', 'status', 'currency', 'updatedAt', 'actions'];
  readonly statuses = STATUS_OPTIONS;

  readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });
  readonly search = computed(() => this.queryParamMap().get('q')?.trim() ?? '');
  readonly statusFilter = computed(() => this.queryParamMap().get('status') ?? '');
  readonly sort = computed(() => this.queryParamMap().get('sort') ?? DEFAULT_SORT);
  readonly page = computed(() => numberParam(this.queryParamMap().get('page'), 0));
  readonly size = computed(() => numberParam(this.queryParamMap().get('size'), PAGE_SIZE));
  readonly hasActiveFilters = computed(() => !!(this.search() || this.statusFilter()));

  readonly actionSuccess = signal<TchErrorViewModel | null>(null);
  readonly actionFeedback = computed(() => this.actionSuccess() ?? this.tenantAction.feedback()?.vm ?? null);

  readonly tenants = this.api.listTenantsResource(
    () => ({
      q: this.search(),
      status: this.statusFilter(),
      sort: this.sort(),
      page: this.page(),
      size: this.size(),
    }),
    { suppressShellFeedback: true },
  );
  readonly tenantsError = resourceErrorVm(this.tenants, 'platform.tenants.list');
  readonly tenantPage = computed<TchPage<TenantSummaryView> | null>(() => {
    const status = this.tenants.status();
    if (status !== 'resolved' && status !== 'local' && status !== 'reloading') return null;
    return this.tenants.value() ?? null;
  });
  readonly items = computed(() => this.tenantPage()?.items ?? []);
  readonly total = computed(() => this.tenantPage()?.totalElements ?? 0);
  readonly pageIndex = computed(() => this.tenantPage()?.page ?? this.page());
  readonly pageSize = computed(() => this.tenantPage()?.size ?? this.size());

  readonly tenantAction = tchMutation<TenantActionInput, unknown>({
    source: 'platform.tenants.action',
    run: input => {
      const id = this.tenantId(input.tenant);
      switch (input.action) {
        case 'activate':
          return this.api.activateTenant(id, { suppressShellFeedback: true });
        case 'suspend':
          return this.api.suspendTenant(id, { suppressShellFeedback: true });
        case 'reactivate':
          return this.api.reactivateTenant(id, { suppressShellFeedback: true });
        case 'archive':
          return this.api.archiveTenant(id, { suppressShellFeedback: true });
      }
    },
    onSuccess: (_result, input) => {
      this.actionSuccess.set({
        title: this.translate.instant(`platform.tenants.action.${input.action}`),
        message: this.translate.instant('platform.tenants.feedback.updated', { name: input.tenant.name }),
        severity: 'info',
      });
      this.tenants.reload();
    },
    onError: () => {
      this.actionSuccess.set(null);
    },
  });

  load(): void {
    this.tenants.reload();
  }

  resetFilters(): void {
    this.navigateWithFilters({ q: null, status: null, sort: null, page: null });
  }

  prevPage(): void {
    this.onPageChange(this.pageIndex() - 1);
  }

  nextPage(): void {
    this.onPageChange(this.pageIndex() + 1);
  }

  onPageChange(page: number): void {
    this.navigateWithFilters({ page: page > 0 ? page : null });
  }

  onSizeChange(size: number): void {
    this.navigateWithFilters({ size: size !== PAGE_SIZE ? size : null, page: null });
  }

  tenantId(tenant: TenantSummaryView): string {
    return tenant.tenantId ?? tenant.id ?? '';
  }

  typeLabel(type: string | null | undefined): string {
    if (!type) return '—';
    const map: Record<string, string> = {
      BORLETTE: 'platform.tenants.type.borlette',
      RESEAU: 'platform.tenants.type.reseau',
      AMBULANT: 'platform.tenants.type.ambulant',
    };
    return this.translate.instant(map[type] ?? type);
  }

  canActivate(tenant: TenantSummaryView): boolean {
    return tenant.status === 'DRAFT';
  }

  canSuspend(tenant: TenantSummaryView): boolean {
    return tenant.status === 'ACTIVE';
  }

  canReactivate(tenant: TenantSummaryView): boolean {
    return tenant.status === 'SUSPENDED' || tenant.status === 'REJECTED';
  }

  canArchive(tenant: TenantSummaryView): boolean {
    return tenant.status !== 'ARCHIVED';
  }

  openSupportAccess(tenant: TenantSummaryView): void {
    this.dialog.open(StartTenantAdminAccessDialog, {
      width: '520px',
      data: {
        tenantId: this.tenantId(tenant),
        tenantName: tenant.name,
        tenantCode: tenant.code,
        tenantStatus: tenant.status,
      },
    });
  }

  activateTenant(tenant: TenantSummaryView): void {
    this.confirmTenantAction(tenant, 'activate');
  }

  suspendTenant(tenant: TenantSummaryView): void {
    this.confirmTenantAction(tenant, 'suspend', { destructive: true });
  }

  reactivateTenant(tenant: TenantSummaryView): void {
    this.confirmTenantAction(tenant, 'reactivate');
  }

  archiveTenant(tenant: TenantSummaryView): void {
    this.confirmTenantAction(tenant, 'archive', {
      destructive: true,
      sensitive: true,
      requireReason: true,
      auditLabel: this.translate.instant('platform.tenants.confirm.reasonLabel'),
    });
  }

  statusBadge(status: TenantStatus): BadgeStatus {
    const map: Record<string, BadgeStatus> = {
      ACTIVE: 'ready',
      DRAFT: 'pending',
      SUSPENDED: 'warning',
      REJECTED: 'blocked',
      ARCHIVED: 'missing',
    };
    return map[status] ?? 'missing';
  }

  statusLabel(status: TenantStatus | string): string {
    return this.translate.instant(`platform.tenants.status.${status.toLowerCase()}`);
  }

  statusFilterOptions(): readonly AdminListStatusOption[] {
    return this.statuses.map(status => ({ value: status, label: this.statusLabel(status) }));
  }

  onSearchFilter(q: string): void {
    this.navigateWithFilters({ q: q.trim() || null, page: null });
  }

  onStatusFilter(status: string): void {
    this.navigateWithFilters({ status: status || null, page: null });
  }

  onSortChange(sort: Sort): void {
    const sortStr = sort.active && sort.direction ? `${sort.active},${sort.direction}` : DEFAULT_SORT;
    this.navigateWithFilters({ sort: sortStr === DEFAULT_SORT ? null : sortStr, page: null });
  }

  parsedSort(): { active: string; direction: 'asc' | 'desc' } {
    const [active, dir] = this.sort().split(',');
    return { active: active || 'updatedAt', direction: dir === 'asc' ? 'asc' : 'desc' };
  }

  private confirmTenantAction(
    tenant: TenantSummaryView,
    action: TenantAction,
    extras: Partial<TchConfirmDialogData> = {},
  ): void {
    const data: TchConfirmDialogData = {
      title: this.translate.instant(`platform.tenants.action.${action}`),
      message: this.translate.instant(`platform.tenants.confirm.${action}`, { name: tenant.name }),
      confirmLabel: this.translate.instant(`platform.tenants.action.${action}`),
      ...extras,
    };
    this.dialog.open(TchConfirmDialog, { data })
      .afterClosed()
      .subscribe(result => {
        if (!result?.confirmed) return;
        this.tenantAction.execute({ tenant, action }, { key: this.tenantId(tenant) });
      });
  }

  private navigateWithFilters(params: {
    readonly q?: string | null;
    readonly status?: string | null;
    readonly sort?: string | null;
    readonly page?: number | null;
    readonly size?: number | null;
  }): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: params,
      queryParamsHandling: 'merge',
    });
  }
}
