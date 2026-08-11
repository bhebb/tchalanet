import { DecimalPipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { GameLabelPipe } from '@tch/page-model';
import { TCH_PUBLIC_ASSETS } from '@tch/shared-assets';
import { TchActionButton, TchCard, TchLoading } from '@tch/ui/components';
import { consoleTicketDrawIdentity } from '@tch/web/console';

import { CODE_PATTERN, STAMP_LINES } from './public-check-ticket.data';
import type { CheckState } from './public-check-ticket.model';
import { formatPublicCode, verificationCopy } from './public-check-ticket.utils';
import {
  PublicTicketVerificationApi,
  PublicTicketVerificationResponse,
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
  readonly scanBusy = signal(false);
  readonly scanErrorKey = signal<string | null>(null);
  readonly state = signal<CheckState>({ kind: 'default' });
  readonly previewImage = TCH_PUBLIC_ASSETS.checkTicketPreview;

  constructor() {
    const rawCode =
      this.route.snapshot.paramMap.get('publicCode') ??
      this.route.snapshot.queryParamMap.get('code');
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
    return s.kind === 'result'
      ? verificationCopy(s.status)
      : verificationCopy('SERVICE_UNAVAILABLE');
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

  readonly resultDraw = computed<VerifyTicketDraw | null>(() => this.resultData()?.draw ?? null);

  readonly resultDrawLabel = computed(() => {
    const draw = this.resultDraw();
    if (!draw) {
      return null;
    }
    return consoleTicketDrawIdentity({
      channelCode: draw.channelName,
      channelLabel: draw.channelLabel,
      resultSlotKey: draw.resultSlotKey,
      drawDateLabel: draw.drawDate,
      scheduledAt: draw.scheduledAt,
      fallbackLabel: draw.channelLabel,
    }).receiptLabel;
  });

  readonly resultLines = computed<readonly VerifyTicketLine[]>(
    () => this.resultData()?.lines ?? [],
  );

  readonly receiptDate = computed(() => {
    const placedAt = this.resultData()?.placedAt;
    const d = placedAt ? new Date(placedAt) : new Date();
    const m = ['JAN', 'FÉV', 'MAR', 'AVR', 'MAI', 'JUN', 'JUL', 'AOU', 'SEP', 'OCT', 'NOV', 'DÉC'];
    return `${String(d.getDate()).padStart(2, '0')} ${m[d.getMonth()]} ${d.getFullYear()}`;
  });

  updateCode(event: Event): void {
    const input = event.target as HTMLInputElement;
    const formatted = formatPublicCode(input.value);
    this.scanErrorKey.set(null);
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

  openQrScanner(input: HTMLInputElement): void {
    this.scanErrorKey.set(null);
    if (!nativeBarcodeDetector()) {
      this.scanErrorKey.set('public.check.scan_unsupported');
      return;
    }
    input.value = '';
    input.click();
  }

  async scanQrImage(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0);
    if (!file) {
      return;
    }

    const BarcodeDetector = nativeBarcodeDetector();
    if (!BarcodeDetector) {
      this.scanErrorKey.set('public.check.scan_unsupported');
      return;
    }

    this.scanBusy.set(true);
    this.scanErrorKey.set(null);
    try {
      const detector = new BarcodeDetector({ formats: ['qr_code'] });
      const bitmap = await createImageBitmap(file);
      try {
        const barcodes = await detector.detect(bitmap);
        const rawValue = barcodes.find(barcode => barcode.rawValue)?.rawValue;
        if (!rawValue) {
          this.scanErrorKey.set('public.check.scan_no_code');
          return;
        }

        const normalized = extractPublicCodeFromQr(rawValue);
        const formatted = formatPublicCode(normalized);
        this.code.set(formatted);
        if (!CODE_PATTERN.test(formatted)) {
          this.scanErrorKey.set('public.check.scan_invalid');
          return;
        }
        this.doVerify(normalized);
      } finally {
        bitmap.close();
      }
    } catch {
      this.scanErrorKey.set('public.check.scan_failed');
    } finally {
      this.scanBusy.set(false);
      input.value = '';
    }
  }

  /** Internal: call the API and update state. Called from constructor (auto-submit) or submit(). */
  private doVerify(normalizedCode: string): void {
    this.state.set({ kind: 'loading' });
    this.svc
      .verify(normalizedCode)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ status, data }) => this.state.set({ kind: 'result', status, data }));
  }

  reset(): void {
    this.code.set('');
    this.scanErrorKey.set(null);
    this.state.set({ kind: 'default' });
  }
}

interface NativeBarcode {
  readonly rawValue?: string;
}

interface NativeBarcodeDetector {
  detect(source: ImageBitmapSource): Promise<readonly NativeBarcode[]>;
}

interface NativeBarcodeDetectorConstructor {
  new (options?: { readonly formats?: readonly string[] }): NativeBarcodeDetector;
}

function nativeBarcodeDetector(): NativeBarcodeDetectorConstructor | null {
  return (
    globalThis as typeof globalThis & {
      readonly BarcodeDetector?: NativeBarcodeDetectorConstructor;
    }
  ).BarcodeDetector ?? null;
}
