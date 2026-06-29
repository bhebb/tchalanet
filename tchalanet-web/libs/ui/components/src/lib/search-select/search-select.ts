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
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Observable, catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';

export interface TchSearchOption<T = unknown> {
  readonly id: string;
  readonly title: string;
  readonly subtitle?: string | null;
  readonly badge?: string | null;
  readonly icon?: string | null;
  readonly disabled?: boolean;
  readonly data?: T;
}

@Component({
  selector: 'tch-search-select',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TchSearchSelect),
      multi: true,
    },
  ],
  template: `
    <mat-form-field appearance="outline" class="tch-search-select">
      <mat-label>{{ label() }}</mat-label>
      @if (icon()) {
        <span matPrefix class="material-symbols-outlined tch-search-select__prefix">{{ icon() }}</span>
      }
      <input
        matInput
        [formControl]="query"
        [matAutocomplete]="auto"
        [placeholder]="placeholder()"
        autocomplete="off"
      />
      @if (loading()) {
        <mat-spinner matSuffix diameter="18" />
      } @else if (selected() || query.value) {
        <button
          mat-icon-button
          matSuffix
          type="button"
          (click)="clear()"
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

      <mat-autocomplete
        #auto="matAutocomplete"
        [displayWith]="displayOption"
        (optionSelected)="select($event.option.value)"
      >
        @for (option of options(); track option.id) {
          <mat-option [value]="option" [disabled]="option.disabled">
            <span class="tch-search-select__option">
              <span class="material-symbols-outlined tch-search-select__option-icon">
                {{ option.icon || icon() || 'search' }}
              </span>
              <span class="tch-search-select__copy">
                <span class="tch-search-select__title">{{ option.title }}</span>
                @if (option.subtitle) {
                  <span class="tch-search-select__subtitle">{{ option.subtitle }}</span>
                }
              </span>
              @if (option.badge) {
                <span class="tch-search-select__badge">{{ option.badge }}</span>
              }
            </span>
          </mat-option>
        }
        @if (!loading() && options().length === 0 && query.value) {
          <mat-option disabled>{{ emptyLabel() }}</mat-option>
        }
      </mat-autocomplete>
    </mat-form-field>
  `,
  styles: [
    `
      :host {
        --comp-search-bg: var(--tch-color-surface);
        --comp-search-border: var(--tch-color-outline-variant);
        --comp-search-focus: var(--tch-color-secondary);
        --comp-search-focus-ring: color-mix(in srgb, var(--tch-color-secondary-container) 50%, transparent);
        display: block;
      }

      .tch-search-select {
        width: 100%;
        font-family: var(--tch-font-family, 'Plus Jakarta Sans', system-ui, sans-serif);
      }

      .tch-search-select__prefix {
        margin-left: 0.25rem;
        margin-right: 0.5rem;
        color: var(--tch-color-outline);
        font-size: 1.25rem;
      }

      :host ::ng-deep .tch-search-select .mat-mdc-text-field-wrapper {
        min-height: var(--tch-touch-target, 48px);
        border-radius: var(--tch-radius-lg, 12px);
        background: var(--comp-search-bg);
      }

      :host ::ng-deep .tch-search-select.mat-focused .mat-mdc-text-field-wrapper {
        box-shadow: 0 0 0 4px var(--comp-search-focus-ring);
      }

      :host ::ng-deep .tch-search-select.mat-focused .mdc-notched-outline__leading,
      :host ::ng-deep .tch-search-select.mat-focused .mdc-notched-outline__notch,
      :host ::ng-deep .tch-search-select.mat-focused .mdc-notched-outline__trailing {
        border-color: var(--comp-search-focus);
        border-width: 2px;
      }

      .tch-search-select__option {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        width: 100%;
        min-width: 0;
      }

      .tch-search-select__option-icon {
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

      .tch-search-select__copy {
        display: grid;
        min-width: 0;
      }

      .tch-search-select__title {
        overflow: hidden;
        color: var(--tch-color-on-surface);
        font-weight: 800;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .tch-search-select__subtitle {
        overflow: hidden;
        color: var(--tch-color-on-surface-variant);
        font-size: 0.75rem;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .tch-search-select__badge {
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
export class TchSearchSelect implements ControlValueAccessor {
  private readonly destroyRef = inject(DestroyRef);

  readonly label = input('Rechercher');
  readonly placeholder = input('');
  readonly hint = input('');
  readonly error = input('');
  readonly icon = input('search');
  readonly clearLabel = input('Effacer');
  readonly emptyLabel = input('Aucun résultat');
  readonly minChars = input(2);
  readonly searchFn = input<(query: string) => Observable<readonly TchSearchOption[]>>(() => of([]));

  readonly valueChange = output<TchSearchOption | null>();

  readonly query = new FormControl('');
  readonly options = signal<readonly TchSearchOption[]>([]);
  readonly loading = signal(false);
  readonly selected = signal<TchSearchOption | null>(null);

  private onChange: (value: TchSearchOption | null) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  constructor() {
    this.query.valueChanges
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        tap(value => {
          const query = this.queryText(value);
          if (!this.selected() || query !== this.selected()?.title) {
            this.selected.set(null);
            this.emitValue(null);
          }
        }),
        switchMap(value => {
          const query = this.queryText(value);
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
        this.loading.set(false);
      });
  }

  readonly displayOption = (option: TchSearchOption | string | null): string => {
    if (option === null) return '';
    if (typeof option === 'string') return option;
    return option.title;
  };

  writeValue(value: TchSearchOption | null): void {
    this.selected.set(value);
    this.query.setValue(value?.title ?? '', { emitEvent: false });
  }

  registerOnChange(fn: (value: TchSearchOption | null) => void): void {
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

  select(option: TchSearchOption): void {
    this.selected.set(option);
    this.query.setValue(option.title, { emitEvent: false });
    this.options.set([]);
    this.emitValue(option);
    this.onTouched();
  }

  clear(): void {
    this.selected.set(null);
    this.query.setValue('', { emitEvent: false });
    this.options.set([]);
    this.emitValue(null);
    this.onTouched();
  }

  private emitValue(value: TchSearchOption | null): void {
    this.onChange(value);
    this.valueChange.emit(value);
  }

  private queryText(value: unknown): string {
    if (typeof value === 'string') return value.trim();
    if (value && typeof value === 'object' && 'title' in value) {
      const title = (value as { title?: unknown }).title;
      return typeof title === 'string' ? title.trim() : '';
    }
    return '';
  }
}
