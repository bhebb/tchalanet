import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { forkJoin } from 'rxjs';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminCrudShellComponent } from '../../../../shared/admin-ui/admin-crud-shell.component';
import {
  PlatformTenantsApi,
  TenantAdminView,
  TenantSummaryView,
} from '../../data-access/platform-tenants-api.service';
import { IdentityUserCrudApi } from '../../../shared/identity-user-crud-api.service';
import {
  AssignUserDialog,
  AssignUserResult,
} from '../../../shared/assign-user-dialog/assign-user-dialog.component';

@Component({
  selector: 'tch-platform-tenant-admins-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminCrudShellComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './platform-tenant-admins.page.html',
  styleUrls: ['./platform-tenant-admins.page.scss'],
})
export class PlatformTenantAdminsPage implements OnInit {
  private readonly api = inject(PlatformTenantsApi);
  private readonly identityApi = inject(IdentityUserCrudApi);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['email', 'displayName', 'roleCodes', 'status'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly tenant = signal<TenantSummaryView | null>(null);
  readonly admins = signal<TenantAdminView[]>([]);

  ngOnInit(): void {
    const tenantId = this.route.snapshot.paramMap.get('tenantId')!;
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      tenant: this.api.getTenant(tenantId),
      admins: this.api.listTenantAdmins(tenantId),
    }).subscribe({
      next: ({ tenant, admins }) => {
        this.tenant.set(tenant);
        this.admins.set(admins);
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

  openAssignUser(): void {
    const tenantId = this.route.snapshot.paramMap.get('tenantId')!;
    this.dialog
      .open<AssignUserDialog, void, AssignUserResult>(AssignUserDialog, { width: '440px' })
      .afterClosed()
      .subscribe(result => {
        if (!result) return;
        this.identityApi
          .assignMembership(result.userId, { tenantId, role: 'TENANT_ADMIN' })
          .subscribe({
            next: () => {
              const name = result.displayName || result.email || result.userId;
              this.snackBar.open(`${name} assigné comme administrateur.`, 'OK', { duration: 4000 });
              this.ngOnInit();
            },
            error: () => this.snackBar.open('Assignation impossible.', 'OK', { duration: 4000 }),
          });
      });
  }
}
