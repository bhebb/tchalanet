import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';

import { TchSectionError } from '@tch/ui/components';
import { ErrorViewModel } from '@tch/web/errors';
import { AdminStatusPillComponent } from '@tch/ui/console';
import { OpsLaunchResponse } from '../../data-access/platform-ops-api.service';

export type AnyBatchResult = OpsLaunchResponse;

@Component({
  selector: 'tch-batch-op-dialog',
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
    MatTableModule,
    AdminStatusPillComponent,
    TchSectionError,
  ],
  templateUrl: './batch-op.dialog.html',
  styleUrls: ['./batch-op.dialog.scss'],
})
export class BatchOpDialog {
  protected readonly data = inject<{
    title: string;
    hasLimit?: boolean;
    execute: (tenantCodes: string[], dryRun: boolean, limit?: number) => void;
  }>(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<BatchOpDialog>);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly result = signal<AnyBatchResult | null>(null);
  readonly outcomeColumns = ['tenantId', 'ok', 'error'];

  readonly form = this.fb.group({
    tenantCodes: [''],
    limit: [10000],
    dryRun: [true],
  });

  submit(): void {
    if (this.submitting() || this.result()) return;
    const v = this.form.value;
    const tenantCodes = v.tenantCodes ? v.tenantCodes.split(',').map(s => s.trim()).filter(Boolean) : [];
    this.submitting.set(true);
    this.data.execute(tenantCodes, v.dryRun ?? true, v.limit ?? 10000);
  }

  setResult(res: AnyBatchResult): void {
    this.submitting.set(false);
    this.result.set(res);
  }

  setError(error: ErrorViewModel): void {
    this.submitting.set(false);
    this.error.set(error);
  }
}
