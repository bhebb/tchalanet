import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';

@Component({
  selector: 'tch-platform-contact-requests-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, AdminPageShellComponent, AdminEmptyStateComponent],
  template: `
    <tch-admin-page-shell
      [title]="'platform.contactRequests.title' | translate"
      [description]="'platform.contactRequests.description' | translate"
    >
      <tch-admin-empty-state
        icon="contact_mail"
        [title]="'common.placeholder.title' | translate"
        [message]="'common.placeholder.message' | translate"
      />
    </tch-admin-page-shell>
  `,
})
export class PlatformContactRequestsPage {}
