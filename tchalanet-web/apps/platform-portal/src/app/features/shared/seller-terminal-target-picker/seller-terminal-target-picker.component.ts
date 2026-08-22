import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { Observable, map, of } from 'rxjs';

import { TchSearchOption, TchSearchSelect } from '@tch/ui/components';

import {
  PlatformRecipientSellerTerminalRow,
  PlatformRecipientSellerTerminalsApi,
} from '../data-access/platform-recipient-seller-terminals-api.service';
import {
  PlatformTenantsApi,
  TenantSummaryView,
} from '../../tenants/data-access/platform-tenants-api.service';

export interface SellerTerminalTargetSelection {
  tenant: TenantSummaryView | null;
  tenantId: string | null;
  sellerTerminal: PlatformRecipientSellerTerminalRow | null;
  sellerTerminalId: string | null;
}

@Component({
  selector: 'tch-seller-terminal-target-picker',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchSearchSelect],
  templateUrl: './seller-terminal-target-picker.component.html',
  styleUrls: ['./seller-terminal-target-picker.component.scss'],
})
export class SellerTerminalTargetPickerComponent {
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly sellerTerminalApi = inject(PlatformRecipientSellerTerminalsApi);

  readonly tenantLabel = input.required<string>();
  readonly tenantPlaceholder = input.required<string>();
  readonly tenantEmptyLabel = input.required<string>();
  readonly sellerTerminalLabel = input.required<string>();
  readonly sellerTerminalPlaceholder = input.required<string>();
  readonly sellerTerminalEmptyLabel = input.required<string>();

  readonly targetChange = output<SellerTerminalTargetSelection>();

  readonly selectedTenant = signal<TenantSummaryView | null>(null);
  readonly selectedSellerTerminal = signal<PlatformRecipientSellerTerminalRow | null>(null);

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi.listTenants({ q: query, page: 0, size: 12, status: 'ACTIVE' }).pipe(
      map(page => (page.items ?? []).map(tenant => this.tenantOption(tenant))),
    );

  readonly searchSellerTerminals = (query: string): Observable<readonly TchSearchOption<PlatformRecipientSellerTerminalRow>[]> => {
    const tenantId = this.selectedTenantId();
    if (!tenantId) return of([]);
    return this.sellerTerminalApi.list({ tenantId, q: query, page: 0, size: 20 }).pipe(
      map(page => (page.items ?? []).map(terminal => this.sellerTerminalOption(terminal))),
    );
  };

  selectTenant(option: TchSearchOption | null): void {
    const tenant = (option?.data as TenantSummaryView | undefined) ?? null;
    this.selectedTenant.set(tenant);
    this.selectedSellerTerminal.set(null);
    this.emitTarget();
  }

  selectSellerTerminal(option: TchSearchOption | null): void {
    const terminal = (option?.data as PlatformRecipientSellerTerminalRow | undefined) ?? null;
    this.selectedSellerTerminal.set(terminal);
    this.emitTarget();
  }

  private selectedTenantId(): string | null {
    const tenant = this.selectedTenant();
    return tenant?.id ?? tenant?.tenantId ?? null;
  }

  private emitTarget(): void {
    const tenant = this.selectedTenant();
    const sellerTerminal = this.selectedSellerTerminal();
    this.targetChange.emit({
      tenant,
      tenantId: tenant?.id ?? tenant?.tenantId ?? null,
      sellerTerminal,
      sellerTerminalId: sellerTerminal?.id.value ?? null,
    });
  }

  private tenantOption(tenant: TenantSummaryView): TchSearchOption<TenantSummaryView> {
    return {
      id: tenant.id ?? tenant.tenantId ?? tenant.code,
      title: tenant.name,
      subtitle: tenant.code,
      badge: tenant.status,
      icon: 'apartment',
      data: tenant,
    };
  }

  private sellerTerminalOption(
    terminal: PlatformRecipientSellerTerminalRow,
  ): TchSearchOption<PlatformRecipientSellerTerminalRow> {
    return {
      id: terminal.id.value,
      title: terminal.displayName || terminal.terminalCode,
      subtitle: terminal.terminalCode,
      badge: terminal.status,
      icon: 'point_of_sale',
      data: terminal,
    };
  }
}
