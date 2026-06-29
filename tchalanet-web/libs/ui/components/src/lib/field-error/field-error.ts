import { AbstractControl } from '@angular/forms';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'tch-field-error',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (displayMessage()) { <p class="tch-field-error" role="alert">{{ displayMessage() }}</p> }`,
  styles: [`
    :host {
      --comp-field-error-fg: var(--tch-color-error);
      --comp-field-error-font-size: var(--tch-font-size-label-sm);
    }
    .tch-field-error { margin: .25rem 0 0; color: var(--comp-field-error-fg); font-size: var(--comp-field-error-font-size); }
  `],
})
export class TchFieldError {
  readonly message = input('');
  readonly control = input<AbstractControl | null>(null);

  readonly displayMessage = computed(() => this.message() || controlErrorMessage(this.control()));
}

function controlErrorMessage(control: AbstractControl | null): string {
  if (!control || !control.touched || !control.errors) return '';

  const server = control.errors['server'];
  if (isMessageCarrier(server)) return server.message;

  if (control.errors['required']) return 'Ce champ est obligatoire.';
  if (control.errors['email']) return 'Adresse courriel invalide.';
  if (control.errors['minlength']) return 'La valeur est trop courte.';
  if (control.errors['maxlength']) return 'La valeur est trop longue.';

  return '';
}

function isMessageCarrier(value: unknown): value is { readonly message: string } {
  return typeof value === 'object' &&
    value !== null &&
    'message' in value &&
    typeof (value as { message?: unknown }).message === 'string';
}
