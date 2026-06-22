import { ChangeDetectionStrategy, Component } from '@angular/core';

import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';

@Component({
  selector: 'tch-platform-audit-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminPageShellComponent, AdminEmptyStateComponent],
  template: `
    <tch-admin-page-shell title="Audit" description="Journal d'audit des actions superadmin.">
      <tch-admin-empty-state
        icon="policy"
        title="Audit log — bientôt disponible"
        message="La consultation du journal d'audit sera disponible prochainement."
      />
    </tch-admin-page-shell>
  `,
})
export class PlatformAuditPage {}
