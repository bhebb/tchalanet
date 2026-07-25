import { Injectable, inject } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import {
  ApiNotice,
  ApiResponse,
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
  PreparedTicketSaleView,
  PosGameBetTypeView,
  PosGameView,
  PosOpenDrawView,
  PosSaleActionAvailabilityView,
  PosSellerTerminalListParams,
  PosSellerTerminalPickerView,
  PosSellerTerminalView,
  PosTerminalActivityView,
  PosTicketDetailsView,
  PosTicketVerificationView,
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
  selectionPolicy?: string | null;
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

interface PrepareTicketSaleRequest {
  drawId: string;
  drawChannelId?: string | null;
  currency: { value: string };
  lines: PrepareTicketSaleLineRequest[];
  serviceOptions?: null;
}

interface PrepareTicketSaleLineRequest {
  lineNumber: number;
  gameCode: string;
  betType: string;
  selection: string;
  betOption: number | null;
  stakeAmount: number;
}

interface PreparedSaleApiResponse {
  preparationId: string;
  status: string;
  currency: string;
  totalAmount: number | string;
  lines?: PreparedSaleLineApiResponse[] | null;
  promotionLines?: PreparedSalePromotionLineApiResponse[] | null;
  notices?: unknown[] | null;
}

interface PreparedSaleLineApiResponse {
  origin?: string | null;
}

interface PreparedSalePromotionLineApiResponse {
  lineRef: string;
}

interface ConfirmPreparedSaleApiResponse {
  preparationId: string;
  ticketId: string | { value?: string | null };
  alreadyConfirmed: boolean;
  sale?: PreparedSellTicketApiResponse | null;
}

interface PreparedSellTicketApiResponse {
  ticket?: {
    ticketId?: string | { value?: string | null } | null;
    ticketCode?: string | null;
    publicCode?: string | null;
    displayCode?: string | null;
    saleStatus?: string | null;
  } | null;
  outcome: 'ACCEPTED' | 'REJECTED' | 'PENDING_APPROVAL';
  notices?: unknown[] | null;
  issues?: PosSaleIssueApiResponse[] | null;
  backup?: {
    displayCode?: string | null;
    verificationShortUrl?: string | null;
    shareableText?: string | null;
  } | null;
  actionAvailability?: PosSaleActionAvailabilityResponse | null;
  sellerInstruction?: string | null;
}

interface PosSaleActionAvailabilityResponse {
  canSell?: boolean;
  canPrint?: boolean;
  canSendSms?: boolean;
  canSendWhatsapp?: boolean;
  canSendEmail?: boolean;
  canCopy?: boolean;
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
  sellerTerminalId?: string | null;
  printOptionsRequest: {
    outputFormat: 'PDF';
    paperSize: 'RECEIPT_80MM';
  };
  recordPrint: boolean;
  reprintReason?: string | null;
  deliveryOptions: readonly ['RETURN_FILE'];
}

interface SendTicketReceiptRequest {
  sellerTerminalId: string;
  channel: 'SMS' | 'WHATSAPP' | 'EMAIL';
  to: string;
  channelKey?: string | null;
  locale?: string | null;
}

interface SendTicketReceiptResponse {
  ticketId: string | { value?: string | null };
  channel: string;
  recipient: string;
  accepted: boolean;
  duplicate: boolean;
}

interface TenantCommunicationConfigResponse {
  buyerTicketDelivery?: {
    sms?: TenantDeliveryChannelResponse | null;
    whatsapp?: TenantDeliveryChannelResponse | null;
    email?: TenantDeliveryChannelResponse | null;
  } | null;
}

interface TenantDeliveryChannelResponse {
  enabled?: boolean | null;
}

interface PosTicketDetailsApiResponse extends Omit<PosTicketDetailsView, 'id' | 'drawId'> {
  id: string | { value?: string | null };
  drawId: string | { value?: string | null };
}

interface PosVerifyTicketRequest {
  scannedValue: string;
}

interface PrintTicketOptions {
  recordPrint?: boolean;
  reprintReason?: string | null;
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
    sellerTerminalId: string,
    lookaheadHours = 24,
    options?: TchRequestOptions,
  ): Observable<PosOpenDrawView[]> {
    return this.backend
      .get<PosAvailableDrawResponse[]>('/tenant/cashier/draws/available', {
        ...withHeaders(options, posContextHeaders(sellerTerminalId)),
        params: { lookaheadHours: String(lookaheadHours) },
      })
      .pipe(
        map(draws =>
          draws.map(d => ({
            drawId: d.drawId,
            drawChannelId: d.drawChannelId,
            drawDate: d.drawDate,
            resultSlotKey: d.resultSlotKey,
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

  prepareTicketSale(
    request: ConfirmTicketSaleRequest,
    sellerTerminalId: string,
    options?: TchRequestOptions,
  ): Observable<PreparedTicketSaleView> {
    const body = prepareTicketSaleRequest(request);

    return this.backend
      .postApiResponse<PreparedSaleApiResponse>('/tenant/sales/preparations', body, {
        ...withHeaders(options, posContextHeaders(sellerTerminalId)),
      })
      .pipe(
        map(response => {
          const r = response.data;
          const freeLineCount =
            r.promotionLines?.length ??
            r.lines?.filter(line => line.origin === 'PROMOTION').length ??
            0;

          return {
            preparationId: r.preparationId,
            status: r.status,
            totalAmount: Number(r.totalAmount ?? 0),
            currency: r.currency,
            freeLineCount,
            notices: response.notices.map(notice =>
              webAppErrorFromPosNotice(
                notice,
                response.trace,
                'admin.sellerTerminal.pos.preparation',
              ),
            ),
            canSell: r.status === 'DRAFT',
            actionAvailability: actionAvailability(null, false),
          };
        }),
      );
  }

  confirmPreparedTicketSale(
    preparationId: string,
    idempotencyKey: string,
    sellerTerminalId: string,
    options?: TchRequestOptions,
  ): Observable<ConfirmedTicketView> {
    return this.backend
      .postApiResponse<ConfirmPreparedSaleApiResponse>(
        `/tenant/sales/preparations/${preparationId}/confirm`,
        {},
        {
          ...withHeaders(options, {
            'Idempotency-Key': idempotencyKey,
            ...posContextHeaders(sellerTerminalId),
          }),
        },
      )
      .pipe(
        map(response => {
          const r = response.data;
          const sale = r.sale;
          const ticket = sale?.ticket;

          return {
            outcome: sale?.outcome ?? 'ACCEPTED',
            ticketId: idValue(ticket?.ticketId ?? r.ticketId),
            ticketCode: ticket?.ticketCode ?? '',
            publicCode: ticket?.publicCode ?? null,
            saleStatus: ticket?.saleStatus ?? null,
            backup:
              sale?.backup ?? (ticket?.displayCode ? { displayCode: ticket.displayCode } : null),
            actionAvailability: actionAvailability(sale?.actionAvailability, true),
            warnings: [
              ...response.notices.map(notice =>
                webAppErrorFromPosNotice(
                  notice,
                  response.trace,
                  'admin.sellerTerminal.pos.confirmPreparation',
                ),
              ),
              ...(sale?.issues ?? []).map(issue =>
                webAppErrorFromSaleIssue(issue, 'admin.sellerTerminal.pos.confirmPreparation'),
              ),
            ],
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
        ...withHeaders(options, posContextHeaders(sellerTerminalId)),
      })
      .pipe(
        map(r => ({
          ticketCount: r.ticketCount,
          salesTotalCents: r.salesTotalCents,
          currency: 'HTG' as const,
        })),
      );
  }

  getTicketDetails(
    ticketId: string,
    options?: TchRequestOptions,
  ): Observable<PosTicketDetailsView> {
    return this.backend
      .get<PosTicketDetailsApiResponse>(`/tenant/cashier/tickets/${ticketId}`, options)
      .pipe(
        map(r => ({
          ...r,
          id: idValue(r.id),
          drawId: idValue(r.drawId),
        })),
      );
  }

  verifyTicket(
    scannedValue: string,
    sellerTerminalId: string,
    options?: TchRequestOptions,
  ): Observable<PosTicketVerificationView> {
    const request: PosVerifyTicketRequest = { scannedValue };
    return this.backend.post<PosTicketVerificationView>('/tenant/cashier/tickets/verify', request, {
      ...withHeaders(options, posContextHeaders(sellerTerminalId)),
    });
  }

  printTicket(
    ticketId: string,
    sellerTerminalId: string,
    options: PrintTicketOptions = {},
  ): Observable<Blob> {
    const request: PrintTicketRequest = {
      sellerTerminalId,
      printOptionsRequest: {
        outputFormat: 'PDF',
        paperSize: 'RECEIPT_80MM',
      },
      recordPrint: options.recordPrint ?? true,
      reprintReason: options.reprintReason ?? 'POS print',
      deliveryOptions: ['RETURN_FILE'],
    };

    return this.backend.postBlob(`/tenant/cashier/tickets/${ticketId}/print`, request, {
      headers: posContextHeaders(sellerTerminalId),
    });
  }

  sendTicketReceipt(
    ticketId: string,
    sellerTerminalId: string,
    channel: SendTicketReceiptRequest['channel'],
    to: string,
  ): Observable<SendTicketReceiptResponse> {
    const request: SendTicketReceiptRequest = {
      sellerTerminalId,
      channel,
      to,
      locale: 'fr',
    };

    return this.backend.post<SendTicketReceiptResponse>(
      `/tenant/cashier/tickets/${ticketId}/send`,
      request,
      { headers: posContextHeaders(sellerTerminalId) },
    );
  }

  getTicketCommunicationAvailability(
    options?: TchRequestOptions,
  ): Observable<PosSaleActionAvailabilityView> {
    return this.backend
      .get<TenantCommunicationConfigResponse>('/admin/tenant-config/communication', options)
      .pipe(
        map(config => {
          const delivery = config?.buyerTicketDelivery;
          return {
            canSell: false,
            canPrint: true,
            canSendSms: delivery?.sms?.enabled ?? false,
            canSendWhatsapp: delivery?.whatsapp?.enabled ?? false,
            canSendEmail: delivery?.email?.enabled ?? false,
            canCopy: true,
          };
        }),
      );
  }
}

export function webAppErrorFromPosNotice(
  notice: ApiNotice,
  trace: ApiResponse<unknown>['trace'],
  source: string,
): WebAppError {
  return webAppErrorFromNotice(notice, trace, source, 'section');
}

function posContextHeaders(sellerTerminalId: string): Record<string, string> {
  return {
    'X-Tch-Act-As-Terminal': sellerTerminalId,
  };
}

function prepareTicketSaleRequest(request: ConfirmTicketSaleRequest): PrepareTicketSaleRequest {
  return {
    drawId: request.drawId,
    drawChannelId: request.drawChannelId ?? null,
    currency: { value: request.currency },
    lines: request.lines.map((line, index) => ({
      lineNumber: index + 1,
      gameCode: line.gameCode,
      betType: line.betType,
      selection: line.selection,
      betOption: line.betOption ?? null,
      stakeAmount: line.stake,
    })),
    serviceOptions: null,
  };
}

function actionAvailability(
  source: PosSaleActionAvailabilityResponse | null | undefined,
  afterSale: boolean,
): PosSaleActionAvailabilityView {
  return {
    canSell: source?.canSell ?? !afterSale,
    canPrint: source?.canPrint ?? afterSale,
    canSendSms: source?.canSendSms ?? false,
    canSendWhatsapp: source?.canSendWhatsapp ?? false,
    canSendEmail: source?.canSendEmail ?? false,
    canCopy: source?.canCopy ?? afterSale,
  };
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
      selectionPolicy: row.selectionPolicy ?? 'EXPLICIT_ONLY',
      options: posBetOptions(row),
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
      selectionPolicy: row.selectionPolicy ?? 'EXPLICIT_ONLY',
      options: posBetOptions(row),
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

  return row.betTypeLabel && row.betTypeLabel !== row.betType ? row.betTypeLabel : row.betType;
}

function posBetOptions(row: PosGameOptionResponse): PosGameBetTypeView['options'] {
  return (row.options ?? []).map(option => ({
    code: option.code,
    label: posBetOptionLabel(option.label),
    selectionHint: option.selectionHint ?? row.selectionHint ?? null,
  }));
}

function posBetOptionLabel(label: string): string {
  const clean = label.trim();
  const labels: Record<string, string> = {
    STRAIGHT: 'Ordre exact',
    BOX: 'N’importe quel ordre',
    BOX_3_WAY: 'N’importe quel ordre',
    BOX_6_WAY: 'N’importe quel ordre',
    BOX_12_WAY: 'N’importe quel ordre',
    BOX_24_WAY: 'N’importe quel ordre',
    STRAIGHT_BOX: 'Ordre exact ou box',
    STRAIGHT_BOX_STRAIGHT_MATCH: 'Ordre exact + box',
    STRAIGHT_BOX_BOX_MATCH: 'Box seulement',
    FRONT_PAIR: 'Première paire',
    BACK_PAIR: 'Dernière paire',
  };

  if (labels[clean]) return labels[clean];

  return clean
    .toLowerCase()
    .split(/[_\s-]+/)
    .filter(Boolean)
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function idValue(value: string | { value?: string | null } | null | undefined): string {
  if (typeof value === 'string') return value;
  return value?.value ?? '';
}

export function webAppErrorFromSaleIssue(
  issue: PosSaleIssueApiResponse,
  source: string,
): WebAppError {
  const severity = saleIssueSeverity(issue.severity);

  return {
    id: `${source}:${issue.code}:${issue.lineIndex}:${issue.severity}`,
    origin: 'backend',
    category: 'validation',
    severity,
    surface: 'section',
    placement: 'top',
    // SaleIssue.message and sellerInstruction are diagnostic/legacy server fields. The rendered
    // copy is resolved from the stable code and the active locale at the POS owner.
    title: '',
    message: '',
    code: issue.code,
    source,
    target: 'admin.sellerTerminal.pos.sale',
    field: issue.lineIndex >= 0 ? `lines.${issue.lineIndex}` : undefined,
    retryable: false,
    dedupeKey: `${source}:${issue.code}:${issue.lineIndex}:${issue.severity}`,
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
