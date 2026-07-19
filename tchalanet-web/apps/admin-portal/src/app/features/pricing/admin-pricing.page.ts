import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { resourceErrorVm, TchAsyncReadyDirective, TchAsyncViewComponent } from '@tch/web/async';
import {
  ConsolePricingActionEvent,
  ConsolePricingRow,
  ConsolePricingTableComponent,
} from '@tch/web/console';
import { AdminPricingApi } from './data-access/admin-pricing-api.service';
import { EditTenantOddsDialog } from './dialogs/edit-tenant-odds.dialog';

@Component({
  selector: 'tch-admin-pricing-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
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
  private readonly api = inject(AdminPricingApi);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);

  readonly oddsResource = this.api.getDefaultOddsResource({ suppressShellFeedback: true });
  readonly oddsError = resourceErrorVm(this.oddsResource, 'admin.controls.pricing');
  readonly odds = computed(() => this.oddsResource.value() ?? []);
  readonly rows = computed<readonly ConsolePricingRow[]>(() =>
    this.odds().map(row => ({
      id: `${row.gameCode}:${row.pricingVariantCode}`,
      gameCode: row.gameCode,
      betType: row.betType,
      betOption: row.betOption,
      odds: row.odds,
      payoutRuleLabel: this.payoutRuleLabel(row.payoutRuleType ?? 'STAKE_MULTIPLIER'),
      payoutValue: this.payoutValue(row),
      statusLabel: this.translate.instant(row.active ? 'common.enabled' : 'common.disabled'),
      statusTone: row.active ? 'success' : 'neutral',
      actions: [
        {
          id: 'edit',
          label: this.translate.instant('admin.pricing.actions.edit'),
          icon: 'edit',
          tone: 'primary',
        },
      ],
    })),
  );

  load(): void {
    this.oddsResource.reload();
  }

  onRowAction(event: ConsolePricingActionEvent): void {
    if (event.action.id !== 'edit') return;

    const odds = this.odds().find(
      row => `${row.gameCode}:${row.pricingVariantCode}` === event.row.id,
    );
    if (!odds) return;

    const ref = this.dialog.open(EditTenantOddsDialog, {
      data: odds,
      width: '480px',
      maxWidth: '100vw',
    });

    ref.afterClosed().subscribe((saved?: boolean) => {
      if (saved) this.load();
    });
  }

  private payoutRuleLabel(ruleType: string): string {
    return this.translate.instant(`console.pricingForm.payoutRuleType.${ruleType}`);
  }

  private payoutValue(row: {
    payoutRuleType?: string | null;
    fixedAmount?: number | null;
    odds?: number | null;
  }): string {
    if (row.payoutRuleType === 'FIXED_AMOUNT') {
      return row.fixedAmount === null || row.fixedAmount === undefined ? '—' : `${row.fixedAmount}`;
    }
    return row.odds === null || row.odds === undefined ? '—' : `×${row.odds}`;
  }
}
