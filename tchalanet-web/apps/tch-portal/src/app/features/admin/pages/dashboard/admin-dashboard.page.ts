import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminDashboardGridComponent } from '../../../private/shared/admin-ui/admin-dashboard-grid.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminKpiCardComponent } from '../../../private/shared/admin-ui/admin-kpi-card.component';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../private/shared/admin-ui/admin-section-card.component';
import { AdminStatusPillComponent } from '../../../private/shared/admin-ui/admin-status-pill.component';
import { AdminDashboardService } from './admin-dashboard.service';
import { AdminDashboardView, DashboardUiState, TenantAttentionItem } from './admin-dashboard.model';

function severityToTone(
  severity: TenantAttentionItem['severity'],
): 'success' | 'warning' | 'danger' | 'neutral' | 'info' {
  if (severity === 'ERROR') return 'danger';
  if (severity === 'WARN') return 'warning';
  return 'info';
}

@Component({
  selector: 'tch-admin-dashboard-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    AdminPageShellComponent,
    AdminDashboardGridComponent,
    AdminKpiCardComponent,
    AdminSectionCardComponent,
    AdminStatusPillComponent,
    AdminEmptyStateComponent,
  ],
  template: `
    @switch (state()) {
      @case ('loading') {
        <tch-admin-page-shell
          [title]="'admin.dashboard.title' | translate"
          [description]="'admin.dashboard.description' | translate"
        >
          <tch-admin-dashboard-grid>
            @for (_ of [1, 2, 3, 4]; track $index) {
              <tch-admin-kpi-card label="…" [loading]="true" />
            }
          </tch-admin-dashboard-grid>
        </tch-admin-page-shell>
      }

      @case ('error') {
        <tch-admin-page-shell
          [title]="'admin.dashboard.title' | translate"
          [description]="'admin.dashboard.description' | translate"
        >
          <p class="dashboard-error">{{ 'common.error.title' | translate }}</p>
        </tch-admin-page-shell>
      }

      @default {
        <tch-admin-page-shell
          [title]="'admin.dashboard.title' | translate"
          [description]="'admin.dashboard.description' | translate"
        >
          <tch-admin-dashboard-grid>
            <tch-admin-kpi-card
              [label]="'admin.dashboard.kpi.sellers' | translate"
              [value]="vm()?.kpis?.sellers ?? null"
              icon="person_pin"
            />
            <tch-admin-kpi-card
              [label]="'admin.dashboard.kpi.outlets' | translate"
              [value]="vm()?.kpis?.outlets ?? null"
              icon="storefront"
            />
            <tch-admin-kpi-card
              [label]="'admin.dashboard.kpi.terminals' | translate"
              [value]="vm()?.kpis?.terminals ?? null"
              icon="terminal"
            />
            <tch-admin-kpi-card
              [label]="'admin.dashboard.kpi.openSessions' | translate"
              [value]="vm()?.kpis?.openSessions ?? null"
              icon="play_circle"
            />
          </tch-admin-dashboard-grid>

          <div class="admin-bento">
            <tch-admin-section-card
              [title]="'admin.dashboard.onboarding.title' | translate"
              icon="checklist"
            >
              @if (vm()?.onboarding; as ob) {
                <div class="onboarding-summary">
                  <p class="onboarding-summary__progress">
                    {{ ob.completedSteps }} / {{ ob.totalSteps }} steps completed
                  </p>
                  <div class="onboarding-summary__bar-track">
                    <div
                      class="onboarding-summary__bar-fill"
                      [style.width.%]="(ob.completedSteps / ob.totalSteps) * 100"
                      role="progressbar"
                      [attr.aria-valuenow]="ob.completedSteps"
                      [attr.aria-valuemax]="ob.totalSteps"
                    ></div>
                  </div>
                  <tch-admin-status-pill
                    [label]="ob.status"
                    [tone]="ob.status === 'READY' ? 'success' : ob.status === 'BLOCKED' ? 'danger' : ob.status === 'IN_PROGRESS' ? 'warning' : 'neutral'"
                  />
                </div>
              }
            </tch-admin-section-card>

            <tch-admin-section-card
              [title]="'admin.dashboard.attention.title' | translate"
              icon="warning"
            >
              @if (vm()?.attentionItems?.length) {
                <ul class="attention-list">
                  @for (item of vm()!.attentionItems; track item.id) {
                    <li class="attention-item">
                      <tch-admin-status-pill
                        [label]="item.severity"
                        [tone]="severityTone(item.severity)"
                      />
                      <span class="attention-item__label">{{ item.label }}</span>
                    </li>
                  }
                </ul>
              } @else {
                <p class="attention-list--empty">No items require attention.</p>
              }
            </tch-admin-section-card>
          </div>

          <tch-admin-section-card
            style="margin-top: 1.5rem"
            [title]="'admin.dashboard.recentSales.title' | translate"
            [description]="'admin.dashboard.recentSales.description' | translate"
            icon="history"
          >
            @if (vm()?.recentSales?.length) {
              <div class="sales-list">
                @for (sale of vm()!.recentSales; track sale.reference) {
                  <div class="sale-row">
                    <span class="sale-row__ref">{{ sale.reference }}</span>
                    <span class="sale-row__date">{{ sale.createdAtLabel }}</span>
                    <span class="sale-row__amount">{{ sale.amount }} {{ sale.currency }}</span>
                  </div>
                }
              </div>
            } @else {
              <tch-admin-empty-state
                icon="receipt_long"
                [title]="'admin.dashboard.recentSales.empty' | translate"
              />
            }
          </tch-admin-section-card>
        </tch-admin-page-shell>
      }
    }
  `,
  styles: [
    `
      .admin-bento {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1.5rem;
        margin-top: 0;
      }

      @media (max-width: 900px) {
        .admin-bento {
          grid-template-columns: 1fr;
        }
      }

      .dashboard-error {
        color: var(--tch-color-error, #ba1a1a);
        font-weight: 600;
        padding: 1rem 0;
      }

      .onboarding-summary {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .onboarding-summary__progress {
        margin: 0;
        font-size: 0.875rem;
        font-weight: 600;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .onboarding-summary__bar-track {
        height: 0.5rem;
        background: var(--tch-color-surface-container, #edeef1);
        border-radius: 9999px;
        overflow: hidden;
      }

      .onboarding-summary__bar-fill {
        height: 100%;
        background: var(--tch-color-secondary-container, #fecb01);
        border-radius: 9999px;
        transition: width 800ms ease;
      }

      .attention-list {
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .attention-item {
        display: flex;
        align-items: flex-start;
        gap: 0.625rem;
      }

      .attention-item__label {
        font-size: 0.875rem;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .attention-list--empty {
        margin: 0;
        font-size: 0.875rem;
        color: var(--tch-color-on-surface-variant, #46464f);
      }

      .sales-list {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }

      .sale-row {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 0.5rem 0;
        border-bottom: 1px solid color-mix(
          in srgb,
          var(--tch-color-outline-variant, #c8c5d0) 50%,
          transparent
        );
        font-size: 0.875rem;
      }

      .sale-row:last-child {
        border-bottom: none;
      }

      .sale-row__ref {
        font-family: 'JetBrains Mono', monospace;
        color: var(--tch-color-on-surface, #1a1c1e);
        font-weight: 600;
        flex: 1;
      }

      .sale-row__date {
        color: var(--tch-color-on-surface-variant, #46464f);
        font-size: 0.8125rem;
      }

      .sale-row__amount {
        font-family: 'JetBrains Mono', monospace;
        font-weight: 700;
        color: var(--tch-color-on-surface, #1a1c1e);
      }
    `,
  ],
})
export class AdminDashboardPage implements OnInit {
  private readonly svc = inject(AdminDashboardService);

  readonly state = signal<DashboardUiState>('loading');
  readonly vm = signal<AdminDashboardView | null>(null);

  severityTone = (
    severity: TenantAttentionItem['severity'],
  ): 'success' | 'warning' | 'danger' | 'neutral' | 'info' => severityToTone(severity);

  ngOnInit(): void {
    this.state.set('loading');
    this.svc.load().subscribe({
      next: data => {
        this.vm.set(data);
        this.state.set('ready');
      },
      error: () => {
        this.state.set('error');
      },
    });
  }
}
