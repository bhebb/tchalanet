import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormField, form, min, required, submit } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { TchSearchOption, TchSearchSelect } from '@tch/ui/components';
import { TchSectionError } from '@tch/ui/components';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { tchMutation } from '@tch/web/async';
import { Observable, map } from 'rxjs';

import { PlatformTenantsApi, TenantSummaryView } from '../../../tenants/data-access/platform-tenants-api.service';
import {
  BetType,
  CatalogPricingView,
  CreatePricingRequest,
  PlatformCatalogApi,
} from '../../data-access/platform-catalog-api.service';
import { CATALOG_BET_TYPES } from './catalog-pricing-options';

interface CreatePricingFormModel {
  readonly gameCode: string;
  readonly betType: BetType | '';
  readonly betOption: number;
  readonly odds: number;
  readonly tenantId: string;
  readonly active: boolean;
}

@Component({
  selector: 'tch-create-pricing-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormField,
    AdminDialogShellComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TchSearchSelect,
    TchSectionError,
    TranslatePipe,
  ],
  templateUrl: './create-pricing.dialog.html',
})
export class CreatePricingDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly dialogRef = inject(MatDialogRef<CreatePricingDialog>);

  readonly betTypes = CATALOG_BET_TYPES;
  readonly model = signal<CreatePricingFormModel>({
    gameCode: '',
    betType: '',
    betOption: 0,
    odds: 0,
    tenantId: '',
    active: true,
  });
  readonly form = form(this.model, path => {
    required(path.gameCode, { message: 'platform.catalog.pricing.validation.gameCodeRequired' });
    required(path.betType, { message: 'platform.catalog.pricing.validation.betTypeRequired' });
    min(path.odds, 0.01, { message: 'platform.catalog.pricing.validation.oddsMin' });
  });

  readonly savePricing = tchMutation<CreatePricingRequest, CatalogPricingView>({
    source: 'platform.catalog.pricing.create',
    run: input => this.api.createPricing(input),
    onSuccess: created => this.dialogRef.close(created),
  });
  readonly feedback = computed(() => this.savePricing.feedback());

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi.listTenants({ q: query, page: 0, size: 12, status: null }).pipe(
      map(page => page.items.map(tenant => this.toTenantOption(tenant))),
    );

  selectTenant(option: TchSearchOption | null): void {
    const tenant = option?.data as TenantSummaryView | undefined;
    this.model.update(current => ({
      ...current,
      tenantId: tenant?.id ?? tenant?.tenantId ?? '',
    }));
  }

  submit(event: Event): void {
    event.preventDefault();
    submit(this.form, async () => {
      this.savePricing.execute(this.toRequest(this.model()));
    });
  }

  private toRequest(value: CreatePricingFormModel): CreatePricingRequest {
    return {
      gameCode: value.gameCode.trim().toUpperCase(),
      betType: value.betType as BetType,
      betOption: value.betOption > 0 ? value.betOption : null,
      odds: value.odds,
      tenantId: value.tenantId || null,
      active: value.active,
    };
  }

  private toTenantOption(tenant: TenantSummaryView): TchSearchOption<TenantSummaryView> {
    return {
      id: tenant.id ?? tenant.tenantId ?? tenant.code,
      title: tenant.name,
      subtitle: tenant.code,
      badge: tenant.status,
      icon: 'apartment',
      data: tenant,
    };
  }
}
