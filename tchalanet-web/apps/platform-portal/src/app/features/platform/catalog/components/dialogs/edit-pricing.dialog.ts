import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormField, form, min, submit } from '@angular/forms/signals';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe } from '@ngx-translate/core';
import { TchSectionError } from '@tch/ui/components';
import { tchMutation } from '@tch/web/async';

import {
  CatalogPricingView,
  PlatformCatalogApi,
  UpdatePricingRequest,
} from '../../data-access/platform-catalog-api.service';

interface EditPricingFormModel {
  readonly odds: number;
  readonly betOption: number;
  readonly active: boolean;
}

@Component({
  selector: 'tch-edit-pricing-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    TchSectionError,
    TranslatePipe,
  ],
  templateUrl: './edit-pricing.dialog.html',
  styleUrl: './catalog-dialog.scss',
})
export class EditPricingDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialogRef = inject(MatDialogRef<EditPricingDialog>);
  readonly row = inject<CatalogPricingView>(MAT_DIALOG_DATA);

  readonly model = signal<EditPricingFormModel>({
    odds: this.row.odds,
    betOption: this.row.betOption ?? 0,
    active: this.row.active,
  });
  readonly form = form(this.model, path => {
    min(path.odds, 0.01, { message: 'platform.catalog.pricing.validation.oddsMin' });
  });

  readonly savePricing = tchMutation<UpdatePricingRequest, CatalogPricingView>({
    source: 'platform.catalog.pricing.edit',
    run: input => this.api.updatePricing(this.row.id, input),
    onSuccess: updated => this.dialogRef.close(updated),
  });
  readonly feedback = computed(() => this.savePricing.feedback());

  submit(event: Event): void {
    event.preventDefault();
    submit(this.form, async () => {
      this.savePricing.execute(this.toRequest(this.model()));
    });
  }

  private toRequest(value: EditPricingFormModel): UpdatePricingRequest {
    return {
      odds: value.odds,
      betOption: value.betOption > 0 ? value.betOption : null,
      active: value.active,
    };
  }
}
