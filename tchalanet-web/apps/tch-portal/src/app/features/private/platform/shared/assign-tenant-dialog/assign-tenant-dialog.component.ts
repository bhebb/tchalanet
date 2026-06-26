import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { TchSearchOption, TchSearchSelect } from '@tch/ui/components';
import { Observable, map } from 'rxjs';

import {
  PlatformTenantsApi,
  TenantSummaryView,
} from '../../tenants/data-access/platform-tenants-api.service';

export interface AssignTenantResult {
  tenantId: string;
  tenantName: string;
}

@Component({
  selector: 'tch-assign-tenant-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatDialogModule,
    MatButtonModule,
    TchSearchSelect,
  ],
  templateUrl: './assign-tenant-dialog.component.html',
})
export class AssignTenantDialog {
  private readonly dialogRef = inject(MatDialogRef<AssignTenantDialog, AssignTenantResult>);
  private readonly tenantsApi = inject(PlatformTenantsApi);

  readonly selected = signal<TchSearchOption<TenantSummaryView> | null>(null);

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi.listTenants({ q: query, page: 0, size: 10, status: 'ACTIVE' }).pipe(
      map(result => result.items.map(tenant => this.toTenantOption(tenant))),
    );

  select(tenant: TchSearchOption | null): void {
    this.selected.set(tenant as TchSearchOption<TenantSummaryView> | null);
  }

  confirm(): void {
    const tenant = this.selected()?.data;
    if (!tenant) return;
    this.dialogRef.close({
      tenantId: tenant.id ?? tenant.tenantId ?? '',
      tenantName: tenant.name,
    });
  }

  cancel(): void {
    this.dialogRef.close();
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
