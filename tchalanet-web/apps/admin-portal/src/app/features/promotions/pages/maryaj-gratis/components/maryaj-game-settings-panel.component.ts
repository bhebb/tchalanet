import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import {
  AdminSectionCardComponent,
  AdminStatusPillComponent,
  type AdminStatusTone,
} from '@tch/ui/console';
import { TenantGamePricingView } from '../../../../games-pricing/data-access/admin-games-pricing.models';
import { TenantGameSettingsSummaryComponent } from '../../../../games-pricing/components/tenant-game-settings-summary/tenant-game-settings-summary.component';

@Component({
  selector: 'tch-maryaj-game-settings-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    MatIconModule,
    AdminSectionCardComponent,
    AdminStatusPillComponent,
    TenantGameSettingsSummaryComponent,
  ],
  templateUrl: './maryaj-game-settings-panel.component.html',
  styleUrls: ['./maryaj-game-settings-panel.component.scss'],
})
export class MaryajGameSettingsPanelComponent {
  private readonly translateService = inject(TranslateService);

  readonly availabilityRoute = '/app/admin/games/channel-matrix';

  readonly game = input<TenantGamePricingView | null>(null);
  readonly configure = output<TenantGamePricingView>();

  readinessLabel(status: string): string {
    const key = `admin.maryajGratis.game.readiness.${status}`;
    const translated = this.translate(key);
    return translated === key ? status : translated;
  }

  readinessReason(reason: string | null): string | null {
    if (!reason) return null;
    const translated = this.translate(reason);
    return translated === reason ? reason : translated;
  }

  readinessTone(status: string): AdminStatusTone {
    if (status === 'READY') return 'success';
    if (status === 'TODO') return 'warning';
    if (status === 'BLOCKED') return 'danger';
    return 'neutral';
  }

  private translate(key: string): string {
    return this.translateService.instant(key);
  }
}
