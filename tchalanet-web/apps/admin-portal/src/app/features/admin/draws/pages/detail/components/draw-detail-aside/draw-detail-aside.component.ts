import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

export interface DrawDetailAsideView {
  readonly title: string;
  readonly dateTime: string;
  readonly slotKey: string;
  readonly countdown: string;
  readonly salesStatus: string;
  readonly salesOpen: boolean;
  readonly ticketCount: string;
  readonly salesAmount: string;
  readonly sellerCount: string;
  readonly hotSelections: string;
  readonly resultFollowup: string;
  readonly advice: string;
}

@Component({
  selector: 'tch-draw-detail-aside',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule],
  templateUrl: './draw-detail-aside.component.html',
  styleUrl: './draw-detail-aside.component.scss',
})
export class DrawDetailAsideComponent {
  readonly view = input.required<DrawDetailAsideView>();
}
