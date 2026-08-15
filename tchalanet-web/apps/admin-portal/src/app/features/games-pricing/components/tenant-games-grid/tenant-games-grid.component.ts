import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { TenantGamePricingView } from '../../data-access/admin-games-pricing.models';
import {
  TenantGameCardComponent,
  TenantGameCardError,
} from '../tenant-game-card/tenant-game-card.component';

@Component({
  selector: 'tch-tenant-games-grid',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TenantGameCardComponent, TranslatePipe],
  templateUrl: './tenant-games-grid.component.html',
  styleUrls: ['./tenant-games-grid.component.scss'],
})
export class TenantGamesGridComponent {
  readonly games = input.required<readonly TenantGamePricingView[]>();
  readonly actionErrors = input<Readonly<Record<string, TenantGameCardError>>>({});
  readonly savingGames = input<ReadonlySet<string>>(new Set<string>());
  readonly pendingEnabled = input<Readonly<Record<string, boolean>>>({});
  readonly canManage = input(true);

  readonly activate = output<string>();
  readonly disable = output<string>();
  readonly configure = output<string>();

  actionError(gameCode: string): TenantGameCardError | null {
    return this.actionErrors()[gameCode] ?? null;
  }

  saving(gameCode: string): boolean {
    return this.savingGames().has(gameCode);
  }

  pendingGameEnabled(gameCode: string): boolean | null {
    return this.pendingEnabled()[gameCode] ?? null;
  }
}
