import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
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
