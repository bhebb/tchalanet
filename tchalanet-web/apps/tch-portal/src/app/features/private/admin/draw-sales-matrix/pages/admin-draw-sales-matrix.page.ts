import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent } from '../../../shared/admin-ui/admin-status-pill.component';
import { OfferedGamesPipe, AvailableGamesPipe } from '../pipes/channel-game-filter.pipe';
import {
  AdminDrawSalesMatrixApi,
  TenantDrawSalesMatrixView,
  MatrixSummary,
  SlotMatrixView,
  ChannelGameSetupView,
  SetupWarning,
} from '../data-access/admin-draw-sales-matrix-api.service';

@Component({
  selector: 'tch-admin-draw-sales-matrix-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatExpansionModule,
    MatIconModule,
    OfferedGamesPipe,
    AvailableGamesPipe,
  ],
  templateUrl: './admin-draw-sales-matrix.page.html',
})
export class AdminDrawSalesMatrixPage implements OnInit {
  private readonly api = inject(AdminDrawSalesMatrixApi);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly matrix = signal<TenantDrawSalesMatrixView | null>(null);
  readonly acting = signal<string | null>(null); // key = `${drawChannelId}:${tenantGameId}`

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getMatrix().subscribe({
      next: data => { this.matrix.set(data); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  summary(): MatrixSummary | null { return this.matrix()?.summary ?? null; }

  actingKey(drawChannelId: string, tenantGameId: string): string {
    return `${drawChannelId}:${tenantGameId}`;
  }

  isActing(drawChannelId: string, tenantGameId: string): boolean {
    return this.acting() === this.actingKey(drawChannelId, tenantGameId);
  }

  offerGame(slot: SlotMatrixView, game: ChannelGameSetupView): void {
    const drawChannelId = slot.channel!.drawChannelId.value;
    const tenantGameId = game.tenantGameId.value;
    const key = this.actingKey(drawChannelId, tenantGameId);
    this.acting.set(key);
    this.api.offerGame(drawChannelId, tenantGameId).subscribe({
      next: () => { this.acting.set(null); this.snackBar.open(`${game.gameCode} ajouté au canal.`, 'OK', { duration: 3000 }); this.load(); },
      error: (err: unknown) => {
        this.acting.set(null);
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 4000 });
      },
    });
  }

  toggleGame(slot: SlotMatrixView, game: ChannelGameSetupView): void {
    const drawChannelId = slot.channel!.drawChannelId.value;
    const tenantGameId = game.tenantGameId.value;
    const key = this.actingKey(drawChannelId, tenantGameId);
    this.acting.set(key);
    const newEnabled = !game.enabledOnChannel;
    this.api.toggleGame(drawChannelId, tenantGameId, newEnabled).subscribe({
      next: () => {
        this.acting.set(null);
        this.snackBar.open(`${game.gameCode} ${newEnabled ? 'activé' : 'désactivé'}.`, 'OK', { duration: 3000 });
        this.load();
      },
      error: (err: unknown) => {
        this.acting.set(null);
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 4000 });
      },
    });
  }

  removeGame(slot: SlotMatrixView, game: ChannelGameSetupView): void {
    const drawChannelId = slot.channel!.drawChannelId.value;
    const tenantGameId = game.tenantGameId.value;
    const key = this.actingKey(drawChannelId, tenantGameId);
    this.acting.set(key);
    this.api.removeGame(drawChannelId, tenantGameId).subscribe({
      next: () => { this.acting.set(null); this.snackBar.open(`${game.gameCode} retiré du canal.`, 'OK', { duration: 3000 }); this.load(); },
      error: (err: unknown) => {
        this.acting.set(null);
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 4000 });
      },
    });
  }

  severityIcon(w: SetupWarning): string {
    if (w.severity === 'ERROR') return 'error';
    if (w.severity === 'WARN') return 'warning';
    return 'info';
  }

  severityColor(w: SetupWarning): string {
    if (w.severity === 'ERROR') return 'var(--tch-error, #d32f2f)';
    if (w.severity === 'WARN') return 'var(--tch-warn, #ed6c02)';
    return 'var(--tch-info, #0288d1)';
  }

  slotStatusTone(slot: SlotMatrixView): 'success' | 'warning' | 'danger' | 'neutral' {
    if (slot.slotReady) return 'success';
    const hasError = slot.warnings.some(w => w.severity === 'ERROR');
    return hasError ? 'danger' : 'warning';
  }

  gameStatusTone(game: ChannelGameSetupView): 'success' | 'warning' | 'danger' | 'neutral' {
    if (game.saleReady) return 'success';
    if (!game.offeredOnChannel) return 'neutral';
    const hasError = game.warnings.some(w => w.severity === 'ERROR');
    return hasError ? 'danger' : 'warning';
  }

  gameStatusLabel(game: ChannelGameSetupView): string {
    if (game.saleReady) return 'Prêt';
    if (!game.offeredOnChannel) return 'Non offert';
    if (!game.enabledOnChannel) return 'Désactivé';
    return 'Incomplet';
  }
}
