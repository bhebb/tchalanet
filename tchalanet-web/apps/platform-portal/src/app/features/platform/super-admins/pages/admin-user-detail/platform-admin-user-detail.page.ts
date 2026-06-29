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
import { TranslateService } from '@ngx-translate/core';
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';

import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
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
    TchSectionError,
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
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly loadTrigger$ = new Subject<void>();
  private readonly superAdmin = signal<PlatformSuperAdminView | null>(null);

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<{ title: string; message: string } | null>(null);

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
          this.error.set(null);
          this.actionNotice.set(null);
          return this.api.getSuperAdmin(userId, { suppressShellFeedback: true }).pipe(
            catchError((err: unknown) => {
              this.error.set(this.errorViewModel(err, 'platform.superAdmins.detail'));
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
    this.actionNotice.set(null);
    this.error.set(null);
    this.identityApi.activate(u.id, { suppressShellFeedback: true }).subscribe({
      next: () => this.loadTrigger$.next(),
      error: err => this.error.set(this.errorViewModel(err, 'platform.superAdmins.activate')),
    });
  }

  block(): void {
    const u = this.superAdmin();
    if (!u) return;
    this.actionNotice.set(null);
    this.error.set(null);
    this.identityApi.suspend(u.id, { suppressShellFeedback: true }).subscribe({
      next: () => this.loadTrigger$.next(),
      error: err => this.error.set(this.errorViewModel(err, 'platform.superAdmins.suspend')),
    });
  }

  archive(): void {
    const u = this.superAdmin();
    if (!u) return;
    this.actionNotice.set(null);
    this.error.set(null);
    this.identityApi.archive(u.id, { suppressShellFeedback: true }).subscribe({
      next: () => void this.router.navigate(['/app/platform/super-admins']),
      error: err => this.error.set(this.errorViewModel(err, 'platform.superAdmins.archive')),
    });
  }

  resetPassword(): void {
    const u = this.superAdmin();
    if (!u) return;
    this.actionNotice.set(null);
    this.error.set(null);
    this.identityApi.resetPassword(u.id, { suppressShellFeedback: true }).subscribe({
      next: ({ tempPassword }) => this.actionNotice.set({
        title: 'Mot de passe temporaire',
        message: tempPassword,
      }),
      error: err => this.error.set(this.errorViewModel(err, 'platform.superAdmins.resetPassword')),
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
        this.actionNotice.set(null);
        this.error.set(null);
        this.identityApi
          .assignMembership(
            u.id,
            { tenantId: result.tenantId, role: 'TENANT_ADMIN' },
            { suppressShellFeedback: true },
          )
          .subscribe({
            next: () => {
              this.actionNotice.set({
                title: 'Tenant assigné',
                message: result.tenantName,
              });
              this.loadTrigger$.next();
            },
            error: err => this.error.set(this.errorViewModel(err, 'platform.superAdmins.assignTenant')),
          });
      });
  }

  goBack(): void {
    void this.router.navigate(['/app/platform/super-admins']);
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
