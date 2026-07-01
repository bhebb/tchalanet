import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';

import { AdminPageShellComponent } from '@tch/ui/console';

@Component({
  selector: 'tch-admin-limits-shell-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatTabsModule,
    AdminPageShellComponent,
  ],
  templateUrl: './admin-limits-shell.page.html',
  styleUrl: './admin-limits-shell.page.scss',
})
export class AdminLimitsShellPage {}
