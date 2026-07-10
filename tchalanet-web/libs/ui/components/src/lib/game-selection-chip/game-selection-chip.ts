import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

type GameSelectionKind = 'bolet' | 'maryaj' | 'loto';

@Component({
  selector: 'tch-game-selection-chip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="selection" [attr.data-kind]="kind()" [attr.aria-label]="ariaLabel()">
      @if (kind() === 'maryaj') {
        @if (maryajParts(); as parts) {
          <span class="selection__maryaj-slot">{{ parts[0] }}</span>
          <span class="selection__maryaj-link" aria-hidden="true">+</span>
          <span class="selection__maryaj-slot">{{ parts[1] }}</span>
        } @else {
          <span class="selection__ball">{{ selection() }}</span>
        }
      } @else if (kind() === 'loto') {
        <span class="selection__loto">
          @for (digit of lotoDigits(); track $index) {
            <span>{{ digit }}</span>
          }
        </span>
      } @else {
        <span class="selection__ball">{{ selection() }}</span>
      }
    </span>
  `,
  styles: [
    `
      :host {
        --tch-selection-size: 3rem;
        --tch-selection-bg: var(--tch-color-primary-container, #141545);
        --tch-selection-fg: var(--tch-color-on-primary-container, #fff);
        --tch-selection-border: var(--tch-color-outline-variant, #d8d5df);
        display: inline-flex;
        max-width: 100%;
      }

      .selection {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        max-width: 100%;
        color: var(--tch-selection-fg);
        font-family: 'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
        font-weight: 900;
        line-height: 1;
      }

      .selection__ball {
        display: inline-grid;
        place-items: center;
        min-width: var(--tch-selection-size);
        height: var(--tch-selection-size);
        border-radius: var(--tch-radius-pill, 999px);
        padding-inline: 0.5rem;
        background: var(--tch-selection-bg);
        color: var(--tch-selection-fg);
        font-size: calc(var(--tch-selection-size) * 0.42);
      }

      .selection[data-kind='maryaj'] {
        gap: 0.25rem;
        padding: 0.25rem;
        border: 1px solid var(--tch-selection-border);
        border-radius: var(--tch-radius-pill, 999px);
        background: var(--tch-color-surface-container-lowest, #fff);
        color: var(--tch-color-primary, #141545);
      }

      .selection__maryaj-slot {
        display: inline-grid;
        place-items: center;
        min-width: calc(var(--tch-selection-size) * 0.9);
        height: calc(var(--tch-selection-size) * 0.9);
        border-radius: var(--tch-radius-pill, 999px);
        background: var(--tch-selection-bg);
        color: var(--tch-selection-fg);
        font-size: calc(var(--tch-selection-size) * 0.34);
      }

      .selection__maryaj-link {
        display: inline-grid;
        place-items: center;
        width: calc(var(--tch-selection-size) * 0.42);
        color: var(--tch-color-primary, #141545);
        font-size: calc(var(--tch-selection-size) * 0.3);
      }

      .selection__loto {
        display: inline-flex;
        align-items: center;
        gap: 0.1875rem;
        padding: 0.25rem;
        border: 1px solid var(--tch-selection-border);
        border-radius: var(--tch-radius-pill, 999px);
        background: var(--tch-color-surface-container-lowest, #fff);
      }

      .selection__loto span {
        display: inline-grid;
        place-items: center;
        width: calc(var(--tch-selection-size) * 0.62);
        height: calc(var(--tch-selection-size) * 0.82);
        border-radius: 0.55rem;
        background: var(--tch-selection-bg);
        color: var(--tch-selection-fg);
        font-size: calc(var(--tch-selection-size) * 0.3);
      }
    `,
  ],
})
export class TchGameSelectionChip {
  readonly gameCode = input('');
  readonly selection = input('');

  readonly kind = computed<GameSelectionKind>(() => {
    const code = normalizeGameCode(this.gameCode());
    if (code.includes('MARYAJ')) return 'maryaj';
    if (code.startsWith('HT_LOTO') || code.startsWith('LOTO')) return 'loto';
    return 'bolet';
  });

  readonly maryajParts = computed<readonly [string, string] | null>(() => {
    const match = /^(\d{2})-(\d{2})$/.exec(this.selection().trim());
    return match ? [match[1], match[2]] : null;
  });

  readonly lotoDigits = computed(() => {
    const digits = this.selection().replace(/\D/g, '').split('');
    return digits.length ? digits : [this.selection()];
  });

  readonly ariaLabel = computed(() => {
    if (this.kind() === 'maryaj' && this.maryajParts()) {
      const [first, second] = this.maryajParts()!;
      return `Maryaj ${first} et ${second}`;
    }
    return this.selection();
  });
}

function normalizeGameCode(value: string): string {
  const code = value.trim().toUpperCase();
  if (code === 'BORLETTE') return 'HT_BOLET';
  if (code === 'HT_LOTO_3') return 'HT_LOTO3';
  if (code === 'HT_LOTO_4') return 'HT_LOTO4';
  if (code === 'HT_LOTO_5') return 'HT_LOTO5';
  if (code === 'HT_MARYAJ_GRATUIT') return 'HT_MARYAJ_GRATIS';
  return code;
}
