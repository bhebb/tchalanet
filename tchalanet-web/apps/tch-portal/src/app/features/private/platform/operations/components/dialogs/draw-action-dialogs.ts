import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslateService } from '@ngx-translate/core';

import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '../../../../../../core/api/error-feedback-copy';
import { ErrorViewModel, toErrorViewModel } from '../../../../../../core/api/local-error-routing';
import {
  CancelDrawRequest,
  CancelDrawsRequest,
  CorrectDrawResultRequest,
  DrawView,
  PlatformOpsApi,
} from '../../data-access/platform-ops-api.service';

export type SimpleDrawActionType = 'lock' | 'unlock' | 'settle' | 'archive';
export type BulkDrawActionType = SimpleDrawActionType | 'cancel';

const SIMPLE_LABELS: Record<SimpleDrawActionType, string> = {
  lock: 'Verrouiller',
  unlock: 'Déverrouiller',
  settle: 'Régler',
  archive: 'Archiver',
};

@Component({
  selector: 'tch-correct-draw-result-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule, TchSectionError],
  templateUrl: './correct-draw-result.dialog.html',
  styleUrls: ['./draw-action-dialog.scss'],
})
export class CorrectDrawResultDialog {
  protected readonly data = inject<{ draw: DrawView; tenantId: string | null }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<CorrectDrawResultDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

  readonly form = this.fb.nonNullable.group({
    correctedDrawResultId: [this.data.draw.lastResult?.id ?? '', Validators.required],
    reason: ['', Validators.required],
    idempotencyKey: [`correct-${this.data.draw.id}-${Date.now()}`, Validators.required],
    force: [false],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const req: CorrectDrawResultRequest = {
      correctedDrawResultId: v.correctedDrawResultId,
      reason: v.reason,
      idempotencyKey: v.idempotencyKey,
      force: v.force,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.correctDrawResult(this.data.draw.id, req, this.data.tenantId, { suppressShellFeedback: true }).subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(errorViewModel(err, 'platform.ops.draws.correct', this.translate));
      },
    });
  }
}

@Component({
  selector: 'tch-cancel-draw-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule, TchSectionError],
  templateUrl: './cancel-draw.dialog.html',
  styleUrls: ['./draw-action-dialog.scss'],
})
export class CancelDrawDialog {
  protected readonly data = inject<{ draw: DrawView; tenantId: string | null }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<CancelDrawDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

  readonly form = this.fb.nonNullable.group({
    reasonCode: ['', [Validators.required, Validators.maxLength(96)]],
    reasonLabel: [''],
    force: [false],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const req: CancelDrawRequest = {
      reasonCode: v.reasonCode.toUpperCase().trim(),
      reasonLabel: v.reasonLabel || undefined,
      force: v.force,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.cancelDraw(this.data.draw.id, req, this.data.tenantId, { suppressShellFeedback: true }).subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(errorViewModel(err, 'platform.ops.draws.cancel', this.translate));
      },
    });
  }
}

@Component({
  selector: 'tch-reschedule-draw-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, TchSectionError],
  templateUrl: './reschedule-draw.dialog.html',
  styleUrls: ['./draw-action-dialog.scss'],
})
export class RescheduleDrawDialog {
  protected readonly data = inject<{ draw: DrawView; tenantId: string | null }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<RescheduleDrawDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

  readonly form = this.fb.nonNullable.group({
    scheduledAt: ['', Validators.required],
    cutoffAt: ['', Validators.required],
    reason: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set(null);
    this.api.rescheduleDraw(
      this.data.draw.id,
      new Date(v.scheduledAt).toISOString(),
      new Date(v.cutoffAt).toISOString(),
      v.reason,
      undefined,
      this.data.tenantId,
      { suppressShellFeedback: true },
    ).subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(errorViewModel(err, 'platform.ops.draws.reschedule', this.translate));
      },
    });
  }
}

@Component({
  selector: 'tch-simple-draw-action-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule, TchSectionError],
  templateUrl: './simple-draw-action.dialog.html',
  styleUrls: ['./draw-action-dialog.scss'],
})
export class SimpleDrawActionDialog {
  protected readonly data = inject<{ draw: DrawView; action: SimpleDrawActionType; tenantId: string | null }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<SimpleDrawActionDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  get label() { return SIMPLE_LABELS[this.data.action]; }

  readonly form = this.fb.nonNullable.group({
    reason: [''],
    force: [false],
  });

  submit(): void {
    const v = this.form.getRawValue();
    const reason = v.reason || undefined;
    const options = { suppressShellFeedback: true };
    let call$;
    switch (this.data.action) {
      case 'lock': call$ = this.api.lockDraw(this.data.draw.id, reason, this.data.tenantId, options); break;
      case 'unlock': call$ = this.api.unlockDraw(this.data.draw.id, reason, this.data.tenantId, options); break;
      case 'settle': call$ = this.api.settleDraw(this.data.draw.id, reason, this.data.tenantId, options); break;
      case 'archive': call$ = this.api.archiveDraw(this.data.draw.id, reason, v.force, this.data.tenantId, options); break;
    }
    this.submitting.set(true);
    this.error.set(null);
    call$.subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(errorViewModel(err, `platform.ops.draws.${this.data.action}`, this.translate));
      },
    });
  }
}

@Component({
  selector: 'tch-bulk-simple-draw-action-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule, TchSectionError],
  templateUrl: './bulk-simple-draw-action.dialog.html',
  styleUrls: ['./draw-action-dialog.scss'],
})
export class BulkSimpleDrawActionDialog {
  protected readonly data = inject<{ draws: DrawView[]; action: SimpleDrawActionType; tenantId: string | null }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<BulkSimpleDrawActionDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  get label() { return SIMPLE_LABELS[this.data.action]; }

  readonly form = this.fb.nonNullable.group({
    reason: [''],
    force: [false],
  });

  submit(): void {
    const v = this.form.getRawValue();
    const reason = v.reason || undefined;
    const req = { drawIds: this.data.draws.map(draw => draw.id), reason, force: v.force };
    const options = { suppressShellFeedback: true };
    const call$ = this.data.action === 'lock'
      ? this.api.lockDraws(req, this.data.tenantId, options)
      : this.data.action === 'unlock'
        ? this.api.unlockDraws(req, this.data.tenantId, options)
        : this.data.action === 'settle'
          ? this.api.settleDraws(req, this.data.tenantId, options)
          : this.api.archiveDraws(req, this.data.tenantId, options);

    this.submitting.set(true);
    this.error.set(null);
    call$.subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(errorViewModel(err, `platform.ops.draws.bulk.${this.data.action}`, this.translate));
      },
    });
  }
}

@Component({
  selector: 'tch-bulk-cancel-draw-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule, TchSectionError],
  templateUrl: './bulk-cancel-draw.dialog.html',
  styleUrls: ['./draw-action-dialog.scss'],
})
export class BulkCancelDrawDialog {
  protected readonly data = inject<{ draws: DrawView[]; tenantId: string | null }>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<BulkCancelDrawDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);

  readonly form = this.fb.nonNullable.group({
    reasonCode: ['', [Validators.required, Validators.maxLength(96)]],
    reasonLabel: [''],
    force: [false],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const req: CancelDrawsRequest = {
      drawIds: this.data.draws.map(draw => draw.id),
      reasonCode: v.reasonCode.toUpperCase().trim(),
      reasonLabel: v.reasonLabel || undefined,
      force: v.force,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.cancelDraws(req, this.data.tenantId, { suppressShellFeedback: true }).subscribe({
      next: () => { this.submitting.set(false); this.dialogRef.close(true); },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(errorViewModel(err, 'platform.ops.draws.bulk.cancel', this.translate));
      },
    });
  }
}

function errorViewModel(err: unknown, source: string, translate: TranslateService): ErrorViewModel {
  const problem = (err as { error?: ProblemDetail })?.error;
  if (problem) {
    const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
    const copy = resolveErrorFeedbackCopy(normalized, key => translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }

  return {
    title: translate.instant('common.errors.fallback.title'),
    message: translate.instant('common.errors.fallback.message'),
    severity: 'error',
  };
}
