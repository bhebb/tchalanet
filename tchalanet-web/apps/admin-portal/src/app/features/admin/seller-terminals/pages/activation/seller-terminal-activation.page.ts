import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';

@Component({
  selector: 'tch-seller-terminal-activation-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminPageShellComponent, AdminSectionCardComponent],
  templateUrl: './seller-terminal-activation.page.html',
  styleUrls: ['./seller-terminal-activation.page.scss'],
})
export class SellerTerminalActivationPage {}
