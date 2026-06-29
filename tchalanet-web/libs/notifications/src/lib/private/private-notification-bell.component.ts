import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PrivateNotificationItem } from './private-notifications-api.service';
import { PrivateNotificationsStore } from './private-notifications.store';

@Component({
  selector: 'tch-private-notification-bell',
  standalone: true,
  imports: [DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'close()' },
  template: `
    <div class="notification-bell">
      <button
        type="button"
        class="notification-bell__trigger"
        [class.has-unread]="store.hasUnread()"
        [attr.aria-expanded]="open()"
        aria-controls="private-notification-menu"
        aria-label="Notifications"
        (click)="toggle()"
      >
        <span class="material-symbols-outlined" aria-hidden="true">notifications</span>
        @if (store.unreadCount() > 0) {
          <span class="notification-bell__badge">{{ badgeCount() }}</span>
        }
      </button>

      @if (open()) {
        <section id="private-notification-menu" class="notification-menu" aria-label="Notifications">
          <header class="notification-menu__header">
            <div>
              <h2>Notifications</h2>
              <p>{{ store.unreadCount() }} non lue(s)</p>
            </div>
            <button type="button" class="icon-button" aria-label="Rafraîchir" (click)="refresh($event)">
              <span class="material-symbols-outlined" aria-hidden="true">refresh</span>
            </button>
          </header>

          @if (store.loading()) {
            <div class="notification-menu__state">Chargement...</div>
          } @else if (store.items().length === 0) {
            <div class="notification-menu__state">Aucune notification récente.</div>
          } @else {
            <div class="notification-list">
              @for (item of store.items(); track itemKey(item)) {
                <article class="notification-item" [class.is-unread]="!item.readAt && !item.archivedAt">
                  <button type="button" class="notification-item__body" (click)="openItem(item)">
                    <span class="notification-item__dot" [class]="severityClass(item)" aria-hidden="true"></span>
                    <span class="notification-item__content">
                      <strong>{{ titleOf(item) }}</strong>
                      @if (messageOf(item)) {
                        <span>{{ messageOf(item) }}</span>
                      }
                      <time>{{ item.createdAt | date:'dd/MM HH:mm' }}</time>
                    </span>
                  </button>
                  <button type="button" class="icon-button" aria-label="Fermer" (click)="dismiss($event, item)">
                    <span class="material-symbols-outlined" aria-hidden="true">close</span>
                  </button>
                </article>
              }
            </div>
          }

          <footer class="notification-menu__footer">
            <button type="button" class="text-button" [disabled]="!store.hasUnread()" (click)="markAllRead($event)">
              Tout marquer lu
            </button>
            <a [routerLink]="store.centerRoute()" (click)="close()">Voir tout</a>
          </footer>
        </section>
      }
    </div>
  `,
  styles: [
    `
      .notification-bell {
        position: relative;
      }

      .notification-bell__trigger,
      .icon-button {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 2.5rem;
        height: 2.5rem;
        border: 0;
        border-radius: 9999px;
        background: transparent;
        color: inherit;
        cursor: pointer;
      }

      .notification-bell__trigger:hover,
      .icon-button:hover {
        background: var(--tch-color-surface-container-high);
      }

      .notification-bell__trigger.has-unread {
        color: var(--tch-color-primary, #141545);
      }

      .notification-bell__badge {
        position: absolute;
        top: 0.18rem;
        right: 0.1rem;
        min-width: 1.15rem;
        height: 1.15rem;
        padding: 0 0.25rem;
        border: 2px solid var(--tch-color-surface, #fff);
        border-radius: 9999px;
        background: var(--tch-color-error, #ba1a1a);
        color: var(--tch-color-on-error, #fff);
        font-size: 0.65rem;
        font-weight: 800;
        line-height: 0.95rem;
        text-align: center;
      }

      .notification-menu {
        position: absolute;
        top: calc(100% + 0.6rem);
        right: 0;
        z-index: calc(var(--tch-z-header, 100) + 20);
        width: min(24rem, calc(100vw - 1.5rem));
        overflow: hidden;
        border: 1px solid var(--tch-color-outline-variant, #ddd7e3);
        border-radius: 8px;
        background: var(--tch-color-surface-container-lowest, #fff);
        box-shadow: var(--tch-elevation-3, 0 16px 40px rgba(0, 0, 0, 0.18));
      }

      .notification-menu__header,
      .notification-menu__footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.75rem;
        padding: 0.9rem 1rem;
      }

      .notification-menu__header {
        border-bottom: 1px solid var(--tch-color-outline-variant, #ddd7e3);
      }

      .notification-menu__header h2 {
        margin: 0;
        font-size: 1rem;
      }

      .notification-menu__header p {
        margin: 0.15rem 0 0;
        color: var(--tch-color-on-surface-variant, #5f5a66);
        font-size: 0.82rem;
      }

      .notification-list {
        max-height: min(26rem, 60vh);
        overflow: auto;
      }

      .notification-item {
        display: grid;
        grid-template-columns: minmax(0, 1fr) auto;
        align-items: start;
        gap: 0.3rem;
        border-bottom: 1px solid var(--tch-color-outline-variant, #ddd7e3);
        padding: 0.45rem;
      }

      .notification-item.is-unread {
        background: color-mix(in srgb, var(--tch-color-primary, #141545) 5%, transparent);
      }

      .notification-item__body {
        display: grid;
        grid-template-columns: auto minmax(0, 1fr);
        gap: 0.65rem;
        width: 100%;
        min-width: 0;
        border: 0;
        border-radius: 8px;
        background: transparent;
        color: inherit;
        cursor: pointer;
        padding: 0.55rem;
        text-align: left;
      }

      .notification-item__body:hover {
        background: var(--tch-color-surface-container-high);
      }

      .notification-item__dot {
        width: 0.6rem;
        height: 0.6rem;
        margin-top: 0.3rem;
        border-radius: 9999px;
        background: var(--tch-color-primary, #141545);
      }

      .notification-item__dot.is-warning {
        background: var(--tch-color-warning, #f59e0b);
      }

      .notification-item__dot.is-danger {
        background: var(--tch-color-error, #ba1a1a);
      }

      .notification-item__content {
        display: grid;
        gap: 0.2rem;
        min-width: 0;
      }

      .notification-item__content strong,
      .notification-item__content span {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .notification-item__content span,
      .notification-item__content time,
      .notification-menu__state {
        color: var(--tch-color-on-surface-variant, #5f5a66);
        font-size: 0.82rem;
      }

      .notification-menu__state {
        padding: 1rem;
      }

      .notification-menu__footer {
        border-top: 1px solid var(--tch-color-outline-variant, #ddd7e3);
      }

      .notification-menu__footer a,
      .text-button {
        border: 0;
        background: transparent;
        color: var(--tch-color-primary, #141545);
        cursor: pointer;
        font-weight: 800;
        text-decoration: none;
      }

      .text-button:disabled {
        color: var(--tch-color-on-surface-variant, #5f5a66);
        cursor: default;
        opacity: 0.55;
      }
    `,
  ],
})
export class PrivateNotificationBellComponent {
  protected readonly store = inject(PrivateNotificationsStore);
  protected readonly open = signal(false);
  protected readonly badgeCount = computed(() => {
    const count = this.store.unreadCount();
    return count > 99 ? '99+' : String(count);
  });

  toggle(): void {
    this.open.update(value => !value);
    if (this.open()) {
      this.store.loadLatest();
    }
  }

  close(): void {
    this.open.set(false);
  }

  refresh(event: Event): void {
    event.stopPropagation();
    this.store.loadLatest();
  }

  markAllRead(event: Event): void {
    event.stopPropagation();
    this.store.markAllRead();
  }

  openItem(item: PrivateNotificationItem): void {
    this.store.open(item);
    this.close();
  }

  dismiss(event: Event, item: PrivateNotificationItem): void {
    event.stopPropagation();
    this.store.dismiss(item);
  }

  itemKey(item: PrivateNotificationItem): string {
    return typeof item.id === 'string' ? item.id : item.id.value;
  }

  titleOf(item: PrivateNotificationItem): string {
    return item.titleText || item.titleKey || 'Notification';
  }

  messageOf(item: PrivateNotificationItem): string {
    return item.messageText || item.messageKey || '';
  }

  severityClass(item: PrivateNotificationItem): string {
    if (item.severity === 'CRITICAL' || item.severity === 'ERROR') return 'is-danger';
    if (item.severity === 'WARNING') return 'is-warning';
    return 'is-info';
  }
}
