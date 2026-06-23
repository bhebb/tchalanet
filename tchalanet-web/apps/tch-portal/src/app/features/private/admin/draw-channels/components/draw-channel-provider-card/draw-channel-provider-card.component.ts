import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { TchCard } from '@tch/ui/components';
import { AdminStatusPillComponent, AdminStatusTone } from '../../../../shared/admin-ui/admin-status-pill.component';
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
  ACTIVE:       'Actif',
  NEEDS_CONFIG: 'À configurer',
  INACTIVE:     'Inactif',
  UNAVAILABLE:  'Non disponible',
};

@Component({
  selector: 'tch-draw-channel-provider-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    LowerCasePipe,
    MatButtonModule,
    TchCard,
    AdminStatusPillComponent,
    DrawChannelSourceBadgeComponent,
    DrawChannelSlotRowComponent,
  ],
  templateUrl: './draw-channel-provider-card.component.html',
  styleUrls: ['./draw-channel-provider-card.component.scss'],
})
export class DrawChannelProviderCardComponent {
  readonly provider = input.required<DrawChannelProviderView>();
  readonly mode     = input<'config' | 'readonly'>('config');

  readonly configure           = output<DrawChannelProviderView>();
  readonly viewProviderResults = output<DrawChannelProviderView>();
  readonly viewSlotResults     = output<{ provider: DrawChannelProviderView; slot: DrawChannelSlotConfigView }>();
  readonly toggleSlot          = output<DrawChannelSlotConfigView>();

  readonly statusTone  = computed<AdminStatusTone>(() => STATUS_TONE[this.provider().tenantStatus]);
  readonly statusLabel = computed<string>(() => STATUS_LABEL[this.provider().tenantStatus]);

  readonly acquisitionSubline = computed<string>(() => {
    const a = this.provider().resultAcquisition;
    const parts: string[] = [];
    if (a.lastSyncAt)        parts.push(`Dernière : ${a.lastSyncAt}`);
    if (a.nextSyncAt)        parts.push(`Prochaine : ${a.nextSyncAt}`);
    if (a.source)            parts.push(a.source);
    if (a.lastManualEntryAt) parts.push(`Dernière saisie : ${a.lastManualEntryAt}`);
    if (a.lastAttemptAt)     parts.push(`Dernière tentative : ${a.lastAttemptAt}`);
    return parts.join(' · ');
  });

  onViewSlotResults(slot: DrawChannelSlotConfigView): void {
    this.viewSlotResults.emit({ provider: this.provider(), slot });
  }
}
