import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent } from '@tch/ui/console';
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
    TchSectionError,
    MatButtonModule,
    MatExpansionModule,
    MatIconModule,
    OfferedGamesPipe,
    AvailableGamesPipe,
  ],
  templateUrl: './admin-draw-sales-matrix.page.html',
  styleUrl: './admin-draw-sales-matrix.page.scss',
})
export class AdminDrawSalesMatrixPage implements OnInit {
  private readonly api = inject(AdminDrawSalesMatrixApi);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly matrix = signal<TenantDrawSalesMatrixView | null>(null);
  readonly acting = signal<string | null>(null); // key = `${drawChannelId}:${tenantGameId}`
  readonly actionErrors = signal<Readonly<Record<string, ErrorViewModel>>>({});
  readonly actionNotices = signal<Readonly<Record<string, string>>>({});

  ngOnInit(): void { this.load(); }

  load(preserveActionFeedback = false): void {
    this.loading.set(true);
    this.error.set(null);
    if (!preserveActionFeedback) {
      this.actionErrors.set({});
      this.actionNotices.set({});
    }
    this.api.getMatrix({ suppressShellFeedback: true }).subscribe({
      next: data => { this.matrix.set(data); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set(this.errorViewModel(err, 'admin.setup.draw_sales_matrix', 'page'));
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
    const drawChannelId = slot.channel?.drawChannelId.value;
    if (!drawChannelId) return;
    const tenantGameId = game.tenantGameId.value;
    const key = this.actingKey(drawChannelId, tenantGameId);
    this.acting.set(key);
    this.clearActionError(key);
    this.clearActionNotice(key);
    this.api.offerGame(drawChannelId, tenantGameId, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.acting.set(null);
        this.setActionNotice(key, `${game.gameCode} ajouté au canal.`);
        this.load(true);
      },
      error: (err: unknown) => {
        this.acting.set(null);
        this.setActionError(key, err, drawChannelId, tenantGameId);
      },
    });
  }

  toggleGame(slot: SlotMatrixView, game: ChannelGameSetupView): void {
    const drawChannelId = slot.channel?.drawChannelId.value;
    if (!drawChannelId) return;
    const tenantGameId = game.tenantGameId.value;
    const key = this.actingKey(drawChannelId, tenantGameId);
    this.acting.set(key);
    this.clearActionError(key);
    this.clearActionNotice(key);
    const newEnabled = !game.enabledOnChannel;
    this.api.toggleGame(drawChannelId, tenantGameId, newEnabled, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.acting.set(null);
        this.setActionNotice(key, `${game.gameCode} ${newEnabled ? 'activé' : 'désactivé'}.`);
        this.load(true);
      },
      error: (err: unknown) => {
        this.acting.set(null);
        this.setActionError(key, err, drawChannelId, tenantGameId);
      },
    });
  }

  removeGame(slot: SlotMatrixView, game: ChannelGameSetupView): void {
    const drawChannelId = slot.channel?.drawChannelId.value;
    if (!drawChannelId) return;
    const tenantGameId = game.tenantGameId.value;
    const key = this.actingKey(drawChannelId, tenantGameId);
    this.acting.set(key);
    this.clearActionError(key);
    this.clearActionNotice(key);
    this.api.removeGame(drawChannelId, tenantGameId, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.acting.set(null);
        this.setActionNotice(key, `${game.gameCode} retiré du canal.`);
        this.load(true);
      },
      error: (err: unknown) => {
        this.acting.set(null);
        this.setActionError(key, err, drawChannelId, tenantGameId);
      },
    });
  }

  actionError(drawChannelId: string, tenantGameId: string): ErrorViewModel | null {
    return this.actionErrors()[this.actingKey(drawChannelId, tenantGameId)] ?? null;
  }

  actionNotice(drawChannelId: string, tenantGameId: string): string | null {
    return this.actionNotices()[this.actingKey(drawChannelId, tenantGameId)] ?? null;
  }

  severityIcon(w: SetupWarning): string {
    if (w.severity === 'ERROR') return 'error';
    if (w.severity === 'WARN') return 'warning';
    return 'info';
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

  private setActionError(
    key: string,
    err: unknown,
    drawChannelId: string,
    tenantGameId: string,
  ): void {
    const error = this.errorViewModel(err, `admin.setup.draw_sales_matrix.${drawChannelId}.${tenantGameId}`, 'section');
    this.actionErrors.update(current => ({ ...current, [key]: error }));
  }

  private setActionNotice(key: string, message: string): void {
    this.actionNotices.update(current => ({ ...current, [key]: message }));
  }

  private clearActionError(key: string): void {
    this.actionErrors.update(current => {
      if (!current[key]) return current;
      const next = { ...current };
      delete next[key];
      return next;
    });
  }

  private clearActionNotice(key: string): void {
    this.actionNotices.update(current => {
      if (!current[key]) return current;
      const next = { ...current };
      delete next[key];
      return next;
    });
  }

  private errorViewModel(
    err: unknown,
    source: string,
    surface: 'page' | 'section',
  ): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (!problem) {
      return {
        severity: 'error',
        title: this.translate.instant('common.errors.fallback.title'),
        message: this.translate.instant('common.errors.fallback.message'),
      };
    }

    const normalized = webAppErrorFromProblemDetail(problem, source, surface);
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }
}
