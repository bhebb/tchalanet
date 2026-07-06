import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TchPaginationComponent } from '@tch/ui/console';

import { AuditEventView } from '../../../operations/data-access/platform-audit-api.service';
import { AuditEventsTableComponent } from '../../../operations/components/audit-events-table/audit-events-table.component';

@Component({
  selector: 'tch-tenant-audit-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AuditEventsTableComponent, TchPaginationComponent],
  templateUrl: './tenant-audit-table.component.html',
})
export class TenantAuditTableComponent {
  readonly events = input<readonly AuditEventView[]>([]);
  readonly page = input(0);
  readonly size = input(20);
  readonly totalElements = input(0);

  readonly pageChange = output<number>();
}
