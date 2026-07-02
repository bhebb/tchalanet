import { Directive, TemplateRef, inject } from '@angular/core';

export interface TchAsyncReadyContext<T> {
  readonly $implicit: T;
}

/** Marque le template `ready` d'un `tch-async-view` ; `let-value` reçoit la valeur du resource. */
@Directive({
  selector: 'ng-template[tchAsyncReady]',
  standalone: true,
})
export class TchAsyncReadyDirective<T = unknown> {
  readonly templateRef = inject(TemplateRef) as TemplateRef<TchAsyncReadyContext<T>>;

  static ngTemplateContextGuard<T>(
    _dir: TchAsyncReadyDirective<T>,
    _ctx: unknown,
  ): _ctx is TchAsyncReadyContext<T> {
    return true;
  }
}
