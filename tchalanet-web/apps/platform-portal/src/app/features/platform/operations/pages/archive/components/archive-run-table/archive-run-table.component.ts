import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import { ArchiveRunView } from '../../../../data-access/platform-archive-api.service';

@Component({
  selector: 'tch-archive-run-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminStatusPillComponent,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './archive-run-table.component.html',
  styleUrls: ['./archive-run-table.component.scss'],
})
export class ArchiveRunTableComponent {
  @Input({ required: true }) runs: ArchiveRunView[] = [];
  @Input() expandedId: string | null = null;

  @Output() readonly expandedIdChange = new EventEmitter<string | null>();

  readonly runColumns = ['startedAt', 'status', 'strategy', 'triggerType', 'duration', 'error'];

  toggleExpand(id: string): void {
    this.expandedIdChange.emit(this.expandedId === id ? null : id);
  }

  duration(run: ArchiveRunView): string {
    if (!run.completedAt || !run.startedAt) return '—';
    const ms = new Date(run.completedAt).getTime() - new Date(run.startedAt).getTime();
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(1)}s`;
  }

  statusTone(status: string): AdminStatusTone {
    if (status === 'COMPLETED') return 'success';
    if (status === 'FAILED') return 'danger';
    if (status === 'STARTED') return 'warning';
    return 'neutral';
  }
}
