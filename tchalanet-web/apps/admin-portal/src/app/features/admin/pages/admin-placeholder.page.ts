import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';

@Component({
  selector: 'tch-admin-placeholder-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminPageShellComponent, AdminEmptyStateComponent],
  templateUrl: './admin-placeholder.page.html',
})
export class AdminPlaceholderPage {
  protected readonly data = inject(ActivatedRoute).snapshot.data;
}
