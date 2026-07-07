import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { BadgeStatus, TchDrawLabel, TchStatusBadge } from '@tch/ui/components';
import { DrawChannelSlotConfigView } from '../../data-access/admin-draw-channels.models';

@Component({
  selector: 'tch-draw-channel-slot-row',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SlicePipe, MatButtonModule, TranslatePipe, TchDrawLabel, TchStatusBadge],
  templateUrl: './draw-channel-slot-row.component.html',
  styleUrls: ['./draw-channel-slot-row.component.scss'],
})
export class DrawChannelSlotRowComponent {
  readonly slot             = input.required<DrawChannelSlotConfigView>();
  readonly index            = input<number>(0);
  readonly resultsAvailable = input<boolean>(false);

  readonly viewResults = output<DrawChannelSlotConfigView>();
  readonly configure   = output<DrawChannelSlotConfigView>();

  protected slotStatus(slot: DrawChannelSlotConfigView): BadgeStatus {
    if (!slot.enabled) return 'missing';
    return slot.drawTime ? 'ready' : 'warning';
  }

  protected slotStatusLabelKey(slot: DrawChannelSlotConfigView): string {
    if (!slot.enabled) return 'admin.drawChannels.slots.status.disabled';
    return slot.drawTime
      ? 'admin.drawChannels.slots.status.active'
      : 'admin.drawChannels.slots.status.needsConfig';
  }
}
