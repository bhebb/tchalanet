import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatSort, MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { EMPTY, Observable, Subject, catchError, switchMap } from 'rxjs';
import {
  AdminListStatusOption,
  AdminListSurface,
  BadgeStatus,
  TchActionButton,
  TchConfirmDialog,
  TchConfirmDialogData,
  TchErrorPanel,
  TchLoading,
  TchSectionError,
  TchStatusBadge,
} from '@tch/ui/components';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';

import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import {
  PlatformTenantsApi,
  TenantStatus,
  TenantSummaryView,
} from '../../data-access/platform-tenants-api.service';
import { StartTenantAdminAccessDialog } from '../../../shared/start-tenant-admin-access-dialog';

const PAGE_SIZE = 20;
const STATUS_OPTIONS = ['DRAFT', 'ACTIVE', 'SUSPENDED', 'ARCHIVED'] as const;

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
    TchLoading,
    TchErrorPanel,
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
export class PlatformTenantsPage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly loadTrigger$ = new Subject<void>();

  @ViewChild(MatSort) matSort?: MatSort;

  readonly displayedColumns = ['tenant', 'code', 'type', 'status', 'currency', 'updatedAt', 'actions'];
  readonly statuses = STATUS_OPTIONS;

  readonly filters = this.fb.nonNullable.group({
    q: '',
    status: '',
    sort: 'updatedAt,desc',
  });

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionFeedback = signal<ErrorViewModel | null>(null);
  readonly items = signal<TenantSummaryView[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly size = signal(PAGE_SIZE);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.size())));
  readonly hasActiveFilters = signal(false);

  ngOnInit(): void {
    // Load pipeline: switchMap cancels the in-flight request when a new trigger arrives,
    // preventing a slower earlier response from overwriting a newer filtered result.
    this.loadTrigger$.pipe(
      switchMap(() => {
        // Read directly from the URL snapshot — avoids any form-state timing skew.
        const snap = this.route.snapshot.queryParamMap;
        const snapPage = Math.max(0, Number(snap.get('page') ?? 0) || 0);
        const snapSize = Math.max(1, Number(snap.get('size') ?? PAGE_SIZE) || PAGE_SIZE);
        this.loading.set(true);
        this.error.set(null);
        return this.api.listTenants({
          q: snap.get('q') ?? '',
          status: snap.get('status') ?? '',
          sort: snap.get('sort') ?? 'updatedAt,desc',
          page: snapPage,
          size: snapSize,
        }, { suppressShellFeedback: true }).pipe(
          catchError((err: unknown) => {
            this.error.set(this.errorViewModel(err, 'platform.tenants.list'));
            this.loading.set(false);
            return EMPTY;
          }),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(res => {
        this.items.set(res.items);
        this.total.set(res.totalElements);
      this.loading.set(false);
    });

    // URL → form + load (source of truth for page/filters)
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        this.page.set(Math.max(0, Number(params.get('page') ?? 0) || 0));
        this.size.set(Math.max(1, Number(params.get('size') ?? PAGE_SIZE) || PAGE_SIZE));
        this.filters.setValue(
          {
            q: params.get('q') ?? '',
            status: params.get('status') ?? '',
            sort: params.get('sort') ?? 'updatedAt,desc',
          },
          { emitEvent: false },
        );
        this.hasActiveFilters.set(!!(params.get('q') || params.get('status')));
        this.loadPage();
      });

  }

  resetFilters(): void {
    this.filters.reset({ q: '', status: '', sort: 'updatedAt,desc' }, { emitEvent: false });
    this.navigateWithFilters();
  }

  loadPage(): void {
    this.loadTrigger$.next();
  }

  prevPage(): void {
    if (this.page() > 0) this.goToPage(this.page() - 1);
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) this.goToPage(this.page() + 1);
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
    const data: TchConfirmDialogData = {
      title: this.translate.instant('platform.tenants.action.activate'),
      message: this.translate.instant('platform.tenants.confirm.activate', { name: tenant.name }),
      confirmLabel: this.translate.instant('platform.tenants.action.activate'),
    };
    this.dialog.open(TchConfirmDialog, { data })
      .afterClosed()
      .subscribe(result => {
        if (!result?.confirmed) return;
        this.runTenantAction(
          tenant,
          'platform.tenants.action.activate',
          'platform.tenants.activate',
          id => this.api.activateTenant(id, { suppressShellFeedback: true }),
        );
      });
  }

  suspendTenant(tenant: TenantSummaryView): void {
    const data: TchConfirmDialogData = {
      title: this.translate.instant('platform.tenants.action.suspend'),
      message: this.translate.instant('platform.tenants.confirm.suspend', { name: tenant.name }),
      confirmLabel: this.translate.instant('platform.tenants.action.suspend'),
      destructive: true,
    };
    this.dialog.open(TchConfirmDialog, { data })
      .afterClosed()
      .subscribe(result => {
        if (!result?.confirmed) return;
        this.runTenantAction(
          tenant,
          'platform.tenants.action.suspend',
          'platform.tenants.suspend',
          id => this.api.suspendTenant(id, { suppressShellFeedback: true }),
        );
      });
  }

  reactivateTenant(tenant: TenantSummaryView): void {
    const data: TchConfirmDialogData = {
      title: this.translate.instant('platform.tenants.action.reactivate'),
      message: this.translate.instant('platform.tenants.confirm.reactivate', { name: tenant.name }),
      confirmLabel: this.translate.instant('platform.tenants.action.reactivate'),
    };
    this.dialog.open(TchConfirmDialog, { data })
      .afterClosed()
      .subscribe(result => {
        if (!result?.confirmed) return;
        this.runTenantAction(
          tenant,
          'platform.tenants.action.reactivate',
          'platform.tenants.reactivate',
          id => this.api.reactivateTenant(id, { suppressShellFeedback: true }),
        );
      });
  }

  archiveTenant(tenant: TenantSummaryView): void {
    const data: TchConfirmDialogData = {
      title: this.translate.instant('platform.tenants.action.archive'),
      message: this.translate.instant('platform.tenants.confirm.archive', { name: tenant.name }),
      confirmLabel: this.translate.instant('platform.tenants.action.archive'),
      destructive: true,
      sensitive: true,
      requireReason: true,
      auditLabel: this.translate.instant('platform.tenants.confirm.reasonLabel'),
    };
    this.dialog.open(TchConfirmDialog, { data })
      .afterClosed()
      .subscribe(result => {
        if (!result?.confirmed) return;
        this.runTenantAction(
          tenant,
          'platform.tenants.action.archive',
          'platform.tenants.archive',
          id => this.api.archiveTenant(id, { suppressShellFeedback: true }),
        );
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
    this.filters.patchValue({ q }, { emitEvent: false });
    this.navigateWithFilters();
  }

  onStatusFilter(status: string): void {
    this.filters.patchValue({ status }, { emitEvent: false });
    this.navigateWithFilters();
  }

  onSortChange(sort: Sort): void {
    const sortStr = sort.active && sort.direction ? `${sort.active},${sort.direction}` : 'updatedAt,desc';
    this.filters.patchValue({ sort: sortStr }, { emitEvent: false });
    this.navigateWithFilters();
  }

  /** Parse "field,direction" URL param into MatSort active/direction. */
  parsedSort(): { active: string; direction: 'asc' | 'desc' } {
    const raw = this.filters.controls.sort.value ?? 'updatedAt,desc';
    const [active, dir] = raw.split(',');
    return { active: active ?? 'updatedAt', direction: dir === 'asc' ? 'asc' : 'desc' };
  }

  private navigateWithFilters(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: this.filters.controls.q.value || null,
        status: this.filters.controls.status.value || null,
        sort: this.filters.controls.sort.value || null,
        page: 0,
        size: this.size(),
      },
      queryParamsHandling: 'merge',
    });
  }

  private goToPage(page: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page },
      queryParamsHandling: 'merge',
    });
  }

  private runTenantAction(
    tenant: TenantSummaryView,
    titleKey: string,
    source: string,
    action: (tenantId: string) => Observable<unknown>,
  ): void {
    const id = this.tenantId(tenant);
    if (!id) return;

    this.actionFeedback.set(null);
    action(id).subscribe({
      next: () => {
        this.actionFeedback.set({
          title: this.translate.instant(titleKey),
          message: this.translate.instant('platform.tenants.feedback.updated', { name: tenant.name }),
          severity: 'info',
        });
        this.loadPage();
      },
      error: err => this.actionFeedback.set(this.errorViewModel(err, source)),
    });
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}
