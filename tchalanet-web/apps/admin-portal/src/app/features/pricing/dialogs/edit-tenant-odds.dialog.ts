import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import {
  mapHttpErrorToProblemDetail,
  webAppErrorFromProblemDetail,
  webAppErrorsFromProblemDetailFields,
} from '@tch/api';
import { TchErrorPanel, TchFieldError, TchFormErrorSummary } from '@tch/ui/components';
import {
  applyServerFieldErrors,
  clearServerFieldErrors,
  clearServerFieldErrorsOnEdit,
  ErrorViewModel,
  resolveErrorFeedbackCopy,
  toErrorViewModel,
  withResolvedErrorCopy,
  withResolvedErrorCopies,
} from '@tch/web/errors';

import {
  AdminPricingApi,
  PayoutRuleType,
  PricingView,
  UpsertTenantOddsRequest,
} from '../data-access/admin-pricing-api.service';
import { PRICING_FIELD_TARGETS, pricingFieldForCode } from '../pricing-error-targets';

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
    TchErrorPanel,
    TchFieldError,
    TchFormErrorSummary,
  ],
  templateUrl: './edit-tenant-odds.dialog.html',
  styleUrls: ['./edit-tenant-odds.dialog.scss'],
})
export class EditTenantOddsDialog {
  protected readonly data = inject<PricingView>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<EditTenantOddsDialog, boolean>);
  private readonly api = inject(AdminPricingApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);
  private readonly destroyRef = inject(DestroyRef);
  readonly payoutRuleTypes: readonly PayoutRuleType[] = ['STAKE_MULTIPLIER', 'FIXED_AMOUNT'];
  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly formSummary = signal<readonly string[]>([]);

  readonly form = this.fb.group({
    payoutRuleType: [
      this.data.payoutRuleType ?? ('STAKE_MULTIPLIER' as PayoutRuleType),
      [Validators.required],
    ],
    odds: [this.data.odds, [Validators.min(0.0001)]],
    fixedAmount: [this.data.fixedAmount ?? null, [Validators.min(0)]],
  });

  readonly isFixedAmount = () => this.form.controls.payoutRuleType.value === 'FIXED_AMOUNT';

  constructor() {
    const cleanup = clearServerFieldErrorsOnEdit(this.form);
    this.destroyRef.onDestroy(() => cleanup.unsubscribe());
  }

  submit(): void {
    if (this.saving()) return;
    clearServerFieldErrors(this.form);
    this.error.set(null);
    this.formSummary.set([]);
    this.form.markAllAsTouched();
    if (this.form.invalid) {
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

    const request: UpsertTenantOddsRequest = {
      gameCode: this.data.gameCode,
      pricingVariantCode: this.data.pricingVariantCode,
      betType: this.data.betType,
      betOption: this.data.betOption ?? null,
      odds: payoutRuleType === 'STAKE_MULTIPLIER' ? odds : null,
      payoutRuleType,
      fixedAmount: payoutRuleType === 'FIXED_AMOUNT' ? fixedAmount : null,
    };

    this.saving.set(true);
    this.api.upsertTenantOdds(request, { suppressShellFeedback: true }).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err: unknown) => {
        this.handleSubmitError(err);
        this.saving.set(false);
      },
    });
  }

  private handleSubmitError(err: unknown): void {
    const problem = mapHttpErrorToProblemDetail(err);
    const fieldErrors = withResolvedErrorCopies(
      webAppErrorsFromProblemDetailFields(problem, 'admin.pricing.odds'),
      key => this.translate.instant(key),
    );
    const remaining = applyServerFieldErrors(this.form, fieldErrors, PRICING_FIELD_TARGETS);
    if (fieldErrors.length && remaining.length) {
      this.formSummary.set(remaining.flatMap(item => (item.message ? [item.message] : [])));
      return;
    }
    if (fieldErrors.length) {
      return;
    }

    const field = pricingFieldForCode(problem.code);
    if (field) {
      const normalized = withResolvedErrorCopy(
        webAppErrorFromProblemDetail(problem, 'admin.pricing.odds', 'field'),
        key => this.translate.instant(key),
      );
      const control = this.form.controls[field];
      control.setErrors({ ...(control.errors ?? {}), server: [normalized] });
      control.markAsTouched();
      return;
    }

    const normalized = webAppErrorFromProblemDetail(problem, 'admin.pricing.odds', 'page');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    this.error.set(toErrorViewModel(normalized, copy));
  }
}
