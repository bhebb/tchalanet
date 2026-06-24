import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { PlatformOpsApi, CacheView } from '../../platform-ops-api.service';
import { ClearAllCachesDialog } from './dialogs/clear-all-caches.dialog';

@Component({
  selector: 'tch-platform-ops-cache-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './platform-ops-cache.page.html',
  styleUrls: ['./platform-ops-cache.page.scss'],
})
export class PlatformOpsCachePage implements OnInit {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['cacheName', 'size', 'hitRate', 'lastClearedAt', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly caches = signal<CacheView[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listCaches().subscribe({
      next: v => { this.caches.set(v); this.loading.set(false); },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
        this.loading.set(false);
      },
    });
  }

  clearOne(cache: CacheView): void {
    this.api.clearCache(cache.cacheName).subscribe({
      next: () => {
        this.snackBar.open(`Cache "${cache.cacheName}" vidé.`, 'OK', { duration: 3000 });
        this.load();
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        const msg = pd?.title ?? 'Erreur.';
        const tid = pd?.errorId ?? pd?.requestId;
        this.snackBar.open(tid ? `${msg} (ID: ${tid})` : msg, 'OK', { duration: 5000 });
      },
    });
  }

  openClearAll(): void {
    const ref = this.dialog.open(ClearAllCachesDialog, { width: '480px' });
    ref.afterClosed().subscribe(ok => {
      if (ok) this.load();
    });
  }
}
