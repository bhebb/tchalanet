import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';

import {
  PayoutRuleType,
  PricingView,
  UpsertTenantOddsRequest,
} from '../data-access/admin-pricing-api.service';

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
    MatSelectModule,
    TranslatePipe,
  ],
  templateUrl: './edit-tenant-odds.dialog.html',
  styleUrls: ['./edit-tenant-odds.dialog.scss'],
})
export class EditTenantOddsDialog {
  protected readonly data = inject<PricingView>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<EditTenantOddsDialog, UpsertTenantOddsRequest>);
  private readonly fb = inject(FormBuilder);
  readonly payoutRuleTypes: readonly PayoutRuleType[] = ['STAKE_MULTIPLIER', 'FIXED_AMOUNT'];

  readonly form = this.fb.group({
    payoutRuleType: [this.data.payoutRuleType ?? 'STAKE_MULTIPLIER' as PayoutRuleType, [Validators.required]],
    odds: [this.data.odds, [Validators.min(0.0001)]],
    fixedAmount: [this.data.fixedAmount ?? null, [Validators.min(0)]],
  });

  readonly isFixedAmount = () => this.form.controls.payoutRuleType.value === 'FIXED_AMOUNT';

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payoutRuleType = this.form.controls.payoutRuleType.value ?? 'STAKE_MULTIPLIER';
    const odds = this.form.controls.odds.value;
    const fixedAmount = this.form.controls.fixedAmount.value;
    if (payoutRuleType === 'STAKE_MULTIPLIER' && (odds == null || odds <= 0)) {
      this.form.controls.odds.setErrors({ min: true });
      this.form.controls.odds.markAsTouched();
      return;
    }
    if (payoutRuleType === 'FIXED_AMOUNT' && (fixedAmount == null || fixedAmount < 0)) {
      this.form.controls.fixedAmount.setErrors({ min: true });
      this.form.controls.fixedAmount.markAsTouched();
      return;
    }

    this.dialogRef.close({
      gameCode: this.data.gameCode,
      pricingVariantCode: this.data.pricingVariantCode,
      betType: this.data.betType,
      betOption: this.data.betOption ?? null,
      odds: payoutRuleType === 'STAKE_MULTIPLIER' ? odds : null,
      payoutRuleType,
      fixedAmount: payoutRuleType === 'FIXED_AMOUNT' ? fixedAmount : null,
    });
  }
}
