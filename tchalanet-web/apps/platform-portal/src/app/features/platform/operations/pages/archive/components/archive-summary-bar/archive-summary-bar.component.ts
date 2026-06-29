import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ArchiveOpsSummary } from '../../../../data-access/platform-archive-api.service';
import { ArchiveRouteView } from '../../archive-view.model';

@Component({
  selector: 'tch-archive-summary-bar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './archive-summary-bar.component.html',
  styleUrls: ['./archive-summary-bar.component.scss'],
})
export class ArchiveSummaryBarComponent {
  @Input({ required: true }) summary!: ArchiveOpsSummary;
  @Input({ required: true }) activeView!: ArchiveRouteView;

  @Output() readonly viewSelected = new EventEmitter<ArchiveRouteView>();

  isRunsActive(): boolean {
    return this.activeView === 'overview' || this.activeView === 'recent';
  }
}
