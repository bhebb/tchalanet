import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '../../../../../../core/api/error-feedback-copy';
import { ErrorViewModel, toErrorViewModel } from '../../../../../../core/api/local-error-routing';
import { PlatformOpsApi } from '../../data-access/platform-ops-api.service';

export interface ClearCacheGroupDialogData {
  readonly group: string;
  readonly title: string;
  readonly description: string;
  readonly confirmLabel: string;
}

@Component({
  selector: 'tch-clear-cache-group-dialog',
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
  templateUrl: './clear-cache-group.dialog.html',
  styleUrls: ['./clear-cache-group.dialog.scss'],
})
export class ClearCacheGroupDialog {
  private readonly dialogRef = inject(MatDialogRef<ClearCacheGroupDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);
  readonly data = inject<ClearCacheGroupDialogData>(MAT_DIALOG_DATA);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

  readonly form = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(10)]],
    confirmed: [false, Validators.requiredTrue],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);

    const reason = this.form.controls.reason.value;
    if (!reason) return;

    this.api.clearCacheGroup(this.data.group, reason, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.dialogRef.close(true);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(this.errorViewModel(err, 'platform.ops.cache.clearGroup'));
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
