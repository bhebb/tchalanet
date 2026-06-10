import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '../../../private/shared/admin-ui/admin-section-card.component';
import { AdminStatusPillComponent } from '../../../private/shared/admin-ui/admin-status-pill.component';
import { AdminStatusTone } from '../../../private/shared/admin-ui/admin-status-pill.component';
import { CashierRecentTicketItem } from '../../pages/dashboard/cashier-dashboard.model';

function ticketTone(status: CashierRecentTicketItem['status']): AdminStatusTone {
  if (status === 'WON') return 'success';
  if (status === 'LOST') return 'neutral';
  if (status === 'CANCELLED') return 'danger';
  return 'info';
}

@Component({
  selector: 'tch-cashier-ticket-history-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminSectionCardComponent, AdminStatusPillComponent],
  template: `
    <tch-admin-section-card
      [title]="'cashier.tickets.title' | translate"
      icon="receipt_long"
    >
      <div class="ticket-list">
        @for (ticket of tickets(); track ticket.ticketId) {
          <div class="ticket-row">
            <div class="ticket-row__left">
              <span class="ticket-row__id">{{ ticket.ticketId }}</span>
              <span class="ticket-row__game">{{ ticket.gameLabel }}</span>
            </div>
            <div class="ticket-row__right">
              <span class="ticket-row__time">{{ ticket.soldAt }}</span>
              <span class="ticket-row__amount">{{ ticket.amount }} {{ ticket.currency }}</span>
              <tch-admin-status-pill [label]="ticket.status" [tone]="tone(ticket.status)" />
              @if (ticket.canPrint) {
                <button class="ticket-row__print" type="button" [attr.aria-label]="'cashier.tickets.print' | translate">
                  <span class="material-symbols-outlined" aria-hidden="true">print</span>
                </button>
              }
            </div>
          </div>
        }
      </div>
    </tch-admin-section-card>
  `,
  styles: [
    `
      .ticket-list {
        display: flex;
        flex-direction: column;
      }

      .ticket-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 1rem;
        padding: 0.625rem 0;
        border-bottom: 1px solid color-mix(
          in srgb,
          var(--tch-color-outline-variant, #c8c5d0) 40%,
          transparent
        );
      }

      .ticket-row:last-child {
        border-bottom: none;
      }

      .ticket-row__left {
        display: flex;
        flex-direction: column;
        gap: 0.125rem;
        min-width: 0;
      }

      .ticket-row__id {
        font-family: 'JetBrains Mono', monospace;
        font-size: 0.8125rem;
        font-weight: 700;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .ticket-row__game {
        font-size: 0.75rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .ticket-row__right {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        flex-shrink: 0;
      }

      .ticket-row__time {
        font-size: 0.75rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        font-family: 'JetBrains Mono', monospace;
      }

      .ticket-row__amount {
        font-family: 'JetBrains Mono', monospace;
        font-size: 0.8125rem;
        font-weight: 600;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .ticket-row__print {
        background: none;
        border: none;
        cursor: pointer;
        padding: 0.25rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        line-height: 1;
        border-radius: 4px;
        transition: background 150ms ease;
      }

      .ticket-row__print:hover {
        background: var(--tch-color-surface-container, #edeef1);
      }

      .ticket-row__print .material-symbols-outlined {
        font-size: 1.125rem;
        font-variation-settings: 'FILL' 0;
      }
    `,
  ],
})
export class CashierTicketHistoryCardComponent {
  readonly tickets = input.required<readonly CashierRecentTicketItem[]>();

  tone(status: CashierRecentTicketItem['status']): AdminStatusTone {
    return ticketTone(status);
  }
}
