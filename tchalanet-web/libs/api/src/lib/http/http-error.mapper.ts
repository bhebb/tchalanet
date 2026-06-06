import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from '../contracts/api.types';

export function mapHttpErrorToProblemDetail(error: unknown): ProblemDetail {
  if (error instanceof HttpErrorResponse) {
    return mapHttpErrorResponse(error);
  }

  if (isProblemDetail(error)) {
    return error;
  }

  return {
    title: 'Unexpected error',
    status: 0,
    detail: error instanceof Error ? error.message : 'An unexpected error occurred.',
  };
}

function mapHttpErrorResponse(error: HttpErrorResponse): ProblemDetail {
  if (isProblemDetail(error.error)) {
    return error.error;
  }

  return {
    type: typeof error.error === 'string' ? undefined : (error.url ?? undefined),
    title: error.statusText || 'HTTP error',
    status: error.status,
    detail: toErrorDetail(error.error) ?? error.message,
    correlationId: error.headers.get('X-Request-Id') ?? undefined,
  };
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  if (!isRecord(value)) {
    return false;
  }

  return typeof value['title'] === 'string' && typeof value['status'] === 'number';
}

function toErrorDetail(value: unknown): string | undefined {
  if (typeof value === 'string') {
    return value;
  }

  if (isRecord(value) && typeof value['message'] === 'string') {
    return value['message'];
  }

  return undefined;
}

function isRecord(value: unknown): value is Readonly<Record<string, unknown>> {
  return typeof value === 'object' && value !== null;
}
