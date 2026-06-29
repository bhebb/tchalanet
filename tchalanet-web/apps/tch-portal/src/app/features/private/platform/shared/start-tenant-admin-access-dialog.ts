import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '../../../../core/api/error-feedback-copy';
import { ErrorViewModel, toErrorViewModel } from '../../../../core/api/local-error-routing';
import { SupportAccessStore } from '../../../../core/access/support-access.store';
import { PlatformTenantAdminAccessApi } from '../platform-tenant-admin-access-api.service';
import type { TenantStatus } from '../tenants/data-access/platform-tenants-api.service';

export interface StartTenantAdminAccessDialogData {
  tenantId: string;
  tenantName: string;
  tenantCode: string;
  tenantStatus: TenantStatus;
}

@Component({
  selector: 'tch-start-tenant-admin-access-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    ReactiveFormsModule,
    TchSectionError,
  ],
  templateUrl: './start-tenant-admin-access-dialog.html',
  styleUrls: ['./start-tenant-admin-access-dialog.scss'],
})
export class StartTenantAdminAccessDialog {
  protected readonly data = inject<StartTenantAdminAccessDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<StartTenantAdminAccessDialog>);
  private readonly api = inject(PlatformTenantAdminAccessApi);
  private readonly store = inject(SupportAccessStore);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  protected readonly mode =
    this.data.tenantStatus === 'ACTIVE' ? 'SUPPORT_OVERRIDE' : 'SUPPORT_READONLY';

  protected readonly loading = signal(false);
  protected readonly error = signal<ErrorViewModel | null>(null);

  readonly form = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(10)]],
    confirmed: [false, [Validators.requiredTrue]],
  });

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    const reason = this.form.controls.reason.value;
    if (!reason) return;

    this.loading.set(true);
    this.error.set(null);

    this.api
      .startAdminAccess(this.data.tenantId, {
        reason,
        mode: this.mode,
      }, { suppressShellFeedback: true })
      .subscribe({
        next: session => {
          this.store.startSession(session);
          this.loading.set(false);
          this.dialogRef.close(session);
          void this.router.navigate(['/app/admin']);
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.error.set(this.errorViewModel(err, 'platform.tenantAdminAccess.start'));
        },
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
}
