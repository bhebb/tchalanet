import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatSort, MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { EMPTY, Observable, Subject, catchError, switchMap } from 'rxjs';
import {
  AdminListSurface,
  BadgeStatus,
  TchConfirmDialog,
  TchConfirmDialogData,
  TchErrorPanel,
  TchLoading,
  TchSectionError,
  TchStatusBadge,
} from '@tch/ui/components';

import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { ProblemDetail, TchBackendClient, webAppErrorFromProblemDetail } from '@tch/api';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import type { TenantAdminGlobalRow, TenantAdminGlobalPage } from '../../data-access/platform-tenant-admins.models';
import { IdentityUserCrudApi } from '../../../shared/identity-user-crud-api.service';

const PAGE_SIZE = 20;

@Component({
  selector: 'tch-platform-tenant-admins-global-page',
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
    MatButtonModule,
    MatMenuModule,
    MatSortModule,
    MatTableModule,
  ],
  templateUrl: './platform-tenant-admins-global.page.html',
  styleUrls: ['./platform-tenant-admins-global.page.scss'],
})
export class PlatformTenantAdminsGlobalPage implements OnInit {
  private readonly backend = inject(TchBackendClient);
  private readonly identityApi = inject(IdentityUserCrudApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild(MatSort) matSort?: MatSort;

  readonly loadTrigger$ = new Subject<void>();
  readonly displayedColumns = ['user', 'tenant', 'status', 'assignedAt', 'actions'];

  readonly searchQuery = signal('');
  readonly sort = signal('displayName,asc');

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<{ title: string; message: string } | null>(null);
  readonly items = signal<TenantAdminGlobalRow[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly size = signal(PAGE_SIZE);
  readonly totalPages = signal(1);
  readonly hasNext = signal(false);
  readonly hasPrevious = signal(false);
  readonly hasActiveFilters = signal(false);

  ngOnInit(): void {
    this.loadTrigger$.pipe(
      switchMap(() => {
        const snap = this.route.snapshot.queryParamMap;
        const snapPage = Math.max(0, Number(snap.get('page') ?? 0) || 0);
        this.loading.set(true);
        this.error.set(null);
        this.actionNotice.set(null);
        return this.search(snap.get('q') ?? '', snap.get('sort') ?? 'displayName,asc', snapPage).pipe(
          catchError((err: unknown) => {
            this.error.set(this.errorViewModel(err, 'platform.tenantAdmins.list'));
            this.loading.set(false);
            return EMPTY;
          }),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(res => {
      this.items.set(res.items ?? []);
      this.total.set(res.totalElements ?? 0);
      this.page.set(res.page);
      this.size.set(res.size);
      this.totalPages.set(res.totalPages || 1);
      this.hasNext.set(res.hasNext ?? false);
      this.hasPrevious.set(res.hasPrevious ?? false);
      this.loading.set(false);
    });

    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        this.page.set(Math.max(0, Number(params.get('page') ?? 0) || 0));
        this.searchQuery.set(params.get('q') ?? '');
        this.sort.set(params.get('sort') ?? 'displayName,asc');
        this.hasActiveFilters.set(!!params.get('q'));
        this.loadTrigger$.next();
      });
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.sort.set('displayName,asc');
    this.navigateWithFilters();
  }

  onSearchFilter(q: string): void {
    this.searchQuery.set(q);
    this.navigateWithFilters();
  }

  onSortChange(sort: Sort): void {
    const sortStr = sort.active && sort.direction ? `${sort.active},${sort.direction}` : 'displayName,asc';
    this.sort.set(sortStr);
    this.navigateWithFilters();
  }

  parsedSort(): { active: string; direction: 'asc' | 'desc' } {
    const raw = this.sort() || 'displayName,asc';
    const [active, dir] = raw.split(',');
    return { active: active ?? 'displayName', direction: dir === 'desc' ? 'desc' : 'asc' };
  }

  prevPage(): void {
    if (this.hasPrevious()) this.goToPage(this.page() - 1);
  }

  nextPage(): void {
    if (this.hasNext()) this.goToPage(this.page() + 1);
  }

  statusBadge(status: string): BadgeStatus {
    const map: Record<string, BadgeStatus> = {
      ACTIVE: 'ready', PENDING: 'pending', SUSPENDED: 'warning', INACTIVE: 'missing',
    };
    return map[status] ?? 'missing';
  }

  statusLabel(status: string): string {
    return this.translate.instant(`platform.tenantAdmins.status.${status.toLowerCase()}`) || status;
  }

  canActivate(row: TenantAdminGlobalRow): boolean {
    return row.status === 'SUSPENDED' || row.status === 'INACTIVE' || row.status === 'PENDING';
  }

  canBlock(row: TenantAdminGlobalRow): boolean {
    return row.status === 'ACTIVE' || row.status === 'PENDING';
  }

  canArchive(row: TenantAdminGlobalRow): boolean {
    return row.status !== 'ARCHIVED';
  }

  openDetail(row: TenantAdminGlobalRow): void {
    void this.router.navigate(['/app/platform/tenant-admins', row.id]);
  }

  resetPassword(row: TenantAdminGlobalRow): void {
    const name = row.displayName || row.email || row.id;
    const data: TchConfirmDialogData = {
      title: this.translate.instant('platform.tenantAdmins.action.resetPassword'),
      message: this.translate.instant('platform.tenantAdmins.confirm.resetPassword', { name }),
      confirmLabel: this.translate.instant('platform.tenantAdmins.action.resetPassword'),
    };
    this.dialog.open(TchConfirmDialog, { data })
      .afterClosed()
      .subscribe(result => {
        if (!result?.confirmed) return;
        this.error.set(null);
        this.actionNotice.set(null);
        this.identityApi.resetPassword(row.id, { suppressShellFeedback: true }).subscribe({
          next: ({ tempPassword }) => this.actionNotice.set({
            title: this.translate.instant('platform.tenantAdmins.action.resetPassword'),
            message: tempPassword,
          }),
          error: err => this.error.set(this.errorViewModel(err, 'platform.tenantAdmins.resetPassword')),
        });
      });
  }

  activateUser(row: TenantAdminGlobalRow): void {
    const name = row.displayName || row.email || row.id;
    const data: TchConfirmDialogData = {
      title: this.translate.instant('platform.tenantAdmins.action.activate'),
      message: this.translate.instant('platform.tenantAdmins.confirm.activate', { name }),
      confirmLabel: this.translate.instant('platform.tenantAdmins.action.activate'),
    };
    this.dialog.open(TchConfirmDialog, { data })
      .afterClosed()
      .subscribe(result => {
        if (!result?.confirmed) return;
        this.error.set(null);
        this.actionNotice.set(null);
        this.identityApi.activate(row.id, { suppressShellFeedback: true }).subscribe({
          next: () => this.loadTrigger$.next(),
          error: err => this.error.set(this.errorViewModel(err, 'platform.tenantAdmins.activate')),
        });
      });
  }

  blockUser(row: TenantAdminGlobalRow): void {
    const name = row.displayName || row.email || row.id;
    const data: TchConfirmDialogData = {
      title: this.translate.instant('platform.tenantAdmins.action.block'),
      message: this.translate.instant('platform.tenantAdmins.confirm.block', { name }),
      confirmLabel: this.translate.instant('platform.tenantAdmins.action.block'),
      destructive: true,
    };
    this.dialog.open(TchConfirmDialog, { data })
      .afterClosed()
      .subscribe(result => {
        if (!result?.confirmed) return;
        this.error.set(null);
        this.actionNotice.set(null);
        this.identityApi.suspend(row.id, { suppressShellFeedback: true }).subscribe({
          next: () => this.loadTrigger$.next(),
          error: err => this.error.set(this.errorViewModel(err, 'platform.tenantAdmins.block')),
        });
      });
  }

  archiveUser(row: TenantAdminGlobalRow): void {
    const name = row.displayName || row.email || row.id;
    const data: TchConfirmDialogData = {
      title: this.translate.instant('platform.tenantAdmins.action.archive'),
      message: this.translate.instant('platform.tenantAdmins.confirm.archive', { name }),
      confirmLabel: this.translate.instant('platform.tenantAdmins.action.archive'),
      destructive: true,
      sensitive: true,
    };
    this.dialog.open(TchConfirmDialog, { data })
      .afterClosed()
      .subscribe(result => {
        if (!result?.confirmed) return;
        this.error.set(null);
        this.actionNotice.set(null);
        this.identityApi.archive(row.id, { suppressShellFeedback: true }).subscribe({
          next: () => this.loadTrigger$.next(),
          error: err => this.error.set(this.errorViewModel(err, 'platform.tenantAdmins.archive')),
        });
      });
  }

  private search(q: string, sort: string, page: number): Observable<TenantAdminGlobalPage> {
    const params: Record<string, string> = { page: String(page), size: String(PAGE_SIZE) };
    if (q) params['q'] = q;
    if (sort) params['sort'] = sort;
    const qs = new URLSearchParams(params).toString();
    return this.backend.get<TenantAdminGlobalPage>(
      `/admin/identity/users?${qs}`,
      { suppressShellFeedback: true },
    );
  }

  private navigateWithFilters(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: this.searchQuery() || null,
        sort: this.sort() || null,
        page: 0,
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
