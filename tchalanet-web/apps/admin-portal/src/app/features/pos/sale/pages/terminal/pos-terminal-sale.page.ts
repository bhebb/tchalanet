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
import { ProblemDetail, WebAppError, webAppErrorFromProblemDetail } from '@tch/api';
import { throwError } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';

import { TchErrorPanel, TchLoading, TchNotice } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminSectionCardComponent } from '@tch/ui/console';
import {
  AdminSectionErrorTargetDirective,
  AdminSectionTargetError,
} from '@tch/ui/console';

import { PosSaleApiService } from '../../data-access/pos-sale-api.service';
import {
  ConfirmTicketSaleRequest,
  ConfirmedTicketView,
  PreviewTicketSaleView,
  PosGameView,
  PosOpenDrawView,
  PosSellerTerminalView,
  PosTerminalActivityView,
  PosTicketDraftLine,
  PosTicketLineInput,
} from '../../data-access/pos-sale.models';

import { PosOpenDrawCardComponent } from '../../components/pos-open-draw-card/pos-open-draw-card.component';
import { PosGameSelectorComponent } from '../../components/pos-game-selector/pos-game-selector.component';
import { PosTicketLineEditorComponent } from '../../components/pos-ticket-line-editor/pos-ticket-line-editor.component';
import { PosTicketPreviewComponent } from '../../components/pos-ticket-preview/pos-ticket-preview.component';
import { PosTerminalActivityCardComponent } from '../../components/pos-terminal-activity-card/pos-terminal-activity-card.component';
import { PosSaleSuccessNoticeComponent } from '../../components/pos-sale-success-notice/pos-sale-success-notice.component';

let lineIdCounter = 0;

@Component({
  selector: 'tch-pos-terminal-sale-page',
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
  templateUrl: './pos-terminal-sale.page.html',
  styleUrls: ['./pos-terminal-sale.page.scss'],
})
export class PosTerminalSalePage implements OnInit {
  private readonly api = inject(PosSaleApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly saving = signal(false);
  readonly printing = signal(false);
  readonly saleError = signal<ErrorViewModel | null>(null);
  readonly printError = signal<ErrorViewModel | null>(null);
  readonly saleNotices = signal<readonly WebAppError[]>([]);
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
      !this.confirmedTicket() &&
      !this.saving(),
  );

  readonly confirmDisabledReason = computed(() => {
    if (this.confirmedTicket()) return 'Ticket déjà vendu. Créez un nouveau ticket pour continuer.';
    if (this.saving()) return 'Vente en cours de confirmation.';
    if (!this.sellerTerminal()) return 'Terminal vendeur non chargé.';
    if (this.isTerminalBlocked()) return 'Ce terminal vendeur ne peut pas vendre.';
    if (!this.selectedDraw()) return 'Sélectionnez un tirage ouvert.';
    if (this.lines().length === 0) return 'Ajoutez au moins un numéro au ticket.';
    if (!this.lines().every(l => l.selection.trim().length > 0 && l.stakeAmount > 0)) {
      return 'Complétez les lignes du ticket avant de confirmer.';
    }
    return null;
  });

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
    this.lines.update(lines => {
      const existingIndex = lines.findIndex(line =>
        line.gameCode === input.gameCode &&
        line.selection === input.selection &&
        line.betType === input.betType,
      );

      if (existingIndex < 0) {
        return [
          ...lines,
          { ...input, localId: `line-${++lineIdCounter}` },
        ];
      }

      return lines.map((line, index) => index === existingIndex
        ? { ...line, stakeAmount: line.stakeAmount + input.stakeAmount }
        : line);
    });
    this.confirmedTicket.set(null);
    this.saleError.set(null);
    this.printError.set(null);
    this.saleNotices.set([]);
  }

  updateLineStake(update: { localId: string; stakeAmount: number }): void {
    this.lines.update(lines => lines.map(line => line.localId === update.localId
      ? { ...line, stakeAmount: update.stakeAmount }
      : line));
    this.confirmedTicket.set(null);
    this.saleError.set(null);
    this.printError.set(null);
    this.saleNotices.set([]);
  }

  removeLine(localId: string): void {
    this.lines.update(ls => ls.filter(l => l.localId !== localId));
    this.confirmedTicket.set(null);
    this.saleError.set(null);
    this.printError.set(null);
    this.saleNotices.set([]);
  }

  clearLines(): void {
    this.lines.set([]);
    this.confirmedTicket.set(null);
    this.saleError.set(null);
    this.printError.set(null);
    this.saleNotices.set([]);
  }

  confirmSale(): void {
    if (!this.canConfirm()) return;
    const terminal = this.sellerTerminal();
    const draw = this.selectedDraw();
    if (!terminal || !draw) return;

    this.saving.set(true);
    this.saleError.set(null);
    this.printError.set(null);
    this.saleNotices.set([]);

    const idempotencyKey = crypto.randomUUID();

    const terminalId = terminal.sellerTerminalId;
    const request = this.ticketSaleRequest(terminalId, draw);

    this.api
      .previewTicketSale(request, terminalId, { suppressShellFeedback: true })
      .pipe(
        switchMap(preview => {
          this.saleNotices.set(preview.notices);

          if (!preview.canSell || preview.decision !== 'ACCEPTABLE') {
            return throwError(() => this.previewRejectedError(preview));
          }

          return this.api.confirmTicketSale(
              request,
              idempotencyKey,
              terminalId,
              { suppressShellFeedback: true },
            )
            .pipe(map(ticket => ({
              ...ticket,
              warnings: [...preview.notices, ...ticket.warnings],
            } satisfies ConfirmedTicketView)));
        }),
      )
      .subscribe({
        next: ticket => {
          this.confirmedTicket.set(ticket);
          this.saving.set(false);
          this.bumpActivityAfterSale();
          const id = this.route.snapshot.paramMap.get('sellerTerminalId') ?? '';
          this.loadActivity(id);
        },
        error: (err: unknown) => {
          const vm = this.errorViewModel(err, 'admin.sellerTerminal.pos.sale');
          this.saleError.set(isPreviewRejectedViewModel(vm) && this.saleNotices().length > 0
            ? null
            : vm);
          this.saving.set(false);
        },
      });
  }

  private ticketSaleRequest(
    terminalId: string,
    draw: PosOpenDrawView,
  ): ConfirmTicketSaleRequest {
    return {
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
    };
  }

  private previewRejectedError(preview: PreviewTicketSaleView): ErrorViewModel {
    const instruction = preview.sellerInstruction
      ?? preview.issues.find(issue => !!issue.sellerInstruction)?.sellerInstruction
      ?? preview.warning
      ?? 'Le ticket doit être corrigé avant la vente.';

    return {
      title: 'Vente à vérifier',
      message: instruction,
      severity: preview.decision === 'REJECTED_FINAL' ? 'error' : 'warn',
      source: 'admin.sellerTerminal.pos.preview',
      target: 'admin.sellerTerminal.pos.sale',
      code: preview.issues[0]?.code ?? preview.decision,
      retryable: false,
    };
  }

  resetAfterSale(): void {
    this.confirmedTicket.set(null);
    this.saleError.set(null);
    this.printError.set(null);
    this.saleNotices.set([]);
    this.lines.set([]);
  }

  noticeType(error: WebAppError): 'info' | 'warning' | 'error' {
    if (error.severity === 'error') return 'error';
    if (error.severity === 'warn') return 'warning';
    return 'info';
  }

  noticeTitle(error: WebAppError): string {
    return resolveErrorFeedbackCopy(error, key => this.translate.instant(key)).title;
  }

  noticeMessage(error: WebAppError): string {
    return resolveErrorFeedbackCopy(error, key => this.translate.instant(key)).message;
  }

  noticeScope(error: WebAppError): string | null {
    const match = /^lines\.(\d+)$/.exec(error.field ?? '');
    if (!match) return null;
    return `Ligne ${Number(match[1]) + 1}`;
  }

  private bumpActivityAfterSale(): void {
    const current = this.activity();
    if (!current) return;
    this.activity.set({
      ...current,
      ticketCount: current.ticketCount + 1,
      salesTotalCents: current.salesTotalCents + Math.round(this.totalAmount() * 100),
    });
  }

  printTicket(ticket: ConfirmedTicketView): void {
    const terminalId = this.sellerTerminal()?.sellerTerminalId;
    if (!terminalId || !ticket.ticketId || this.printing()) return;

    this.printing.set(true);
    this.printError.set(null);

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
        const vm = this.errorViewModel(err, 'admin.sellerTerminal.pos.print');
        this.printError.set({
          ...vm,
          title: 'Impression non disponible',
          message: 'Le ticket est vendu, mais le reçu n’a pas pu être imprimé. Réessayez depuis ce ticket.',
          severity: 'warn',
        });
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
    if (isErrorViewModel(err)) {
      return err;
    }

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

function isErrorViewModel(value: unknown): value is ErrorViewModel {
  return typeof value === 'object' &&
    value !== null &&
    'title' in value &&
    'message' in value &&
    'severity' in value;
}

function isPreviewRejectedViewModel(value: ErrorViewModel): boolean {
  return value.source === 'admin.sellerTerminal.pos.preview';
}
