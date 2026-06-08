import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';
import { stringProp, toPublicPath } from '@tch/page-model';
import { TchActionButton, TchCard } from '@tch/ui/components';

@Component({
  selector: 'tch-ticket-verification-widget',
  imports: [LabelPipe, TchCard, TchActionButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <tch-card class="check-ticket-widget">
      <div class="check-ticket-widget__copy">
        <p class="check-ticket-widget__eyebrow">{{ 'public.ticket.eyebrow' | tchLabel }}</p>
        <h2 class="check-ticket-widget__title">{{ titleKey() | tchLabel }}</h2>
        <p class="check-ticket-widget__description">{{ descriptionKey() | tchLabel }}</p>
      </div>

      <form class="check-ticket-widget__form" action="#" (submit)="$event.preventDefault()">
        <div class="check-ticket-widget__field">
          <label class="check-ticket-widget__label" for="check-ticket-code">
            {{ 'public.ticket.code_label' | tchLabel }}
          </label>
          <div class="check-ticket-widget__input-row">
            <input
              id="check-ticket-code"
              class="check-ticket-widget__input"
              name="code"
              type="text"
              autocomplete="off"
              inputmode="text"
              [value]="code()"
              [placeholder]="'public.ticket.placeholder' | tchLabel"
              (input)="onCodeInput($event)"
            />
            <a
              tch-action
              variant="secondary"
              class="check-ticket-widget__scan-btn"
              [attr.href]="path()"
              aria-label="Scanner QR"
            >
              {{ 'public.ticket.scan_qr' | tchLabel }}
            </a>
          </div>
        </div>

        <div class="check-ticket-widget__actions">
          <a
            tch-action
            variant="primary"
            class="check-ticket-widget__submit"
            [attr.href]="submitHref()"
            style="--comp-action-bg: var(--tch-color-accent, #fecb00); --comp-action-fg: #1a1a1a;"
          >
            {{ ctaKey() | tchLabel }}
          </a>
        </div>

        <a class="check-ticket-widget__help" href="/public/help">
          {{ 'public.ticket.help' | tchLabel }}
        </a>
      </form>
    </tch-card>
  `,
  styles: [
    `
      @use 'mixins' as tch;
      @use 'breakpoints' as bp;

      /* Stretch TchCard to fill column */
      tch-card.check-ticket-widget {
        display: grid;
        grid-template-columns: 1fr;
        gap: 1.5rem;
        padding: clamp(1.25rem, 4vw, 2rem);
        border-radius: var(--tch-radius-xl, 20px);

        @include bp.up(medium) {
          grid-template-columns: minmax(0, 0.9fr) minmax(280px, 1.1fr);
        }
      }

      /* ── Copy ── */

      .check-ticket-widget__copy {
        display: grid;
        align-content: start;
        gap: 0.75rem;
      }

      .check-ticket-widget__eyebrow {
        margin: 0;
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      .check-ticket-widget__title {
        margin: 0;
        font-size: var(--tch-font-size-headline-mobile, 1.5rem);
        line-height: var(--tch-line-height-headline-mobile, 2rem);
        font-weight: 800;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .check-ticket-widget__description {
        margin: 0;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: 1.6;
      }

      /* ── Form ── */

      .check-ticket-widget__form {
        display: grid;
        gap: 1rem;
        align-content: start;
      }

      .check-ticket-widget__field {
        display: grid;
        gap: 0.5rem;
      }

      .check-ticket-widget__label {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        text-transform: uppercase;
        letter-spacing: 0.04em;
      }

      .check-ticket-widget__input-row {
        display: grid;
        grid-template-columns: 1fr auto;
        gap: 0.5rem;
      }

      .check-ticket-widget__input {
        min-height: var(--tch-touch-target, 48px);
        width: 100%;
        box-sizing: border-box;
        border: 1.5px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface, var(--mat-sys-surface));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        padding: 0 1rem;
        font-family: var(--tch-font-family-mono, monospace);
        font-size: 1rem;
        letter-spacing: 0.06em;
        @include tch.transition(border-color, standard, stay);

        &:focus {
          outline: none;
          border-color: var(--tch-color-primary, var(--mat-sys-primary));
        }

        &::placeholder {
          color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
          opacity: 0.6;
          letter-spacing: 0.04em;
        }
      }

      .check-ticket-widget__scan-btn {
        white-space: nowrap;
      }

      /* ── Actions ── */

      .check-ticket-widget__actions {
        display: grid;
      }

      .check-ticket-widget__submit {
        width: 100%;
        justify-content: center;
      }

      /* ── Help ── */

      .check-ticket-widget__help {
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        text-decoration: underline;
        text-underline-offset: 3px;
        @include tch.focus-visible;
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
  readonly ctaKey = computed(
    () => stringProp(this.config(), 'ctaKey') ?? 'public.ticket.cta',
  );
  readonly path = computed(() =>
    toPublicPath(stringProp(this.config(), 'path') ?? '/public/check-ticket'),
  );

  readonly code = signal('');

  readonly submitHref = computed(() => {
    const c = this.code().trim();
    return c ? `${this.path()}?code=${encodeURIComponent(c)}` : this.path();
  });

  onCodeInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const formatted = formatWidgetCode(input.value);
    this.code.set(formatted);
    input.value = formatted;
  }
}

function formatWidgetCode(value: string): string {
  const compact = value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 10);
  if (compact.length <= 4) return compact;
  if (compact.length <= 8) return `${compact.slice(0, 4)}-${compact.slice(4)}`;
  return `${compact.slice(0, 4)}-${compact.slice(4, 7)}-${compact.slice(7)}`;
}
