import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { TranslateService } from '@ngx-translate/core';

import { TchNotice } from '@tch/ui/components';
import { AdminSectionCardComponent } from '@tch/ui/console';

import { TenantProvisioningResultView } from '../../../../data-access/platform-provisioning-api.service';
import { warningLabel } from '../tenant-provisioning-display';

@Component({
  selector: 'tch-tenant-provisioning-success',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, TchNotice, AdminSectionCardComponent],
  templateUrl: './tenant-provisioning-success.component.html',
  styleUrls: ['./tenant-provisioning-success.component.scss'],
})
export class TenantProvisioningSuccessComponent {
  private readonly translate = inject(TranslateService);

  readonly result = input.required<TenantProvisioningResultView>();

  readonly warningLabels = computed(() =>
    (this.result().warnings ?? []).map(warning => warningLabel(this.translate, warning)),
  );
}
