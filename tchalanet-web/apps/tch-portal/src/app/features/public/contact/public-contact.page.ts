import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { CONTACT_INTENTS, ContactIntent } from './public-contact.model';
import { PublicContactService } from './public-contact.service';

type PageState = 'idle' | 'submitting' | 'success' | 'error';

@Component({
  selector: 'tch-public-contact-page',
  imports: [ReactiveFormsModule, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="contact">
      <p class="contact__eyebrow">{{ 'public.pages.contact.eyebrow' | translate }}</p>
      <h1>{{ 'public.pages.contact.title' | translate }}</h1>
      <p class="contact__lead">{{ 'public.pages.contact.description' | translate }}</p>

      @switch (state()) {
        @case ('success') {
          <div class="contact__feedback contact__feedback--ok" role="status">
            <p class="contact__feedback-title">{{ 'public.contact.success_title' | translate }}</p>
            <p>{{ 'public.contact.success_body' | translate }}</p>
            <button type="button" class="contact__btn-secondary" (click)="reset()">
              {{ 'public.contact.reset_cta' | translate }}
            </button>
          </div>
        }
        @case ('error') {
          <div class="contact__feedback contact__feedback--error" role="alert">
            <p class="contact__feedback-title">{{ 'public.contact.error_title' | translate }}</p>
            <p>{{ 'public.contact.error_body' | translate }}</p>
            <button type="button" class="contact__btn-secondary" (click)="reset()">
              {{ 'public.contact.reset_cta' | translate }}
            </button>
          </div>
        }
        @default {
          <form class="contact__form" [formGroup]="form" (ngSubmit)="submit()">

            <label class="contact__field">
              <span>{{ 'public.contact.intent_label' | translate }}</span>
              <select formControlName="intent">
                @for (intent of intents; track intent) {
                  <option [value]="intent">{{ 'public.contact.intents.' + intent | translate }}</option>
                }
              </select>
            </label>

            <label class="contact__field">
              <span>{{ 'public.contact.full_name_label' | translate }}</span>
              <input type="text" formControlName="fullName" autocomplete="name" />
            </label>

            <label class="contact__field">
              <span>{{ 'public.contact.phone_label' | translate }}</span>
              <input type="tel" formControlName="phone" autocomplete="tel" />
            </label>

            <label class="contact__field">
              <span>{{ 'public.contact.email_label' | translate }}</span>
              <input type="email" formControlName="email" autocomplete="email" />
            </label>

            <label class="contact__field">
              <span>{{ 'public.contact.organization_label' | translate }}</span>
              <input type="text" formControlName="organizationName" autocomplete="organization" />
            </label>

            <div class="contact__row">
              <label class="contact__field">
                <span>{{ 'public.contact.city_label' | translate }}</span>
                <input type="text" formControlName="city" autocomplete="address-level2" />
              </label>
              <label class="contact__field">
                <span>{{ 'public.contact.country_label' | translate }}</span>
                <input type="text" formControlName="country" autocomplete="country-name" />
              </label>
            </div>

            <label class="contact__field">
              <span>{{ 'public.contact.message_label' | translate }}</span>
              <textarea formControlName="message" rows="4"></textarea>
            </label>

            <label class="contact__consent">
              <input type="checkbox" formControlName="consentToContact" />
              <span>{{ 'public.contact.consent_label' | translate }}</span>
            </label>

            <button
              type="submit"
              class="contact__btn-primary"
              [disabled]="form.invalid || state() === 'submitting'"
            >
              {{ (state() === 'submitting' ? 'public.contact.submitting' : 'public.contact.submit_cta') | translate }}
            </button>

          </form>
        }
      }
    </section>
  `,
  styles: [`
    .contact {
      display: grid;
      gap: 1.25rem;
      width: min(100% - 2 * var(--tch-page-margin-mobile, 16px), 640px);
      margin: 0 auto;
      padding: clamp(2rem, 8vw, 4rem) 0;
    }
    .contact__eyebrow {
      margin: 0;
      color: var(--tch-color-primary, var(--mat-sys-primary));
      font-size: var(--tch-font-size-label-sm, 0.75rem);
      font-weight: 800;
      text-transform: uppercase;
    }
    h1, .contact__lead { margin: 0; }
    h1 {
      font-size: var(--tch-font-size-headline-lg, 2rem);
      line-height: var(--tch-line-height-headline-lg, 2.5rem);
      color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
    }
    .contact__lead {
      color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
    }
    .contact__form {
      display: grid;
      gap: 1rem;
    }
    .contact__field {
      display: grid;
      gap: 0.35rem;
      font-weight: 600;
      font-size: 0.875rem;
      color: var(--tch-color-on-surface-variant, var(--mat-sys-on-surface-variant));
    }
    .contact__field input,
    .contact__field select,
    .contact__field textarea {
      min-height: var(--tch-touch-target, 48px);
      padding: 0 0.875rem;
      border-radius: var(--tch-radius-control, 8px);
      border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      background: var(--tch-color-surface, var(--mat-sys-surface));
      color: var(--tch-color-on-surface, var(--mat-sys-on-surface));
      font-size: 1rem;
      font-family: inherit;
    }
    .contact__field textarea {
      min-height: unset;
      padding: 0.75rem 0.875rem;
      resize: vertical;
    }
    .contact__row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
    }
    .contact__consent {
      display: flex;
      align-items: flex-start;
      gap: 0.625rem;
      font-size: 0.875rem;
      cursor: pointer;
    }
    .contact__consent input { margin-top: 2px; flex-shrink: 0; }
    .contact__btn-primary {
      min-height: var(--tch-touch-target, 48px);
      padding: 0 1.5rem;
      border: 0;
      border-radius: var(--tch-radius-control, 8px);
      background: var(--tch-color-primary, var(--mat-sys-primary));
      color: var(--tch-color-primary-contrast, var(--mat-sys-on-primary));
      font-size: 1rem;
      font-weight: 700;
      cursor: pointer;
    }
    .contact__btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
    .contact__btn-secondary {
      min-height: 40px;
      padding: 0 1rem;
      border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
      border-radius: var(--tch-radius-control, 8px);
      background: transparent;
      color: var(--tch-color-primary, var(--mat-sys-primary));
      font-weight: 600;
      cursor: pointer;
    }
    .contact__feedback {
      display: grid;
      gap: 0.75rem;
      padding: 1.25rem;
      border-radius: var(--tch-radius-lg, 12px);
      border: 1px solid var(--tch-color-outline-variant, var(--mat-sys-outline-variant));
    }
    .contact__feedback-title { font-weight: 700; margin: 0; }
    .contact__feedback p { margin: 0; }
    .contact__feedback--ok { border-color: var(--tch-color-primary, var(--mat-sys-primary)); }
    .contact__feedback--error { border-color: var(--tch-color-error, var(--mat-sys-error)); color: var(--tch-color-error, var(--mat-sys-error)); }
  `],
})
export class PublicContactPage {
  private readonly fb = inject(FormBuilder);
  private readonly contactService = inject(PublicContactService);

  readonly state = signal<PageState>('idle');
  readonly intents: readonly ContactIntent[] = CONTACT_INTENTS;

  readonly form = this.fb.nonNullable.group({
    intent: ['REQUEST_DEMO' as ContactIntent, Validators.required],
    fullName: ['', Validators.required],
    phone: ['', Validators.required],
    email: ['', Validators.email],
    organizationName: [''],
    city: [''],
    country: [''],
    message: ['', Validators.required],
    consentToContact: [false, Validators.requiredTrue],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.state.set('submitting');

    const raw = this.form.getRawValue();
    this.contactService
      .submit({
        intent: raw.intent,
        fullName: raw.fullName,
        phone: raw.phone,
        email: raw.email || undefined,
        organizationName: raw.organizationName || undefined,
        city: raw.city || undefined,
        country: raw.country || undefined,
        message: raw.message,
        consentToContact: raw.consentToContact,
        sourcePage: window.location.pathname,
      })
      .subscribe({
        next: () => this.state.set('success'),
        error: () => this.state.set('error'),
      });
  }

  reset(): void {
    this.form.reset({ intent: 'REQUEST_DEMO', consentToContact: false });
    this.state.set('idle');
  }
}
