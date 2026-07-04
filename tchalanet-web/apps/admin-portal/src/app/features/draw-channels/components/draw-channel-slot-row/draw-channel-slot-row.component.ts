import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { DrawChannelSlotConfigView } from '../../data-access/admin-draw-channels.models';

@Component({
  selector: 'tch-draw-channel-slot-row',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SlicePipe, MatButtonModule],
  templateUrl: './draw-channel-slot-row.component.html',
  styleUrls: ['./draw-channel-slot-row.component.scss'],
})
export class DrawChannelSlotRowComponent {
  readonly slot             = input.required<DrawChannelSlotConfigView>();
  readonly index            = input<number>(0);
  readonly resultsAvailable = input<boolean>(false);

  readonly viewResults = output<DrawChannelSlotConfigView>();
  readonly configure   = output<DrawChannelSlotConfigView>();
}
