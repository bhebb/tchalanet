import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSort, MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { EMPTY, Subject, catchError, debounceTime, switchMap } from 'rxjs';
import {
  BadgeStatus,
  TchConfirmDialog,
  TchConfirmDialogData,
  TchErrorPanel,
  TchLoading,
  TchStatusBadge,
} from '@tch/ui/components';

import { AdminCrudShellComponent } from '../../../../shared/admin-ui/admin-crud-shell.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';
import type { TenantAdminGlobalRow, TenantAdminGlobalPage } from '../../data-access/platform-tenant-admins.models';
import { IdentityUserCrudApi } from '../../../shared/identity-user-crud-api.service';

type ProblemLike = { title?: string; detail?: string };

const PAGE_SIZE = 20;

@Component({
  selector: 'tch-platform-tenant-admins-global-page',
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
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
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
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild(MatSort) matSort?: MatSort;

  readonly loadTrigger$ = new Subject<void>();
  readonly displayedColumns = ['user', 'tenant', 'status', 'assignedAt', 'actions'];

  readonly filters = this.fb.nonNullable.group({ q: '', sort: 'displayName,asc' });

  readonly loading = signal(false);
  readonly errorTitle = signal<string | null>(null);
  readonly items = signal<TenantAdminGlobalRow[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly size = signal(PAGE_SIZE);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / this.size())));
  readonly hasActiveFilters = signal(false);

  ngOnInit(): void {
    this.loadTrigger$.pipe(
      switchMap(() => {
        const snap = this.route.snapshot.queryParamMap;
        const snapPage = Math.max(0, Number(snap.get('page') ?? 0) || 0);
        this.loading.set(true);
        this.errorTitle.set(null);
        return this.search(snap.get('q') ?? '', snap.get('sort') ?? 'displayName,asc', snapPage).pipe(
          catchError((err: unknown) => {
            const pd = this.problem(err);
            this.errorTitle.set(pd.title ?? this.translate.instant('platform.tenantAdmins.error.load'));
            this.loading.set(false);
            return EMPTY;
          }),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(res => {
      this.items.set(res.items ?? []);
      this.total.set(res.totalElements ?? 0);
      this.loading.set(false);
    });

    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        this.page.set(Math.max(0, Number(params.get('page') ?? 0) || 0));
        this.filters.setValue(
          { q: params.get('q') ?? '', sort: params.get('sort') ?? 'displayName,asc' },
          { emitEvent: false },
        );
        this.hasActiveFilters.set(!!params.get('q'));
        this.loadTrigger$.next();
      });

    this.filters.valueChanges
      .pipe(debounceTime(400), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.navigateWithFilters());
  }

  resetFilters(): void {
    this.filters.reset({ q: '', sort: 'displayName,asc' });
  }

  onSortChange(sort: Sort): void {
    const sortStr = sort.active && sort.direction ? `${sort.active},${sort.direction}` : 'displayName,asc';
    this.filters.patchValue({ sort: sortStr }, { emitEvent: false });
    this.navigateWithFilters();
  }

  parsedSort(): { active: string; direction: 'asc' | 'desc' } {
    const raw = this.filters.controls.sort.value ?? 'displayName,asc';
    const [active, dir] = raw.split(',');
    return { active: active ?? 'displayName', direction: dir === 'desc' ? 'desc' : 'asc' };
  }

  prevPage(): void {
    if (this.page() > 0) this.goToPage(this.page() - 1);
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) this.goToPage(this.page() + 1);
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
        this.identityApi.resetPassword(row.id).subscribe({
          next: ({ tempPassword }) => this.snackBar.open(
            `Mot de passe temporaire : ${tempPassword}`, 'OK', { duration: 15000 },
          ),
          error: () => this.snackBar.open(
            this.translate.instant('platform.tenantAdmins.error.resetPassword'), 'OK', { duration: 4000 },
          ),
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
        this.identityApi.activate(row.id).subscribe({
          next: () => this.loadTrigger$.next(),
          error: () => this.snackBar.open(
            this.translate.instant('platform.tenantAdmins.error.activate'), 'OK', { duration: 4000 },
          ),
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
        this.identityApi.suspend(row.id).subscribe({
          next: () => this.loadTrigger$.next(),
          error: () => this.snackBar.open(
            this.translate.instant('platform.tenantAdmins.error.block'), 'OK', { duration: 4000 },
          ),
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
        this.identityApi.archive(row.id).subscribe({
          next: () => this.loadTrigger$.next(),
          error: () => this.snackBar.open(
            this.translate.instant('platform.tenantAdmins.error.archive'), 'OK', { duration: 4000 },
          ),
        });
      });
  }

  private search(q: string, sort: string, page: number): Observable<TenantAdminGlobalPage> {
    const params: Record<string, string> = { page: String(page), size: String(PAGE_SIZE) };
    if (q) params['q'] = q;
    if (sort) params['sort'] = sort;
    const qs = new URLSearchParams(params).toString();
    return this.backend.get<TenantAdminGlobalPage>(`/admin/identity/users?${qs}`);
  }

  private navigateWithFilters(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: this.filters.controls.q.value || null,
        sort: this.filters.controls.sort.value || null,
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

  private problem(err: unknown): ProblemLike {
    return ((err as { error?: ProblemLike })?.error ?? {}) as ProblemLike;
  }
}
