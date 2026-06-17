import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import {
  AdminStatusPillComponent,
  AdminStatusTone,
} from '../../../private/shared/admin-ui/admin-status-pill.component';
import {
  AdminDrawResultsApi,
  DrawResultView,
  DrawResultStatus,
  DrawResultQuality,
} from '../../admin-draw-results-api.service';

@Component({
  selector: 'tch-admin-draw-results-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './admin-draw-results.page.html',
  styleUrls: ['./admin-draw-results.page.scss'],
})
export class AdminDrawResultsPage implements OnInit {
  private readonly api = inject(AdminDrawResultsApi);

  readonly columns = ['channelCode', 'slotLabel', 'drawDate', 'numbers', 'status', 'quality', 'appliedAt'];

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly results = signal<DrawResultView[]>([]);
  readonly statusFilter = signal<DrawResultStatus | ''>('');
  readonly qualityFilter = signal<DrawResultQuality | ''>('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listLastDays(30, {
      status: this.statusFilter() || undefined,
      quality: this.qualityFilter() || undefined,
      size: 100,
    }).subscribe({
      next: p => { this.results.set(p.content); this.loading.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  onStatusFilter(status: DrawResultStatus | ''): void {
    this.statusFilter.set(status);
    this.load();
  }

  onQualityFilter(quality: DrawResultQuality | ''): void {
    this.qualityFilter.set(quality);
    this.load();
  }

  statusTone(status: DrawResultStatus): AdminStatusTone {
    switch (status) {
      case 'APPLIED': return 'success';
      case 'CORRECTED': return 'warning';
      case 'VOIDED': return 'danger';
      default: return 'neutral';
    }
  }

  qualityTone(quality: DrawResultQuality): AdminStatusTone {
    switch (quality) {
      case 'OFFICIAL': return 'success';
      case 'MANUAL': return 'warning';
      case 'ESTIMATED': return 'warning';
      default: return 'neutral';
    }
  }
}
