import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { TchCard, TchDrawLabel } from '@tch/ui/components';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import { consoleLotteryProviderLogoUrl } from '@tch/web/console';
import {
  DrawChannelProviderView,
  DrawChannelProviderTenantStatus,
  DrawChannelSlotConfigView,
} from '../../data-access/admin-draw-channels.models';
import { DrawChannelSourceBadgeComponent } from '../draw-channel-source-badge/draw-channel-source-badge.component';
import { DrawChannelSlotRowComponent } from '../draw-channel-slot-row/draw-channel-slot-row.component';

const STATUS_TONE: Record<DrawChannelProviderTenantStatus, AdminStatusTone> = {
  ACTIVE:       'success',
  NEEDS_CONFIG: 'warning',
  INACTIVE:     'neutral',
  UNAVAILABLE:  'danger',
};

const STATUS_LABEL: Record<DrawChannelProviderTenantStatus, string> = {
  ACTIVE:       'admin.drawChannels.status.active',
  NEEDS_CONFIG: 'admin.drawChannels.status.needsConfig',
  INACTIVE:     'admin.drawChannels.status.inactive',
  UNAVAILABLE:  'admin.drawChannels.status.unavailable',
};

@Component({
  selector: 'tch-draw-channel-provider-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    LowerCasePipe,
    RouterLink,
    MatButtonModule,
    TranslatePipe,
    TchCard,
    TchDrawLabel,
    AdminStatusPillComponent,
    DrawChannelSourceBadgeComponent,
    DrawChannelSlotRowComponent,
  ],
  templateUrl: './draw-channel-provider-card.component.html',
  styleUrls: ['./draw-channel-provider-card.component.scss'],
})
export class DrawChannelProviderCardComponent {
  readonly provider = input.required<DrawChannelProviderView>();
  readonly savingChannelId = input<string | null>(null);

  readonly toggleChannelEnabled = output<{ slot: DrawChannelSlotConfigView; enabled: boolean }>();

  readonly statusTone  = computed<AdminStatusTone>(() => STATUS_TONE[this.provider().tenantStatus]);
  readonly statusLabel = computed<string>(() => STATUS_LABEL[this.provider().tenantStatus]);
  readonly providerLogo = computed<string | null>(() => consoleLotteryProviderLogoUrl(this.provider().providerCode));
}
