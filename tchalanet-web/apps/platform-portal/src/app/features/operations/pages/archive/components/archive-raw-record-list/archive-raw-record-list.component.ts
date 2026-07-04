import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { AdminEmptyStateComponent } from '@tch/ui/console';

@Component({
  selector: 'tch-archive-raw-record-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AdminEmptyStateComponent],
  templateUrl: './archive-raw-record-list.component.html',
  styleUrls: ['./archive-raw-record-list.component.scss'],
})
export class ArchiveRawRecordListComponent {
  @Input({ required: true }) rows: Record<string, unknown>[] = [];
  @Input() emptyMessage = 'Aucun résultat.';

  rawEntries(row: Record<string, unknown>): { key: string; value: string }[] {
    return Object.entries(row).map(([key, value]) => ({
      key,
      value: value == null ? '—' : String(value),
    }));
  }
}
