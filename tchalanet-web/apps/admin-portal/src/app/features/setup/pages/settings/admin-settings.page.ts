import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';

interface SettingsLink {
  icon: string;
  labelKey: string;
  descriptionKey: string;
  route: string;
}

@Component({
  selector: 'tch-admin-settings-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    TranslatePipe,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './admin-settings.page.html',
  styleUrls: ['./admin-settings.page.scss'],
})
export class AdminSettingsPage {
  private readonly route = inject(ActivatedRoute);

  readonly fromSetup = this.route.snapshot.queryParamMap.get('from') === 'setup';
  readonly backRoute = this.fromSetup ? '/app/admin/setup' : '/app/admin/business-profile';
  readonly backLabel = this.fromSetup ? 'admin.setup.backToSetup' : 'admin.businessProfile.title';
  readonly linkQueryParams = this.fromSetup ? { from: 'setup' } : undefined;

  readonly links: SettingsLink[] = [
    {
      icon: 'dns',
      labelKey: 'admin.settings.runtime.title',
      descriptionKey: 'admin.settings.runtime.description',
      route: '/app/admin/company/settings/runtime',
    },
    {
      icon: 'tune',
      labelKey: 'admin.settings.config.title',
      descriptionKey: 'admin.settings.config.description',
      route: '/app/admin/company/settings/config',
    },
  ];
}
