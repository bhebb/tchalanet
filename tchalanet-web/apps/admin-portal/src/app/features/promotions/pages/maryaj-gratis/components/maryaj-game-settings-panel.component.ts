import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent, AdminStatusPillComponent } from '@tch/ui/console';
import { TenantGamePricingView } from '../../../../games-pricing/data-access/admin-games-pricing.models';
import { TenantGameCardComponent } from '../../../../games-pricing/components/tenant-game-card/tenant-game-card.component';

@Component({
  selector: 'tch-maryaj-game-settings-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    MatIconModule,
    AdminSectionCardComponent,
    AdminStatusPillComponent,
    TenantGameCardComponent,
  ],
  templateUrl: './maryaj-game-settings-panel.component.html',
  styleUrls: ['./maryaj-game-settings-panel.component.scss'],
})
export class MaryajGameSettingsPanelComponent {
  readonly game = input<TenantGamePricingView | null>(null);
  readonly saving = input(false);
  readonly configure = output<TenantGamePricingView>();
  readonly activate = output<string>();
  readonly disable = output<string>();

  onConfigure(gameCode: string): void {
    const game = this.game();
    if (!game || game.gameCode !== gameCode) return;
    this.configure.emit(game);
  }
}
