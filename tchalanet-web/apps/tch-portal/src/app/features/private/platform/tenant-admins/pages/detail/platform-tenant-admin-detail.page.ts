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
import { MatSnackBar } from '@angular/material/snack-bar';
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import { TchBackendClient } from '@tch/api';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';

import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { PlatformAdminUserCardComponent } from '../../../shared/admin-user-card/platform-admin-user-card.component';
import type { AdminUserCardData } from '../../../shared/admin-user-card/admin-user-card.model';
import type { TenantAdminGlobalRow } from '../../data-access/platform-tenant-admins.models';
import { IdentityUserCrudApi } from '../../../shared/identity-user-crud-api.service';

@Component({
  selector: 'tch-platform-tenant-admin-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    PlatformAdminUserCardComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
  ],
  templateUrl: './platform-tenant-admin-detail.page.html',
  styleUrls: ['./platform-tenant-admin-detail.page.scss'],
})
export class PlatformTenantAdminDetailPage implements OnInit {
  private readonly backend = inject(TchBackendClient);
  private readonly identityApi = inject(IdentityUserCrudApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  private readonly loadTrigger$ = new Subject<void>();
  private readonly row = signal<TenantAdminGlobalRow | null>(null);

  readonly loading = signal(false);
  readonly errorTitle = signal<string | null>(null);

  readonly cardUser = computed((): AdminUserCardData | null => {
    const r = this.row();
    if (!r) return null;
    return {
      id: r.id,
      email: r.email,
      displayName: r.displayName,
      status: r.status,
      assignedAt: r.createdAt,
      tenantId: r.tenantId,
      tenantName: r.tenantName,
      tenantCode: r.tenantCode,
    };
  });

  ngOnInit(): void {
    this.loadTrigger$
      .pipe(
        switchMap(() => {
          const userId = this.route.snapshot.paramMap.get('userId') ?? '';
          this.loading.set(true);
          this.errorTitle.set(null);
          return this.backend.get<TenantAdminGlobalRow>(`/admin/identity/users/${userId}`).pipe(
            catchError(() => {
              this.errorTitle.set('Chargement impossible.');
              this.loading.set(false);
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(r => {
        this.row.set(r);
        this.loading.set(false);
      });

    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadTrigger$.next());
  }

  activate(): void {
    const r = this.row();
    if (!r) return;
    this.identityApi.activate(r.id).subscribe({
      next: () => this.loadTrigger$.next(),
      error: () => this.snackBar.open('Activation impossible.', 'OK', { duration: 4000 }),
    });
  }

  block(): void {
    const r = this.row();
    if (!r) return;
    this.identityApi.suspend(r.id).subscribe({
      next: () => this.loadTrigger$.next(),
      error: () => this.snackBar.open('Suspension impossible.', 'OK', { duration: 4000 }),
    });
  }

  archive(): void {
    const r = this.row();
    if (!r) return;
    this.identityApi.archive(r.id).subscribe({
      next: () => void this.router.navigate(['/app/platform/tenant-admins']),
      error: () => this.snackBar.open('Archivage impossible.', 'OK', { duration: 4000 }),
    });
  }

  resetPassword(): void {
    const r = this.row();
    if (!r) return;
    this.identityApi.resetPassword(r.id).subscribe({
      next: ({ tempPassword }) =>
        this.snackBar.open(`Mot de passe temporaire : ${tempPassword}`, 'OK', { duration: 15000 }),
      error: () => this.snackBar.open('Réinitialisation impossible.', 'OK', { duration: 4000 }),
    });
  }

  goBack(): void {
    void this.router.navigate(['/app/platform/tenant-admins']);
  }
}
