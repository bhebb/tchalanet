import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { BusinessDayStatus, UpsertBusinessDayRequest } from '../../../business-days-api.service';

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

  readonly form = this.fb.group({
    date: ['', [Validators.required, Validators.pattern(/^\d{4}-\d{2}-\d{2}$/)]],
    status: ['CLOSED' as BusinessDayStatus, Validators.required],
    reason: [''],
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
