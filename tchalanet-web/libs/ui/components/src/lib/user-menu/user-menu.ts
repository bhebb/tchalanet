import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatMenuModule } from '@angular/material/menu';
import { TranslatePipe } from '@ngx-translate/core';

/**
 * Account menu: an avatar button that opens a dropdown with Profil / Paramètres / Déconnexion.
 * Presentational — the host wires the emitted actions.
 */
@Component({
  selector: 'tch-user-menu',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatMenuModule, TranslatePipe],
  template: `
    <button
      type="button"
      class="user-menu__trigger"
      [matMenuTriggerFor]="menu"
      [attr.aria-label]="name() || ('nav.profile' | translate)"
    >
      @if (initials()) {
        <span class="user-menu__avatar" aria-hidden="true">{{ initials() }}</span>
      } @else {
        <span class="user-menu__avatar material-symbols-outlined" aria-hidden="true">person</span>
      }
    </button>

    <mat-menu #menu="matMenu" class="user-menu__panel">
      @if (name()) {
        <div class="user-menu__name">{{ name() }}</div>
      }
      <button mat-menu-item type="button" (click)="profile.emit()">
        <span class="material-symbols-outlined" aria-hidden="true">person</span>
        {{ 'nav.profile' | translate }}
      </button>
      <button mat-menu-item type="button" (click)="settings.emit()">
        <span class="material-symbols-outlined" aria-hidden="true">settings</span>
        {{ 'nav.settings' | translate }}
      </button>
      <button mat-menu-item type="button" (click)="logout.emit()">
        <span class="material-symbols-outlined" aria-hidden="true">logout</span>
        {{ 'auth.logout' | translate }}
      </button>
    </mat-menu>
  `,
  styles: [
    `
      .user-menu__trigger {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 2.5rem;
        height: 2.5rem;
        padding: 0;
        border: 0;
        border-radius: 9999px;
        background: var(--tch-color-primary-container, #dfe0ff);
        color: var(--tch-color-on-primary-container, #1a1b4b);
        cursor: pointer;
      }

      .user-menu__trigger:hover {
        filter: brightness(0.97);
      }

      .user-menu__avatar {
        font-size: 0.875rem;
        font-weight: 700;
        line-height: 1;
        text-transform: uppercase;
      }

      .user-menu__avatar.material-symbols-outlined {
        font-size: 1.375rem;
        font-weight: 400;
      }
    `,
  ],
})
export class TchUserMenu {
  readonly name = input('');
  readonly profile = output<void>();
  readonly settings = output<void>();
  readonly logout = output<void>();

  readonly initials = computed(() => {
    const parts = this.name().trim().split(/\s+/).filter(Boolean);
    if (!parts.length) return '';
    const letters = parts.length === 1 ? parts[0].slice(0, 2) : parts[0][0] + parts[parts.length - 1][0];
    return letters.toUpperCase();
  });
}
