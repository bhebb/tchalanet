import { HttpInterceptorFn } from '@angular/common/http';

const requestIdHeader = 'X-Request-Id';

export const correlationRequestInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.headers.has(requestIdHeader)) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        [requestIdHeader]: createRequestId(),
      },
    }),
  );
};

function createRequestId(): string {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }

  return `web-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
