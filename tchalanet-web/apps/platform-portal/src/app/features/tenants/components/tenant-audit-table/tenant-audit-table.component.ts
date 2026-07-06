import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { AdminStatusPillComponent, TchPaginationComponent } from '@tch/ui/console';

import { AuditEventView, auditActionTone, auditActorTone } from '../../../operations/data-access/platform-audit-api.service';

@Component({
  selector: 'tch-tenant-audit-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatTableModule, TchPaginationComponent, AdminStatusPillComponent],
  templateUrl: './tenant-audit-table.component.html',
  styleUrls: ['./tenant-audit-table.component.scss'],
})
export class TenantAuditTableComponent {
  readonly events = input<readonly AuditEventView[]>([]);
  readonly page = input(0);
  readonly size = input(20);
  readonly totalElements = input(0);

  readonly pageChange = output<number>();

  readonly columns = ['occurredAt', 'actor', 'entity', 'action', 'ip'];

  readonly actorTone = auditActorTone;
  readonly actionTone = auditActionTone;
}
