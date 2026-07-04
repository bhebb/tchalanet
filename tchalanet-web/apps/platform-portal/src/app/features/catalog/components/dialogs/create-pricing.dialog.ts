import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogClose, MatDialogRef } from '@angular/material/dialog';
import { TranslatePipe } from '@ngx-translate/core';
import { TchSearchOption, TchSearchSelect } from '@tch/ui/components';
import { TchSectionError } from '@tch/ui/components';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { ConsolePricingFormComponent, ConsolePricingFormValue } from '@tch/web/console';
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

@Component({
  selector: 'tch-create-pricing-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminDialogShellComponent,
    ConsolePricingFormComponent,
    MatButtonModule,
    MatDialogClose,
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

  readonly pricingForm = viewChild.required(ConsolePricingFormComponent);
  readonly betTypes = CATALOG_BET_TYPES;
  readonly formValue = signal<ConsolePricingFormValue>({
    gameCode: '',
    betType: '',
    betOption: 0,
    odds: 0,
    active: true,
  });
  readonly tenantId = signal('');

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
    this.tenantId.set(tenant?.id ?? tenant?.tenantId ?? '');
  }

  submit(): void {
    this.pricingForm().submit();
  }

  save(value: ConsolePricingFormValue): void {
    this.savePricing.execute(this.toRequest(value));
  }

  private toRequest(value: ConsolePricingFormValue): CreatePricingRequest {
    return {
      gameCode: value.gameCode.trim().toUpperCase(),
      betType: value.betType as BetType,
      betOption: value.betOption > 0 ? value.betOption : null,
      odds: value.odds,
      tenantId: this.tenantId() || null,
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
