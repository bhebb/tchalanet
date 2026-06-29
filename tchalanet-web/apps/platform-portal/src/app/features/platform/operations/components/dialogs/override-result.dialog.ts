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
  DrawResultOpsResponse,
  OverrideDrawResultRequest,
} from '../../data-access/platform-ops-api.service';
import { haitiLotGameMappings } from '../../../../../shared/results/haiti-lot-game-mapping';

@Component({
  selector: 'tch-override-result-dialog',
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
  templateUrl: './override-result.dialog.html',
  styleUrls: ['./override-result.dialog.scss'],
})
export class OverrideResultDialog {
  protected readonly data = inject<{ row: DrawResultOpsResponse; onSuccess: () => void }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<OverrideResultDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

  readonly form = this.fb.group({
    drawDate: [this.drawDateFromRow(), Validators.required],
    lot1: [this.lotValue('lot1'), [Validators.required, Validators.pattern(/^\d{3}$/)]],
    lot2: [this.lotValue('lot2'), [Validators.required, Validators.pattern(/^\d{2}$/)]],
    lot3: [this.lotValue('lot3'), [Validators.required, Validators.pattern(/^\d{2}$/)]],
    reason: ['', Validators.required],
    force: [false],
  });

  lotMappings() {
    return haitiLotGameMappings({ slotKey: this.data.row.slotKey });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    if (!v.drawDate || !v.lot1 || !v.lot2 || !v.lot3 || !v.reason) return;
    const req: OverrideDrawResultRequest = {
      slotKey: this.data.row.slotKey,
      drawDate: v.drawDate,
      lot1: v.lot1,
      lot2: v.lot2,
      lot3: v.lot3,
      reason: v.reason,
      force: v.force ?? false,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.overrideDrawResult(req, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.data.onSuccess();
        this.dialogRef.close(true);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(this.errorViewModel(err, 'platform.ops.drawResults.override'));
      },
    });
  }

  private lotValue(key: 'lot1' | 'lot2' | 'lot3'): string {
    const value = this.data.row.haitiResult;
    if (!value || typeof value !== 'object') return '';
    const lot = (value as Record<string, unknown>)[key];
    return typeof lot === 'string' ? lot.trim() : '';
  }

  private drawDateFromRow(): string {
    return this.data.row.occurredAt?.slice(0, 10) ?? '';
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
