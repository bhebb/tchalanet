import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';

@Component({
  selector: 'tch-cashier-sell-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminPageShellComponent, AdminEmptyStateComponent],
  templateUrl: './pos-sell.page.html',
  styleUrls: ['./pos-sell.page.scss'],
})
export class PosSellPage {}
