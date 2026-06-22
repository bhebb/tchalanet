import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminEmptyStateComponent } from '../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../shared/admin-ui/admin-page-shell.component';

@Component({
  selector: 'tch-admin-placeholder-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminPageShellComponent, AdminEmptyStateComponent],
  template: `
    <tch-admin-page-shell [title]="data['titleKey'] | translate">
      <tch-admin-empty-state
        [icon]="data['icon'] ?? 'construction'"
        [title]="'common.placeholder.title' | translate"
        [message]="'common.placeholder.message' | translate"
      />
    </tch-admin-page-shell>
  `,
})
export class AdminPlaceholderPage {
  protected readonly data = inject(ActivatedRoute).snapshot.data;
}
