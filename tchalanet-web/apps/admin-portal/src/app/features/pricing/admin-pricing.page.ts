import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { resourceErrorVm, TchAsyncReadyDirective, TchAsyncViewComponent } from '@tch/web/async';
import {
  ConsolePricingRow,
  ConsolePricingTableComponent,
} from '@tch/web/console';
import { AdminPricingApi } from './data-access/admin-pricing-api.service';

@Component({
  selector: 'tch-admin-pricing-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    ConsolePricingTableComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './admin-pricing.page.html',
  styleUrls: ['./admin-pricing.page.scss'],
})
export class AdminPricingPage {
  private readonly api       = inject(AdminPricingApi);
  private readonly translate = inject(TranslateService);

  readonly oddsResource = this.api.getDefaultOddsResource({ suppressShellFeedback: true });
  readonly oddsError = resourceErrorVm(this.oddsResource, 'admin.controls.pricing');
  readonly odds = computed(() => this.oddsResource.value() ?? []);
  readonly rows = computed<readonly ConsolePricingRow[]>(() =>
    this.odds().map(row => ({
      id: `${row.gameCode}:${row.betType}:${row.betOption}`,
      gameCode: row.gameCode,
      betType: row.betType,
      betOption: row.betOption,
      odds: row.odds,
      statusLabel: this.translate.instant(row.active ? 'common.enabled' : 'common.disabled'),
      statusTone: row.active ? 'success' : 'neutral',
    })),
  );

  load(): void {
    this.oddsResource.reload();
  }
}
