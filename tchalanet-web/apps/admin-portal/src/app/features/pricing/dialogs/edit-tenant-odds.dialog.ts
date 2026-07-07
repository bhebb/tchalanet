import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';

import { PricingView, UpsertTenantOddsRequest } from '../data-access/admin-pricing-api.service';

@Component({
  selector: 'tch-edit-tenant-odds-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    TranslatePipe,
  ],
  templateUrl: './edit-tenant-odds.dialog.html',
  styleUrls: ['./edit-tenant-odds.dialog.scss'],
})
export class EditTenantOddsDialog {
  protected readonly data = inject<PricingView>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<EditTenantOddsDialog, UpsertTenantOddsRequest>);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.group({
    odds: [this.data.odds, [Validators.required, Validators.min(0.0001)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const odds = this.form.controls.odds.value;
    if (odds == null) return;

    this.dialogRef.close({
      gameCode: this.data.gameCode,
      pricingVariantCode: this.data.pricingVariantCode,
      betType: this.data.betType,
      betOption: this.data.betOption ?? null,
      odds,
    });
  }
}
