import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

/**
 * Pied de liste standard console : « N–M sur Total », précédent/suivant,
 * sélecteur de taille. Présentationnel — l'état `page`/`size` vit dans l'URL,
 * la page navigue sur `pageChange`/`sizeChange` (voir feature-playbook §2).
 */
@Component({
  selector: 'tch-pagination',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="tch-pagination" [attr.aria-label]="ariaLabel()">
      <span class="tch-pagination__range">
        @if (totalElements() > 0) {
          {{ rangeFrom() }}–{{ rangeTo() }} {{ ofLabel() }} {{ totalElements() }}
        } @else {
          0 {{ ofLabel() }} 0
        }
      </span>

      <div class="tch-pagination__controls">
        <label class="tch-pagination__size">
          <span>{{ sizeLabel() }}</span>
          <select
            [value]="size()"
            (change)="onSizeChange($event)"
            [attr.aria-label]="sizeLabel()"
          >
            @for (option of sizeOptions(); track option) {
              <option [value]="option">{{ option }}</option>
            }
          </select>
        </label>

        <button
          type="button"
          class="tch-pagination__nav"
          [disabled]="!hasPrevious()"
          (click)="pageChange.emit(page() - 1)"
        >
          <span class="material-symbols-outlined" aria-hidden="true">chevron_left</span>
          {{ prevLabel() }}
        </button>
        <button
          type="button"
          class="tch-pagination__nav"
          [disabled]="!hasNext()"
          (click)="pageChange.emit(page() + 1)"
        >
          {{ nextLabel() }}
          <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
        </button>
      </div>
    </nav>
  `,
  styles: [
    `
      .tch-pagination {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 0.75rem;
        flex-wrap: wrap;
        font-size: 0.875rem;
        color: var(--tch-color-on-surface-variant, #46464f);
      }

      .tch-pagination__controls {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        flex-wrap: wrap;
      }

      .tch-pagination__size {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
      }

      .tch-pagination__size select {
        padding: 0.25rem 0.5rem;
        border: 1px solid var(--tch-color-outline-variant, #c8c5d0);
        border-radius: var(--tch-radius-sm, 6px);
        background: var(--tch-color-surface, #fff);
        color: inherit;
        font: inherit;
      }

      .tch-pagination__nav {
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
        padding: 0.375rem 0.75rem;
        border: none;
        border-radius: var(--tch-radius-sm, 6px);
        background: transparent;
        color: var(--tch-color-primary, #4750c8);
        font: inherit;
        cursor: pointer;
      }

      .tch-pagination__nav:disabled {
        color: var(--tch-color-on-surface-variant, #46464f);
        opacity: 0.5;
        cursor: default;
      }

      .tch-pagination__nav:not(:disabled):hover {
        background: var(--tch-color-surface-container-high, #e6e6ee);
      }

      .tch-pagination__nav .material-symbols-outlined {
        font-size: 1.125rem;
      }
    `,
  ],
})
export class TchPaginationComponent {
  /** Index 0-based, reflété dans le query param `page`. */
  readonly page = input.required<number>();
  /** Taille de page, reflétée dans le query param `size`. */
  readonly size = input.required<number>();
  readonly totalElements = input.required<number>();
  readonly sizeOptions = input<readonly number[]>([10, 20, 50]);

  readonly ofLabel = input('sur');
  readonly prevLabel = input('Précédent');
  readonly nextLabel = input('Suivant');
  readonly sizeLabel = input('Par page');
  readonly ariaLabel = input('Pagination');

  readonly pageChange = output<number>();
  readonly sizeChange = output<number>();

  protected readonly rangeFrom = computed(() =>
    Math.min(this.page() * this.size() + 1, this.totalElements()),
  );
  protected readonly rangeTo = computed(() =>
    Math.min((this.page() + 1) * this.size(), this.totalElements()),
  );
  protected readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.totalElements() / Math.max(1, this.size()))),
  );
  protected readonly hasPrevious = computed(() => this.page() > 0);
  protected readonly hasNext = computed(() => this.page() + 1 < this.totalPages());

  protected onSizeChange(event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    if (Number.isFinite(value) && value > 0) this.sizeChange.emit(value);
  }
}
