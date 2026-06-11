import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { isRecord, stringProp, WidgetConfig } from '@tch/page-model';
import { LabelPipe } from '@tch/page-model';

interface FormFieldDef {
  id: string;
  type: 'text' | 'textarea';
  labelKey: string;
  placeholderKey: string;
  required: boolean;
}

type SendState = 'idle' | 'sending' | 'sent' | 'error';

@Component({
  selector: 'tch-public-business-lead-form-widget',
  imports: [LabelPipe, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="pblead" [id]="sectionId()">
      <div class="pblead__header">
        <h2 class="pblead__title">{{ titleKey() | tchLabel }}</h2>
        @if (subtitleKey(); as sk) {
          <p class="pblead__subtitle">{{ sk | tchLabel }}</p>
        }
      </div>

      @if (sendState() === 'sent') {
        <div class="pblead__success" role="status">
          <span class="pblead__success-icon material-symbols-outlined">check_circle</span>
          <p>{{ sentLabelKey() | tchLabel }}</p>
        </div>
      } @else {
        <form class="pblead__form" [formGroup]="form" (ngSubmit)="submit()">
          <div class="pblead__fields">
            @for (field of fields(); track field.id) {
              <div class="pblead__field">
                <label class="pblead__label" [attr.for]="field.id">
                  {{ field.labelKey | tchLabel }}
                  @if (field.required) { <span class="pblead__required" aria-hidden="true">*</span> }
                </label>
                @if (field.type === 'textarea') {
                  <textarea
                    class="pblead__input pblead__input--textarea"
                    [id]="field.id"
                    [formControlName]="field.id"
                    [attr.placeholder]="field.placeholderKey | tchLabel"
                    rows="4"
                  ></textarea>
                } @else {
                  <input
                    class="pblead__input"
                    type="text"
                    [id]="field.id"
                    [formControlName]="field.id"
                    [attr.placeholder]="field.placeholderKey | tchLabel"
                  />
                }
              </div>
            }
          </div>

          @if (sendState() === 'error') {
            <p class="pblead__error" role="alert">{{ 'public.operator.form_error' | tchLabel }}</p>
          }

          <button type="submit" class="pblead__submit" [disabled]="sendState() === 'sending'">
            @if (sendState() === 'sending') {
              {{ sendingLabelKey() | tchLabel }}
            } @else {
              {{ submitLabelKey() | tchLabel }}
            }
          </button>
        </form>
      }
    </section>
  `,
  styles: [
    `
      @use 'breakpoints' as bp;

      .pblead {
        padding: 3rem 1.25rem;

        @include bp.up(medium) {
          padding: 4rem 2rem;
        }

        @include bp.up(expanded) {
          padding: 5rem clamp(2rem, 6vw, 5rem);
          display: grid;
          grid-template-columns: 1fr 2fr;
          gap: 4rem;
          align-items: start;
        }
      }

      .pblead__header {
        display: grid;
        gap: 0.75rem;
        margin-bottom: 2rem;

        @include bp.up(expanded) {
          margin-bottom: 0;
        }
      }

      .pblead__title {
        margin: 0;
        font-size: clamp(1.5rem, 3vw, 2rem);
        font-weight: 800;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pblead__subtitle {
        margin: 0;
        font-size: var(--tch-font-size-body-md, 1rem);
        line-height: 1.65;
        color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
      }

      .pblead__form {
        display: grid;
        gap: 2rem;
      }

      .pblead__fields {
        display: grid;
        gap: 1.25rem;

        @include bp.up(medium) {
          grid-template-columns: repeat(2, 1fr);
        }
      }

      .pblead__field {
        display: grid;
        gap: 0.5rem;
      }

      .pblead__field:has(textarea) {
        @include bp.up(medium) {
          grid-column: 1 / -1;
        }
      }

      .pblead__label {
        font-size: 0.875rem;
        font-weight: 600;
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      }

      .pblead__required {
        color: var(--tch-color-error, var(--mat-sys-error));
        margin-left: 0.125rem;
      }

      .pblead__input {
        width: 100%;
        box-sizing: border-box;
        padding: 0.875rem 1rem;
        border-radius: var(--tch-radius-md, 8px);
        border: 1.5px solid var(--tch-color-outline, var(--mat-sys-outline));
        background: var(--tch-color-surface, var(--mat-sys-surface));
        color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
        font-size: 0.9375rem;
        font-family: inherit;
        outline: none;
        transition: border-color 0.15s;

        &:focus {
          border-color: var(--tch-color-primary, var(--mat-sys-primary));
        }

        &::placeholder {
          color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
          opacity: 0.6;
        }
      }

      .pblead__input--textarea {
        resize: vertical;
        min-height: 7rem;
      }

      .pblead__error {
        margin: 0;
        font-size: 0.875rem;
        color: var(--tch-color-error, var(--mat-sys-error));
      }

      .pblead__submit {
        width: 100%;
        border: none;
        cursor: pointer;
        border-radius: var(--tch-radius-pill, 9999px);
        padding: 1rem 2rem;
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary, var(--mat-sys-on-primary));
        font-size: 1rem;
        font-weight: 600;
        font-family: inherit;
        transition: opacity 0.15s;

        &:hover:not(:disabled) { opacity: 0.85; }
        &:disabled { opacity: 0.5; cursor: not-allowed; }
      }

      .pblead__success {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 1.5rem;
        border-radius: var(--tch-radius-lg, 16px);
        background: var(--tch-color-secondary-container, var(--mat-sys-secondary-container));
        color: var(--tch-color-on-secondary-container, var(--mat-sys-on-secondary-container));
        font-weight: 600;
      }

      .pblead__success-icon {
        font-size: 1.5rem;
        flex-shrink: 0;
      }
    `,
  ],
})
export class PublicBusinessLeadFormWidget {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(FormBuilder);

  readonly config = input.required<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly widgetId = input<string>('');

  readonly sendState = signal<SendState>('idle');

  readonly sectionId = computed(() => stringProp(this.config(), 'id') ?? 'operator-demo-form');
  readonly titleKey = computed(() => stringProp(this.config(), 'titleKey') ?? '');
  readonly subtitleKey = computed(() => stringProp(this.config(), 'subtitleKey'));
  readonly submitLabelKey = computed(() => stringProp(this.config(), 'submitLabelKey') ?? 'public.operator.form_cta');
  readonly sendingLabelKey = computed(() => stringProp(this.config(), 'sendingLabelKey') ?? 'public.operator.form_sending');
  readonly sentLabelKey = computed(() => stringProp(this.config(), 'sentLabelKey') ?? 'public.operator.form_sent');

  readonly fields = computed<FormFieldDef[]>(() => {
    const raw = this.config().props?.['fields'];
    if (!Array.isArray(raw)) return [];
    return raw.filter(isRecord).map((f) => ({
      id: String(f['id'] ?? ''),
      type: f['type'] === 'textarea' ? 'textarea' : 'text',
      labelKey: String(f['labelKey'] ?? ''),
      placeholderKey: String(f['placeholderKey'] ?? ''),
      required: f['required'] === true,
    }));
  });

  readonly form = this.fb.nonNullable.group({} as Record<string, string>);

  constructor() {
    // Build form controls lazily once fields are available.
    // Angular signals run after construction, so we patch form in ngOnInit equivalent.
  }

  ngOnInit(): void {
    this.fields().forEach((f) => {
      this.form.addControl(f.id, this.fb.nonNullable.control('', f.required ? Validators.required : []));
    });
  }

  submit(): void {
    if (this.form.invalid || this.sendState() === 'sending') return;

    const submitAction = this.config().props?.['submitAction'];
    const path = isRecord(submitAction) ? String(submitAction['path'] ?? '') : '/api/v1/public/operator-leads';

    this.sendState.set('sending');
    this.http.post(path, this.form.value).subscribe({
      next: () => this.sendState.set('sent'),
      error: () => this.sendState.set('error'),
    });
  }
}
