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
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '../../../../../../core/api/error-feedback-copy';
import { ErrorViewModel, toErrorViewModel } from '../../../../../../core/api/local-error-routing';
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
    TchSectionError,
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
  private readonly translate = inject(TranslateService);

  readonly displayedColumns = ['email', 'displayName', 'roleCodes', 'status'];
  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly actionFeedback = signal<ErrorViewModel | null>(null);
  readonly tenant = signal<TenantSummaryView | null>(null);
  readonly admins = signal<TenantAdminView[]>([]);

  ngOnInit(): void {
    const tenantId = this.tenantId();
    if (!tenantId) {
      this.error.set(this.localErrorViewModel());
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      tenant: this.api.getTenant(tenantId, { suppressShellFeedback: true }),
      admins: this.api.listTenantAdmins(tenantId, { suppressShellFeedback: true }),
    }).subscribe({
      next: ({ tenant, admins }) => {
        this.tenant.set(tenant);
        this.admins.set(admins);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'platform.tenants.admins.list'));
        this.loading.set(false);
      },
    });
  }

  openAssignUser(): void {
    const tenantId = this.tenantId();
    if (!tenantId) {
      this.actionFeedback.set(this.localErrorViewModel());
      return;
    }

    this.dialog
      .open<AssignUserDialog, void, AssignUserResult>(AssignUserDialog, { width: '440px' })
      .afterClosed()
      .subscribe(result => {
        if (!result) return;
        this.actionFeedback.set(null);
        this.identityApi
          .assignMembership(result.userId, { tenantId, role: 'TENANT_ADMIN' }, { suppressShellFeedback: true })
          .subscribe({
            next: () => {
              const name = result.displayName || result.email || result.userId;
              this.actionFeedback.set({
                title: 'Administrateur assigné',
                message: `${name} est maintenant administrateur du tenant.`,
                severity: 'info',
              });
              this.ngOnInit();
            },
            error: err => {
              this.actionFeedback.set(this.errorViewModel(err, 'platform.tenants.admins.assign'));
            },
          });
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

  private tenantId(): string | null {
    return this.route.snapshot.paramMap.get('tenantId');
  }

  private localErrorViewModel(): ErrorViewModel {
    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}
