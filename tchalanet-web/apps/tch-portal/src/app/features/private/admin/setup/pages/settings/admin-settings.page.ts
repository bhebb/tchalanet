import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';

interface SettingsLink {
  icon: string;
  label: string;
  description: string;
  route: string;
}

@Component({
  selector: 'tch-admin-settings-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, AdminPageShellComponent, AdminSectionCardComponent, MatButtonModule, MatIconModule],
  template: `
    <tch-admin-page-shell title="Paramètres" description="Configuration du tenant.">
      <tch-admin-section-card title="Accès rapide">
        <div class="settings-page__grid">
          @for (link of links; track link.route) {
            <a class="settings-page__card" [routerLink]="link.route">
              <span class="material-symbols-outlined settings-page__card-icon">{{ link.icon }}</span>
              <div class="settings-page__card-body">
                <span class="settings-page__card-label">{{ link.label }}</span>
                <span class="settings-page__card-desc">{{ link.description }}</span>
              </div>
              <span class="material-symbols-outlined settings-page__card-arrow">chevron_right</span>
            </a>
          }
        </div>
      </tch-admin-section-card>
    </tch-admin-page-shell>
  `,
  styles: [`
    .settings-page__grid {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }
    .settings-page__card {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 0.875rem 1rem;
      border: 1px solid var(--tch-color-outline-variant);
      border-radius: var(--tch-radius-md);
      text-decoration: none;
      color: inherit;
      transition: background 0.15s;

      &:hover { background: var(--tch-color-surface-container); }
    }
    .settings-page__card-icon {
      font-size: 1.5rem;
      color: var(--tch-color-primary);
      flex-shrink: 0;
    }
    .settings-page__card-body {
      display: flex;
      flex-direction: column;
      flex: 1;
      gap: 0.125rem;
    }
    .settings-page__card-label { font-weight: 500; font-size: 0.9375rem; }
    .settings-page__card-desc { font-size: 0.8125rem; color: var(--tch-color-on-surface-variant); }
    .settings-page__card-arrow { color: var(--tch-color-on-surface-variant); flex-shrink: 0; }
  `],
})
export class AdminSettingsPage {
  readonly links: SettingsLink[] = [
    {
      icon: 'dns',
      label: 'Runtime',
      description: 'Informations d\'exécution du tenant',
      route: '/app/admin/settings/runtime',
    },
    {
      icon: 'tune',
      label: 'Configuration',
      description: 'Locale, communication, documents',
      route: '/app/admin/settings/config',
    },
  ];
}
