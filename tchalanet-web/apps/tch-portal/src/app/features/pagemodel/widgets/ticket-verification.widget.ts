import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { WidgetConfig } from '../../../shared/types';
import { LabelPipe } from '../label.pipe';
import { stringProp, toPublicPath } from '../widget.contract';

@Component({
  selector: 'tch-ticket-verification-widget',
  imports: [LabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="ticket">
      <div class="ticket__copy">
        <p class="ticket__eyebrow">{{ 'public.ticket.eyebrow' | tchLabel }}</p>
        <h2>{{ titleKey() | tchLabel }}</h2>
        <p>{{ descriptionKey() | tchLabel }}</p>
      </div>

      <form class="ticket__form" action="#" aria-label="Ticket verification">
        <label for="public-ticket-code">{{ 'public.ticket.code_label' | tchLabel }}</label>
        <div class="ticket__input-row">
          <input
            id="public-ticket-code"
            name="code"
            autocomplete="off"
            inputmode="text"
            [placeholder]="'public.ticket.placeholder' | tchLabel"
          />
          <a class="ticket__scan" [attr.href]="path()">
            {{ 'public.ticket.scan_qr' | tchLabel }}
          </a>
        </div>
        <a class="ticket__submit" [attr.href]="path()">
          {{ ctaKey() | tchLabel }}
        </a>
        <a class="ticket__help" href="/public/help">{{ 'public.ticket.help' | tchLabel }}</a>
      </form>
    </section>
  `,
  styles: [
    `
      .ticket {
        display: grid;
        grid-template-columns: minmax(0, 0.9fr) minmax(280px, 1.1fr);
        gap: 1.5rem;
        padding: clamp(1.25rem, 4vw, 2rem);
        border-radius: var(--tch-radius-xl, 24px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }
      .ticket__copy {
        display: grid;
        align-content: start;
        gap: 0.75rem;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }
      .ticket__eyebrow,
      .ticket h2,
      .ticket p {
        margin: 0;
      }
      .ticket__eyebrow {
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        text-transform: uppercase;
      }
      .ticket h2 {
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
      }
      .ticket__form {
        display: grid;
        gap: 0.75rem;
      }
      .ticket__form label,
      .ticket__help {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
      }
      .ticket__input-row {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 0.5rem;
      }
      .ticket input {
        min-height: var(--tch-touch-target, 48px);
        width: 100%;
        box-sizing: border-box;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-bright, var(--mat-sys-surface));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        padding: 0 0.875rem;
        font-family: var(--tch-font-family-mono, monospace);
      }
      .ticket__scan,
      .ticket__submit {
        min-height: var(--tch-touch-target, 48px);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border-radius: var(--tch-radius-control, 8px);
        text-decoration: none;
        font-weight: 800;
      }
      .ticket__scan {
        padding: 0 0.75rem;
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-primary, var(--mat-sys-primary));
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      }
      .ticket__submit {
        background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
        color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
      }
      @media (max-width: 720px) {
        .ticket {
          grid-template-columns: 1fr;
        }
        .ticket__input-row {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class TicketVerificationWidget {
  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly titleKey = computed(
    () => stringProp(this.config(), 'titleKey') ?? 'public.ticket.title',
  );
  readonly descriptionKey = computed(
    () => stringProp(this.config(), 'descriptionKey') ?? 'public.ticket.description',
  );
  readonly ctaKey = computed(() => stringProp(this.config(), 'ctaKey') ?? 'public.ticket.cta');
  readonly path = computed(() => toPublicPath(stringProp(this.config(), 'path') ?? '/public/check-ticket'));
}
