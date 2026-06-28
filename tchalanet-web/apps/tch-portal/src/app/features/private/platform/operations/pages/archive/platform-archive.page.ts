import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../../shared/admin-ui/admin-crud-shell.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import {
  ArchiveOpsSummary,
  ArchiveRunView,
  PlatformArchiveApi,
} from '../../data-access/platform-archive-api.service';
import { ArchiveRouteView } from './archive-view.model';
import { ArchiveRawRecordListComponent } from './components/archive-raw-record-list/archive-raw-record-list.component';
import { ArchivePurgePanelComponent } from './components/archive-purge-panel/archive-purge-panel.component';
import { ArchiveRunTableComponent } from './components/archive-run-table/archive-run-table.component';
import { ArchiveSummaryBarComponent } from './components/archive-summary-bar/archive-summary-bar.component';
import { ArchiveTriggerRunDialogComponent } from './components/archive-trigger-run-dialog/archive-trigger-run-dialog.component';

@Component({
  selector: 'tch-platform-archive-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminCrudShellComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    ArchiveRawRecordListComponent,
    ArchivePurgePanelComponent,
    ArchiveRunTableComponent,
    ArchiveSummaryBarComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './platform-archive.page.html',
  styleUrls: ['./platform-archive.page.scss'],
})
export class PlatformArchivePage implements OnInit {
  private readonly api = inject(PlatformArchiveApi);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly summary = signal<ArchiveOpsSummary | null>(null);
  readonly runs = signal<ArchiveRunView[]>([]);
  readonly rawRows = signal<Record<string, unknown>[]>([]);
  readonly activeView = signal<ArchiveRouteView>('overview');
  readonly expandedId = signal<string | null>(null);

  ngOnInit(): void {
    this.loadSummary();
    this.loadRouteView();
  }

  private loadSummary(): void {
    this.api.getOpsSummary().subscribe({ next: s => this.summary.set(s) });
  }

  loadRuns(): void {
    this.loading.set(true);
    this.error.set(null);
    this.activeView.set('recent');
    this.api.listRuns(50).subscribe({
      next: list => { this.runs.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  loadFailed(): void {
    this.loading.set(true);
    this.error.set(null);
    this.activeView.set('failed');
    this.api.listFailedRuns(20).subscribe({
      next: list => { this.rawRows.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
        this.loading.set(false);
      },
    });
  }

  loadInvalid(): void {
    this.loading.set(true);
    this.error.set(null);
    this.activeView.set('invalid');
    this.api.listInvalidObjects(20).subscribe({
      next: list => { this.rawRows.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
        this.loading.set(false);
      },
    });
  }

  loadLegalHolds(): void {
    this.loading.set(true);
    this.error.set(null);
    this.activeView.set('legal-holds');
    this.api.listActiveLegalHolds(50).subscribe({
      next: list => { this.rawRows.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
        this.loading.set(false);
      },
    });
  }

  loadPartitions(): void {
    this.loading.set(true);
    this.error.set(null);
    this.activeView.set('partitions');
    const retentionCutoff = this.defaultRetentionCutoff();
    this.api.getPartitionCleanupPlan('audit_log', retentionCutoff).subscribe({
      next: list => { this.rawRows.set(list as unknown as Record<string, unknown>[]); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
        this.loading.set(false);
      },
    });
  }

  refresh(): void {
    this.loadSummary();
    if (this.activeView() === 'overview' || this.activeView() === 'recent') this.loadRuns();
    else if (this.activeView() === 'failed') this.loadFailed();
    else if (this.activeView() === 'invalid') this.loadInvalid();
    else if (this.activeView() === 'legal-holds') this.loadLegalHolds();
    else if (this.activeView() === 'partitions') this.loadPartitions();
    else this.activeView.set('purges');
  }

  openTrigger(): void {
    const ref = this.dialog.open(ArchiveTriggerRunDialogComponent, { width: '520px' });
    ref.afterClosed().subscribe((result: ArchiveRunView | { __error: string } | null) => {
      if (result && '__error' in result) {
        this.error.set(result.__error);
        setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 50);
        return;
      }
      if (result) {
        this.snackBar.open(`Archive déclenchée — run ${result.id.slice(0, 8)}…`, 'OK', { duration: 5000 });
        this.loadSummary();
        this.loadRuns();
      }
    });
  }

  selectView(view: ArchiveRouteView): void {
    if (view === 'recent' || view === 'overview') this.loadRuns();
    else if (view === 'failed') this.loadFailed();
    else if (view === 'invalid') this.loadInvalid();
    else if (view === 'legal-holds') this.loadLegalHolds();
    else if (view === 'partitions') this.loadPartitions();
    else this.activeView.set('purges');
  }

  rawEmptyMessage(): string {
    if (this.activeView() === 'failed') return 'Aucun run échoué.';
    if (this.activeView() === 'invalid') return 'Aucun objet invalide.';
    if (this.activeView() === 'legal-holds') return 'Aucune rétention légale active.';
    if (this.activeView() === 'partitions') return 'Aucune partition à afficher.';
    return 'Aucune anomalie.';
  }

  private loadRouteView(): void {
    const view = this.route.snapshot.data['archiveView'] as ArchiveRouteView | undefined;
    switch (view) {
      case 'failed':
        this.loadFailed();
        break;
      case 'invalid':
        this.loadInvalid();
        break;
      case 'legal-holds':
        this.loadLegalHolds();
        break;
      case 'partitions':
        this.loadPartitions();
        break;
      case 'purges':
        this.activeView.set('purges');
        break;
      case 'recent':
      case 'overview':
      default:
        this.loadRuns();
        if (view === 'overview' || !view) this.activeView.set('overview');
        break;
    }
  }

  private defaultRetentionCutoff(): string {
    const d = new Date();
    d.setUTCFullYear(d.getUTCFullYear() - 1);
    return d.toISOString().slice(0, 10);
  }
}
