import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AdminPageShellComponent } from '@tch/ui/console';

@Component({
  selector: 'tch-admin-limits-shell-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterOutlet,
    AdminPageShellComponent,
  ],
  templateUrl: './admin-limits-shell.page.html',
  styleUrl: './admin-limits-shell.page.scss',
})
export class AdminLimitsShellPage {}
