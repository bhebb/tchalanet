import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { TenantProvisioningPreviewView } from '../../../../data-access/platform-provisioning-api.service';
import { DOMAIN_LABEL_KEYS, translatedLabel } from '../tenant-provisioning-display';

@Component({
  selector: 'tch-tenant-provisioning-profile-summary',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tenant-provisioning-profile-summary.component.html',
  styleUrls: ['./tenant-provisioning-profile-summary.component.scss'],
})
export class TenantProvisioningProfileSummaryComponent {
  private readonly translate = inject(TranslateService);

  readonly description = input<string | null>(null);
  readonly preview = input<TenantProvisioningPreviewView | null>(null);

  readonly domainLabels = computed(() => {
    const preview = this.preview();
    if (!preview?.includedDomains?.length) return [];
    return preview.includedDomains.map(domain =>
      translatedLabel(this.translate, DOMAIN_LABEL_KEYS[domain] ?? domain),
    );
  });
}
