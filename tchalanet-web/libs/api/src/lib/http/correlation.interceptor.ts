import { HttpInterceptorFn } from '@angular/common/http';

const X_REQUEST_ID = 'X-Request-Id';

export const correlationRequestInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.headers.has(X_REQUEST_ID)) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: { [X_REQUEST_ID]: createRequestId() },
    }),
  );
};

function createRequestId(): string {
  if (globalThis.crypto?.randomUUID) {
    return `tch_req_${globalThis.crypto.randomUUID()}`;
  }

  return `tch_req_${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
