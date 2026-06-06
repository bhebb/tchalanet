import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { LanguageSwitcherComponent } from '../../../core/i18n';
import { LabelPipe, PublicShellRuntime } from '@tch/page-model';
import { ActionItem, actionText, TchBrand, TchNav, TchOverlayNav } from '@tch/ui/components';

@Component({
  selector: 'tch-public-header',
  imports: [LanguageSwitcherComponent, LabelPipe, TchBrand, TchNav, TchOverlayNav],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="public-header">
      <div class="public-header__inner">
        <tch-brand class="public-header__brand" [brand]="brand()" [showName]="false" />

        <button
            type="button"
            class="public-header__burger"
            [attr.aria-expanded]="mobileMenuOpen()"
            (click)="toggleMobileMenu()"
        >
          <span aria-hidden="true"></span>
          <span aria-hidden="true"></span>
          <span aria-hidden="true"></span>
          <span class="public-header__sr">{{ 'public.nav.menu' | tchLabel }}</span>
        </button>

        <tch-nav class="public-header__nav" [items]="nav()" ariaLabel="Navigation publique" />

        <div class="public-header__actions">
          <div class="public-header__tools" aria-label="Langue">
            <tch-language-switcher/>
          </div>
          @if (loginAction(); as loginItem) {
            <button type="button" class="public-header__login" (click)="login()">
              {{ actionText(loginItem) | tchLabel }}
            </button>
          }
        </div>
      </div>

      <tch-overlay-nav
        [open]="mobileMenuOpen()"
        [items]="nav()"
        ariaLabel="Navigation publique mobile"
        (requestClose)="closeMobileMenu()"
      />
    </header>
  `,
  styles: [
    `
      .public-header {
        --comp-header-bg: var(--tch-color-surface-container-lowest);
        --comp-header-fg: var(--tch-color-on-surface);
        --comp-header-border: var(--tch-color-outline-variant);
        position: sticky;
        top: 0;
        z-index: 30;
        background: color-mix(
          in oklab,
          var(--comp-header-bg) 92%,
          transparent
        );
        color: var(--comp-header-fg);
        border-bottom: 1px solid var(--comp-header-border);
        backdrop-filter: blur(16px);
      }

      .public-header__inner {
        min-height: 4.5rem;
        width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1120px);
        margin: 0 auto;
        display: flex;
        align-items: center;
        gap: clamp(0.75rem, 2vw, 1.25rem);
      }

      .public-header__brand {
        display: inline-flex;
        align-items: center;
        gap: 0.625rem;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        text-decoration: none;
        font-weight: 900;
      }

      .public-header__brand img {
        width: 10rem;
        height: auto;
        display: block;
      }

      .public-header__burger {
        display: none;
      }

      .public-header__sr {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip: rect(0, 0, 0, 0);
        white-space: nowrap;
        border: 0;
      }

      .public-header__nav {
        display: flex;
        flex: 1 1 auto;
        flex-wrap: nowrap;
        justify-content: flex-start;
        gap: 0.25rem;
        min-width: 0;
      }

      .public-header__nav a {
        min-height: 2.5rem;
        display: inline-flex;
        align-items: center;
        padding: 0 0.625rem;
        border-radius: var(--tch-radius-pill, 9999px);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        text-decoration: none;
        font-weight: 800;
        font-size: 0.875rem;
        white-space: nowrap;
      }

      .public-header__nav a:hover {
        background: var(--tch-color-surface-tonal, var(--mat-sys-surface-container));
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }

      .public-header__actions {
        flex: 0 0 auto;
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 0.625rem;
      }

      .public-header__tools {
        display: flex;
        align-items: center;
        gap: 0.375rem;
      }

      .public-header__login {
        min-height: var(--tch-touch-target, 48px);
        padding: 0 1.125rem;
        border-radius: var(--tch-radius-control, 8px);
        border: 0;
        background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
        color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
        cursor: pointer;
        font-weight: 900;
      }

      .public-header__mobile-panel {
        display: none;
      }

      @media (max-width: 720px) {
        .public-header__inner {
          min-height: auto;
          gap: 0.75rem;
          padding: 0.75rem 0;
        }

        .public-header__brand {
          flex: 1 1 auto;
        }

        .public-header__brand img {
          width: 8.75rem;
        }

        .public-header__nav {
          display: none;
        }

        .public-header__burger {
          flex: 0 0 auto;
          width: var(--tch-touch-target, 48px);
          height: var(--tch-touch-target, 48px);
          display: grid;
          place-items: center;
          gap: 0;
          border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
          border-radius: var(--tch-radius-control, 8px);
          background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
          color: var(--tch-color-primary, var(--mat-sys-primary));
        }

        .public-header__burger span:not(.public-header__sr) {
          width: 1.25rem;
          height: 2px;
          display: block;
          border-radius: 9999px;
          background: currentColor;
        }

        .public-header__actions {
          gap: 0;
        }

        .public-header__tools {
          display: none;
        }

        .public-header__login {
          min-height: 2.5rem;
          padding: 0 0.875rem;
        }

        .public-header__mobile-panel {
          width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1120px);
          margin: 0 auto;
          padding: 0 0 0.875rem;
        }

        .public-header__mobile-panel--open {
          display: grid;
          gap: 0.75rem;
        }

        .public-header__mobile-nav {
          display: grid;
          gap: 0.375rem;
          padding: 0.75rem;
          border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
          border-radius: var(--tch-radius-lg, 12px);
          background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
          box-shadow: var(--mat-sys-level2, 0 8px 24px rgba(0, 0, 0, 0.12));
        }

        .public-header__mobile-nav a {
          min-height: var(--tch-touch-target, 48px);
          display: flex;
          align-items: center;
          padding: 0 0.875rem;
          border-radius: var(--tch-radius-control, 8px);
          color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
          text-decoration: none;
          font-weight: 850;
        }

        .public-header__mobile-nav a:nth-child(2) {
          background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
          color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
        }

        .public-header__mobile-tools {
          display: flex;
          gap: 0.5rem;
          align-items: center;
          justify-content: space-between;
          padding: 0.75rem;
          border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
          border-radius: var(--tch-radius-lg, 12px);
          background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        }
      }
    `,
  ],
})
export class PublicHeader {
  private readonly auth = inject(AuthSessionService);

  readonly shell = input<PublicShellRuntime | undefined>();
  readonly mobileMenuOpen = signal(false);
  readonly brand = computed(() => publicBrand(this.shell()));
  readonly nav = computed(() => publicHeaderNav(this.shell()));
  readonly loginAction = computed(() => publicLoginAction(this.shell()));

  login(): void {
    void this.auth.login();
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update((open) => !open);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  protected readonly actionText = actionText;
}

function publicBrand(shell: PublicShellRuntime | undefined): ActionItem | undefined {
  const brand = shell?.header.brand;
  return {
    id: brand?.id ?? 'public-brand',
    ...brand,
    image: brand?.image ?? '/assets/brand/tchalanet-logo.svg',
    destination: brand?.destination ?? { kind: 'route', value: '/public' },
  };
}

function publicHeaderNav(shell: PublicShellRuntime | undefined): readonly ActionItem[] {
  return shell?.header.primary ?? [];
}

function publicLoginAction(shell: PublicShellRuntime | undefined): ActionItem | undefined {
  const actions = [
    ...(shell?.header.actions ?? []),
    ...(shell?.header.secondary ?? []),
  ];
  return actions.find((item) => item.id === 'login') ?? actions[0];
}
