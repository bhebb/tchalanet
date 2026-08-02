import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchNotice } from '@tch/ui/components';
import {
  AdminDetailLayoutComponent,
  AdminPageShellComponent,
  AdminSectionCardComponent,
  AdminStatusTone,
  TchIdentityCardComponent,
  type TchIdentityCardMeta,
} from '@tch/ui/console';
import {
  ConsoleTicketDrawCardComponent,
  ConsoleTicketSelectionsCardComponent,
  consoleTicketDrawIdentity,
} from '@tch/web/console';
import type {
  ConsoleFact,
  ConsoleTicketPricingLabel,
  ConsoleTicketSelectionView,
} from '@tch/web/console';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

import { PosSaleSuccessDialogComponent } from '../../components/pos-sale-success-dialog/pos-sale-success-dialog.component';
import { PosSaleApiService } from '../../data-access/pos-sale-api.service';
import { PosTicketDetailsView, PosTicketPricingTermView } from '../../data-access/pos-sale.models';
import {
  ticketStatusLabelKey,
  ticketStatusTone,
} from '../../../../../shared/ticket/admin-ticket-status.util';

@Component({
  selector: 'tch-pos-ticket-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    AdminDetailLayoutComponent,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchIdentityCardComponent,
    TchErrorPanel,
    ConsoleTicketDrawCardComponent,
    ConsoleTicketSelectionsCardComponent,
    TchLoading,
    TchNotice,
    TranslatePipe,
  ],
  templateUrl: './pos-ticket-detail.page.html',
  styleUrls: ['./pos-ticket-detail.page.scss'],
})
export class PosTicketDetailPage implements OnInit {
  private readonly api = inject(PosSaleApiService);
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly ticket = signal<PosTicketDetailsView | null>(null);
  readonly reprinting = signal(false);
  readonly reprintError = signal<string | null>(null);

  readonly summaryMeta = computed<readonly TchIdentityCardMeta[]>(() => {
    const ticket = this.ticket();
    if (!ticket) return [];

    const items: TchIdentityCardMeta[] = [
      {
        label: this.translate.instant('admin.pos.detail.meta.totalPaid'),
        value: `${this.amountDisplay(ticket.totalAmountCents)} ${ticket.currency}`,
      },
      {
        label: this.translate.instant('admin.pos.detail.meta.seller'),
        value: ticket.sellerDisplayName || '—',
      },
      {
        label: this.translate.instant('admin.pos.detail.meta.terminal'),
        value: ticket.terminalCode || '—',
      },
    ];

    if (this.freeLineCount(ticket) > 0) {
      items.splice(1, 0, {
        label: this.translate.instant('admin.pos.detail.meta.freeLines'),
        value: this.freeLineCount(ticket),
      });
    }

    return items;
  });

  readonly drawCard = computed(() => {
    const ticket = this.ticket();
    if (!ticket) return null;

    return consoleTicketDrawIdentity({
      channelCode: ticket.drawChannelCode,
      channelLabel: ticket.drawChannelName,
      resultSlotKey: ticket.resultSlotKey,
      drawDateLabel: this.formatDate(ticket.drawScheduledAt, 'dd/MM/yyyy'),
      scheduledAt: ticket.drawScheduledAt,
      fallbackLabel: ticket.drawChannelName,
    });
  });

  readonly drawFacts = computed<readonly ConsoleFact[]>(() => {
    const ticket = this.ticket();
    const draw = this.drawCard();
    if (!ticket || !draw) return [];

    return [
      {
        label: this.translate.instant('admin.pos.detail.field.dateTime'),
        value: draw.receiptDateTimeLabel,
      },
    ];
  });

  readonly selectionLines = computed<readonly ConsoleTicketSelectionView[]>(() => {
    const ticket = this.ticket();
    if (!ticket) return [];

    return ticket.lines.map(line => ({
      lineNumber: line.lineNumber,
      gameCode: line.gameCode,
      gameLabel: this.gameLabel(line.gameCode, line.gameLabel),
      selection: line.selection,
      betTypeLabel: line.betTypeLabel || line.pricingTerms?.[0]?.commercialLabel || null,
      amountLabel: `${this.amountDisplay(line.stakeAmountCents)} ${ticket.currency}`,
      promotional: line.promotional,
      promotionLabel: line.promotional ? this.promotionLabel(line.promotionLabel) : null,
      pricingLabels: this.pricingLabels(line.pricingTerms ?? [], ticket.currency),
    }));
  });

  readonly title = computed(() => {
    const ticket = this.ticket();
    return ticket ? `Ticket ${ticket.ticketCode}` : 'Détail du ticket';
  });

  readonly description = computed(() => {
    const ticket = this.ticket();
    return ticket
      ? `${this.drawLabel(ticket)} · ${this.amountDisplay(ticket.totalAmountCents)} ${ticket.currency}`
      : 'Consultez les informations de vente, de tirage et de lignes du ticket.';
  });

  drawLabel(ticket: PosTicketDetailsView): string {
    return consoleTicketDrawIdentity({
      channelCode: ticket.drawChannelCode,
      channelLabel: ticket.drawChannelName,
      resultSlotKey: ticket.resultSlotKey,
      scheduledAt: ticket.drawScheduledAt,
      fallbackLabel: ticket.drawChannelName,
    }).receiptLabel;
  }

  private formatDate(value: string, format: 'dd/MM/yyyy' | 'dd/MM/yyyy HH:mm'): string {
    const date = new Date(value);
    if (!Number.isFinite(date.getTime())) return value;

    const pad = (part: number): string => String(part).padStart(2, '0');
    const dateLabel = `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
    if (format === 'dd/MM/yyyy') return dateLabel;
    return `${dateLabel} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const ticketId = this.route.snapshot.paramMap.get('ticketId');
    if (!ticketId) {
      this.error.set({
        title: this.translate.instant('admin.pos.detail.error.notFoundTitle'),
        message: this.translate.instant('admin.pos.detail.error.notFoundMessage'),
        severity: 'error',
      });
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.api.getTicketDetails(ticketId, { suppressShellFeedback: true }).subscribe({
      next: ticket => {
        this.ticket.set(ticket);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(this.errorViewModel(err, 'admin.pos.ticket.detail'));
        this.loading.set(false);
      },
    });
  }

  amountDisplay(cents: number): string {
    return (cents / 100).toFixed(2);
  }

  statusLabel(status: string): string {
    return this.translate.instant(ticketStatusLabelKey(status));
  }

  statusTone(status: string): AdminStatusTone {
    return ticketStatusTone(status);
  }

  freeLineCount(ticket: PosTicketDetailsView): number {
    return ticket.lines.filter(line => line.promotional).length;
  }

  gameLabel(gameCode: string, fallback?: string | null): string {
    const normalizedCode = normalizeGameCode(gameCode);
    const key = `common.game.${normalizedCode}`;
    const translated = this.translate.instant(key);
    if (translated !== key) return translated;

    const cleanFallback = fallback?.trim();
    return cleanFallback && cleanFallback !== gameCode ? cleanFallback : gameCode;
  }

  promotionLabel(label?: string | null): string {
    const clean = label?.trim();
    if (!clean) return this.translate.instant('common.receipt.promotion.applied');

    const key = clean.startsWith('receipt.') ? `common.${clean}` : clean;
    const translated = this.translate.instant(key);
    return translated !== key ? translated : clean;
  }

  pricingLabels(
    terms: readonly PosTicketPricingTermView[],
    currency: string,
  ): readonly ConsoleTicketPricingLabel[] {
    return terms.map(term => ({
      value: this.pricingValue(term, currency),
      source: this.pricingSource(term.source),
    }));
  }

  private pricingValue(term: PosTicketPricingTermView, currency: string): string {
    const value =
      term.payoutRuleType === 'FIXED_AMOUNT'
        ? this.formatDecimal(term.fixedAmount)
        : `x${this.formatDecimal(term.multiplier)}`;
    const suffix = term.payoutRuleType === 'FIXED_AMOUNT' ? ` ${currency}` : '';
    return `${this.translate.instant('admin.pos.detail.pricing.label')} ${value}${suffix}`;
  }

  private pricingSource(source: string): string {
    if (source === 'SELLER_TERMINAL_OVERRIDE') {
      return this.translate.instant('admin.pos.detail.pricing.sourceSeller');
    }
    if (source === 'TENANT_DEFAULT') {
      return this.translate.instant('admin.pos.detail.pricing.sourceTenant');
    }
    return this.translate.instant('admin.pos.detail.pricing.sourceUnknown');
  }

  private formatDecimal(value?: number | string | null): string {
    if (value === null || value === undefined || value === '') return '—';
    const numeric = Number(value);
    if (!Number.isFinite(numeric)) return String(value);
    return new Intl.NumberFormat(this.translate.currentLang || 'fr', {
      maximumFractionDigits: 2,
    }).format(numeric);
  }

  canReprint(ticket: PosTicketDetailsView): boolean {
    return (!!this.sellerTerminalId(ticket) || !!ticket.terminalCode) && !this.reprinting();
  }

  reprint(ticket: PosTicketDetailsView): void {
    if (!this.canReprint(ticket)) return;

    this.reprinting.set(true);
    this.reprintError.set(null);

    this.resolveSellerTerminalId(ticket).subscribe({
      next: sellerTerminalId => {
        if (!sellerTerminalId) {
          this.reprintError.set(
            this.reprintFailureMessage(new Error('seller-terminal-id-missing')),
          );
          this.reprinting.set(false);
          return;
        }
        this.dialog.open(PosSaleSuccessDialogComponent, {
          width: 'min(42rem, calc(100vw - 2rem))',
          data: {
            mode: 'ticket-actions',
            sellerTerminalId,
            totalAmount: ticket.totalAmountCents / 100,
            currency: ticket.currency,
            lineCount: ticket.lines.length,
            freeLineCount: ticket.lines.filter(line => line.promotional).length,
            ticket: {
              ticketId: ticket.id,
              ticketCode: ticket.ticketCode,
              publicCode: ticket.publicCode,
              actionAvailability: {
                canSell: false,
                canPrint: true,
                canSendSms: false,
                canSendWhatsapp: false,
                canSendEmail: false,
                canCopy: true,
              },
            },
          },
        });
        this.reprinting.set(false);
      },
      error: err => {
        this.reprintError.set(this.reprintFailureMessage(err));
        this.reprinting.set(false);
      },
    });
  }

  reprintDisabledReason(ticket: PosTicketDetailsView): string | null {
    if (this.reprinting()) return this.translate.instant('admin.pos.detail.notice.reprinting');
    if (!this.sellerTerminalId(ticket) && !ticket.terminalCode) {
      return this.translate.instant('admin.pos.detail.notice.missingTerminal');
    }
    return null;
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const normalized = webAppErrorFromProblemDetail(
      mapHttpErrorToProblemDetail(err),
      source,
      'page',
    );
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }

  private sellerTerminalId(ticket: PosTicketDetailsView): string | null {
    const value = ticket.sellerTerminalId;
    if (typeof value === 'string') return value;
    return value?.value ?? null;
  }

  private resolveSellerTerminalId(ticket: PosTicketDetailsView): Observable<string | null> {
    const direct = this.sellerTerminalId(ticket);
    if (direct) return of(direct);

    const terminalCode = ticket.terminalCode?.trim();
    if (!terminalCode) return of(null);

    return this.api
      .listSellerTerminalsForSale(
        { q: terminalCode, page: 0, size: 20 },
        { suppressShellFeedback: true },
      )
      .pipe(
        map(page => {
          const normalizedTerminalCode = normalizeTerminalCode(terminalCode);
          return (
            page.items.find(
              item => normalizeTerminalCode(item.terminalCode) === normalizedTerminalCode,
            )?.sellerTerminalId ?? null
          );
        }),
      );
  }

  private reprintFailureMessage(err: unknown): string {
    if (err instanceof Error && err.message === 'seller-terminal-id-missing') {
      const terminalCode = this.ticket()?.terminalCode?.trim();
      return terminalCode
        ? this.translate.instant('admin.pos.detail.error.terminalNotFound', { terminalCode })
        : this.translate.instant('admin.pos.detail.notice.missingTerminal');
    }

    return this.translate.instant('admin.pos.detail.error.reprintFailed');
  }
}

function normalizeTerminalCode(value: string): string {
  return value.trim().toUpperCase();
}

function normalizeGameCode(value: string): string {
  const code = value.trim().toUpperCase();
  if (code === 'BORLETTE') return 'HT_BOLET';
  if (code === 'HT_LOTO_3') return 'HT_LOTO3';
  if (code === 'HT_LOTO_4') return 'HT_LOTO4';
  if (code === 'HT_LOTO_5') return 'HT_LOTO5';
  if (code === 'HT_MARYAJ_GRATUIT') return 'HT_MARYAJ_GRATIS';
  return code;
}
