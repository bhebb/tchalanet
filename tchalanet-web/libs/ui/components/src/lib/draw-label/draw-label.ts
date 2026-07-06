import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type TchDrawLabelDensity = 'comfortable' | 'compact';

@Component({
  selector: 'tch-draw-label',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="tch-draw-label__logo" aria-hidden="true">
      @if (logoUrl()) {
        <img [src]="logoUrl()" [alt]="logoAlt()" />
      } @else {
        <span>{{ fallbackMark() }}</span>
      }
    </span>

    <span class="tch-draw-label__body">
      @if (eyebrow()) {
        <span class="tch-draw-label__eyebrow">{{ eyebrow() }}</span>
      }
      <span class="tch-draw-label__primary">{{ primary() }}</span>
      @if (secondary()) {
        <span class="tch-draw-label__secondary">{{ secondary() }}</span>
      }
      @if (meta()) {
        <span class="tch-draw-label__meta">{{ meta() }}</span>
      }
    </span>
  `,
  host: {
    class: 'tch-draw-label',
    '[class.tch-draw-label--compact]': "density() === 'compact'",
    '[attr.aria-label]': 'ariaLabel()',
  },
  styles: [
    `
      :host {
        --comp-draw-label-logo-size: 2.25rem;
        --comp-draw-label-logo-bg: var(--tch-color-surface-container-low);
        --comp-draw-label-logo-fg: var(--tch-color-primary);
        --comp-draw-label-primary-fg: var(--tch-color-on-surface);
        --comp-draw-label-secondary-fg: var(--tch-color-on-surface-variant);
        --comp-draw-label-meta-fg: var(--tch-color-on-surface-variant);

        display: inline-grid;
        grid-template-columns: var(--comp-draw-label-logo-size) minmax(0, 1fr);
        align-items: center;
        gap: 0.625rem;
        max-width: 100%;
        min-width: 0;
        vertical-align: middle;
      }

      .tch-draw-label__logo {
        display: inline-flex;
        width: var(--comp-draw-label-logo-size);
        height: var(--comp-draw-label-logo-size);
        align-items: center;
        justify-content: center;
        overflow: hidden;
        border: 1px solid var(--tch-color-outline-variant);
        border-radius: var(--tch-radius-sm, 6px);
        background: var(--comp-draw-label-logo-bg);
        color: var(--comp-draw-label-logo-fg);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        line-height: 1;
      }

      .tch-draw-label__logo img {
        display: block;
        width: 100%;
        height: 100%;
        object-fit: contain;
      }

      .tch-draw-label__body {
        display: grid;
        min-width: 0;
        gap: 0.0625rem;
      }

      .tch-draw-label__eyebrow,
      .tch-draw-label__secondary,
      .tch-draw-label__meta {
        overflow: hidden;
        color: var(--comp-draw-label-secondary-fg);
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        line-height: var(--tch-line-height-label-sm, 1rem);
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .tch-draw-label__eyebrow {
        font-weight: 800;
        text-transform: uppercase;
      }

      .tch-draw-label__primary {
        overflow: hidden;
        color: var(--comp-draw-label-primary-fg);
        font-size: var(--tch-font-size-body-md, 0.875rem);
        font-weight: 800;
        line-height: var(--tch-line-height-body-md, 1.25rem);
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .tch-draw-label__meta {
        color: var(--comp-draw-label-meta-fg);
      }

      :host.tch-draw-label--compact {
        --comp-draw-label-logo-size: 1.75rem;
        gap: 0.5rem;
      }

      :host.tch-draw-label--compact .tch-draw-label__secondary,
      :host.tch-draw-label--compact .tch-draw-label__meta {
        display: none;
      }
    `,
  ],
})
export class TchDrawLabel {
  readonly primary = input('Tirage');
  readonly secondary = input<string | null | undefined>(null);
  readonly eyebrow = input<string | null | undefined>(null);
  readonly meta = input<string | null | undefined>(null);
  readonly logoUrl = input<string | null | undefined>(null);
  readonly logoAlt = input('');
  readonly density = input<TchDrawLabelDensity>('comfortable');

  protected readonly fallbackMark = computed(() => {
    const value = this.primary().trim();
    if (!value) return 'DR';
    return value
      .split(/\s+/)
      .slice(0, 2)
      .map(part => part.charAt(0).toUpperCase())
      .join('');
  });

  protected readonly ariaLabel = computed(() => {
    return [this.eyebrow(), this.primary(), this.secondary(), this.meta()]
      .filter(Boolean)
      .join(' ');
  });
}
