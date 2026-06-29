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
import { TranslateService } from '@ngx-translate/core';
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import { ProblemDetail, TchBackendClient, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';

import { resolveErrorFeedbackCopy } from '../../../../../../core/api/error-feedback-copy';
import { ErrorViewModel, toErrorViewModel } from '../../../../../../core/api/local-error-routing';
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
    TchSectionError,
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
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly loadTrigger$ = new Subject<void>();
  private readonly row = signal<TenantAdminGlobalRow | null>(null);

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<{ title: string; message: string } | null>(null);

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
          this.error.set(null);
          this.actionNotice.set(null);
          return this.backend.get<TenantAdminGlobalRow>(
            `/admin/identity/users/${userId}`,
            { suppressShellFeedback: true },
          ).pipe(
            catchError((err: unknown) => {
              this.error.set(this.errorViewModel(err, 'platform.tenantAdmins.detail'));
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
    this.error.set(null);
    this.actionNotice.set(null);
    this.identityApi.activate(r.id, { suppressShellFeedback: true }).subscribe({
      next: () => this.loadTrigger$.next(),
      error: err => this.error.set(this.errorViewModel(err, 'platform.tenantAdmins.activate')),
    });
  }

  block(): void {
    const r = this.row();
    if (!r) return;
    this.error.set(null);
    this.actionNotice.set(null);
    this.identityApi.suspend(r.id, { suppressShellFeedback: true }).subscribe({
      next: () => this.loadTrigger$.next(),
      error: err => this.error.set(this.errorViewModel(err, 'platform.tenantAdmins.block')),
    });
  }

  archive(): void {
    const r = this.row();
    if (!r) return;
    this.error.set(null);
    this.actionNotice.set(null);
    this.identityApi.archive(r.id, { suppressShellFeedback: true }).subscribe({
      next: () => void this.router.navigate(['/app/platform/tenant-admins']),
      error: err => this.error.set(this.errorViewModel(err, 'platform.tenantAdmins.archive')),
    });
  }

  resetPassword(): void {
    const r = this.row();
    if (!r) return;
    this.error.set(null);
    this.actionNotice.set(null);
    this.identityApi.resetPassword(r.id, { suppressShellFeedback: true }).subscribe({
      next: ({ tempPassword }) => this.actionNotice.set({
        title: 'Mot de passe temporaire',
        message: tempPassword,
      }),
      error: err => this.error.set(this.errorViewModel(err, 'platform.tenantAdmins.resetPassword')),
    });
  }

  goBack(): void {
    void this.router.navigate(['/app/platform/tenant-admins']);
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
