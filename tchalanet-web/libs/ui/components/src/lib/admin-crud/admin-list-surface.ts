import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  ViewEncapsulation,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Subject, debounceTime, distinctUntilChanged, filter } from 'rxjs';

export interface AdminListStatusOption {
  readonly value: string;
  readonly label: string;
  readonly disabled?: boolean;
}

@Component({
  selector: 'tch-admin-list-surface',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  imports: [MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  template: `
    <section class="tch-admin-list-surface">
      <div class="tch-admin-list-surface__toolbar">
        <mat-form-field appearance="outline" class="tch-admin-list-surface__search" subscriptSizing="dynamic">
          <mat-icon matPrefix>search</mat-icon>
          <mat-label>{{ searchLabel() }}</mat-label>
          <input
            matInput
            type="search"
            [placeholder]="searchPlaceholder()"
            [value]="searchValue()"
            (input)="onSearchInput($event)"
          />
        </mat-form-field>

        @if (hasFilters() && filtersDisplay() === 'panel') {
          <button
            mat-stroked-button
            type="button"
            class="tch-admin-list-surface__filter-toggle"
            [class.tch-admin-list-surface__filter-toggle--active]="filtersOpen()"
            [attr.aria-expanded]="filtersOpen()"
            (click)="toggleFilters()"
          >
            <mat-icon>filter_list</mat-icon>
            {{ filterToggleLabel() }}
          </button>
        }

        @if (hasFilters() && (filtersDisplay() === 'inline' || filtersOpen())) {
          <div class="tch-admin-list-surface__filters">
            @if (statusOptions().length > 0) {
              <mat-form-field appearance="outline" class="tch-admin-list-surface__status" subscriptSizing="dynamic">
                <mat-select
                  panelClass="tch-admin-list-surface__status-panel"
                  [attr.aria-label]="statusLabel()"
                  [value]="statusValue()"
                  (valueChange)="statusChange.emit($event)"
                >
                  <mat-option value="">{{ allStatusesLabel() }}</mat-option>
                  @for (option of statusOptions(); track option.value) {
                    <mat-option [value]="option.value" [disabled]="option.disabled">
                      {{ option.label }}
                    </mat-option>
                  }
                </mat-select>
              </mat-form-field>
            }

            <ng-content select="[list-filters]" />

            @if (showReset()) {
              <button mat-button type="button" class="tch-admin-list-surface__reset" (click)="resetFilters.emit()">
                <mat-icon>filter_alt_off</mat-icon>
                {{ resetLabel() }}
              </button>
            }
          </div>
        }

        <div class="tch-admin-list-surface__actions">
          <ng-content select="[list-actions]" />
        </div>
      </div>

      <div class="tch-admin-list-surface__content">
        <ng-content select="[list-content]" />
      </div>

      <div class="tch-admin-list-surface__footer">
        <ng-content select="[list-footer]" />
      </div>
    </section>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .tch-admin-list-surface {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .tch-admin-list-surface__toolbar,
      .tch-admin-list-surface__filters,
      .tch-admin-list-surface__footer {
        display: flex;
        align-items: stretch;
        gap: 0.75rem;
        flex-wrap: wrap;
      }

      .tch-admin-list-surface__search {
        flex: 1 1 100%;
        max-width: none;
      }

      .tch-admin-list-surface__status {
        width: 100%;
        max-width: none;
      }

      .tch-admin-list-surface__filter-toggle {
        min-height: var(--tch-size-touch-target, 48px);
        border-color: var(--tch-color-outline-variant, #c7c5d0);
        color: var(--tch-color-on-surface-variant, #46464f);
        background: var(--tch-color-surface-container-lowest, #fff);
        transition:
          background-color 150ms ease,
          border-color 150ms ease,
          color 150ms ease,
          transform 150ms ease;
      }

      .tch-admin-list-surface__filter-toggle:hover {
        border-color: var(--tch-color-outline, #777680);
        background: var(--tch-color-surface-container-low, #f3f3f6);
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .tch-admin-list-surface__filter-toggle:focus-visible {
        outline: 3px solid color-mix(in srgb, var(--tch-color-secondary, #745b00) 24%, transparent);
        outline-offset: 2px;
      }

      .tch-admin-list-surface__filter-toggle:active {
        transform: scale(0.98);
      }

      .tch-admin-list-surface__filter-toggle--active {
        border-color: var(--tch-color-secondary, #745b00);
        background: var(--tch-color-secondary-container, #fecb02);
        color: var(--tch-color-on-secondary-container, #241a00);
      }

      .tch-admin-list-surface__filter-toggle--active:hover {
        background: color-mix(in srgb, var(--tch-color-secondary-container, #fecb02) 86%, white);
        border-color: var(--tch-color-secondary, #745b00);
      }

      .tch-admin-list-surface__actions {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        width: 100%;
        justify-content: flex-start;
      }

      .tch-admin-list-surface__filters {
        width: 100%;
        padding: 0.875rem;
        border: 1px solid var(--tch-color-outline-variant);
        border-radius: var(--tch-radius-lg, 0.75rem);
        background: var(--tch-color-surface-container-lowest);
      }

      .tch-admin-list-surface__reset {
        min-height: var(--tch-size-touch-target, 48px);
      }

      .tch-admin-list-surface__status-panel {
        --mat-option-selected-state-label-text-color: var(--tch-color-on-primary-fixed, #141545);
        --mdc-list-list-item-label-text-color: var(--tch-color-on-surface, #1a1c1e);
        --mdc-list-list-item-hover-label-text-color: var(--tch-color-on-secondary-container, #241a00);
        --mdc-list-list-item-focus-label-text-color: var(--tch-color-on-secondary-container, #241a00);
        --mdc-list-list-item-selected-label-text-color: var(--tch-color-primary, #1a1b4b);
        --mdc-list-list-item-hover-state-layer-color: var(--tch-color-secondary-container, #fecb02);
        --mdc-list-list-item-hover-state-layer-opacity: 0.34;
        --mdc-list-list-item-focus-state-layer-color: var(--tch-color-secondary-container, #fecb02);
        --mdc-list-list-item-focus-state-layer-opacity: 0.44;
        --mat-option-hover-state-layer-color: color-mix(
          in srgb,
          var(--tch-color-secondary-container, #fecb02) 36%,
          transparent
        );
        --mat-option-focus-state-layer-color: color-mix(
          in srgb,
          var(--tch-color-secondary-container, #fecb02) 44%,
          transparent
        );
        --mat-option-selected-state-layer-color: color-mix(
          in srgb,
          var(--tch-color-primary-container, #141545) 12%,
          transparent
        );
        border-radius: var(--tch-radius-lg, 0.75rem);
        background: var(--tch-color-surface-container-lowest, #fff);
      }

      .tch-admin-list-surface__status-panel .mat-mdc-option {
        min-height: var(--tch-size-touch-target, 48px);
        color: var(--tch-color-on-surface, #1a1c1e);
        font-weight: 500;
      }

      .tch-admin-list-surface__status-panel .mat-mdc-option:hover:not(.mdc-list-item--disabled) {
        background: color-mix(
          in srgb,
          var(--tch-color-secondary-container, #fecb02) 40%,
          var(--tch-color-surface-container-lowest, #fff)
        );
        color: var(--tch-color-on-secondary-container, #241a00);
      }

      .tch-admin-list-surface__status-panel .mat-mdc-option.mdc-list-item--selected:not(.mdc-list-item--disabled) {
        background: color-mix(
          in srgb,
          var(--tch-color-primary-container, #141545) 14%,
          var(--tch-color-surface-container-lowest, #fff)
        );
        color: var(--tch-color-primary, #1a1b4b);
        font-weight: 800;
      }

      .tch-admin-list-surface__status-panel
        .mat-mdc-option:hover:not(.mdc-list-item--disabled)
        .mdc-list-item__primary-text {
        color: var(--tch-color-on-secondary-container, #241a00);
      }

      .tch-admin-list-surface__status-panel
        .mat-mdc-option.mdc-list-item--selected:not(.mdc-list-item--disabled)
        .mdc-list-item__primary-text {
        color: var(--tch-color-primary, #1a1b4b);
      }

      .tch-admin-list-surface__status-panel
        .mat-mdc-option.mdc-list-item--selected:not(.mdc-list-item--disabled)
        .mat-pseudo-checkbox {
        color: var(--tch-color-primary, #1a1b4b);
      }

      .tch-admin-list-surface__content {
        min-width: 0;
      }

      .tch-admin-list-surface__footer {
        justify-content: space-between;
        padding-top: 0.75rem;
        border-top: 1px solid var(--tch-color-outline-variant);
      }

      .tch-admin-list-surface__footer:empty {
        display: none;
      }

      :host ::ng-deep .tch-admin-list-surface__search .mat-mdc-text-field-wrapper,
      :host ::ng-deep .tch-admin-list-surface__status .mat-mdc-text-field-wrapper {
        border-radius: var(--tch-radius-lg, 0.75rem);
        background: var(--tch-color-surface-container-low, #f3f3f6);
        box-shadow: inset 0 0 0 1px var(--tch-color-outline-variant, #c7c5d0);
        transition:
          background-color 150ms ease,
          box-shadow 150ms ease;
      }

      :host ::ng-deep .tch-admin-list-surface__search:hover .mat-mdc-text-field-wrapper,
      :host ::ng-deep .tch-admin-list-surface__status:hover .mat-mdc-text-field-wrapper {
        background: var(--tch-color-surface-container, #edeef1);
        box-shadow: inset 0 0 0 1px var(--tch-color-outline, #777680);
      }

      :host ::ng-deep .tch-admin-list-surface__search.mat-focused .mat-mdc-text-field-wrapper,
      :host ::ng-deep .tch-admin-list-surface__status.mat-focused .mat-mdc-text-field-wrapper {
        background: var(--tch-color-surface-container-lowest, #fff);
        box-shadow:
          inset 0 0 0 2px var(--tch-color-secondary, #745b00),
          0 0 0 4px color-mix(in srgb, var(--tch-color-secondary-container, #fecb02) 30%, transparent);
      }

      :host ::ng-deep .tch-admin-list-surface__search .mat-mdc-form-field-icon-prefix,
      :host ::ng-deep .tch-admin-list-surface__status .mat-mdc-select-arrow,
      :host ::ng-deep .tch-admin-list-surface__status .mat-mdc-select-value {
        color: var(--tch-color-on-surface-variant, #46464f);
      }

      :host ::ng-deep .tch-admin-list-surface__search.mat-focused .mat-mdc-form-field-icon-prefix,
      :host ::ng-deep .tch-admin-list-surface__status.mat-focused .mat-mdc-select-arrow,
      :host ::ng-deep .tch-admin-list-surface__status.mat-focused .mat-mdc-select-value {
        color: var(--tch-color-secondary, #745b00);
      }

      :host ::ng-deep .tch-admin-list-surface__search .mdc-notched-outline,
      :host ::ng-deep .tch-admin-list-surface__status .mdc-notched-outline {
        display: none;
      }

      @media (min-width: 600px) {
        .tch-admin-list-surface__toolbar {
          align-items: center;
          flex-wrap: nowrap;
        }

        .tch-admin-list-surface__search {
          flex: 0 1 21rem;
        }

        .tch-admin-list-surface__filters {
          width: auto;
          padding: 0;
          border: 0;
          background: transparent;
          align-items: center;
          flex: 0 0 auto;
        }

        .tch-admin-list-surface__status {
          width: 13rem;
        }

        .tch-admin-list-surface__actions {
          width: auto;
          margin-left: auto;
        }
      }
    `,
  ],
})
export class AdminListSurface implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchInput$ = new Subject<string>();

  readonly searchLabel = input('Recherche');
  readonly searchPlaceholder = input('Rechercher...');
  readonly searchValue = input('');
  readonly searchMinLength = input(3);
  readonly searchDebounceMs = input(350);
  readonly filterToggleLabel = input('Filtres');
  readonly filtersDisplay = input<'panel' | 'inline'>('panel');
  readonly filtersExpanded = input(false);
  readonly statusLabel = input('Statut');
  readonly statusValue = input('');
  readonly statusOptions = input<readonly AdminListStatusOption[]>([]);
  readonly allStatusesLabel = input('Tous les statuts');
  readonly resetLabel = input('Réinitialiser');
  readonly showReset = input(true);

  readonly searchChange = output<string>();
  readonly statusChange = output<string>();
  readonly resetFilters = output<void>();
  readonly filtersExpandedChange = output<boolean>();

  readonly filtersOpen = signal(false);
  readonly hasFilters = computed(() => this.statusOptions().length > 0);

  ngOnInit(): void {
    this.filtersOpen.set(this.filtersExpanded());

    this.searchInput$.pipe(
      debounceTime(this.searchDebounceMs()),
      distinctUntilChanged(),
      filter(value => value.length === 0 || value.length >= this.searchMinLength()),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(value => this.searchChange.emit(value));
  }

  onSearchInput(event: Event): void {
    this.searchInput$.next((event.target as HTMLInputElement).value.trim());
  }

  toggleFilters(): void {
    this.filtersOpen.update(open => {
      const next = !open;
      this.filtersExpandedChange.emit(next);
      return next;
    });
  }
}
