import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'tch-admin-data-toolbar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="data-toolbar">
      @if (searchPlaceholder()) {
        <div class="data-toolbar__search">
          <span class="data-toolbar__search-icon material-symbols-outlined" aria-hidden="true">
            search
          </span>
          <input
            class="data-toolbar__search-input"
            type="search"
            [placeholder]="searchPlaceholder()!"
            [value]="searchValue()"
            (input)="onSearch($event)"
          />
        </div>
      }
      <div class="data-toolbar__actions">
        <ng-content />
      </div>
    </div>
  `,
  styles: [
    `
      .data-toolbar {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        flex-wrap: wrap;
      }

      .data-toolbar__search {
        position: relative;
        flex: 1;
        min-width: 200px;
        max-width: 320px;
      }

      .data-toolbar__search-icon {
        position: absolute;
        left: 0.75rem;
        top: 50%;
        transform: translateY(-50%);
        font-size: 1.125rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        font-family: 'Material Symbols Outlined';
        pointer-events: none;
      }

      .data-toolbar__search-input {
        width: 100%;
        padding: 0.5rem 0.75rem 0.5rem 2.25rem;
        border: 1px solid var(--tch-color-outline-variant, #c8c5d0);
        border-radius: 9999px;
        background: var(--tch-color-surface-container-low, #f3f3f6);
        color: var(--tch-color-on-surface, #1a1c1e);
        font-size: 0.875rem;
        outline: none;
        transition: border-color 150ms ease;
        box-sizing: border-box;
      }

      .data-toolbar__search-input:focus {
        border-color: var(--tch-color-primary, #020135);
      }

      .data-toolbar__actions {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        flex-shrink: 0;
      }
    `,
  ],
})
export class AdminDataToolbarComponent {
  readonly searchPlaceholder = input<string | null>(null);
  readonly searchValue = input('');
  readonly searchChange = output<string>();

  onSearch(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.searchChange.emit(value);
  }
}
