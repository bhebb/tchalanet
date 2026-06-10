import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';

@Component({
  selector: 'tch-admin-outlets-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminPageShellComponent, AdminEmptyStateComponent],
  template: `
    <tch-admin-page-shell
      [title]="'admin.outlets.title' | translate"
      [description]="'admin.outlets.description' | translate"
    >
      <tch-admin-empty-state
        icon="store"
        [title]="'common.placeholder.title' | translate"
        [message]="'common.placeholder.message' | translate"
      />
    </tch-admin-page-shell>
  `,
})
export class AdminOutletsPage {}
