import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSort, MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { EMPTY, Subject, catchError, debounceTime, switchMap } from 'rxjs';
import {
  BadgeStatus,
  TchActionButton,
  TchConfirmDialog,
  TchConfirmDialogData,
  TchErrorPanel,
  TchLoading,
  TchStatusBadge,
} from '@tch/ui/components';

import { AdminCrudShellComponent } from '../../../../shared/admin-ui/admin-crud-shell.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import {
  PlatformTenantsApi,
  TenantStatus,
  TenantSummaryView,
} from '../../data-access/platform-tenants-api.service';

type ProblemLike = { title?: string; detail?: string; traceId?: string; errorId?: string; requestId?: string };

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
    ReactiveFormsModule,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminCrudShellComponent,
    TchLoading,
    TchErrorPanel,
    TchStatusBadge,
    TchActionButton,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatSelectModule,
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
  private readonly snackBar = inject(MatSnackBar);
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
  readonly errorTitle = signal<string | null>(null);
  readonly errorDetail = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
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
        this.errorTitle.set(null);
        this.errorDetail.set(null);
        this.traceId.set(null);
        return this.api.listTenants({
          q: snap.get('q') ?? '',
          status: snap.get('status') ?? '',
          sort: snap.get('sort') ?? 'updatedAt,desc',
          page: snapPage,
          size: snapSize,
        }).pipe(
          catchError((err: unknown) => {
            const pd = this.problem(err);
            this.errorTitle.set(pd.title ?? this.translate.instant('platform.tenants.error.load'));
            this.errorDetail.set(pd.detail ?? null);
            this.traceId.set(pd.traceId ?? pd.errorId ?? pd.requestId ?? null);
            this.loading.set(false);
            return EMPTY;
          }),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(res => {
      this.items.set(res.items ?? []);
      this.total.set(res.total ?? res.items?.length ?? 0);
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

    // Filter changes → navigate to page 0 (URL change triggers loadPage above)
    this.filters.valueChanges
      .pipe(debounceTime(500), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.navigateWithFilters());
  }

  resetFilters(): void {
    this.filters.reset({ q: '', status: '', sort: 'updatedAt,desc' });
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
        this.api.activateTenant(this.tenantId(tenant)).subscribe({
          next: () => this.loadPage(),
          error: () => this.snackBar.open(
            this.translate.instant('platform.tenants.error.activate'), 'OK', { duration: 4000 },
          ),
        });
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
        this.api.suspendTenant(this.tenantId(tenant)).subscribe({
          next: () => this.loadPage(),
          error: () => this.snackBar.open(
            this.translate.instant('platform.tenants.error.suspend'), 'OK', { duration: 4000 },
          ),
        });
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
        this.api.reactivateTenant(this.tenantId(tenant)).subscribe({
          next: () => this.loadPage(),
          error: () => this.snackBar.open(
            this.translate.instant('platform.tenants.error.reactivate'), 'OK', { duration: 4000 },
          ),
        });
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
        this.api.archiveTenant(this.tenantId(tenant)).subscribe({
          next: () => this.loadPage(),
          error: () => this.snackBar.open(
            this.translate.instant('platform.tenants.error.archive'), 'OK', { duration: 4000 },
          ),
        });
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

  private problem(err: unknown): ProblemLike {
    return ((err as { error?: ProblemLike })?.error ?? {}) as ProblemLike;
  }
}
