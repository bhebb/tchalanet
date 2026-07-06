import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AdminSectionCardComponent } from '@tch/ui/console';
import { ConsoleAddressSummaryComponent } from '@tch/web/console';

import { TenantDetailView } from '../../data-access/platform-tenants-api.service';
import { TenantProvisioningProfile } from '../../data-access/platform-tenant-contracts';

@Component({
  selector: 'tch-tenant-detail-overview',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminSectionCardComponent, ConsoleAddressSummaryComponent],
  templateUrl: './tenant-detail-overview.component.html',
  styleUrls: ['./tenant-detail-overview.component.scss'],
})
export class TenantDetailOverviewComponent {
  private readonly translate = inject(TranslateService);

  readonly address = input<TenantDetailView['address']>(null);
  readonly profile = input<TenantProvisioningProfile | string | null | undefined>(null);
  readonly primaryAdminEmail = input<string | null | undefined>(null);
  readonly themeCode = input<string | null | undefined>(null);

  profileLabel(): string {
    const profile = this.profile();
    if (!profile) return this.translate.instant('common.not_set');
    const keys: Record<string, string> = {
      MINIMAL: 'platform.tenants.profile.minimal',
      DEFAULT_HAITI_LOTTERY: 'platform.tenants.profile.defaultHaitiLottery',
      DEMO: 'platform.tenants.profile.demo',
    };
    return this.translate.instant(keys[profile] ?? profile);
  }
}
