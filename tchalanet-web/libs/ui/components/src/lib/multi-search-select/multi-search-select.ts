import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  forwardRef,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, FormControl, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Observable, catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';

import { TchSearchOption } from '../search-select/search-select';

@Component({
  selector: 'tch-multi-search-select',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TchMultiSearchSelect),
      multi: true,
    },
  ],
  template: `
    <mat-form-field appearance="outline" class="tch-multi-search-select">
      <mat-label>{{ label() }}</mat-label>
      <mat-chip-grid #chipGrid [attr.aria-label]="label()">
        @for (item of selected(); track item.id) {
          <mat-chip-row (removed)="remove(item)">
            <span class="tch-multi-search-select__chip-title">{{ item.title }}</span>
            @if (item.badge) {
              <span class="tch-multi-search-select__chip-badge">{{ item.badge }}</span>
            }
            <button matChipRemove type="button" [attr.aria-label]="removeLabel()">
              <span class="material-symbols-outlined">close</span>
            </button>
          </mat-chip-row>
        }
        <input
          matInput
          [formControl]="query"
          [matChipInputFor]="chipGrid"
          [matAutocomplete]="auto"
          [placeholder]="placeholder()"
          autocomplete="off"
        />
      </mat-chip-grid>
      @if (loading()) {
        <mat-spinner matSuffix diameter="18" />
      } @else if (query.value) {
        <button
          mat-icon-button
          matSuffix
          type="button"
          (click)="clearQuery()"
          [attr.aria-label]="clearLabel()"
        >
          <span class="material-symbols-outlined">close</span>
        </button>
      }
      @if (hint()) {
        <mat-hint>{{ hint() }}</mat-hint>
      }
      @if (error()) {
        <mat-error>{{ error() }}</mat-error>
      }

      <mat-autocomplete #auto="matAutocomplete" (optionSelected)="add($event.option.value)">
        @for (option of visibleOptions(); track option.id) {
          <mat-option [value]="option" [disabled]="option.disabled">
            <span class="tch-multi-search-select__option">
              <span class="material-symbols-outlined tch-multi-search-select__option-icon">
                {{ option.icon || icon() || 'search' }}
              </span>
              <span class="tch-multi-search-select__copy">
                <span class="tch-multi-search-select__title">{{ option.title }}</span>
                @if (option.subtitle) {
                  <span class="tch-multi-search-select__subtitle">{{ option.subtitle }}</span>
                }
              </span>
              @if (option.badge) {
                <span class="tch-multi-search-select__badge">{{ option.badge }}</span>
              }
            </span>
          </mat-option>
        }
        @if (!loading() && visibleOptions().length === 0 && query.value) {
          <mat-option disabled>{{ emptyLabel() }}</mat-option>
        }
      </mat-autocomplete>
    </mat-form-field>
  `,
  styles: [
    `
      :host {
        --comp-multi-search-bg: var(--tch-color-surface);
        --comp-multi-search-focus: var(--tch-color-secondary);
        --comp-multi-search-focus-ring: color-mix(in srgb, var(--tch-color-secondary-container) 50%, transparent);
        display: block;
      }

      .tch-multi-search-select {
        width: 100%;
        font-family: var(--tch-font-family, 'Plus Jakarta Sans', system-ui, sans-serif);
      }

      :host ::ng-deep .tch-multi-search-select .mat-mdc-text-field-wrapper {
        min-height: var(--tch-touch-target, 48px);
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--comp-multi-search-bg);
      }

      :host ::ng-deep .tch-multi-search-select.mat-focused .mat-mdc-text-field-wrapper {
        box-shadow: 0 0 0 4px var(--comp-multi-search-focus-ring);
      }

      :host ::ng-deep .tch-multi-search-select.mat-focused .mdc-notched-outline__leading,
      :host ::ng-deep .tch-multi-search-select.mat-focused .mdc-notched-outline__notch,
      :host ::ng-deep .tch-multi-search-select.mat-focused .mdc-notched-outline__trailing {
        border-color: var(--comp-multi-search-focus);
        border-width: 2px;
      }

      .tch-multi-search-select__chip-title {
        font-weight: 800;
      }

      .tch-multi-search-select__chip-badge {
        margin-left: 0.35rem;
        color: var(--tch-color-on-surface-variant);
        font-size: 0.68rem;
        font-weight: 800;
        text-transform: uppercase;
      }

      .tch-multi-search-select__option {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        width: 100%;
        min-width: 0;
      }

      .tch-multi-search-select__option-icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 2rem;
        height: 2rem;
        border-radius: var(--tch-radius-md, 8px);
        background: var(--tch-color-surface-container);
        color: var(--tch-color-on-primary-fixed, #141545);
        font-size: 1.1rem;
        flex: 0 0 auto;
      }

      .tch-multi-search-select__copy {
        display: grid;
        min-width: 0;
      }

      .tch-multi-search-select__title {
        overflow: hidden;
        color: var(--tch-color-on-surface);
        font-weight: 800;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .tch-multi-search-select__subtitle {
        overflow: hidden;
        color: var(--tch-color-on-surface-variant);
        font-size: 0.75rem;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .tch-multi-search-select__badge {
        margin-left: auto;
        padding: 0.125rem 0.45rem;
        border-radius: var(--tch-radius-pill, 999px);
        background: var(--tch-color-surface-container-high);
        color: var(--tch-color-on-surface-variant);
        font-size: 0.68rem;
        font-weight: 800;
        text-transform: uppercase;
      }

      .material-symbols-outlined {
        font-family: 'Material Symbols Outlined';
      }
    `,
  ],
})
export class TchMultiSearchSelect implements ControlValueAccessor {
  private readonly destroyRef = inject(DestroyRef);

  readonly label = input('Rechercher');
  readonly placeholder = input('');
  readonly hint = input('');
  readonly error = input('');
  readonly icon = input('search');
  readonly clearLabel = input('Effacer la recherche');
  readonly removeLabel = input('Retirer');
  readonly emptyLabel = input('Aucun résultat');
  readonly minChars = input(2);
  readonly maxSelected = input<number | null>(null);
  readonly searchFn = input<(query: string) => Observable<readonly TchSearchOption[]>>(() => of([]));

  readonly valueChange = output<readonly TchSearchOption[]>();

  readonly query = new FormControl('');
  readonly options = signal<readonly TchSearchOption[]>([]);
  readonly visibleOptions = signal<readonly TchSearchOption[]>([]);
  readonly selected = signal<readonly TchSearchOption[]>([]);
  readonly loading = signal(false);

  private onChange: (value: readonly TchSearchOption[]) => void = () => {};
  private onTouched: () => void = () => {};

  constructor() {
    this.query.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        switchMap(value => {
          const query = (value ?? '').trim();
          if (query.length < this.minChars()) {
            this.loading.set(false);
            return of<readonly TchSearchOption[]>([]);
          }
          this.loading.set(true);
          return this.searchFn()(query).pipe(catchError(() => of<readonly TchSearchOption[]>([])));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(options => {
        this.options.set(options);
        this.visibleOptions.set(this.filterAlreadySelected(options));
        this.loading.set(false);
      });
  }

  writeValue(value: readonly TchSearchOption[] | null): void {
    this.selected.set(value ?? []);
    this.visibleOptions.set(this.filterAlreadySelected(this.options()));
  }

  registerOnChange(fn: (value: readonly TchSearchOption[]) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    if (disabled) {
      this.query.disable({ emitEvent: false });
    } else {
      this.query.enable({ emitEvent: false });
    }
  }

  add(option: TchSearchOption): void {
    if (option.disabled || this.selected().some(item => item.id === option.id)) return;
    const max = this.maxSelected();
    if (max !== null && this.selected().length >= max) return;
    this.selected.update(items => [...items, option]);
    this.query.setValue('', { emitEvent: false });
    this.options.set([]);
    this.visibleOptions.set([]);
    this.emitValue();
    this.onTouched();
  }

  remove(option: TchSearchOption): void {
    this.selected.update(items => items.filter(item => item.id !== option.id));
    this.visibleOptions.set(this.filterAlreadySelected(this.options()));
    this.emitValue();
    this.onTouched();
  }

  clearQuery(): void {
    this.query.setValue('', { emitEvent: false });
    this.options.set([]);
    this.visibleOptions.set([]);
  }

  private emitValue(): void {
    const value = this.selected();
    this.onChange(value);
    this.valueChange.emit(value);
  }

  private filterAlreadySelected(options: readonly TchSearchOption[]): readonly TchSearchOption[] {
    const selectedIds = new Set(this.selected().map(item => item.id));
    return options.filter(option => !selectedIds.has(option.id));
  }
}
