import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AuthSessionService } from '../auth/auth-session.service';
import {
  PrivateNotificationItem,
  PrivateNotificationScope,
  PrivateNotificationsApi,
} from './private-notifications-api.service';

@Injectable({ providedIn: 'root' })
export class PrivateNotificationsStore {
  private readonly api = inject(PrivateNotificationsApi);
  private readonly auth = inject(AuthSessionService);
  private readonly router = inject(Router);

  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly itemsSignal = signal<PrivateNotificationItem[]>([]);
  private readonly unreadCountSignal = signal(0);

  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly items = this.itemsSignal.asReadonly();
  readonly unreadCount = this.unreadCountSignal.asReadonly();
  readonly hasUnread = computed(() => this.unreadCountSignal() > 0);
  readonly scope = computed<PrivateNotificationScope>(() =>
    this.auth.session().roles.includes('SUPER_ADMIN') ? 'platform' : 'admin',
  );
  readonly centerRoute = computed(() =>
    this.scope() === 'platform' ? '/app/platform/notifications' : '/app/admin/notifications',
  );

  loadLatest(): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    const scope = this.scope();
    forkJoin({
      latest: this.api.listLatest(scope, 5),
      count: this.api.unreadCount(scope),
    }).subscribe({
      next: ({ latest, count }) => {
        this.itemsSignal.set(latest.items ?? latest.content ?? []);
        this.unreadCountSignal.set(count.unreadCount ?? 0);
        this.loadingSignal.set(false);
      },
      error: () => {
        this.itemsSignal.set([]);
        this.unreadCountSignal.set(0);
        this.errorSignal.set('notifications.load_failed');
        this.loadingSignal.set(false);
      },
    });
  }

  markRead(item: PrivateNotificationItem): void {
    const id = notificationId(item);
    this.api.markRead(this.scope(), id).subscribe({
      next: () => this.afterItemRead(id),
      error: () => this.errorSignal.set('notifications.action_failed'),
    });
  }

  dismiss(item: PrivateNotificationItem): void {
    const id = notificationId(item);
    this.api.dismiss(this.scope(), id).subscribe({
      next: () => this.afterItemDismissed(id),
      error: () => this.errorSignal.set('notifications.action_failed'),
    });
  }

  markAllRead(): void {
    this.api.markAllRead(this.scope()).subscribe({
      next: () => {
        this.itemsSignal.update(items => items.map(item => ({ ...item, readAt: new Date().toISOString() })));
        this.unreadCountSignal.set(0);
      },
      error: () => this.errorSignal.set('notifications.action_failed'),
    });
  }

  open(item: PrivateNotificationItem): void {
    const id = notificationId(item);
    const route = item.action?.url || this.centerRoute();
    this.api.markRead(this.scope(), id).subscribe({
      next: () => {
        this.afterItemRead(id);
        void this.router.navigateByUrl(route);
      },
      error: () => void this.router.navigateByUrl(route),
    });
  }

  private afterItemRead(id: string): void {
    const before = this.itemsSignal().find(item => notificationId(item) === id);
    this.itemsSignal.update(items =>
      items.map(item => (notificationId(item) === id ? { ...item, readAt: item.readAt ?? new Date().toISOString() } : item)),
    );
    if (before && !before.readAt && !before.archivedAt) {
      this.unreadCountSignal.update(count => Math.max(0, count - 1));
    }
  }

  private afterItemDismissed(id: string): void {
    const before = this.itemsSignal().find(item => notificationId(item) === id);
    this.itemsSignal.update(items => items.filter(item => notificationId(item) !== id));
    if (before && !before.readAt && !before.archivedAt) {
      this.unreadCountSignal.update(count => Math.max(0, count - 1));
    }
  }
}

export function notificationId(item: PrivateNotificationItem): string {
  return typeof item.id === 'string' ? item.id : item.id.value;
}
