import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogClose, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { TchSectionError } from '@tch/ui/components';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { ConsolePricingFormComponent, ConsolePricingFormValue } from '@tch/web/console';
import { tchMutation } from '@tch/web/async';

import {
  CatalogPricingView,
  PlatformCatalogApi,
  UpdatePricingRequest,
} from '../../data-access/platform-catalog-api.service';

@Component({
  selector: 'tch-edit-pricing-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminDialogShellComponent,
    ConsolePricingFormComponent,
    MatButtonModule,
    MatDialogClose,
    TchSectionError,
    TranslatePipe,
  ],
  templateUrl: './edit-pricing.dialog.html',
})
export class EditPricingDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialogRef = inject(MatDialogRef<EditPricingDialog>);
  readonly row = inject<CatalogPricingView>(MAT_DIALOG_DATA);

  readonly pricingForm = viewChild.required(ConsolePricingFormComponent);
  readonly formValue = signal<ConsolePricingFormValue>({
    gameCode: this.row.gameCode,
    betType: this.row.betType,
    odds: this.row.odds,
    betOption: this.row.betOption ?? 0,
    active: this.row.active,
  });

  readonly savePricing = tchMutation<UpdatePricingRequest, CatalogPricingView>({
    source: 'platform.catalog.pricing.edit',
    run: input => this.api.updatePricing(this.row.id, input),
    onSuccess: updated => this.dialogRef.close(updated),
  });
  readonly feedback = computed(() => this.savePricing.feedback());

  submit(): void {
    this.pricingForm().submit();
  }

  save(value: ConsolePricingFormValue): void {
    this.savePricing.execute(this.toRequest(value));
  }

  private toRequest(value: ConsolePricingFormValue): UpdatePricingRequest {
    return {
      odds: value.odds,
      betOption: value.betOption > 0 ? value.betOption : null,
      active: value.active,
    };
  }
}
