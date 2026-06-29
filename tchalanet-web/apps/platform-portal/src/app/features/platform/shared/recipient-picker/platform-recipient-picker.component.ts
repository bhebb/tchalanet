import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { TchMultiSearchSelect, TchSearchOption, TchSearchSelect } from '@tch/ui/components';
import { Observable, forkJoin, map } from 'rxjs';

import {
  PlatformSuperAdminView,
  PlatformTenantsApi,
  TenantAdminView,
  TenantSummaryView,
} from '../../tenants/data-access/platform-tenants-api.service';
import {
  PlatformRecipientSellerTerminalRow,
  PlatformRecipientSellerTerminalsApi,
} from '../platform-recipient-seller-terminals-api.service';

export type PlatformRecipientKind = 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'SELLER_TERMINAL';
export type PlatformRecipientActorType = 'APP_USER' | 'SELLER_TERMINAL';

export interface PlatformRecipientTarget {
  actorType: PlatformRecipientActorType;
  actorId: string;
}

export interface PlatformRecipientOption {
  key: string;
  kind: PlatformRecipientKind;
  label: string;
  detail: string;
  status: string;
  email: string | null;
  phone: string | null;
  target: PlatformRecipientTarget;
}

export interface PlatformRecipientPickerSelection {
  tenantId: string | null;
  tenantName: string | null;
  recipients: PlatformRecipientOption[];
  targets: PlatformRecipientTarget[];
}

@Component({
  selector: 'tch-platform-recipient-picker',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TchSearchSelect, TchMultiSearchSelect],
  templateUrl: './platform-recipient-picker.component.html',
  styleUrl: './platform-recipient-picker.component.scss',
})
export class PlatformRecipientPickerComponent {
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly sellerTerminalApi = inject(PlatformRecipientSellerTerminalsApi);

  readonly selectionChange = output<PlatformRecipientPickerSelection>();

  readonly selectedTenant = signal<TchSearchOption<TenantSummaryView> | null>(null);
  readonly selectedRecipients = signal<readonly TchSearchOption<PlatformRecipientOption>[]>([]);

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi
      .listTenants({ q: query, page: 0, size: 12, status: 'ACTIVE' })
      .pipe(map(page => (page.items ?? []).map(tenant => this.tenantOption(tenant))));

  readonly searchRecipients = (query: string): Observable<readonly TchSearchOption<PlatformRecipientOption>[]> => {
    const tenant = this.selectedTenant()?.data ?? null;
    const tenantId = tenant?.id ?? tenant?.tenantId ?? '';
    const normalized = query.trim().toLowerCase();

    const superAdmins$ = this.tenantsApi.listSuperAdmins().pipe(
      map(admins => admins
        .map(admin => this.recipientSearchOption(this.superAdminRecipient(admin)))
        .filter(option => this.matchesQuery(option, normalized))),
    );

    if (!tenantId) {
      return superAdmins$;
    }

    return forkJoin({
      superAdmins: superAdmins$,
      admins: this.tenantsApi.listTenantAdmins(tenantId),
      terminals: this.sellerTerminalApi.list({ tenantId, q: query, page: 0, size: 20 }),
    }).pipe(
      map(result => [
        ...result.superAdmins,
        ...result.admins
          .map(admin => this.recipientSearchOption(this.tenantAdminRecipient(admin)))
          .filter(option => this.matchesQuery(option, normalized)),
        ...(result.terminals.items ?? []).map(terminal =>
          this.recipientSearchOption(this.sellerTerminalRecipient(terminal)),
        ),
      ]),
    );
  };

  updateTenant(option: TchSearchOption | null): void {
    this.selectedTenant.set(option as TchSearchOption<TenantSummaryView> | null);
    this.selectedRecipients.update(recipients =>
      recipients.filter(item => item.data?.kind === 'SUPER_ADMIN'),
    );
    this.emitSelection();
  }

  updateRecipients(options: readonly TchSearchOption[]): void {
    this.selectedRecipients.set(options as readonly TchSearchOption<PlatformRecipientOption>[]);
    this.emitSelection();
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

  private recipientSearchOption(recipient: PlatformRecipientOption): TchSearchOption<PlatformRecipientOption> {
    return {
      id: recipient.key,
      title: recipient.label,
      subtitle: recipient.detail,
      badge: recipient.status,
      icon: recipient.kind === 'SELLER_TERMINAL' ? 'point_of_sale' : 'person',
      disabled: !this.canSelect(recipient),
      data: recipient,
    };
  }

  private superAdminRecipient(admin: PlatformSuperAdminView): PlatformRecipientOption {
    return {
      key: `SUPER_ADMIN:${admin.id}`,
      kind: 'SUPER_ADMIN',
      label: admin.displayName || admin.email || admin.id,
      detail: admin.email || '',
      status: admin.status,
      email: admin.email || null,
      phone: null,
      target: { actorType: 'APP_USER', actorId: admin.id },
    };
  }

  private tenantAdminRecipient(admin: TenantAdminView): PlatformRecipientOption {
    return {
      key: `TENANT_ADMIN:${admin.id}`,
      kind: 'TENANT_ADMIN',
      label: admin.displayName || admin.email || admin.id,
      detail: admin.email || admin.phone || '',
      status: admin.status,
      email: admin.email || null,
      phone: admin.phone || null,
      target: { actorType: 'APP_USER', actorId: admin.id },
    };
  }

  private sellerTerminalRecipient(terminal: PlatformRecipientSellerTerminalRow): PlatformRecipientOption {
    return {
      key: `SELLER_TERMINAL:${terminal.id.value}`,
      kind: 'SELLER_TERMINAL',
      label: terminal.displayName || terminal.terminalCode || terminal.id.value,
      detail: [terminal.terminalCode, terminal.email, terminal.phoneNumber].filter(Boolean).join(' · '),
      status: terminal.status,
      email: terminal.email || null,
      phone: terminal.phoneNumber || null,
      target: { actorType: 'SELLER_TERMINAL', actorId: terminal.id.value },
    };
  }

  private canSelect(recipient: PlatformRecipientOption): boolean {
    return recipient.status !== 'DELETED';
  }

  private matchesQuery(option: TchSearchOption<PlatformRecipientOption>, query: string): boolean {
    if (!query) return true;
    return `${option.title} ${option.subtitle ?? ''} ${option.badge ?? ''}`.toLowerCase().includes(query);
  }

  private emitSelection(): void {
    const tenant = this.selectedTenant()?.data ?? null;
    const recipients = this.selectedRecipients()
      .map(item => item.data)
      .filter((item): item is PlatformRecipientOption => !!item);
    this.selectionChange.emit({
      tenantId: tenant?.id ?? tenant?.tenantId ?? null,
      tenantName: tenant?.name ?? null,
      recipients,
      targets: recipients.map(item => item.target),
    });
  }
}
