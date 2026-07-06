import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { ConsoleHaitiLotGameMapping } from './console-haiti-lot-game-mapping';

@Component({
  selector: 'tch-console-haiti-lot-game-mapping',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './console-haiti-lot-game-mapping.component.html',
  styleUrls: ['./console-haiti-lot-game-mapping.component.scss'],
})
export class ConsoleHaitiLotGameMappingComponent {
  readonly mappings = input.required<readonly ConsoleHaitiLotGameMapping[]>();
}
