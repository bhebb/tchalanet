import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { EMPTY, Observable } from 'rxjs';

import { ResultStatus } from '@tch/page-model';
import {
  PublicDrawResultDetail,
  PublicDrawResultsService,
} from '../results/public-draw-results.service';
import type { PublicResultDetail } from './public-result-detail.model';
import { publicResultFallback, resultStatusView } from './public-result-detail.utils';

@Component({
  selector: 'tch-public-result-detail-page',
  imports: [RouterLink, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-result-detail.page.html',
  styleUrls: ['./public-result-detail.page.scss'],
})
export class PublicResultDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly svc = inject(PublicDrawResultsService);

  private readonly drawResultId = this.route.snapshot.paramMap.get('drawResultId') ?? null;

  readonly resource = rxResource({
    params: () => this.drawResultId,
    stream: ({ params: id }): Observable<PublicDrawResultDetail> =>
      id ? this.svc.detail(id) : EMPTY,
  });

  /** Resolved data: live from server if available, otherwise fallback mock */
  readonly result = computed(() => {
    const live = this.resource.value();
    if (live) {
      return {
        drawResultId: live.drawResultId,
        slotKey: live.slotKey,
        drawChannelLabelKey: live.drawChannelLabelKey,
        drawChannelLabel: live.drawChannelLabel,
        resultDate: live.resultDate,
        drawTime: live.drawTime,
        timezone: live.timezone,
        status: live.status as ResultStatus,
        numbers: live.numbers,
        sourceLabel: live.sourceLabel ?? '',
        publishedAt: live.publishedAt,
        related: [],
      } satisfies PublicResultDetail;
    }
    return publicResultFallback(this.drawResultId);
  });

  readonly isLoading = computed(() => this.resource.isLoading());
  readonly hasError = computed(() => !!this.resource.error() && !this.resource.value());

  /** Channel label: uses drawChannelLabelKey translated, fallback to drawChannelLabel. */
  readonly channelLabel = computed(() => {
    const r = this.result();
    return r.drawChannelLabelKey || r.drawChannelLabel;
  });

  readonly statusView = computed(() => resultStatusView(this.result().status));

  statusLabel(status: ResultStatus): string {
    return `public.results.status.${status}`;
  }

  /** Strips seconds from a time string: "14:30:00" → "14:30". */
  hhmm(time: string | undefined): string {
    if (!time) return '';
    return time.length > 5 ? time.substring(0, 5) : time;
  }

  /**
   * Formats an ISO timestamp for public display.
   * "2026-06-09T02:55:02.192926Z" → "2026-06-09 · 02:55 UTC"
   */
  fmtIso(iso: string | null | undefined): string {
    if (!iso) return '';
    const ms = Date.parse(iso);
    if (isNaN(ms)) return iso;
    const d = new Date(ms);
    const date = d.toISOString().substring(0, 10); // "2026-06-09"
    const time = d.toISOString().substring(11, 16); // "02:55"
    return `${date} · ${time} UTC`;
  }

  /**
   * Truncates a UUID for mobile display.
   * "71f2ff4a-e52d-44f5-93f6-943db0e94ed9" → "71f2ff4a…"
   */
  shortId(id: string): string {
    return id.length > 12 ? `${id.substring(0, 8)}…` : id;
  }

  readonly copiedRef = signal(false);

  async copyRef(id: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(id);
      this.copiedRef.set(true);
      setTimeout(() => this.copiedRef.set(false), 2000);
    } catch {
      // Clipboard API unavailable (non-secure context) — silently ignore
    }
  }
}

