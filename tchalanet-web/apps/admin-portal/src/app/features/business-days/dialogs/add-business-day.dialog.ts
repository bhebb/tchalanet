import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { BusinessDayStatus, UpsertBusinessDayRequest } from '.././data-access/business-days-api.service';

export interface AddBusinessDayDialogData {
  readonly date?: string;
  readonly status?: BusinessDayStatus;
  readonly reason?: string;
}

@Component({
  selector: 'tch-add-business-day-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './add-business-day.dialog.html',
  styleUrls: ['./add-business-day.dialog.scss'],
})
export class AddBusinessDayDialog {
  private readonly dialogRef = inject(MatDialogRef<AddBusinessDayDialog>);
  private readonly fb = inject(FormBuilder);
  private readonly data = inject<AddBusinessDayDialogData | null>(MAT_DIALOG_DATA, { optional: true });

  readonly form = this.fb.group({
    date: [this.data?.date ?? '', [Validators.required, Validators.pattern(/^\d{4}-\d{2}-\d{2}$/)]],
    status: [this.data?.status ?? 'CLOSED' as BusinessDayStatus, Validators.required],
    reason: [this.data?.reason ?? ''],
  });

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const req: UpsertBusinessDayRequest = {
      date: v.date ?? '',
      status: v.status ?? 'CLOSED',
      reason: v.reason || undefined,
    };
    this.dialogRef.close(req);
  }
}
