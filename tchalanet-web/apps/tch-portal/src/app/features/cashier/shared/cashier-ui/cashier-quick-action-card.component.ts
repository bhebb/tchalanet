import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminSectionCardComponent } from '../../../private/shared/admin-ui/admin-section-card.component';

@Component({
  selector: 'tch-cashier-quick-action-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, RouterLink, AdminSectionCardComponent],
  template: `
    <tch-admin-section-card
      [title]="'cashier.quickActions.title' | translate"
      icon="bolt"
    >
      <div class="actions">
        <a class="action-btn action-btn--primary" [routerLink]="['/app/cashier/sell']">
          <span class="material-symbols-outlined" aria-hidden="true">add_circle</span>
          {{ 'cashier.quickActions.newTicket' | translate }}
        </a>
        <button class="action-btn" type="button" disabled>
          <span class="material-symbols-outlined" aria-hidden="true">history</span>
          {{ 'cashier.quickActions.history' | translate }}
        </button>
        <button class="action-btn" type="button" disabled>
          <span class="material-symbols-outlined" aria-hidden="true">logout</span>
          {{ 'cashier.quickActions.closeSession' | translate }}
        </button>
      </div>
    </tch-admin-section-card>
  `,
  styles: [
    `
      .actions {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }

      .action-btn {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.625rem 1rem;
        border-radius: 0.5rem;
        font-size: 0.875rem;
        font-weight: 500;
        cursor: pointer;
        border: 1px solid var(--tch-color-outline-variant, #c8c5d0);
        background: var(--tch-color-surface-container-lowest, #fff);
        color: var(--tch-color-on-surface, #1a1c1e);
        text-decoration: none;
        transition: background 150ms ease;
      }

      .action-btn:hover:not(:disabled) {
        background: var(--tch-color-surface-container, #edeef1);
      }

      .action-btn:disabled {
        opacity: 0.48;
        cursor: not-allowed;
      }

      .action-btn--primary {
        background: var(--tch-color-primary, #006590);
        color: var(--tch-color-on-primary, #fff);
        border-color: transparent;
      }

      .action-btn--primary:hover {
        opacity: 0.88;
        background: var(--tch-color-primary, #006590) !important;
      }

      .action-btn .material-symbols-outlined {
        font-size: 1.125rem;
        font-variation-settings: 'FILL' 1;
      }
    `,
  ],
})
export class CashierQuickActionCardComponent {}
