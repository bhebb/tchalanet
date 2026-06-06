import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ActionItem, PageShell } from '../../../shared/types';
import { LabelPipe } from '../../pagemodel/label.pipe';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';

interface PublicFooterColumn {
    readonly id?: string;
    readonly titleKey: string;
    readonly links: readonly ActionItem[];
}

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
            background: var(--tch-color-primary, var(--mat-sys-primary));
            color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
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
            color: var(--tch-color-primary-fixed, var(--mat-sys-primary-fixed));
          }

          .public-footer__status {
            display: inline-flex;
            align-items: center;
            gap: 0.5rem;
            color: var(--tch-color-primary-fixed, var(--mat-sys-primary-fixed));
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
            color: var(--tch-color-primary-fixed, var(--mat-sys-primary-fixed));
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
            color: var(--tch-color-primary-fixed, var(--mat-sys-primary-fixed));
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
            color: var(--tch-color-primary-fixed, var(--mat-sys-primary-fixed));
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
    readonly shell = input<PageShell | undefined>();

    readonly brand = computed(() => readActionItem(this.shell()?.footer?.brand));
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

    constructor(iconReg: MatIconRegistry, sanitizer: DomSanitizer) {
        iconReg.addSvgIcon('x', sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/x.svg'));
        iconReg.addSvgIcon(
            'facebook',
            sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/facebook.svg'),
        );
        iconReg.addSvgIcon(
            'youtube',
            sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/youtube.svg'),
        );
        iconReg.addSvgIcon(
            'instagram',
            sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/instagram.svg'),
        );
        iconReg.addSvgIcon(
            'linkedin',
            sanitizer.bypassSecurityTrustResourceUrl('/assets/svg/socials/linkedin.svg'),
        );

    }
}

function publicFooterText(shell: PageShell | undefined): FooterText {
    const props = shell?.footer?.props;

    return {
        descriptionKey: readString(props, 'descriptionKey') ?? '',
        statusKey: readString(props, 'statusKey') ?? '',
        copyrightKey: readString(props, 'copyrightKey') ?? '',
    };
}

function publicFooterColumns(shell: PageShell | undefined): readonly PublicFooterColumn[] {
    const columns = readColumns(shell?.footer?.props);
    if (columns.length) {
        return columns;
    }

    const links = normalizeNavItems([
        ...(shell?.footer?.nav?.primary ?? []),
        ...(shell?.footer?.nav?.secondary ?? []),
    ]);

    return links.length ? [{id: 'fallback', titleKey: '', links}] : [];
}

function publicFooterSocial(shell: PageShell | undefined): readonly ActionItem[] {
    const props = shell?.footer?.props;
    if (!props || typeof props !== 'object') {
        return [];
    }

    const social = (props as Record<string, unknown>)['social'];
    return Array.isArray(social) ? social.filter(isActionItem) : [];
}

function readColumns(source: unknown): readonly PublicFooterColumn[] {
    if (!source || typeof source !== 'object') {
        return [];
    }

    const columns = (source as Record<string, unknown>)['columns'];
    if (!Array.isArray(columns)) {
        return [];
    }

    return columns.flatMap((column) => {
        if (!column || typeof column !== 'object') {
            return [];
        }

        const record = column as Record<string, unknown>;
        const id = readString(record, 'id');
        const titleKey = readString(record, 'titleKey');
        const links = Array.isArray(record['links']) ? record['links'].filter(isActionItem) : [];

        return titleKey ? [{id, titleKey, links}] : [];
    });
}

function normalizeNavItems(items: readonly unknown[]): readonly ActionItem[] {
    return items.filter(isActionItem);
}

function readActionItem(value: unknown): ActionItem | undefined {
    return isActionItem(value) ? value : undefined;
}

function readString(source: unknown, key: string): string | undefined {
    return source && typeof source === 'object' && typeof (source as Record<string, unknown>)[key] === 'string'
        ? ((source as Record<string, unknown>)[key] as string)
        : undefined;
}

function actionText(item: ActionItem | undefined): string {
    return item?.labelKey ?? item?.label ?? '';
}

function actionRoute(item: ActionItem | undefined): string {
    return item?.destination?.kind === 'route' ? item.destination.value : '/public';
}

function actionHref(item: ActionItem | undefined): string {
    return item?.destination?.value ?? '#';
}

function isExternalAction(item: ActionItem): boolean {
    return item.destination?.kind === 'url';
}

function isActionItem(value: unknown): value is ActionItem {
    return !!value && typeof value === 'object' && typeof (value as ActionItem).id === 'string';
}
