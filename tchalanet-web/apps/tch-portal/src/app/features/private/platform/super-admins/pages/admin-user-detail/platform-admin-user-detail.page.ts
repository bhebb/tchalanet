import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';

import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { PlatformAdminUserCardComponent } from '../../../shared/admin-user-card/platform-admin-user-card.component';
import type { AdminUserCardData } from '../../../shared/admin-user-card/admin-user-card.model';
import { PlatformAdminApi, PlatformSuperAdminView } from '../../../tenants/data-access/platform-admin-api.service';
import { IdentityUserCrudApi } from '../../../shared/identity-user-crud-api.service';
import { AssignTenantDialog, AssignTenantResult } from '../../../shared/assign-tenant-dialog/assign-tenant-dialog.component';

@Component({
  selector: 'tch-platform-admin-user-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    PlatformAdminUserCardComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
  ],
  templateUrl: './platform-admin-user-detail.page.html',
  styleUrls: ['./platform-admin-user-detail.page.scss'],
})
export class PlatformAdminUserDetailPage implements OnInit {
  private readonly api = inject(PlatformAdminApi);
  private readonly identityApi = inject(IdentityUserCrudApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  private readonly loadTrigger$ = new Subject<void>();
  private readonly superAdmin = signal<PlatformSuperAdminView | null>(null);

  readonly loading = signal(false);
  readonly errorTitle = signal<string | null>(null);

  readonly cardUser = computed((): AdminUserCardData | null => {
    const u = this.superAdmin();
    if (!u) return null;
    return {
      id: u.id,
      email: u.email,
      displayName: u.displayName,
      status: u.status,
      assignedAt: u.assignedAt,
    };
  });

  ngOnInit(): void {
    this.loadTrigger$
      .pipe(
        switchMap(() => {
          const userId = this.route.snapshot.paramMap.get('userId') ?? '';
          this.loading.set(true);
          this.errorTitle.set(null);
          return this.api.getSuperAdmin(userId).pipe(
            catchError(() => {
              this.errorTitle.set('Chargement impossible.');
              this.loading.set(false);
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(u => {
        this.superAdmin.set(u);
        this.loading.set(false);
      });

    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadTrigger$.next());
  }

  activate(): void {
    const u = this.superAdmin();
    if (!u) return;
    this.identityApi.activate(u.id).subscribe({
      next: () => this.loadTrigger$.next(),
      error: () => this.snackBar.open('Activation impossible.', 'OK', { duration: 4000 }),
    });
  }

  block(): void {
    const u = this.superAdmin();
    if (!u) return;
    this.identityApi.suspend(u.id).subscribe({
      next: () => this.loadTrigger$.next(),
      error: () => this.snackBar.open('Suspension impossible.', 'OK', { duration: 4000 }),
    });
  }

  archive(): void {
    const u = this.superAdmin();
    if (!u) return;
    this.identityApi.archive(u.id).subscribe({
      next: () => void this.router.navigate(['/app/platform/super-admins']),
      error: () => this.snackBar.open('Archivage impossible.', 'OK', { duration: 4000 }),
    });
  }

  resetPassword(): void {
    const u = this.superAdmin();
    if (!u) return;
    this.identityApi.resetPassword(u.id).subscribe({
      next: ({ tempPassword }) =>
        this.snackBar.open(`Mot de passe temporaire : ${tempPassword}`, 'OK', { duration: 15000 }),
      error: () => this.snackBar.open('Réinitialisation impossible.', 'OK', { duration: 4000 }),
    });
  }

  assignTenant(): void {
    const u = this.superAdmin();
    if (!u) return;
    this.dialog
      .open<AssignTenantDialog, void, AssignTenantResult>(AssignTenantDialog, { width: '420px' })
      .afterClosed()
      .subscribe(result => {
        if (!result) return;
        this.identityApi
          .assignMembership(u.id, { tenantId: result.tenantId, role: 'TENANT_ADMIN' })
          .subscribe({
            next: () => {
              this.snackBar.open(`Assigné au tenant ${result.tenantName}.`, 'OK', { duration: 4000 });
              this.loadTrigger$.next();
            },
            error: () => this.snackBar.open('Assignation impossible.', 'OK', { duration: 4000 }),
          });
      });
  }

  goBack(): void {
    void this.router.navigate(['/app/platform/super-admins']);
  }
}
