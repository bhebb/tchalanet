import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { TchErrorViewModel } from '@tch/web/errors';

import {
  ChannelGameSetupView,
  ProviderMatrixView,
  SlotMatrixView,
} from '../../data-access/admin-draw-sales-matrix-api.service';
import {
  DrawSalesMatrixSlotPanelComponent,
  MatrixSlotGameActionEvent,
} from '../matrix-slot-panel/matrix-slot-panel.component';
import { MatrixGameAction } from '../matrix-game-card/matrix-game-card.component';

export interface MatrixProviderGameActionEvent {
  readonly action: MatrixGameAction;
  readonly slot: SlotMatrixView;
  readonly game: ChannelGameSetupView;
}

@Component({
  selector: 'tch-draw-sales-matrix-provider-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatExpansionModule, DrawSalesMatrixSlotPanelComponent],
  templateUrl: './matrix-provider-list.component.html',
  styleUrls: ['./matrix-provider-list.component.scss'],
})
export class DrawSalesMatrixProviderListComponent {
  readonly providers = input.required<readonly ProviderMatrixView[]>();
  readonly actingKey = input<string | null>(null);
  readonly actionErrors = input<Readonly<Record<string, TchErrorViewModel>>>({});
  readonly actionNotices = input<Readonly<Record<string, string>>>({});

  readonly gameAction = output<MatrixProviderGameActionEvent>();

  protected onGameAction(event: MatrixSlotGameActionEvent): void {
    this.gameAction.emit(event);
  }
}
