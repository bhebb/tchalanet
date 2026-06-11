import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { actionFrom, destinationHref, stringProp, WidgetAction } from '@tch/page-model';
import { TchActionButton } from '@tch/ui/components';

@Component({
  selector: 'tch-contact-cta-widget',
  imports: [LabelPipe, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="contact-cta">
      <h2 class="contact-cta__title">{{ titleKey() | tchLabel }}</h2>
      <p class="contact-cta__body">{{ bodyKey() | tchLabel }}</p>
      <div class="contact-cta__actions">
        @if (primaryAction(); as action) {
          <a tch-action variant="primary" [attr.href]="href(action)">
            {{ labelKey(action) | tchLabel }}
          </a>
        } @else {
          <a tch-action variant="primary" href="/public/contact">
            {{ 'public.contact_cta.cta' | tchLabel }}
          </a>
        }
        @if (helpAction(); as action) {
          <a tch-action variant="tertiary" [attr.href]="href(action)">
            {{ labelKey(action) | tchLabel }}
          </a>
        } @else {
          <a tch-action variant="tertiary" href="/public/help">
            {{ 'public.contact_cta.help' | tchLabel }}
          </a>
        }
      </div>
    </section>
  `,
  styles: [
    `
      @use 'mixins' as tch;
      @use 'breakpoints' as bp;

      .contact-cta {
        display: grid;
        justify-items: center;
        text-align: center;
        gap: 1rem;
        padding: 2.5rem 1.25rem;
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));

        @include bp.up(medium) {
          padding: 3rem 2rem;
          gap: 1.25rem;
        }

        @include bp.up(expanded) {
          padding: clamp(3rem, 5vw, 4.5rem) clamp(2rem, 8vw, 6rem);
        }
      }

      .contact-cta__title {
        margin: 0;
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
        font-weight: 800;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        max-width: 32ch;

        @include bp.up(medium) {
          font-size: var(--tch-font-size-headline-sm, 1.75rem);
        }

        @include bp.up(expanded) {
          font-size: var(--tch-font-size-headline-lg, 2rem);
          line-height: var(--tch-line-height-headline-lg, 2.5rem);
        }
      }

      .contact-cta__body {
        margin: 0;
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: 1.6;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        max-width: 52ch;
      }

      .contact-cta__actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
        justify-content: center;
      }
    `,
  ],
})
export class ContactCtaWidget {
  readonly config = input<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? 'public.contact_cta.title');
  readonly bodyKey = computed(() => stringProp(this.config(), 'bodyKey') ?? 'public.contact_cta.body');
  readonly primaryAction = computed(() => actionFrom(this.config()?.props?.['primaryAction']));
  readonly helpAction = computed(() => actionFrom(this.config()?.props?.['helpAction']));

  href(action: WidgetAction): string {
    return destinationHref(action.destination);
  }

  labelKey(action: WidgetAction): string {
    return action.labelKey ?? '';
  }
}
