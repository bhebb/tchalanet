import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslateService } from '@ngx-translate/core';
import { TchSearchOption, TchSearchSelect, TchSectionError } from '@tch/ui/components';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { Observable, map } from 'rxjs';

import {
  PlatformOpsApi,
  GateUpdateRequest,
} from '../../data-access/platform-ops-api.service';
import { PlatformTenantsApi, TenantSummaryView } from '../../../tenants/data-access/platform-tenants-api.service';

@Component({
  selector: 'tch-update-gate-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    TchSearchSelect,
    TchSectionError,
  ],
  templateUrl: './update-gate.dialog.html',
  styleUrls: ['./update-gate.dialog.scss'],
})
export class UpdateGateDialog {
  protected readonly data = inject<{ jobKey: string; enable: boolean }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<UpdateGateDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

  readonly form = this.fb.group({
    scope: ['GLOBAL' as 'GLOBAL' | 'TENANT'],
    tenantId: [''],
    reason: ['', Validators.required],
  });

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi.listTenants({ q: query, page: 0, size: 12, status: null }).pipe(
      map(page => page.items.map(tenant => this.toTenantOption(tenant))),
    );

  selectTenant(option: TchSearchOption | null): void {
    const tenant = option?.data as TenantSummaryView | undefined;
    this.form.patchValue({
      tenantId: tenant?.id ?? tenant?.tenantId ?? '',
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    if (v.scope === 'TENANT' && !v.tenantId) {
      this.form.controls.tenantId.setValidators(Validators.required);
      this.form.controls.tenantId.updateValueAndValidity();
      this.form.markAllAsTouched();
      return;
    }
    if (!v.scope || !v.reason) return;
    const req: GateUpdateRequest = {
      scope: v.scope,
      tenant_id: v.scope === 'TENANT' ? v.tenantId : null,
      enabled: this.data.enable,
      reason: v.reason,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.updateGate(this.data.jobKey, req, { suppressShellFeedback: true }).subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(this.errorViewModel(err, 'platform.ops.batch.gate.update'));
      },
    });
  }

  private toTenantOption(tenant: TenantSummaryView): TchSearchOption<TenantSummaryView> {
    return {
      id: tenant.id ?? tenant.tenantId ?? tenant.code,
      title: tenant.name,
      subtitle: tenant.code,
      badge: tenant.status,
      icon: 'apartment',
      data: tenant,
    };
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
