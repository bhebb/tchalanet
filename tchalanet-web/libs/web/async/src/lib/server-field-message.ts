import { AbstractControl } from '@angular/forms';

/**
 * Messages d'erreur serveur posés sur un contrôle. Le routeur conserve toutes les
 * erreurs d'un champ; les consommateurs historiques gardent le premier message.
 */
export function serverFieldMessage(control: AbstractControl | null): string {
  return serverFieldMessages(control)[0] ?? '';
}

export function serverFieldMessages(control: AbstractControl | null): readonly string[] {
  const server = control?.errors?.['server'];
  const values = Array.isArray(server) ? server : [server];
  return values.flatMap(value => isMessageCarrier(value) ? [value.message] : []);
}

function isMessageCarrier(value: unknown): value is { readonly message: string } {
  return typeof value === 'object' &&
    value !== null &&
    'message' in value &&
    typeof (value as { message?: unknown }).message === 'string';
}
