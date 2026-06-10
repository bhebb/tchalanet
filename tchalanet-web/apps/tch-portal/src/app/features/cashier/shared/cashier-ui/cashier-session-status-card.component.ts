import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '../../../private/shared/admin-ui/admin-section-card.component';
import { AdminStatusPillComponent } from '../../../private/shared/admin-ui/admin-status-pill.component';
import { CashierSessionView } from '../../pages/dashboard/cashier-dashboard.model';

@Component({
  selector: 'tch-cashier-session-status-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminSectionCardComponent, AdminStatusPillComponent],
  template: `
    <tch-admin-section-card
      [title]="'cashier.session.title' | translate"
      icon="schedule"
    >
      <div class="session-body">
        <tch-admin-status-pill
          [label]="session().status"
          [tone]="session().status === 'OPEN' ? 'success' : session().status === 'CLOSED' ? 'neutral' : 'danger'"
        />
        @if (session().openedAt) {
          <p class="session-detail">
            <span class="material-symbols-outlined session-icon" aria-hidden="true">login</span>
            {{ 'cashier.session.openedAt' | translate }} {{ session().openedAt }}
          </p>
        }
        @if (session().durationLabel) {
          <p class="session-detail">
            <span class="material-symbols-outlined session-icon" aria-hidden="true">timer</span>
            {{ session().durationLabel }}
          </p>
        }
      </div>
    </tch-admin-section-card>
  `,
  styles: [
    `
      .session-body {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .session-detail {
        margin: 0;
        font-size: 0.875rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        display: flex;
        align-items: center;
        gap: 0.375rem;
      }

      .session-icon {
        font-size: 1rem;
        font-variation-settings: 'FILL' 0, 'wght' 400;
      }
    `,
  ],
})
export class CashierSessionStatusCardComponent {
  readonly session = input.required<CashierSessionView>();
}
