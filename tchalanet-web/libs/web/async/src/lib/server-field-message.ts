import { AbstractControl } from '@angular/forms';

/**
 * Message d'erreur serveur posé sur un contrôle (`errors['server'].message`) —
 * copie unique, alimente `tch-field-error`. Remplace les helpers privés des pages.
 */
export function serverFieldMessage(control: AbstractControl | null): string {
  const server = control?.errors?.['server'];
  return typeof server === 'object' &&
    server !== null &&
    'message' in server &&
    typeof (server as { message?: unknown }).message === 'string'
    ? (server as { message: string }).message
    : '';
}
