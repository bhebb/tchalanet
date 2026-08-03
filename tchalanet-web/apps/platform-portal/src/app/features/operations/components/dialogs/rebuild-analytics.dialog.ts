import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';

export interface RebuildAnalyticsDialogData {
  tenantLabel: string;
  from: string;
  to: string;
}

@Component({
  selector: 'tch-rebuild-analytics-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './rebuild-analytics.dialog.html',
  styleUrl: './rebuild-analytics.dialog.scss',
})
export class RebuildAnalyticsDialog {
  readonly data = inject<RebuildAnalyticsDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<RebuildAnalyticsDialog, string>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    reason: ['', [Validators.required, Validators.minLength(10)]],
  });

  cancel(): void {
    this.dialogRef.close();
  }

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.form.controls.reason.value?.trim());
  }
}
