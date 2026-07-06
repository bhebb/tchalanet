import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AdminEmptyStateComponent, AdminSectionCardComponent } from '@tch/ui/console';

import { TenantCapabilitySnapshot } from '../../data-access/platform-tenant-contracts';

@Component({
  selector: 'tch-tenant-entitlements-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, DatePipe, AdminSectionCardComponent, AdminEmptyStateComponent],
  templateUrl: './tenant-entitlements-panel.component.html',
  styleUrls: ['./tenant-entitlements-panel.component.scss'],
})
export class TenantEntitlementsPanelComponent {
  readonly snapshot = input<TenantCapabilitySnapshot | null>(null);

  readonly featureEntries = computed(() => Object.entries(this.snapshot()?.features ?? {}));
  readonly limitEntries = computed(() => Object.entries(this.snapshot()?.limits ?? {}));
}
