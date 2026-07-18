import { AbstractControl, FormArray, FormGroup } from '@angular/forms';
import { WebAppError } from '@tch/api';
import { Subscription } from 'rxjs';

import { resolveErrorFeedbackCopy, TchTranslateLookup } from '../copy/error-feedback-copy';
import { TchErrorViewModel } from '../models/error-view-model';

export type FieldTargetMap = Readonly<Record<string, string>>;

export function errorsForTarget(
  errors: readonly WebAppError[],
  surface: WebAppError['surface'],
  target: string,
): readonly WebAppError[] {
  return errors.filter(error => error.surface === surface && error.target === target);
}

export function errorsForTargetPrefix(
  errors: readonly WebAppError[],
  surface: WebAppError['surface'],
  targetPrefix: string,
): readonly WebAppError[] {
  return errors.filter(
    error =>
      error.surface === surface &&
      error.target !== undefined &&
      error.target.startsWith(targetPrefix),
  );
}

export function applyServerFieldErrors(
  form: FormGroup,
  errors: readonly WebAppError[],
  fieldTargets: FieldTargetMap = {},
): readonly WebAppError[] {
  const unconsumed: WebAppError[] = [];

  for (const error of errors) {
    if (error.surface !== 'field') {
      unconsumed.push(error);
      continue;
    }

    const control = controlForError(form, error, fieldTargets);
    if (!control) {
      unconsumed.push(error);
      continue;
    }

    control.setErrors({
      ...(control.errors ?? {}),
      server: [...serverErrors(control.errors?.['server']), error],
    });
    control.markAsTouched();
  }

  return unconsumed;
}

export function clearServerFieldErrors(control: AbstractControl): void {
  const errors = control.errors;
  if (errors?.['server']) {
    const rest = { ...errors };
    delete rest['server'];
    control.setErrors(Object.keys(rest).length ? rest : null);
  }

  if (control instanceof FormGroup) {
    Object.values(control.controls).forEach(child => clearServerFieldErrors(child));
  }

  if (control instanceof FormArray) {
    control.controls.forEach(child => clearServerFieldErrors(child));
  }
}

/**
 * Clears a server violation only after the user changes the affected control. The caller owns the
 * subscription lifetime, normally through `DestroyRef.onDestroy`.
 */
export function clearServerFieldErrorsOnEdit(control: AbstractControl): Subscription {
  const subscriptions = new Subscription();

  if (control instanceof FormGroup) {
    Object.values(control.controls).forEach(child => subscriptions.add(clearServerFieldErrorsOnEdit(child)));
    return subscriptions;
  }

  if (control instanceof FormArray) {
    control.controls.forEach(child => subscriptions.add(clearServerFieldErrorsOnEdit(child)));
    return subscriptions;
  }

  subscriptions.add(control.valueChanges.subscribe(() => clearServerFieldErrors(control)));
  return subscriptions;
}

export function toErrorViewModel(
  error: WebAppError,
  copy: { title: string; message: string },
): TchErrorViewModel {
  return {
    title: copy.title,
    message: copy.message,
    severity: error.severity,
    surface: error.surface,
    placement: error.placement,
    code: error.code,
    status: error.status,
    requestId: error.requestId,
    traceId: error.traceId,
    spanId: error.spanId,
    errorId: error.errorId,
    source: error.source,
    target: error.target,
    field: error.field,
    retryable: error.retryable,
    dedupeKey: error.dedupeKey,
  };
}

export function withResolvedErrorCopy(
  error: WebAppError,
  translate: TchTranslateLookup,
): WebAppError {
  const copy = resolveErrorFeedbackCopy(error, translate);
  return {
    ...error,
    title: copy.title,
    message: copy.message,
  };
}

export function withResolvedErrorCopies(
  errors: readonly WebAppError[],
  translate: TchTranslateLookup,
): readonly WebAppError[] {
  return errors.map(error => withResolvedErrorCopy(error, translate));
}

function controlForError(
  form: FormGroup,
  error: WebAppError,
  fieldTargets: FieldTargetMap,
): AbstractControl | null {
  if (error.field) {
    const byField = form.get(error.field);
    if (byField) return byField;
  }

  if (error.target) {
    const field = fieldTargets[error.target];
    if (field) return form.get(field);

    const byTarget = form.get(error.target);
    if (byTarget) return byTarget;
  }

  return null;
}

function serverErrors(value: unknown): readonly WebAppError[] {
  if (Array.isArray(value)) return value.filter(isWebAppError);
  return isWebAppError(value) ? [value] : [];
}

function isWebAppError(value: unknown): value is WebAppError {
  return typeof value === 'object' && value !== null && 'code' in value && 'surface' in value;
}
