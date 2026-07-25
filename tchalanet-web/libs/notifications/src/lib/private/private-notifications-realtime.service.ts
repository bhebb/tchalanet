import { isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';

import { TchBackendClient, createRequestId } from '@tch/api';
import { AUTH_CLIENT, SupportAccessStore } from '@tch/core/auth';

import { PrivateNotificationScope } from './private-notifications-api.service';

type NotificationChangeHandler = () => void;

const RETRY_DELAYS_MS = [5_000, 15_000, 30_000, 60_000] as const;

@Injectable({ providedIn: 'root' })
export class PrivateNotificationsRealtimeService {
  private readonly backend = inject(TchBackendClient);
  private readonly auth = inject(AUTH_CLIENT);
  private readonly supportAccess = inject(SupportAccessStore);
  private readonly platformId = inject(PLATFORM_ID);
  private active: ActiveStream | null = null;

  connect(scope: PrivateNotificationScope, onChange: NotificationChangeHandler): void {
    this.disconnect();
    if (!isPlatformBrowser(this.platformId)) return;

    const stream: ActiveStream = {
      scope,
      onChange,
      controller: new AbortController(),
      retryTimer: null,
      retryAttempt: 0,
    };
    this.active = stream;
    void this.open(stream);
  }

  disconnect(): void {
    const stream = this.active;
    this.active = null;
    if (!stream) return;
    stream.controller.abort();
    if (stream.retryTimer !== null) globalThis.clearTimeout(stream.retryTimer);
  }

  private async open(stream: ActiveStream): Promise<void> {
    try {
      const token = await this.auth.getAccessToken();
      if (!token || this.active !== stream) return;

      const response = await fetch(this.backend.resolveUrl(`${this.basePath(stream.scope)}/stream`), {
        headers: this.headers(token, stream.scope),
        signal: stream.controller.signal,
      });
      if (!response.ok || !response.body) throw new Error(`notification stream failed: ${response.status}`);

      await this.consume(response.body, stream);
    } catch {
      // The stream is opportunistic: REST remains available and reconnect handles transient faults.
    } finally {
      if (this.active === stream && !stream.controller.signal.aborted) {
        stream.retryTimer = globalThis.setTimeout(
          () => void this.open(stream),
          retryDelayMs(stream.retryAttempt++),
        );
      }
    }
  }

  private headers(token: string, scope: PrivateNotificationScope): Headers {
    const headers = new Headers({
      Authorization: `Bearer ${token}`,
      Accept: 'text/event-stream',
      'X-Request-Id': createRequestId(),
    });
    const support = scope === 'admin' ? this.supportAccess.session() : null;
    if (support) {
      headers.set('X-Tch-Tenant-Override', support.tenantId);
      headers.set('X-Tch-Act-As', 'TENANT_ADMIN');
      headers.set('X-Tch-Override-Reason', `SUPER_ADMIN support session (${support.mode})`);
    }
    return headers;
  }

  private async consume(body: ReadableStream<Uint8Array>, stream: ActiveStream): Promise<void> {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    try {
      while (this.active === stream && !stream.controller.signal.aborted) {
        const next = await reader.read();
        if (next.done) return;
        buffer += decoder.decode(next.value, { stream: true });
        const frames = buffer.split(/\r?\n\r?\n/);
        buffer = frames.pop() ?? '';
        for (const frame of frames) {
          if (frame.split(/\r?\n/).some(line => line === 'event: notification-change')) {
            stream.retryAttempt = 0;
            stream.onChange();
          }
        }
      }
    } finally {
      reader.releaseLock();
    }
  }

  private basePath(scope: PrivateNotificationScope): string {
    return scope === 'platform' ? '/platform/notifications' : '/admin/notifications';
  }
}

interface ActiveStream {
  readonly scope: PrivateNotificationScope;
  readonly onChange: NotificationChangeHandler;
  readonly controller: AbortController;
  retryTimer: ReturnType<typeof globalThis.setTimeout> | null;
  retryAttempt: number;
}

function retryDelayMs(attempt: number): number {
  return RETRY_DELAYS_MS[Math.min(Math.max(attempt, 0), RETRY_DELAYS_MS.length - 1)];
}
