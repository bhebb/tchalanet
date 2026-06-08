import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, PublicFooterColumn, PublicShellRuntime } from '@tch/page-model';
import { ActionItem, actionHref, actionRoute, actionText, isExternalAction } from '@tch/api';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

interface FooterText {
  readonly descriptionKey: string;
  readonly statusKey: string;
  readonly copyrightKey: string;
}

@Component({
  selector: 'tch-public-footer',
  imports: [RouterLink, LabelPipe, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <footer class="public-footer">
      <div class="public-footer__inner">
        <section class="public-footer__brand" aria-label="Tchalanet">
          <a class="public-footer__brand-link" [routerLink]="actionRoute(brand())">
            <img [src]="brandImage()" [alt]="actionText(brand()) || 'Tchalanet'" />
          </a>

          @if (text().descriptionKey) {
            <p>{{ text().descriptionKey | tchLabel }}</p>
          }

          @if (text().statusKey) {
            <div class="public-footer__status">
              <span aria-hidden="true"></span>
              {{ text().statusKey | tchLabel }}
            </div>
          }

          @if (social().length) {
            <nav class="public-footer__social" aria-label="Réseaux sociaux">
              @for (item of social(); track item.id) {
                <a
                  [href]="actionHref(item)"
                  target="_blank"
                  rel="noopener noreferrer"
                  [attr.aria-label]="actionText(item) | tchLabel"
                >
                  <!-- icon from backend when available, fallback to item.id which matches registered SVG names -->
                  <mat-icon [svgIcon]="item.icon ?? item.id" aria-hidden="true" />
                </a>
              }
            </nav>
          }
        </section>

        @if (columns().length) {
          <nav class="public-footer__columns" aria-label="Pied de page public">
            @for (column of columns(); track column.id ?? column.titleKey) {
              <section class="public-footer__column">
                <h2 class="public-footer__heading">{{ column.titleKey | tchLabel }}</h2>

                <div class="public-footer__links">
                  @for (item of column.links; track item.id) {
                    @if (isExternalAction(item)) {
                      <a [href]="actionHref(item)" target="_blank" rel="noopener noreferrer">
                        {{ actionText(item) | tchLabel }}
                      </a>
                    } @else {
                      <a [routerLink]="actionRoute(item)">
                        {{ actionText(item) | tchLabel }}
                      </a>
                    }
                  }
                </div>
              </section>
            }
          </nav>
        }
      </div>

      <div class="public-footer__bottom">
        <small>© {{ currentYear }} Tchalanet</small>
      </div>
    </footer>
  `,
  styles: [`
    .public-footer {
      --comp-footer-bg: var(--tch-color-primary);
      --comp-footer-fg: var(--tch-color-on-primary);
      --comp-footer-link: var(--tch-color-primary-fixed);
      background: var(--comp-footer-bg);
      color: var(--comp-footer-fg);
    }

    .public-footer__inner {
      width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1120px);
      margin: 0 auto;
      display: grid;
      grid-template-columns: 1fr;
      gap: 1.5rem;
      padding: 2rem 0 1.5rem;
    }

    .public-footer__brand {
      display: grid;
      gap: 1rem;
      align-content: start;
    }

    .public-footer__brand-link {
      display: inline-flex;
      align-items: center;
      width: fit-content;
      color: inherit;
      text-decoration: none;
    }

    .public-footer__brand-link img {
      width: 9.5rem;
      height: auto;
      display: block;
    }

    .public-footer__brand p {
      margin: 0;
      max-width: 22rem;
      font-size: 0.875rem;
      line-height: 1.5;
      overflow: hidden;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      color: var(--comp-footer-link);
    }

    .public-footer__status {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      color: var(--comp-footer-link);
      font-weight: var(--tch-weight-extra-bold, 800);
    }

    .public-footer__status span {
      width: 0.625rem;
      height: 0.625rem;
      border-radius: 9999px;
      background: var(--tch-color-status-ready, #10b981);
    }

    .public-footer__social {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.5rem;
      margin-top: 0.25rem;
    }

    .public-footer__social a {
      width: 2rem;
      height: 2rem;
      display: inline-grid;
      place-items: center;
      border: 1px solid color-mix(in oklab, currentColor 20%, transparent);
      border-radius: var(--tch-radius-pill, 9999px);
      color: var(--comp-footer-link);
      text-decoration: none;
    }

    .public-footer__social a:hover {
      border-color: color-mix(in oklab, currentColor 45%, transparent);
      color: var(--tch-color-on-primary);
      background: color-mix(in oklab, currentColor 8%, transparent);
    }

    .public-footer__social .material-symbols-outlined {
      font-size: 1.25rem;
      line-height: 1;
    }

    .public-footer__social img {
      width: 1.125rem;
      height: 1.125rem;
      display: block;
    }

    .public-footer__columns {
      display: grid;
      grid-template-columns: 1fr;
      gap: 1rem;
    }

    .public-footer__column {
      display: grid;
      align-content: start;
      gap: 0.375rem;
    }

    .public-footer__heading {
      margin: 0 0 0.25rem;
      font-size: var(--tch-font-size-label-sm, 0.75rem);
      line-height: var(--tch-line-height-label-sm, 1rem);
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: var(--tch-color-accent);
    }

    .public-footer__links {
      display: grid;
      gap: 0.375rem;
    }

    .public-footer__links a {
      font-size: 0.875rem;
      color: var(--comp-footer-link);
      text-decoration: none;
    }

    .public-footer__links a:hover {
      color: var(--tch-color-on-primary);
    }

    .public-footer__bottom {
      width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1120px);
      margin: 0 auto;
      padding: 1rem 0 calc(1rem + env(safe-area-inset-bottom, 0px));
      border-top: 1px solid color-mix(in oklab, currentColor 18%, transparent);
      color: var(--comp-footer-link);
    }

    /* Expanded layout ≥ 840px */
    @media (min-width: 840px) {
      .public-footer__inner {
        grid-template-columns: minmax(16rem, 1.2fr) 2fr;
        gap: clamp(2rem, 6vw, 4rem);
        padding: clamp(3rem, 8vw, 5rem) 0;
      }

      .public-footer__brand-link img {
        width: 11rem;
      }

      .public-footer__columns {
        grid-template-columns: repeat(auto-fill, minmax(10rem, 1fr));
      }
    }
  `],
})
export class PublicFooter {
  private readonly iconReg = inject(MatIconRegistry);
  private readonly sanitizer = inject(DomSanitizer);

  readonly shell = input<PublicShellRuntime | undefined>();
  readonly currentYear = new Date().getFullYear();

  readonly brand = computed(() => this.shell()?.footer.brand);
  readonly brandImage = computed(
    () => this.brand()?.image ?? '/assets/brand/tchalanet-logo-inverse.svg',
  );

  readonly columns = computed(() => publicFooterColumns(this.shell()));
  readonly social = computed(() => publicFooterSocial(this.shell()));
  readonly text = computed(() => publicFooterText(this.shell()));

  readonly actionText = actionText;
  readonly actionRoute = actionRoute;
  readonly actionHref = actionHref;
  readonly isExternalAction = isExternalAction;

  constructor() {
    this.iconReg.addSvgIcon('x', this.sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/x.svg'));
    this.iconReg.addSvgIcon('facebook', this.sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/facebook.svg'));
    this.iconReg.addSvgIcon('youtube', this.sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/youtube.svg'));
    this.iconReg.addSvgIcon('instagram', this.sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/instagram.svg'));
    this.iconReg.addSvgIcon('linkedin', this.sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/linkedin.svg'));
  }
}

function publicFooterText(shell: PublicShellRuntime | undefined): FooterText {
  return {
    descriptionKey: shell?.footer.descriptionKey ?? '',
    statusKey: shell?.footer.statusKey ?? '',
    copyrightKey: shell?.footer.copyrightKey ?? '',
  };
}

function publicFooterColumns(shell: PublicShellRuntime | undefined): readonly PublicFooterColumn[] {
  return shell?.footer.columns ?? [];
}

function publicFooterSocial(shell: PublicShellRuntime | undefined): readonly ActionItem[] {
  return shell?.footer.social ?? [];
}
