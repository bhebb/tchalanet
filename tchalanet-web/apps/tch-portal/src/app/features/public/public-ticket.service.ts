import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { ApiResponse, TCH_API_BASE } from '@tch/api';
import { VerificationStatus } from '@tch/page-model';

// ── Normalisation helpers ─────────────────────────────────────────────────────

/**
 * Normalise a raw public code before sending it to the API:
 * trim, uppercase, remove all whitespace (hyphens are kept — backend accepts them).
 */
export function normalizePublicCode(code: string): string {
  return (code ?? '').trim().toUpperCase().replace(/\s+/g, '');
}

/**
 * Extract the public code from a QR value that may be:
 *   - a full URL: https://tchalanet.com/public/check-ticket?code=QVQE-NRVR
 *   - or the raw code itself: QVQE-NRVR
 */
export function extractPublicCodeFromQr(value: string): string {
  try {
    const url = new URL(value);
    return normalizePublicCode(url.searchParams.get('code') ?? value);
  } catch {
    return normalizePublicCode(value);
  }
}

// ── API shapes ────────────────────────────────────────────────────────────────

export interface MoneyAmount {
  readonly amount: number;
  readonly currency: { readonly value: string };
  readonly zero: boolean;
}

export interface VerifyTicketLine {
  readonly lineNumber: number;
  readonly gameDisplayName: string;
  readonly betTypeLabel: string;
  readonly optionLabel: string;
  readonly selection: string;
  readonly stake: MoneyAmount;
  readonly potentialPayout: MoneyAmount;
  readonly promotional: boolean;
  readonly promotionLabel: string | null;
}

export interface VerifyTicketDraw {
  readonly channelName: string;
  readonly channelLabel: string;
  readonly drawDate: string;        // "2026-06-04"
  readonly scheduledAt: string;     // ISO
}

export interface VerifyTicketOutlet {
  readonly name: string;
}

/**
 * Full response body of POST /public/tickets/verify.
 *
 * `status` uses the backend enum — map to {@link VerificationStatus} via
 * {@link mapBackendStatus} before passing to the UI.
 */
export interface PublicTicketVerificationResponse {
  readonly publicCode: string;
  readonly displayCode: string;
  /** Backend status string — convert with mapBackendStatus() for the UI. */
  readonly status: string;
  readonly totalAmount: MoneyAmount | null;
  readonly winningAmount: MoneyAmount | null;
  readonly placedAt: string;
  readonly outlet: VerifyTicketOutlet | null;
  readonly draw: VerifyTicketDraw | null;
  readonly lines: readonly VerifyTicketLine[];
}

// ── Status mapping ────────────────────────────────────────────────────────────

/**
 * Map the backend `data.status` enum to the frontend {@link VerificationStatus}.
 *
 * Backend values observed / documented:
 *   AWAITING_RESULT  → ticket exists, draw not yet run
 *   WINNING_PAYABLE  → won, not yet collected
 *   WINNING_PAID     → won, already paid out
 *   LOST             → draw run, did not win
 *   CANCELLED        → annulled
 *   EXPIRED          → claim window passed
 *   BLOCKED          → requires manual intervention
 */
export function mapBackendStatus(raw: string | undefined): VerificationStatus {
  if (!raw) return 'SERVICE_UNAVAILABLE';
  switch (raw) {
    case 'AWAITING_RESULT': return 'PENDING_RESULT';
    case 'WINNING_PAYABLE': return 'WINNING_PAYABLE';
    case 'WINNING_PAID':    return 'WINNING_PAID';
    case 'LOST':            return 'LOST';
    case 'CANCELLED':       return 'CANCELLED';
    case 'EXPIRED':         return 'EXPIRED';
    case 'BLOCKED':         return 'BLOCKED';
    case 'NOT_FOUND':       return 'NOT_FOUND';
    default:                return 'SERVICE_UNAVAILABLE';
  }
}

// ── Service ───────────────────────────────────────────────────────────────────

/**
 * Data-access service for the public ticket verification endpoint.
 * Calls POST /public/tickets/verify with { publicCode } only.
 * Never sends ticketId, tenantId, drawId, terminalId or any internal identifier.
 */
@Injectable({ providedIn: 'root' })
export class PublicTicketVerificationApi {
  private readonly http = inject(HttpClient);
  private readonly apiBase = inject(TCH_API_BASE);

  /**
   * Verify a ticket by its public code.
   * Returns the full response so the page can show draw/line/outlet details.
   * Always resolves — HTTP errors are mapped to a minimal error response.
   */
  verify(
    publicCode: string,
  ): Observable<{ status: VerificationStatus; data: PublicTicketVerificationResponse | null }> {
    return this.http
      .post<ApiResponse<PublicTicketVerificationResponse>>(
        `${this.apiBase}/public/tickets/verify`,
        { publicCode: normalizePublicCode(publicCode) },
      )
      .pipe(
        map(res => ({
          status: mapBackendStatus(res.data?.status),
          data: res.data ?? null,
        })),
        catchError((err: HttpErrorResponse) => {
          const status: VerificationStatus =
            err.status === 404 || err.status === 400 ? 'NOT_FOUND' : 'SERVICE_UNAVAILABLE';
          return of({ status, data: null });
        }),
      );
  }
}

/** @deprecated Use PublicTicketVerificationApi. */
export { PublicTicketVerificationApi as PublicTicketService };
