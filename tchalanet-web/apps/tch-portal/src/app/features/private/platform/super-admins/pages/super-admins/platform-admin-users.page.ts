import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import { PlatformAdminApi, PlatformSuperAdminView } from '../../../tenants/data-access/platform-admin-api.service';
import { IdentityUserCrudApi } from '../../../shared/identity-user-crud-api.service';

@Component({
  selector: 'tch-platform-admin-users-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    DatePipe,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
  ],
  templateUrl: './platform-admin-users.page.html',
  styleUrls: ['./platform-admin-users.page.scss'],
})
export class PlatformAdminUsersPage implements OnInit {
  private readonly api = inject(PlatformAdminApi);
  private readonly identityApi = inject(IdentityUserCrudApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly displayedColumns = ['email', 'displayName', 'status', 'assignedAt', 'actions'];
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly superAdmins = signal<PlatformSuperAdminView[]>([]);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    displayName: ['', Validators.required],
    phoneNumber: [''],
  });

  ngOnInit(): void {
    this.load();
  }

  submit(): void {
    if (this.form.invalid || this.saving()) return;
    const v = this.form.value;
    if (!v.email || !v.displayName) return;
    this.saving.set(true);
    this.error.set(null);
    this.api
      .createSuperAdmin({
        email: v.email,
        displayName: v.displayName,
        phoneNumber: v.phoneNumber || null,
      }, { suppressShellFeedback: true })
      .subscribe({
        next: created => {
          this.superAdmins.set([created, ...this.superAdmins()]);
          this.form.reset({ email: '', displayName: '', phoneNumber: '' });
          this.saving.set(false);
        },
        error: err => {
          this.error.set(this.errorViewModel(err, 'platform.superAdmins.create'));
          this.saving.set(false);
        },
      });
  }

  revoke(row: PlatformSuperAdminView): void {
    this.error.set(null);
    this.api.revokeSuperAdmin(row.id, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.superAdmins.set(this.superAdmins().filter(item => item.id !== row.id));
      },
      error: err => this.error.set(this.errorViewModel(err, 'platform.superAdmins.revoke')),
    });
  }

  suspend(row: PlatformSuperAdminView): void {
    this.error.set(null);
    this.identityApi.suspend(row.id, { suppressShellFeedback: true }).subscribe({
      next: () => this.updateStatus(row.id, 'SUSPENDED'),
      error: err => this.error.set(this.errorViewModel(err, 'platform.superAdmins.suspend')),
    });
  }

  reactivate(row: PlatformSuperAdminView): void {
    this.error.set(null);
    this.identityApi.activate(row.id, { suppressShellFeedback: true }).subscribe({
      next: () => this.updateStatus(row.id, 'ACTIVE'),
      error: err => this.error.set(this.errorViewModel(err, 'platform.superAdmins.reactivate')),
    });
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listSuperAdmins({ suppressShellFeedback: true }).subscribe({
      next: rows => {
        this.superAdmins.set(rows);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(this.errorViewModel(err, 'platform.superAdmins.list'));
        this.loading.set(false);
      },
    });
  }

  private updateStatus(userId: string, status: string): void {
    this.superAdmins.set(
      this.superAdmins().map(row => (row.id === userId ? { ...row, status } : row)),
    );
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
