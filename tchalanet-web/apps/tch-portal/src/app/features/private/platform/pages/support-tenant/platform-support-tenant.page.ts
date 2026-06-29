import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent, AdminStatusTone } from '../../../shared/admin-ui/admin-status-pill.component';
import { StartTenantAdminAccessDialog } from '../../shared/start-tenant-admin-access-dialog';
import {
  PlatformTenantsApi,
  TenantStatus,
  TenantSummaryView,
} from '../../tenants/data-access/platform-tenants-api.service';

const PAGE_SIZE = 20;
const STATUS_OPTIONS = ['', 'ACTIVE', 'SUSPENDED', 'DRAFT', 'ARCHIVED'] as const;

@Component({
  selector: 'tch-platform-support-tenant-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './platform-support-tenant.page.html',
  styleUrls: ['./platform-support-tenant.page.scss'],
})
export class PlatformSupportTenantPage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly columns = ['tenant', 'code', 'status', 'updatedAt', 'actions'];
  readonly statusOptions = STATUS_OPTIONS;
  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly tenants = signal<TenantSummaryView[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);

  readonly filters = this.fb.nonNullable.group({
    q: '',
    status: '',
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const value = this.filters.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.api.listTenants({
      q: value.q,
      status: value.status,
      page: this.page(),
      size: PAGE_SIZE,
      sort: 'updatedAt,desc',
    }, { suppressShellFeedback: true }).subscribe({
      next: result => {
        this.tenants.set(result.items ?? []);
        this.total.set(result.total ?? result.items?.length ?? 0);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'platform.supportTenant.list'));
        this.loading.set(false);
      },
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.load();
  }

  resetFilters(): void {
    this.filters.reset({ q: '', status: '' });
    this.applyFilters();
  }

  prevPage(): void {
    if (this.page() === 0) return;
    this.page.set(this.page() - 1);
    this.load();
  }

  nextPage(): void {
    if ((this.page() + 1) * PAGE_SIZE >= this.total()) return;
    this.page.set(this.page() + 1);
    this.load();
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

  tenantId(tenant: TenantSummaryView): string {
    return tenant.tenantId ?? tenant.id ?? '';
  }

  statusTone(status: TenantStatus): AdminStatusTone {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'SUSPENDED':
        return 'warning';
      case 'ARCHIVED':
      case 'REJECTED':
        return 'danger';
      default:
        return 'neutral';
    }
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
