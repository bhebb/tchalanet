import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '../../../private/shared/admin-ui/admin-section-card.component';
import { AdminStatusPillComponent } from '../../../private/shared/admin-ui/admin-status-pill.component';
import { AdminStatusTone } from '../../../private/shared/admin-ui/admin-status-pill.component';
import { CashierPrinterStatusView } from '../../pages/dashboard/cashier-dashboard.model';

@Component({
  selector: 'tch-cashier-printer-status-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminSectionCardComponent, AdminStatusPillComponent],
  template: `
    <tch-admin-section-card
      [title]="'cashier.printer.title' | translate"
      icon="print"
    >
      <div class="printer-body">
        <tch-admin-status-pill [label]="printer().status" [tone]="tone()" />
        @if (printer().modelLabel) {
          <p class="printer-model">{{ printer().modelLabel }}</p>
        }
      </div>
    </tch-admin-section-card>
  `,
  styles: [
    `
      .printer-body {
        display: flex;
        flex-direction: column;
        gap: 0.625rem;
      }

      .printer-model {
        margin: 0;
        font-size: 0.8125rem;
        font-family: 'JetBrains Mono', monospace;
        color: var(--tch-color-on-surface-variant, #46464f);
      }
    `,
  ],
})
export class CashierPrinterStatusCardComponent {
  readonly printer = input.required<CashierPrinterStatusView>();

  readonly tone = computed((): AdminStatusTone => {
    switch (this.printer().status) {
      case 'READY': return 'success';
      case 'OFFLINE': return 'neutral';
      case 'ERROR': return 'danger';
      default: return 'neutral';
    }
  });
}
