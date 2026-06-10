import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { GameLabelPipe } from '@tch/page-model';
import { TchActionButton, TchCard, TchLoading } from '@tch/ui/components';

import { CODE_PATTERN, STAMP_LINES } from './public-check-ticket.data';
import type { CheckState } from './public-check-ticket.model';
import { formatPublicCode, verificationCopy } from './public-check-ticket.utils';
import {
  PublicTicketVerificationApi,
  VerifyTicketDraw,
  VerifyTicketLine,
  extractPublicCodeFromQr,
  normalizePublicCode,
} from './public-ticket.service';

@Component({
  selector: 'tch-public-check-ticket-page',
  imports: [DecimalPipe, GameLabelPipe, TranslatePipe, TchCard, TchActionButton, TchLoading],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-check-ticket.page.html',
  styleUrls: ['./public-check-ticket.page.scss'],
})
export class PublicCheckTicketPage {
  private readonly route = inject(ActivatedRoute);
  private readonly svc = inject(PublicTicketVerificationApi);
  private readonly destroyRef = inject(DestroyRef);

  readonly code = signal('');
  readonly state = signal<CheckState>({ kind: 'default' });

  constructor() {
    const rawCode = this.route.snapshot.queryParamMap.get('code');
    if (rawCode) {
      // Support both plain code and full URL (from QR scanner)
      const normalized = extractPublicCodeFromQr(rawCode);
      this.code.set(formatPublicCode(normalized));
      // Auto-trigger verification when code is pre-filled via query param
      this.doVerify(normalized);
    }
  }

  readonly resultCopy = computed(() => {
    const s = this.state();
    return s.kind === 'result' ? verificationCopy(s.status) : verificationCopy('SERVICE_UNAVAILABLE');
  });

  readonly stampLines = computed(() => {
    const s = this.state();
    return s.kind === 'result' ? STAMP_LINES[s.status] : [];
  });

  /** The raw API data from the last successful verify call. */
  readonly resultData = computed<PublicTicketVerificationResponse | null>(() => {
    const s = this.state();
    return s.kind === 'result' ? s.data : null;
  });

  readonly resultDraw = computed<VerifyTicketDraw | null>(
    () => this.resultData()?.draw ?? null,
  );

  readonly resultLines = computed<readonly VerifyTicketLine[]>(
    () => this.resultData()?.lines ?? [],
  );

  readonly receiptDate = computed(() => {
    const placedAt = this.resultData()?.placedAt;
    const d = placedAt ? new Date(placedAt) : new Date();
    const m = ['JAN','FÉV','MAR','AVR','MAI','JUN','JUL','AOU','SEP','OCT','NOV','DÉC'];
    return `${String(d.getDate()).padStart(2, '0')} ${m[d.getMonth()]} ${d.getFullYear()}`;
  });

  updateCode(event: Event): void {
    const input = event.target as HTMLInputElement;
    const formatted = formatPublicCode(input.value);
    this.code.set(formatted);
    input.value = formatted;
  }

  submit(event: Event): void {
    event.preventDefault();
    const normalized = normalizePublicCode(this.code());
    if (!CODE_PATTERN.test(this.code())) {
      this.state.set({ kind: 'result', status: 'NOT_FOUND', data: null });
      return;
    }
    this.doVerify(normalized);
  }

  /** Internal: call the API and update state. Called from constructor (auto-submit) or submit(). */
  private doVerify(normalizedCode: string): void {
    this.state.set({ kind: 'loading' });
    this.svc
      .verify(normalizedCode)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ status, data }) =>
        this.state.set({ kind: 'result', status, data }),
      );
  }

  reset(): void {
    this.code.set('');
    this.state.set({ kind: 'default' });
  }
}

