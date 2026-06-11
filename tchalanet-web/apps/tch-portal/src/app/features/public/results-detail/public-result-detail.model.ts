import type { ResultStatus } from '@tch/page-model';

export interface PublicResultDetail {
  readonly drawResultId: string;
  readonly slotKey: string;
  /** Stable i18n key from the server (e.g. "draw_channel.ny.eve.label"). */
  readonly drawChannelLabelKey: string;
  /** Human-readable fallback label from the server. */
  readonly drawChannelLabel: string;
  readonly resultDate: string;
  readonly drawTime: string;
  readonly timezone: string;
  readonly status: ResultStatus;
  readonly numbers: readonly string[];
  readonly sourceLabel: string;
  readonly publishedAt: string | null;
  readonly related: readonly RelatedResult[];
}

export interface RelatedResult {
  readonly drawResultId: string;
  readonly label: string;
  readonly status: ResultStatus;
}

export interface ResultStatusView {
  readonly icon: string;
  readonly titleKey: string;
  readonly bodyKey: string;
}
