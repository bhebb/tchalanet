import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '../../../../../../core/api/error-feedback-copy';
import { ErrorViewModel, toErrorViewModel } from '../../../../../../core/api/local-error-routing';
import {
  PlatformOpsApi,
  ApplyExternalResultsRequest,
  OpsLaunchResponse,
} from '../../data-access/platform-ops-api.service';

@Component({
  selector: 'tch-apply-results-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    TchSectionError,
  ],
  templateUrl: './apply-results.dialog.html',
  styleUrls: ['./apply-results.dialog.scss'],
})
export class ApplyResultsDialog {
  readonly dialogRef = inject(MatDialogRef<ApplyResultsDialog>);
  protected readonly data = inject<{ slotKeys?: string[]; tenantCode?: string | null } | null>(MAT_DIALOG_DATA, { optional: true });
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly result = signal<OpsLaunchResponse | null>(null);

  readonly form = this.fb.group({
    slotKeys: [this.data?.slotKeys ? this.data.slotKeys.join(', ') : ''],
    baseDate: [''],
    daysBack: [0],
    maxSlots: [200],
    reason: [''],
    dryRun: [true],
    force: [false],
  });

  submit(): void {
    if (this.submitting() || this.result()) return;
    const v = this.form.value;
    const req: ApplyExternalResultsRequest = {
      tenantCodes: this.data?.tenantCode ? [this.data.tenantCode] : undefined,
      baseDate: v.baseDate || undefined,
      daysBack: v.daysBack ?? 0,
      slotKeys: v.slotKeys ? v.slotKeys.split(',').map((s: string) => s.trim()).filter(Boolean) : undefined,
      maxSlots: v.maxSlots ?? 200,
      dryRun: v.dryRun ?? true,
      force: v.force ?? false,
      reason: v.reason || undefined,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.applyDrawResults(req, { suppressShellFeedback: true }).subscribe({
      next: (res) => { this.submitting.set(false); this.result.set(res); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(this.errorViewModel(err, 'platform.ops.draws.apply'));
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
