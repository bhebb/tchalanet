import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { PLATFORM_ID } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { PortalHandoffApi, SupportAccessStore } from '@tch/core/auth';
import { TchRuntimeConfigStore } from '@tch/shared-config';
import { TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { firstValueFrom } from 'rxjs';
import { PlatformTenantAdminAccessApi } from '../../tenant-admins/data-access/platform-tenant-admin-access-api.service';
import type { TenantStatus } from '../../tenants/data-access/platform-tenant-contracts';

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
  private readonly document = inject(DOCUMENT);
  private readonly fb = inject(FormBuilder);
  private readonly handoffs = inject(PortalHandoffApi);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly runtimeConfig = inject(TchRuntimeConfigStore);
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
          void this.openAdminPortal(session.sessionId);
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

  private async openAdminPortal(supportAccessSessionId: string): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const adminBaseUrl = withoutTrailingSlash(
      this.runtimeConfig.config().portalBaseUrls?.['admin-portal'] ?? '/admin',
    );
    if (!this.isCrossOrigin(adminBaseUrl)) {
      this.document.defaultView?.location.assign(`${adminBaseUrl}/app/admin`);
      return;
    }

    const handoff = await firstValueFrom(
      this.handoffs.create(
        {
          targetPortal: 'ADMIN',
          entryRoute: '/app/admin',
          supportAccessSessionId,
        },
        { suppressShellFeedback: true },
      ),
    );
    const handoffTargetUrl = resolveHandoffTargetUrl(handoff.targetUrl, adminBaseUrl);
    this.document.defaultView?.location.assign(
      `${handoffTargetUrl}/login/handoff#code=${handoff.handoffId}.${handoff.code}`,
    );
  }

  private isCrossOrigin(targetBaseUrl: string): boolean {
    const window = this.document.defaultView;
    if (!window) {
      return false;
    }
    return new URL(targetBaseUrl, window.location.origin).origin !== window.location.origin;
  }
}

function withoutTrailingSlash(value: string): string {
  return value.length > 1 ? value.replace(/\/+$/, '') : value;
}

function resolveHandoffTargetUrl(handoffTargetUrl: string, runtimeTargetBaseUrl: string): string {
  if (/^https?:\/\//i.test(handoffTargetUrl)) {
    return withoutTrailingSlash(handoffTargetUrl);
  }
  return withoutTrailingSlash(runtimeTargetBaseUrl);
}
