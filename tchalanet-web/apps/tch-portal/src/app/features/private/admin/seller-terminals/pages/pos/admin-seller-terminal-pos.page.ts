import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';

import { TchErrorPanel, TchLoading, TchNotice } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import {
  AdminSectionErrorTargetDirective,
  AdminSectionTargetError,
} from '../../../../shared/admin-ui/admin-section-error-target.directive';

import { AdminSellerTerminalPosApiService } from '../../data-access/admin-seller-terminal-pos-api.service';
import {
  ConfirmedTicketView,
  PosGameView,
  PosOpenDrawView,
  PosSellerTerminalView,
  PosTerminalActivityView,
  PosTicketDraftLine,
  PosTicketLineInput,
} from '../../data-access/admin-seller-terminal-pos.models';

import { PosOpenDrawCardComponent } from '../../components/pos-open-draw-card/pos-open-draw-card.component';
import { PosGameSelectorComponent } from '../../components/pos-game-selector/pos-game-selector.component';
import { PosTicketLineEditorComponent } from '../../components/pos-ticket-line-editor/pos-ticket-line-editor.component';
import { PosTicketPreviewComponent } from '../../components/pos-ticket-preview/pos-ticket-preview.component';
import { PosTerminalActivityCardComponent } from '../../components/pos-terminal-activity-card/pos-terminal-activity-card.component';
import { PosSaleSuccessNoticeComponent } from '../../components/pos-sale-success-notice/pos-sale-success-notice.component';

let lineIdCounter = 0;

@Component({
  selector: 'tch-admin-seller-terminal-pos-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminSectionErrorTargetDirective,
    TchLoading,
    TchErrorPanel,
    TchNotice,
    PosOpenDrawCardComponent,
    PosGameSelectorComponent,
    PosTicketLineEditorComponent,
    PosTicketPreviewComponent,
    PosTerminalActivityCardComponent,
    PosSaleSuccessNoticeComponent,
  ],
  templateUrl: './admin-seller-terminal-pos.page.html',
  styleUrls: ['./admin-seller-terminal-pos.page.scss'],
})
export class AdminSellerTerminalPosPage implements OnInit {
  private readonly api = inject(AdminSellerTerminalPosApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly saving = signal(false);
  readonly printing = signal(false);
  readonly saleError = signal<ErrorViewModel | null>(null);
  readonly activityLoading = signal(false);
  readonly sectionErrors = signal<readonly AdminSectionTargetError[]>([]);

  readonly sellerTerminal = signal<PosSellerTerminalView | null>(null);
  readonly openDraws = signal<PosOpenDrawView[]>([]);
  readonly games = signal<PosGameView[]>([]);
  readonly selectedDraw = signal<PosOpenDrawView | null>(null);
  readonly selectedGameCode = signal<string | null>(null);
  readonly lines = signal<PosTicketDraftLine[]>([]);
  readonly confirmedTicket = signal<ConfirmedTicketView | null>(null);
  readonly activity = signal<PosTerminalActivityView | null>(null);

  readonly pageDescription = computed(() => {
    const t = this.sellerTerminal();
    return t ? `${t.displayName} · ${t.terminalCode}` : '';
  });

  readonly isTerminalBlocked = computed(
    () => this.sellerTerminal()?.status !== 'ACTIVE',
  );

  readonly totalAmount = computed(() =>
    this.lines().reduce((sum, l) => sum + l.stakeAmount, 0),
  );

  readonly canConfirm = computed(
    () =>
      !!this.sellerTerminal() &&
      !this.isTerminalBlocked() &&
      !!this.selectedDraw() &&
      this.lines().length > 0 &&
      this.lines().every(l => l.selection.trim().length > 0 && l.stakeAmount > 0) &&
      !this.saving(),
  );

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('sellerTerminalId') ?? '';
    this.loadPage(id);
  }

  private loadPage(sellerTerminalId: string): void {
    this.loading.set(true);
    this.pageError.set(null);
    this.sectionErrors.set([]);

    this.api.getSellerTerminalForPos(sellerTerminalId, { suppressShellFeedback: true }).subscribe({
      next: terminal => {
        this.sellerTerminal.set(terminal);
        this.loading.set(false);
        this.loadDraws();
        this.loadGames();
        this.loadActivity(sellerTerminalId);
      },
      error: (err: unknown) => {
        this.pageError.set(this.errorViewModel(err, 'admin.sellerTerminal.pos.terminal'));
        this.loading.set(false);
      },
    });
  }

  private loadDraws(): void {
    this.clearSectionError('admin.sellerTerminal.pos.draws');
    this.api.getOpenDrawsForPos(24, { suppressShellFeedback: true }).subscribe({
      next: draws => {
        this.openDraws.set(draws);
        this.selectedDraw.set(draws[0] ?? null);
      },
      error: err => {
        this.openDraws.set([]);
        this.selectedDraw.set(null);
        this.setSectionError('admin.sellerTerminal.pos.draws', err);
      },
    });
  }

  private loadGames(): void {
    this.clearSectionError('admin.sellerTerminal.pos.games');
    this.api.getActiveGamesForPos({ suppressShellFeedback: true }).subscribe({
      next: games => {
        this.games.set(games);
        const borlette = games.find(g => g.gameCode === 'BORLETTE' && g.enabled);
        this.selectedGameCode.set(borlette?.gameCode ?? games.find(g => g.enabled)?.gameCode ?? null);
      },
      error: err => {
        this.games.set([]);
        this.selectedGameCode.set(null);
        this.setSectionError('admin.sellerTerminal.pos.games', err);
      },
    });
  }

  private loadActivity(sellerTerminalId: string): void {
    this.activityLoading.set(true);
    this.clearSectionError('admin.sellerTerminal.pos.activity');
    this.api.getTerminalActivity(sellerTerminalId, { suppressShellFeedback: true }).subscribe({
      next: a => {
        this.activity.set(a);
        this.activityLoading.set(false);
      },
      error: err => {
        this.activity.set(null);
        this.setSectionError('admin.sellerTerminal.pos.activity', err);
        this.activityLoading.set(false);
      },
    });
  }

  selectDraw(draw: PosOpenDrawView): void {
    this.selectedDraw.set(draw);
  }

  selectGame(gameCode: string): void {
    this.selectedGameCode.set(gameCode);
  }

  hasSectionError(target: string): boolean {
    return this.sectionErrors().some(error => error.target === target);
  }

  addLine(input: PosTicketLineInput): void {
    this.lines.update(ls => [
      ...ls,
      { ...input, localId: `line-${++lineIdCounter}` },
    ]);
    this.confirmedTicket.set(null);
    this.saleError.set(null);
  }

  removeLine(localId: string): void {
    this.lines.update(ls => ls.filter(l => l.localId !== localId));
  }

  clearLines(): void {
    this.lines.set([]);
    this.confirmedTicket.set(null);
    this.saleError.set(null);
  }

  confirmSale(): void {
    if (!this.canConfirm()) return;
    const terminal = this.sellerTerminal();
    const draw = this.selectedDraw();
    if (!terminal || !draw) return;

    this.saving.set(true);
    this.saleError.set(null);

    const idempotencyKey = crypto.randomUUID();

    const terminalId = terminal.sellerTerminalId;

    this.api
      .confirmTicketSale(
        {
          sellerTerminalId: terminalId,
          drawId: draw.drawId,
          drawChannelId: draw.drawChannelId ?? null,
          currency: 'HTG',
          lines: this.lines().map(l => ({
            gameCode: l.gameCode,
            betType: l.betType,
            selection: l.selection,
            stake: l.stakeAmount,
          })),
        },
        idempotencyKey,
        terminalId,
        { suppressShellFeedback: true },
      )
      .subscribe({
        next: ticket => {
          this.confirmedTicket.set(ticket);
          this.lines.set([]);
          this.saving.set(false);
          const id = this.route.snapshot.paramMap.get('sellerTerminalId') ?? '';
          this.loadActivity(id);
        },
        error: (err: unknown) => {
          this.saleError.set(this.errorViewModel(err, 'admin.sellerTerminal.pos.sale'));
          this.saving.set(false);
        },
      });
  }

  resetAfterSale(): void {
    this.confirmedTicket.set(null);
    this.saleError.set(null);
    this.lines.set([]);
  }

  printTicket(ticket: ConfirmedTicketView): void {
    const terminalId = this.sellerTerminal()?.sellerTerminalId;
    if (!terminalId || this.printing()) return;

    this.printing.set(true);
    this.saleError.set(null);

    this.api.printTicket(ticket.ticketId, terminalId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const opened = window.open(url, '_blank', 'noopener,noreferrer');

        if (!opened) {
          const link = document.createElement('a');
          link.href = url;
          link.download = `${ticket.ticketCode}.pdf`;
          link.click();
        }

        window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
        this.printing.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.saleError.set(pd?.title ?? 'Erreur lors de la génération du ticket.');
        this.printing.set(false);
      },
    });
  }

  viewTicketDetails(ticket: ConfirmedTicketView): void {
    void this.router.navigate(['/app/admin/tickets', ticket.ticketId]);
  }

  goBack(): void {
    void this.router.navigate(['/app/admin/seller-terminals']);
  }

  private setSectionError(target: string, err: unknown): void {
    const vm = this.errorViewModel(err, target);
    this.sectionErrors.update(errors => [
      ...errors.filter(error => error.target !== target),
      { ...vm, target },
    ]);
  }

  private clearSectionError(target: string): void {
    this.sectionErrors.update(errors => errors.filter(error => error.target !== target));
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}
