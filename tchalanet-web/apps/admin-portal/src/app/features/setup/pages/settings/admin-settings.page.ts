import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';

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
  templateUrl: './admin-settings.page.html',
  styleUrls: ['./admin-settings.page.scss'],
})
export class AdminSettingsPage {
  private readonly route = inject(ActivatedRoute);

  readonly fromSetup = this.route.snapshot.queryParamMap.get('from') === 'setup';
  readonly backRoute = this.fromSetup ? '/app/admin/setup' : '/app/admin/business-profile';
  readonly backLabel = this.fromSetup ? 'Configuration générale' : 'Mon entreprise';
  readonly linkQueryParams = this.fromSetup ? { from: 'setup' } : undefined;

  readonly links: SettingsLink[] = [
    {
      icon: 'dns',
      label: 'Runtime',
      description: 'Informations d\'exécution du tenant',
      route: '/app/admin/company/settings/runtime',
    },
    {
      icon: 'tune',
      label: 'Configuration',
      description: 'Locale, communication, documents',
      route: '/app/admin/company/settings/config',
    },
  ];
}
