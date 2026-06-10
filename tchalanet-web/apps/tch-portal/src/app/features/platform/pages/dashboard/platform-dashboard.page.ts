import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminDashboardGridComponent } from '../../../private/shared/admin-ui/admin-dashboard-grid.component';
import { AdminKpiCardComponent } from '../../../private/shared/admin-ui/admin-kpi-card.component';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../private/shared/admin-ui/admin-section-card.component';
import { PlatformOperationalIntegrityCardComponent } from './components/platform-operational-integrity-card.component';
import { PlatformProvisioningProgressListComponent } from './components/platform-provisioning-progress-list.component';
import { PlatformRecentContactRequestsTableComponent } from './components/platform-recent-contact-requests-table.component';
import { PlatformDashboardService } from './platform-dashboard.service';
import { DashboardUiState, PlatformDashboardView } from './platform-dashboard.model';

@Component({
  selector: 'tch-platform-dashboard-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    AdminPageShellComponent,
    AdminDashboardGridComponent,
    AdminKpiCardComponent,
    AdminSectionCardComponent,
    PlatformProvisioningProgressListComponent,
    PlatformOperationalIntegrityCardComponent,
    PlatformRecentContactRequestsTableComponent,
  ],
  template: `
    @switch (state()) {
      @case ('loading') {
        <tch-admin-page-shell
          [title]="'platform.dashboard.title' | translate"
          [description]="'platform.dashboard.description' | translate"
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
          [title]="'platform.dashboard.title' | translate"
          [description]="'platform.dashboard.description' | translate"
        >
          <p class="dashboard-error">{{ 'common.error.title' | translate }}</p>
        </tch-admin-page-shell>
      }

      @default {
        <tch-admin-page-shell
          [title]="'platform.dashboard.title' | translate"
          [description]="'platform.dashboard.description' | translate"
        >
          <tch-admin-dashboard-grid>
            <tch-admin-kpi-card
              [label]="'platform.dashboard.kpi.activeTenants' | translate"
              [value]="vm()?.kpis?.activeTenants ?? null"
              icon="hub"
            />
            <tch-admin-kpi-card
              [label]="'platform.dashboard.kpi.receivedContacts' | translate"
              [value]="vm()?.kpis?.receivedContacts ?? null"
              icon="contact_support"
            />
            <tch-admin-kpi-card
              [label]="'platform.dashboard.kpi.pendingNotifications' | translate"
              [value]="vm()?.kpis?.pendingNotifications ?? null"
              icon="mark_email_unread"
            />
            <tch-admin-kpi-card
              [label]="'platform.dashboard.kpi.degradedServices' | translate"
              [value]="vm()?.kpis?.degradedServices ?? null"
              icon="warning"
              variant="strong"
            />
          </tch-admin-dashboard-grid>

          <div class="platform-bento">
            <tch-admin-section-card
              class="platform-bento__provisioning"
              [title]="'platform.dashboard.provisioning.title' | translate"
              icon="cloud_sync"
            >
              <tch-platform-provisioning-progress-list
                [items]="vm()?.provisioning ?? []"
              />
            </tch-admin-section-card>

            <tch-admin-section-card
              class="platform-bento__integrity"
              [title]="'platform.dashboard.integrity.title' | translate"
              icon="verified_user"
            >
              <tch-platform-operational-integrity-card
                [integrity]="vm()?.operationalIntegrity ?? null"
              />
            </tch-admin-section-card>
          </div>

          <tch-admin-section-card
            style="margin-top: 1.5rem"
            [title]="'platform.dashboard.contacts.title' | translate"
            icon="mail"
          >
            <tch-platform-recent-contact-requests-table
              [items]="vm()?.recentContactRequests ?? []"
            />
          </tch-admin-section-card>
        </tch-admin-page-shell>
      }
    }
  `,
  styles: [
    `
      .platform-bento {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1.5rem;
      }

      @media (max-width: 900px) {
        .platform-bento {
          grid-template-columns: 1fr;
        }
      }

      .dashboard-error {
        color: var(--tch-color-error, #ba1a1a);
        font-weight: 600;
        padding: 1rem 0;
      }
    `,
  ],
})
export class PlatformDashboardPage implements OnInit {
  private readonly svc = inject(PlatformDashboardService);

  readonly state = signal<DashboardUiState>('loading');
  readonly vm = signal<PlatformDashboardView | null>(null);

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
