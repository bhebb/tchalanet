import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminCrudShellComponent } from '../../../shared/admin-ui/admin-crud-shell.component';
import { AdminStatusPillComponent } from '../../../shared/admin-ui/admin-status-pill.component';
import {
  PlatformOpsApi,
  DrawResultOpsResponse,
} from '../../platform-ops-api.service';
import { FetchResultsDialog } from './dialogs/fetch-results.dialog';
import { OverrideResultDialog } from './dialogs/override-result.dialog';

@Component({
  selector: 'tch-platform-ops-draw-results-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    AdminCrudShellComponent,
    AdminStatusPillComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatSelectModule,
    MatTableModule,
  ],
  templateUrl: './platform-ops-draw-results.page.html',
  styleUrls: ['./platform-ops-draw-results.page.scss'],
})
export class PlatformOpsDrawResultsPage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['slotKey', 'occurredAt', 'status', 'source', 'quality', 'fetchedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly page = signal<{ items: DrawResultOpsResponse[]; totalElements: number; totalPages: number; number: number; size: number } | null>(null);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(20);
  readonly actionLoading = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listDrawResults({ page: this.pageIndex(), size: this.pageSize(), sort: 'occurredAt,DESC' }).subscribe({
      next: v => { this.page.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  onPage(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    this.pageSize.set(e.pageSize);
    this.load();
  }

  openFetch(mode: 'fetch' | 'refresh'): void {
    this.dialog.open(FetchResultsDialog, {
      data: {
        title: mode === 'fetch' ? 'Fetch résultats externes' : 'Refresh résultats (fetch + apply)',
        mode,
        onSuccess: () => this.load(),
      },
      width: '500px',
    });
  }

  openOverride(row: DrawResultOpsResponse): void {
    this.dialog.open(OverrideResultDialog, {
      data: { row, onSuccess: () => this.load() },
      width: '500px',
    });
  }

  confirmResult(row: DrawResultOpsResponse): void {
    this.actionLoading.set(true);
    this.api.confirmDrawResult(row.id).subscribe({
      next: () => {
        this.actionLoading.set(false);
        this.snackBar.open('Résultat confirmé.', 'OK', { duration: 3000 });
        this.load();
      },
      error: (err: unknown) => {
        this.actionLoading.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.snackBar.open(pd?.title ?? 'Erreur de confirmation.', 'OK', { duration: 5000 });
      },
    });
  }

  statusTone(status: string): 'success' | 'warning' | 'danger' | 'neutral' | 'info' {
    const map: Record<string, 'success' | 'warning' | 'danger' | 'neutral' | 'info'> = {
      CONFIRMED: 'success',
      PROVISIONAL: 'warning',
      OVERRIDDEN: 'info',
      MANUAL: 'info',
      REJECTED: 'danger',
    };
    return map[status] ?? 'neutral';
  }

  qualityTone(quality: string): 'success' | 'warning' | 'danger' | 'neutral' {
    const map: Record<string, 'success' | 'warning' | 'danger' | 'neutral'> = {
      HIGH: 'success',
      MEDIUM: 'warning',
      LOW: 'danger',
    };
    return map[quality] ?? 'neutral';
  }
}
