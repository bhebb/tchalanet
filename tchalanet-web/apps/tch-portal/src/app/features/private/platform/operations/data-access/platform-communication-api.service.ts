import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchRequestOptions } from '@tch/api';
import { Observable } from 'rxjs';
import { TchPage } from './platform-ops-api.service';

export type CommunicationChannel =
  | 'SLACK'
  | 'SLACK_INTERNAL'
  | 'SLACK_TENANT_WEBHOOK'
  | 'EMAIL'
  | 'SMS'
  | 'WHATSAPP'
  | 'PUSH';

export type DeliveryStatus = 'PENDING' | 'DISPATCHING' | 'SENT' | 'FAILED' | 'SKIPPED' | 'CANCELLED';

export interface CommunicationQueueSummary {
  pending: number;
  dispatching: number;
  sent: number;
  failed: number;
  skipped: number;
  cancelled: number;
}

export interface CommunicationAttemptView {
  id: string;
  attemptedAt: string;
  status: DeliveryStatus;
  provider: string;
  providerMessageId: string | null;
  errorCode: string | null;
  errorMessage: string | null;
}

export interface CommunicationMessageView {
  id: string;
  tenantId: string | null;
  channel: CommunicationChannel;
  recipientType: string;
  recipientValue: string;
  templateKey: string;
  locale: string | null;
  subject: string | null;
  priority: string;
  status: DeliveryStatus;
  correlationKey: string | null;
  nextAttemptAt: string | null;
  sentAt: string | null;
  failedAt: string | null;
  failureReason: string | null;
  createdAt: string;
  attempts: CommunicationAttemptView[];
}

export interface CommunicationQueueView {
  summary: CommunicationQueueSummary;
  messages: TchPage<CommunicationMessageView>;
}

export interface CommunicationDispatchResult {
  dispatched: number;
}

export interface CommunicationTestResponse {
  sent: boolean;
  provider: string | null;
  reason: string | null;
  channel: string;
}

@Injectable({ providedIn: 'root' })
export class PlatformCommunicationApi {
  private readonly backend = inject(TchBackendClient);

  listMessages(params: {
    status?: DeliveryStatus | '';
    channel?: CommunicationChannel | '';
    tenantId?: string;
    recipient?: string;
    page?: number;
    size?: number;
  }, options?: TchRequestOptions): Observable<CommunicationQueueView> {
    const q = new URLSearchParams(
      Object.fromEntries(
        Object.entries(params)
          .filter(([, v]) => v !== undefined && v !== '')
          .map(([k, v]) => [k, String(v)]),
      ),
    ).toString();
    return this.backend.get<CommunicationQueueView>(
      `/platform/ops/communication/messages${q ? '?' + q : ''}`,
      options,
    );
  }

  dispatchDue(options?: TchRequestOptions): Observable<CommunicationDispatchResult> {
    return this.backend.post<CommunicationDispatchResult>('/platform/ops/communication/dispatch-due', {}, options);
  }

  testSlack(
    body: { channelKey: string; title: string; message: string },
    options?: TchRequestOptions,
  ): Observable<CommunicationTestResponse> {
    return this.backend.post<CommunicationTestResponse>('/platform/ops/communication/slack-test', body, options);
  }

  testEmail(
    body: { to: string; subject: string; message: string },
    options?: TchRequestOptions,
  ): Observable<CommunicationTestResponse> {
    return this.backend.post<CommunicationTestResponse>('/platform/ops/communication/email-test', body, options);
  }

  testSms(
    body: { to: string; title: string; message: string },
    options?: TchRequestOptions,
  ): Observable<CommunicationTestResponse> {
    return this.backend.post<CommunicationTestResponse>('/platform/ops/communication/sms-test', body, options);
  }

  testWhatsapp(
    body: { to: string; title: string; message: string },
    options?: TchRequestOptions,
  ): Observable<CommunicationTestResponse> {
    return this.backend.post<CommunicationTestResponse>('/platform/ops/communication/whatsapp-test', body, options);
  }
}
