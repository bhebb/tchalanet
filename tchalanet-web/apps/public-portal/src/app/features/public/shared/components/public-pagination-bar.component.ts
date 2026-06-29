import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'tch-public-pagination-bar',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pub-pag-bar" [class.pub-pag-bar--centered]="!showMeta()">
      @if (showMeta()) {
        <div class="pub-pag-bar__meta">
          <span class="pub-pag-bar__total">
            {{ total() }} {{ 'public.shared.pagination.total' | translate }}
          </span>
          <label class="pub-pag-bar__size-label">
            {{ 'public.shared.pagination.per_page' | translate }}
            <select
              class="pub-pag-bar__size-select"
              [value]="size()"
              (change)="sizeChange.emit(+$any($event.target).value)"
            >
              @for (opt of sizeOptions(); track opt) {
                <option [value]="opt">{{ opt }}</option>
              }
            </select>
          </label>
        </div>
      }
      @if (totalPages() > 1) {
        <nav
          class="pub-pag-bar__nav"
          [attr.aria-label]="'public.shared.pagination.aria' | translate"
        >
          <button
            type="button"
            class="pub-pag-bar__btn"
            [disabled]="page() === 0"
            (click)="pageChange.emit(page() - 1)"
          >
            <span class="material-symbols-outlined" aria-hidden="true">chevron_left</span>
            <span>{{ 'common.previous' | translate }}</span>
          </button>
          <span class="pub-pag-bar__info">{{ page() + 1 }} / {{ totalPages() }}</span>
          <button
            type="button"
            class="pub-pag-bar__btn"
            [disabled]="page() >= totalPages() - 1"
            (click)="pageChange.emit(page() + 1)"
          >
            <span>{{ 'common.next' | translate }}</span>
            <span class="material-symbols-outlined" aria-hidden="true">chevron_right</span>
          </button>
        </nav>
      }
    </div>
  `,
  styles: [
    `
      .pub-pag-bar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 0.75rem;
      }

      .pub-pag-bar--centered {
        justify-content: center;
      }

      .pub-pag-bar__meta {
        display: flex;
        align-items: center;
        gap: 1rem;
        flex-wrap: wrap;
      }

      .pub-pag-bar__total {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-variant-numeric: tabular-nums;
      }

      .pub-pag-bar__size-label {
        display: flex;
        align-items: center;
        gap: 0.375rem;
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 700;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .pub-pag-bar__size-select {
        padding: 0.25rem 0.5rem;
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-container-low, var(--mat-sys-surface-container-low));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-family: inherit;
        cursor: pointer;
      }

      .pub-pag-bar__nav {
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }

      .pub-pag-bar__btn {
        display: inline-flex;
        align-items: center;
        gap: 0.25rem;
        padding: 0 0.75rem;
        min-height: var(--tch-touch-target, 48px);
        border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
        border-radius: var(--tch-radius-control, 8px);
        background: var(--tch-color-surface-container-lowest, var(--mat-sys-surface));
        color: var(--tch-color-primary, var(--mat-sys-primary));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-weight: 800;
        font-family: inherit;
        cursor: pointer;
      }

      .pub-pag-bar__btn:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      .pub-pag-bar__info {
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
        font-size: var(--tch-font-size-label-sm, 0.75rem);
        font-variant-numeric: tabular-nums;
      }
    `,
  ],
})
export class PublicPaginationBarComponent {
  readonly page = input.required<number>();
  readonly totalPages = input.required<number>();
  readonly size = input(20);
  readonly total = input(0);
  readonly sizeOptions = input<readonly number[]>([10, 20, 50]);
  readonly showMeta = input(true);

  readonly pageChange = output<number>();
  readonly sizeChange = output<number>();
}
