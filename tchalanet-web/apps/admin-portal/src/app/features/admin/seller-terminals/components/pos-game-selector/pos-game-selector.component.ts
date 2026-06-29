import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { PosGameView } from '../../data-access/admin-seller-terminal-pos.models';

@Component({
  selector: 'tch-pos-game-selector',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pos-game-selector.component.html',
  styleUrls: ['./pos-game-selector.component.scss'],
})
export class PosGameSelectorComponent {
  readonly games = input<PosGameView[]>([]);
  readonly selectedCode = input<string | null>(null);
  readonly gameSelected = output<string>();

  select(gameCode: string): void {
    this.gameSelected.emit(gameCode);
  }
}
