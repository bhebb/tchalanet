import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LabelPipe, PublicFooterColumn, PublicShellRuntime } from '@tch/page-model';
import { ActionItem, actionHref, actionRoute, actionText, isExternalAction } from '@tch/api';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

interface FooterText {
    readonly descriptionKey: string;
    readonly statusKey: string;
    readonly copyrightKey: string;
}

@Component({
    selector: 'tch-public-footer',
    imports: [RouterLink, LabelPipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <footer class="public-footer">
            <div class="public-footer__inner">
                <section class="public-footer__brand" aria-label="Tchalanet">
                    <a class="public-footer__brand-link" [routerLink]="actionRoute(brand())">
                        <img [src]="brandImage()" [alt]="actionText(brand()) || 'Tchalanet'"/>
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
                                    @if (item.icon) {
                                        <span class="material-symbols-outlined" aria-hidden="true">
                      {{ item.icon }}
                    </span>
                                    } @else {
                                        <span aria-hidden="true">{{ actionText(item) | tchLabel }}</span>
                                    }
                                </a>
                            }
                        </nav>
                    }
                </section>

                @if (columns().length) {
                    <nav class="public-footer__columns" aria-label="Pied de page public">
                        @for (column of columns(); track column.id ?? column.titleKey) {
                            <section class="public-footer__column">
                                <h2>{{ column.titleKey | tchLabel }}</h2>

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
                            </section>
                        }
                    </nav>
                }
            </div>

            @if (text().copyrightKey) {
                <div class="public-footer__bottom">
                    <small>{{ text().copyrightKey | tchLabel }}</small>
                </div>
            }
        </footer>
    `,
    styles: [
        `
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
            grid-template-columns: minmax(16rem, 1.2fr) 2fr;
            gap: clamp(2rem, 6vw, 4rem);
            padding: clamp(2.5rem, 8vw, 4rem) 0;
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
            width: 11rem;
            height: auto;
            display: block;
          }

          .public-footer__brand p {
            margin: 0;
            max-width: 22rem;
            color: var(--comp-footer-link);
          }

          .public-footer__status {
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            color: var(--comp-footer-link);
            font-weight: 800;
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
            width: 2.5rem;
            height: 2.5rem;
            display: inline-grid;
            place-items: center;
            border: 1px solid color-mix(in oklab, currentColor 20%, transparent);
            border-radius: var(--tch-radius-pill, 9999px);
            color: var(--comp-footer-link);
            text-decoration: none;
          }

          .public-footer__social a:hover {
            border-color: color-mix(in oklab, currentColor 45%, transparent);
            color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
            background: color-mix(in oklab, currentColor 8%, transparent);
          }

          .public-footer__social .material-symbols-outlined {
            font-size: 1.25rem;
            line-height: 1;
          }

          .public-footer__columns {
            display: grid;
            grid-template-columns: repeat(3, minmax(0, 1fr));
            gap: 1.5rem;
          }

          .public-footer__column {
            display: grid;
            align-content: start;
            gap: 0.625rem;
          }

          .public-footer__column h2 {
            margin: 0 0 0.25rem;
            font-size: var(--tch-font-size-label-sm, 0.75rem);
            line-height: var(--tch-line-height-label-sm, 1rem);
            text-transform: uppercase;
            letter-spacing: 0.04em;
            color: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
          }

          .public-footer__column a {
            color: var(--comp-footer-link);
            text-decoration: none;
          }

          .public-footer__column a:hover {
            color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
          }

          .public-footer__bottom {
            width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 1120px);
            margin: 0 auto;
            padding: 1rem 0;
            border-top: 1px solid color-mix(in oklab, currentColor 18%, transparent);
            color: var(--comp-footer-link);
          }
          
          .public-footer__social img {
            width: 1.125rem;
            height: 1.125rem;
            display: block;
          }

          @media (max-width: 720px) {
            .public-footer__inner {
              grid-template-columns: 1fr;
              padding-bottom: 2rem;
            }

            .public-footer__brand-link img {
              width: 9.5rem;
            }

            .public-footer__columns {
              grid-template-columns: 1fr;
            }

            .public-footer__bottom {
              padding-bottom: calc(5rem + env(safe-area-inset-bottom, 0px));
            }
          }
        `,
    ],
})
export class PublicFooter {
    private readonly iconReg = inject(MatIconRegistry);
    private readonly sanitizer = inject(DomSanitizer);

    readonly shell = input<PublicShellRuntime | undefined>();

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
        this.iconReg.addSvgIcon(
            'facebook',
            this.sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/facebook.svg'),
        );
        this.iconReg.addSvgIcon(
            'youtube',
            this.sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/youtube.svg'),
        );
        this.iconReg.addSvgIcon(
            'instagram',
            this.sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/instagram.svg'),
        );
        this.iconReg.addSvgIcon(
            'linkedin',
            this.sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/linkedin.svg'),
        );

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
