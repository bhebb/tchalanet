import { ChangeDetectionStrategy, Component } from '@angular/core';

import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';

@Component({
  selector: 'tch-platform-contact-config-page',
  standalone: true,
  imports: [AdminPageShellComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './platform-contact-config.page.html',
  styleUrl: './platform-contact-config.page.scss',
})
export class PlatformContactConfigPage {}
