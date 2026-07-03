import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export interface DrawDetailFact {
  readonly label: string;
  readonly value: string;
  readonly code?: boolean;
}

export interface DrawDetailOverviewView {
  readonly providerLogo: string | null;
  readonly providerLabel: string;
  readonly providerCode: string;
  readonly slotCode: string;
  readonly displayName: string;
  readonly supporting: string;
  readonly salesStatus: string;
  readonly salesOpen: boolean;
  readonly facts: readonly DrawDetailFact[];
}

@Component({
  selector: 'tch-draw-detail-overview',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './draw-detail-overview.component.html',
  styleUrl: './draw-detail-overview.component.scss',
})
export class DrawDetailOverviewComponent {
  readonly view = input.required<DrawDetailOverviewView>();
}
