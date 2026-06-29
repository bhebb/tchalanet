import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import {
  PlatformOpsApi,
  FetchExternalResultsRequest,
  OpsLaunchResponse,
} from '../../data-access/platform-ops-api.service';

@Component({
  selector: 'tch-fetch-results-dialog',
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
  templateUrl: './fetch-results.dialog.html',
  styleUrls: ['./fetch-results.dialog.scss'],
})
export class FetchResultsDialog {
  protected readonly data = inject<{ title: string; mode: 'fetch'; slotKeys?: string[]; onSuccess: () => void }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<FetchResultsDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly result = signal<OpsLaunchResponse | null>(null);

  readonly form = this.fb.group({
    baseDate: [''],
    daysBack: [0],
    slotKeys: [this.data.slotKeys ? this.data.slotKeys.join(', ') : ''],
    maxSlots: [200],
    force: [false],
    reason: [''],
    dryRun: [false],
    includeRaw: [false],
  });

  submit(): void {
    if (this.submitting() || this.result()) return;
    const v = this.form.value;
    if (v.force && !v.reason) {
      this.form.controls.reason.setValidators(Validators.required);
      this.form.controls.reason.updateValueAndValidity();
      this.form.markAllAsTouched();
      return;
    }

    const req: FetchExternalResultsRequest = {
      baseDate: v.baseDate || undefined,
      daysBack: v.daysBack ?? 0,
      slotKeys: v.slotKeys ? v.slotKeys.split(',').map(s => s.trim()).filter(Boolean) : undefined,
      force: v.force ?? false,
      dryRun: v.dryRun ?? false,
      maxSlots: v.maxSlots ?? 200,
      reason: v.reason || undefined,
      includeRaw: v.includeRaw ?? false,
    };

    this.submitting.set(true);
    this.error.set(null);

    const onNext = (res: OpsLaunchResponse) => {
      this.submitting.set(false);
      this.result.set(res);
      this.data.onSuccess();
    };
    const onError = (err: unknown) => {
      this.submitting.set(false);
      this.error.set(this.errorViewModel(err, 'platform.ops.drawResults.fetch'));
    };

    this.api.fetchDrawResults(req, { suppressShellFeedback: true }).subscribe({ next: onNext, error: onError });
  }

  close(): void {
    this.dialogRef.close();
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
