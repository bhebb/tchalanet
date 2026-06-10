import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminKpiCardComponent } from '../../../private/shared/admin-ui/admin-kpi-card.component';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { CashierCurrentDrawCountdownComponent } from '../../shared/cashier-ui/cashier-draw-countdown-card.component';
import { CashierPrinterStatusCardComponent } from '../../shared/cashier-ui/cashier-printer-status-card.component';
import { CashierQuickActionCardComponent } from '../../shared/cashier-ui/cashier-quick-action-card.component';
import { CashierSessionStatusCardComponent } from '../../shared/cashier-ui/cashier-session-status-card.component';
import { CashierTicketHistoryCardComponent } from '../../shared/cashier-ui/cashier-ticket-history-card.component';
import { CashierDashboardService } from './cashier-dashboard.service';
import { CashierDashboardView, DashboardUiState } from './cashier-dashboard.model';

@Component({
  selector: 'tch-cashier-dashboard-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    RouterLink,
    AdminPageShellComponent,
    AdminKpiCardComponent,
    AdminEmptyStateComponent,
    CashierSessionStatusCardComponent,
    CashierCurrentDrawCountdownComponent,
    CashierPrinterStatusCardComponent,
    CashierQuickActionCardComponent,
    CashierTicketHistoryCardComponent,
  ],
  template: `
    @switch (state()) {
      @case ('loading') {
        <tch-admin-page-shell
          [title]="'cashier.dashboard.title' | translate"
          [description]="'cashier.dashboard.description' | translate"
        >
          <div class="cashier-kpi-row">
            @for (_ of [1, 2, 3]; track $index) {
              <tch-admin-kpi-card label="…" [loading]="true" />
            }
          </div>
        </tch-admin-page-shell>
      }

      @case ('blocked') {
        <tch-admin-page-shell
          [title]="'cashier.dashboard.title' | translate"
          [description]="'cashier.dashboard.description' | translate"
        >
          <tch-admin-empty-state
            icon="block"
            [title]="'cashier.dashboard.blocked.title' | translate"
            [message]="'cashier.dashboard.blocked.message' | translate"
          />
        </tch-admin-page-shell>
      }

      @case ('error') {
        <tch-admin-page-shell
          [title]="'cashier.dashboard.title' | translate"
          [description]="'cashier.dashboard.description' | translate"
        >
          <p class="dashboard-error">{{ 'common.error.title' | translate }}</p>
        </tch-admin-page-shell>
      }

      @default {
        <tch-admin-page-shell
          [title]="'cashier.dashboard.title' | translate"
          [description]="vm()?.seller?.displayName ?? ''"
        >
          <ng-container actions>
            <a class="cashier-sell-btn" [routerLink]="['/app/cashier/sell']">
              <span class="material-symbols-outlined" aria-hidden="true">add_circle</span>
              {{ 'cashier.dashboard.action.newTicket' | translate }}
            </a>
          </ng-container>

          <div class="cashier-kpi-row">
            <tch-admin-kpi-card
              [label]="'cashier.dashboard.kpi.todaySales' | translate"
              [value]="vm()?.kpis?.todaySalesAmount ?? null"
              [hint]="vm()?.kpis?.todaySalesCurrency ?? null"
              icon="payments"
              variant="strong"
            />
            <tch-admin-kpi-card
              [label]="'cashier.dashboard.kpi.commission' | translate"
              [value]="vm()?.kpis?.commissionAmount ?? null"
              [hint]="vm()?.kpis?.commissionCurrency ?? null"
              icon="savings"
            />
            <tch-admin-kpi-card
              [label]="'cashier.dashboard.kpi.tickets' | translate"
              [value]="vm()?.kpis?.ticketCount ?? null"
              icon="confirmation_number"
            />
          </div>

          <div class="cashier-bento">
            @if (vm()?.session; as session) {
              <tch-cashier-session-status-card [session]="session" />
            }

            @if (vm()?.currentDraw; as draw) {
              <tch-cashier-draw-countdown-card [draw]="draw" />
            }

            @if (vm()?.printer; as printer) {
              <tch-cashier-printer-status-card [printer]="printer" />
            }

            <tch-cashier-quick-action-card />
          </div>

          @if (vm()?.recentTickets?.length) {
            <tch-cashier-ticket-history-card [tickets]="vm()!.recentTickets" />
          }
        </tch-admin-page-shell>
      }
    }
  `,
  styles: [
    `
      .cashier-kpi-row {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
        gap: 1rem;
        margin-bottom: 1.5rem;
      }

      .cashier-bento {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
        gap: 1.5rem;
        margin-bottom: 1.5rem;
      }

      .dashboard-error {
        color: var(--tch-color-error, #ba1a1a);
        font-weight: 600;
        padding: 1rem 0;
      }

      .cashier-sell-btn {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.5rem 1.25rem;
        background: var(--tch-color-primary, #006590);
        color: var(--tch-color-on-primary, #fff);
        border-radius: 9999px;
        font-size: 0.875rem;
        font-weight: 600;
        text-decoration: none;
        cursor: pointer;
        transition: opacity 200ms ease;
      }

      .cashier-sell-btn:hover {
        opacity: 0.88;
      }

      .cashier-sell-btn .material-symbols-outlined {
        font-size: 1.125rem;
        font-variation-settings: 'FILL' 1;
      }
    `,
  ],
})
export class CashierDashboardPage implements OnInit {
  private readonly svc = inject(CashierDashboardService);

  readonly state = signal<DashboardUiState>('loading');
  readonly vm = signal<CashierDashboardView | null>(null);

  ngOnInit(): void {
    this.state.set('loading');
    this.svc.load().subscribe({
      next: data => {
        this.vm.set(data);
        this.state.set(data.session?.status === 'BLOCKED' ? 'blocked' : 'ready');
      },
      error: () => {
        this.state.set('error');
      },
    });
  }
}
