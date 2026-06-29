import { Injectable, inject } from '@angular/core';
import { HttpHeaders } from '@angular/common/http';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import {
  ConfirmTicketSaleRequest,
  ConfirmedTicketView,
  PosGameView,
  PosOpenDrawView,
  PosSellerTerminalView,
  PosTerminalActivityView,
} from './admin-seller-terminal-pos.models';

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

interface PosSellTicketApiResponse {
  outcome: 'ACCEPTED' | 'REJECTED' | 'PENDING_APPROVAL';
  ticketId: string;
  ticketCode: string;
  publicCode?: string | null;
  saleStatus?: string | null;
  backup?: {
    displayCode?: string | null;
    verificationShortUrl?: string | null;
    shareableText?: string | null;
  } | null;
  sellerInstruction?: string | null;
}

interface SellerTerminalStatsResponse {
  ticketCount: number;
  salesTotalCents: number;
  currency: string;
}

interface PrintTicketRequest {
  sellerTerminalId: string;
  printOptionsRequest: {
    outputFormat: 'PDF';
    paperSize: 'RECEIPT_80MM';
  };
  recordPrint: boolean;
  deliveryOptions: readonly ['RETURN_FILE'];
}

// ── Service ────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AdminSellerTerminalPosApiService {
  private readonly backend = inject(TchBackendClient);

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
      .pipe(
        map(games =>
          games.map(g => ({
            gameCode: g.gameCode,
            label: g.gameLabel,
            enabled: true,
            betType: g.betType,
            betTypeLabel: g.betTypeLabel,
            requiresOption: g.requiresOption,
            options: g.options ?? [],
            selectionHint: g.selectionHint ?? null,
          })),
        ),
      );
  }

  confirmTicketSale(
    request: ConfirmTicketSaleRequest,
    idempotencyKey: string,
    sellerTerminalId: string,
    options?: TchRequestOptions,
  ): Observable<ConfirmedTicketView> {
    return this.backend
      .post<PosSellTicketApiResponse>('/tenant/cashier/tickets/sell', request, {
        ...withHeaders(options, {
          'Idempotency-Key': idempotencyKey,
          'X-Tch-Act-As-Terminal': sellerTerminalId,
        }),
      })
      .pipe(
        map(r => ({
          outcome: r.outcome,
          ticketId: r.ticketId,
          ticketCode: r.ticketCode,
          publicCode: r.publicCode ?? null,
          saleStatus: r.saleStatus ?? null,
          backup: r.backup ?? null,
          sellerInstruction: r.sellerInstruction ?? null,
        })),
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
      sellerTerminalId,
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
