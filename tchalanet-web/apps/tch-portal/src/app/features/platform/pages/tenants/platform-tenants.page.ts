import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { DatePipe } from '@angular/common';
import { TchLoading, TchErrorPanel } from '@tch/ui/components';

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminCrudShellComponent } from '../../../private/shared/admin-ui/admin-crud-shell.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../private/shared/admin-ui/admin-status-pill.component';
import {
  PlatformTenantsApi,
  TenantSummaryView,
} from '../../platform-tenants-api.service';
import { StartTenantAdminAccessDialog } from '../../shared/start-tenant-admin-access-dialog';

type TenantStatus = 'ACTIVE' | 'DRAFT' | 'SUSPENDED' | 'ARCHIVED';

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
    AdminCrudShellComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTableModule,
  ],
  templateUrl: './platform-tenants.page.html',
  styleUrls: ['./platform-tenants.page.scss'],
})
export class PlatformTenantsPage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['code', 'name', 'type', 'timezone', 'currency', 'status', 'actions'];

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly items = signal<TenantSummaryView[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);
  readonly totalPages = computed(() => Math.max(1, Math.ceil(this.total() / 20)));

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listTenants({ page: this.page(), size: 20 }).subscribe({
      next: res => {
        this.items.set(res.items);
        this.total.set(res.total);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })
          ?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
        this.loading.set(false);
      },
    });
  }

  prevPage(): void {
    if (this.page() > 0) {
      this.page.update(p => p - 1);
      this.loadPage();
    }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.page.update(p => p + 1);
      this.loadPage();
    }
  }

  activate(tenant: TenantSummaryView): void {
    this.api.activateTenant(tenant.id).subscribe({
      next: () => {
        this.snackBar.open(`Tenant ${tenant.code} activé.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () => this.snackBar.open('Erreur lors de l\'activation.', 'OK', { duration: 4000 }),
    });
  }

  suspend(tenant: TenantSummaryView): void {
    this.api.suspendTenant(tenant.id).subscribe({
      next: () => {
        this.snackBar.open(`Tenant ${tenant.code} suspendu.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () => this.snackBar.open('Erreur lors de la suspension.', 'OK', { duration: 4000 }),
    });
  }

  reactivate(tenant: TenantSummaryView): void {
    this.api.reactivateTenant(tenant.id).subscribe({
      next: () => {
        this.snackBar.open(`Tenant ${tenant.code} réactivé.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () => this.snackBar.open('Erreur lors de la réactivation.', 'OK', { duration: 4000 }),
    });
  }

  archive(tenant: TenantSummaryView): void {
    this.api.archiveTenant(tenant.id).subscribe({
      next: () => {
        this.snackBar.open(`Tenant ${tenant.code} archivé.`, 'OK', { duration: 3000 });
        this.loadPage();
      },
      error: () => this.snackBar.open('Erreur lors de l\'archivage.', 'OK', { duration: 4000 }),
    });
  }

  statusTone(status: TenantStatus): AdminStatusTone {
    const map: Record<TenantStatus, AdminStatusTone> = {
      ACTIVE: 'success',
      DRAFT: 'neutral',
      SUSPENDED: 'warning',
      ARCHIVED: 'danger',
    };
    return map[status] ?? 'neutral';
  }

  openAdminAccess(tenant: TenantSummaryView): void {
    this.dialog.open(StartTenantAdminAccessDialog, {
      data: {
        tenantId: tenant.id,
        tenantName: tenant.name,
        tenantCode: tenant.code,
        tenantStatus: tenant.status,
      },
      width: '520px',
    });
  }
}
