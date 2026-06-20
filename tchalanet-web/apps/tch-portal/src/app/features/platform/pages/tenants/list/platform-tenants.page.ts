import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TchLoading, TchErrorPanel } from '@tch/ui/components';

import { AdminCrudShellComponent } from '../../../../private/shared/admin-ui/admin-crud-shell.component';
import { AdminEmptyStateComponent } from '../../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../private/shared/admin-ui/admin-page-shell.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../../private/shared/admin-ui/admin-status-pill.component';
import {
  PlatformTenantsApi,
  TenantProvisioningProfile,
  TenantReadinessStatus,
  TenantStatus,
  TenantSummaryView,
} from '../../../platform-tenants-api.service';

type ProblemLike = { title?: string; detail?: string; traceId?: string; errorId?: string; requestId?: string };

const PAGE_SIZE = 20;
const STATUS_OPTIONS = ['ONBOARDING', 'ACTIVE', 'SUSPENDED', 'DISABLED'] as const;
const PROFILE_OPTIONS: readonly TenantProvisioningProfile[] = ['MINIMAL', 'DEFAULT_HAITI_LOTTERY', 'DEMO'];

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
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatMenuModule,
    MatSelectModule,
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
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);

  readonly displayedColumns = ['tenant', 'code', 'status', 'profile', 'admin', 'readiness', 'createdAt', 'actions'];
  readonly statuses = STATUS_OPTIONS;
  readonly profiles = PROFILE_OPTIONS;

  readonly filters = this.fb.nonNullable.group({
    q: '',
    status: '',
    profile: '',
    sort: 'createdAt,desc',
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

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      const page = Math.max(0, Number(params.get('page') ?? 0) || 0);
      const size = Math.max(1, Number(params.get('size') ?? PAGE_SIZE) || PAGE_SIZE);
      this.page.set(page);
      this.size.set(size);
      this.filters.setValue(
        {
          q: params.get('q') ?? '',
          status: params.get('status') ?? '',
          profile: params.get('profile') ?? '',
          sort: params.get('sort') ?? 'createdAt,desc',
        },
        { emitEvent: false },
      );
      this.loadPage();
    });
  }

  applyFilters(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        q: this.filters.controls.q.value || null,
        status: this.filters.controls.status.value || null,
        profile: this.filters.controls.profile.value || null,
        sort: this.filters.controls.sort.value || null,
        page: 0,
        size: this.size(),
      },
      queryParamsHandling: 'merge',
    });
  }

  resetFilters(): void {
    this.filters.reset({ q: '', status: '', profile: '', sort: 'createdAt,desc' });
    this.applyFilters();
  }

  loadPage(): void {
    this.loading.set(true);
    this.errorTitle.set(null);
    this.errorDetail.set(null);
    this.traceId.set(null);
    this.api
      .listTenants({
        q: this.filters.controls.q.value,
        status: this.filters.controls.status.value,
        profile: this.filters.controls.profile.value,
        sort: this.filters.controls.sort.value,
        page: this.page(),
        size: this.size(),
      })
      .subscribe({
        next: res => {
          this.items.set(res.items ?? []);
          this.total.set(res.total ?? res.items?.length ?? 0);
          this.loading.set(false);
        },
        error: (err: unknown) => {
          const pd = this.problem(err);
          this.errorTitle.set(pd.title ?? this.translate.instant('platform.tenants.error.load'));
          this.errorDetail.set(pd.detail ?? null);
          this.traceId.set(pd.traceId ?? pd.errorId ?? pd.requestId ?? null);
          this.loading.set(false);
        },
      });
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

  tenantType(tenant: TenantSummaryView): string {
    return tenant.type || this.translate.instant('common.not_available');
  }

  statusTone(status: TenantStatus): AdminStatusTone {
    const map: Record<string, AdminStatusTone> = {
      ACTIVE: 'success',
      ONBOARDING: 'info',
      DRAFT: 'info',
      SUSPENDED: 'warning',
      DISABLED: 'danger',
      ARCHIVED: 'danger',
    };
    return map[status] ?? 'neutral';
  }

  readinessTone(status: TenantReadinessStatus | null | undefined): AdminStatusTone {
    const map: Record<string, AdminStatusTone> = {
      READY: 'success',
      INCOMPLETE: 'warning',
      MISSING: 'warning',
      BLOCKED: 'danger',
      UNKNOWN: 'neutral',
    };
    return status ? (map[status] ?? 'neutral') : 'neutral';
  }

  profileLabel(profile: TenantProvisioningProfile | null | undefined): string {
    if (!profile) return this.translate.instant('common.not_available');
    const keys: Record<TenantProvisioningProfile, string> = {
      MINIMAL: 'platform.tenants.profile.minimal',
      DEFAULT_HAITI_LOTTERY: 'platform.tenants.profile.defaultHaitiLottery',
      DEMO: 'platform.tenants.profile.demo',
    };
    return this.translate.instant(keys[profile]);
  }

  statusLabel(status: TenantStatus): string {
    return this.translate.instant(`platform.tenants.status.${status.toLowerCase()}`);
  }

  readinessLabel(status: TenantReadinessStatus | null | undefined): string {
    return status
      ? this.translate.instant(`platform.tenants.readiness.${status.toLowerCase()}`)
      : this.translate.instant('common.not_available');
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
