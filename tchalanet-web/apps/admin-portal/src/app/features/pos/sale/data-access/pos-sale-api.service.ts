import { Injectable, inject } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import {
  TchBackendClient,
  TchPage,
  TchRequestOptions,
  WebAppError,
  webAppErrorFromNotice,
} from '@tch/api';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import {
  ConfirmTicketSaleRequest,
  ConfirmedTicketView,
  PreviewTicketSaleView,
  PosGameBetTypeView,
  PosGameView,
  PosOpenDrawView,
  PosSellerTerminalListParams,
  PosSellerTerminalPickerView,
  PosSellerTerminalView,
  PosTerminalActivityView,
} from './pos-sale.models';

// ── Server response shapes (internal to this service) ──────────────────────

interface PosAvailableDrawResponse {
  drawId: string;
  drawChannelId: string;
  drawDate: string;
  resultSlotKey: string;
  channelCode: string;
  channelLabel: string;
  gameCodes: string[];
  status: string;
  scheduledAt: string;
  cutoffAt: string;
}

interface PosGameOptionResponse {
  gameCode: string;
  gameLabel: string;
  betType: string;
  betTypeLabel: string;
  requiresOption: boolean;
  options: { code: number; label: string; selectionHint?: string | null }[];
  selectionHint?: string | null;
}

interface SellerTerminalDetailResponse {
  id: { value: string };
  terminalCode: string;
  displayName: string;
  status: string;
  commissionRate?: number | null;
}

interface SellerTerminalSummaryResponse extends SellerTerminalDetailResponse {
  lastSeenAt?: string | null;
  todayTicketCount?: number | null;
  todaySalesAmount?: number | null;
}

interface PosSellTicketApiResponse {
  outcome: 'ACCEPTED' | 'REJECTED' | 'PENDING_APPROVAL';
  ticketId?: string | { value?: string | null } | null;
  ticketCode?: string | null;
  publicCode?: string | null;
  saleStatus?: string | null;
  issues?: PosSaleIssueApiResponse[] | null;
  backup?: {
    displayCode?: string | null;
    verificationShortUrl?: string | null;
    shareableText?: string | null;
  } | null;
  sellerInstruction?: string | null;
}

interface PosTicketPreviewApiResponse {
  decision: string;
  issues?: PosSaleIssueApiResponse[] | null;
  actionAvailability?: {
    canSell?: boolean;
    canPrint?: boolean;
    canSendSms?: boolean;
    canSendWhatsapp?: boolean;
    canSendEmail?: boolean;
    canCopy?: boolean;
  } | null;
  sellerInstruction?: string | null;
  warning?: string | null;
}

interface PosSaleIssueApiResponse {
  code: string;
  severity: string;
  message?: string | null;
  sellerInstruction?: string | null;
  lineIndex: number;
}

interface SellerTerminalStatsResponse {
  ticketCount: number;
  salesTotalCents: number;
  currency: string;
}

interface PrintTicketRequest {
  printOptionsRequest: {
    outputFormat: 'PDF';
    paperSize: 'RECEIPT_80MM';
  };
  recordPrint: boolean;
  deliveryOptions: readonly ['RETURN_FILE'];
}

// ── Service ────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class PosSaleApiService {
  private readonly backend = inject(TchBackendClient);

  listSellerTerminalsForSale(
    params: PosSellerTerminalListParams = {},
    options?: TchRequestOptions,
  ): Observable<TchPage<PosSellerTerminalPickerView>> {
    return this.backend
      .getPage<SellerTerminalSummaryResponse>('/admin/seller-terminals', {
        ...(options ?? {}),
        params: {
          ...(params.q ? { q: params.q } : {}),
          ...(params.status ? { status: params.status } : {}),
          ...(params.sort ? { sort: params.sort } : {}),
          page: String(params.page ?? 0),
          size: String(params.size ?? 20),
        },
      })
      .pipe(
        map(page => ({
          ...page,
          items: page.items.map(r => ({
            sellerTerminalId: r.id.value,
            terminalCode: r.terminalCode,
            displayName: r.displayName,
            status: r.status,
            commissionRate: r.commissionRate ?? null,
            lastSeenAt: r.lastSeenAt ?? null,
            todayTicketCount: r.todayTicketCount ?? null,
            todaySalesAmount: r.todaySalesAmount ?? null,
          })),
        })),
      );
  }

  getSellerTerminalForPos(
    sellerTerminalId: string,
    options?: TchRequestOptions,
  ): Observable<PosSellerTerminalView> {
    return this.backend
      .get<SellerTerminalDetailResponse>(`/admin/seller-terminals/${sellerTerminalId}`, options)
      .pipe(
        map(r => ({
          sellerTerminalId: r.id.value,
          terminalCode: r.terminalCode,
          displayName: r.displayName,
          status: r.status,
          commissionRate: r.commissionRate ?? null,
        })),
      );
  }

  getOpenDrawsForPos(
    lookaheadHours = 24,
    options?: TchRequestOptions,
  ): Observable<PosOpenDrawView[]> {
    return this.backend
      .get<PosAvailableDrawResponse[]>('/tenant/cashier/draws/available', {
        ...(options ?? {}),
        params: { lookaheadHours: String(lookaheadHours) },
      })
      .pipe(
        map(draws =>
          draws.map(d => ({
            drawId: d.drawId,
            drawChannelId: d.drawChannelId,
            drawDate: d.drawDate,
            channelCode: d.channelCode,
            channelLabel: d.channelLabel,
            gameCodes: d.gameCodes ?? [],
            status: d.status,
            scheduledAt: d.scheduledAt,
            cutoffAt: d.cutoffAt,
            label: d.channelLabel,
          })),
        ),
      );
  }

  getActiveGamesForPos(options?: TchRequestOptions): Observable<PosGameView[]> {
    return this.backend
      .get<PosGameOptionResponse[]>('/tenant/cashier/games/available', options)
      .pipe(map(games => groupPosGames(games)));
  }

  confirmTicketSale(
    request: ConfirmTicketSaleRequest,
    idempotencyKey: string,
    sellerTerminalId: string,
    options?: TchRequestOptions,
  ): Observable<ConfirmedTicketView> {
    return this.backend
      .postApiResponse<PosSellTicketApiResponse>('/tenant/cashier/tickets/sell', request, {
        ...withHeaders(options, {
          'Idempotency-Key': idempotencyKey,
          'X-Tch-Act-As-Terminal': sellerTerminalId,
        }),
      })
      .pipe(
        map(response => {
          const r = response.data;
          return {
            outcome: r.outcome,
            ticketId: idValue(r.ticketId),
            ticketCode: r.ticketCode ?? '',
            publicCode: r.publicCode ?? null,
            saleStatus: r.saleStatus ?? null,
            backup: r.backup ?? null,
            sellerInstruction: r.sellerInstruction ?? null,
            warnings: [
              ...response.notices.map(notice =>
                webAppErrorFromNotice(
                  notice,
                  response.trace,
                  'admin.sellerTerminal.pos.sale',
                  'section',
                ),
              ),
              ...(r.issues ?? []).map(issue =>
                webAppErrorFromSaleIssue(issue, 'admin.sellerTerminal.pos.sale'),
              ),
            ],
          };
        }),
      );
  }

  previewTicketSale(
    request: ConfirmTicketSaleRequest,
    sellerTerminalId: string,
    options?: TchRequestOptions,
  ): Observable<PreviewTicketSaleView> {
    return this.backend
      .postApiResponse<PosTicketPreviewApiResponse>('/tenant/cashier/tickets/preview', request, {
        ...withHeaders(options, {
          'X-Tch-Act-As-Terminal': sellerTerminalId,
        }),
      })
      .pipe(
        map(response => {
          const r = response.data;
          return {
            decision: r.decision,
            sellerInstruction: r.sellerInstruction ?? null,
            warning: r.warning ?? null,
            issues: (r.issues ?? []).map(issue => ({
              code: issue.code,
              severity: issue.severity,
              message: issue.message ?? null,
              sellerInstruction: issue.sellerInstruction ?? null,
              lineIndex: issue.lineIndex,
            })),
            notices: [
              ...response.notices.map(notice =>
                webAppErrorFromNotice(
                  notice,
                  response.trace,
                  'admin.sellerTerminal.pos.preview',
                  'section',
                ),
              ),
              ...(r.issues ?? []).map(issue =>
                webAppErrorFromSaleIssue(issue, 'admin.sellerTerminal.pos.preview'),
              ),
              ...(r.warning
                ? [webAppErrorFromSaleWarning(r.warning, 'admin.sellerTerminal.pos.preview')]
                : []),
            ],
            canSell: r.actionAvailability?.canSell ?? r.decision === 'ACCEPTABLE',
          };
        }),
      );
  }

  getTerminalActivity(
    sellerTerminalId: string,
    options?: TchRequestOptions,
  ): Observable<PosTerminalActivityView> {
    return this.backend
      .get<SellerTerminalStatsResponse>('/tenant/cashier/tickets/stats', {
        ...withHeaders(options, { 'X-Tch-Act-As-Terminal': sellerTerminalId }),
      })
      .pipe(
        map(r => ({
          ticketCount: r.ticketCount,
          salesTotalCents: r.salesTotalCents,
          currency: 'HTG' as const,
        })),
      );
  }

  printTicket(ticketId: string, sellerTerminalId: string): Observable<Blob> {
    const request: PrintTicketRequest = {
      printOptionsRequest: {
        outputFormat: 'PDF',
        paperSize: 'RECEIPT_80MM',
      },
      recordPrint: true,
      deliveryOptions: ['RETURN_FILE'],
    };

    return this.backend.postBlob(`/tenant/cashier/tickets/${ticketId}/print`, request, {
      headers: { 'X-Tch-Act-As-Terminal': sellerTerminalId },
    });
  }
}

function groupPosGames(rows: PosGameOptionResponse[]): PosGameView[] {
  const groups = new Map<string, PosGameView>();

  rows.forEach(row => {
    const existing = groups.get(row.gameCode);
    if (existing && row.gameCode === 'HT_BOLET') {
      return;
    }

    const betType: PosGameBetTypeView = {
      betType: row.betType,
      label: posBetTypeLabel(row),
      requiresOption: row.requiresOption,
      options: row.options ?? [],
      selectionHint: row.selectionHint ?? null,
    };

    if (existing) {
      existing.betTypes.push(betType);
      return;
    }

    groups.set(row.gameCode, {
      gameCode: row.gameCode,
      label: posGameLabel(row),
      enabled: true,
      betType: row.betType,
      betTypeLabel: posBetTypeLabel(row),
      requiresOption: row.requiresOption,
      options: row.options ?? [],
      betTypes: [betType],
      selectionHint: row.selectionHint ?? null,
    });
  });

  return [...groups.values()];
}

function posGameLabel(row: PosGameOptionResponse): string {
  return row.gameCode === 'HT_BOLET' ? 'Borlette' : row.gameLabel;
}

function posBetTypeLabel(row: PosGameOptionResponse): string {
  const labels: Record<string, string> = {
    MATCH_1_2D: 'Boul',
    MATCH_2_2D: 'Boul',
    MATCH_3_2D: 'Boul',
    MARRIAGE_2D2D: 'Maryaj',
    LOTTO3_3D: 'Loto 3',
    LOTTO4_PATTERN: 'Loto 4',
    LOTTO5_PATTERN: 'Loto 5',
  };

  const normalized = labels[row.betType];
  if (normalized) return normalized;

  return row.betTypeLabel && row.betTypeLabel !== row.betType
    ? row.betTypeLabel
    : row.betType;
}

function idValue(value: string | { value?: string | null } | null | undefined): string {
  if (typeof value === 'string') return value;
  return value?.value ?? '';
}

function webAppErrorFromSaleIssue(issue: PosSaleIssueApiResponse, source: string): WebAppError {
  const message =
    issue.sellerInstruction ??
    issue.message ??
    'La vente doit être vérifiée avant de continuer.';
  const severity = saleIssueSeverity(issue.severity);

  return {
    id: `${source}:${issue.code}:${issue.lineIndex}:${issue.severity}`,
    origin: 'backend',
    category: 'validation',
    severity,
    surface: 'section',
    placement: 'top',
    title: severity === 'error' ? 'Vente bloquée' : 'Vente à vérifier',
    message,
    code: issue.code,
    source,
    target: 'admin.sellerTerminal.pos.sale',
    field: issue.lineIndex >= 0 ? `lines.${issue.lineIndex}` : undefined,
    retryable: false,
    dedupeKey: `${source}:${issue.code}:${issue.lineIndex}:${issue.severity}`,
  };
}

function webAppErrorFromSaleWarning(message: string, source: string): WebAppError {
  const code = message.startsWith('sales.') ? message : 'sales.preview_accepted';
  return {
    id: `${source}:warning:${code}`,
    origin: 'backend',
    category: 'validation',
    severity: 'warn',
    surface: 'section',
    placement: 'top',
    title: 'Vente à vérifier',
    message,
    code,
    source,
    target: 'admin.sellerTerminal.pos.sale',
    retryable: false,
    dedupeKey: `${source}:warning:${code}`,
  };
}

function saleIssueSeverity(severity: string): WebAppError['severity'] {
  if (severity === 'ERROR') return 'error';
  if (severity === 'INFO') return 'info';
  return 'warn';
}

function withHeaders(
  options: TchRequestOptions | undefined,
  headers: Record<string, string>,
): TchRequestOptions {
  if (options?.headers instanceof HttpHeaders) {
    return {
      ...options,
      headers: Object.entries(headers).reduce(
        (acc, [key, value]) => acc.set(key, value),
        options.headers,
      ),
    };
  }

  return {
    ...(options ?? {}),
    headers: {
      ...(options?.headers ?? {}),
      ...headers,
    },
  };
}
