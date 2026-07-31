import {
  afterNextRender,
  booleanAttribute,
  Directive,
  ElementRef,
  effect,
  Input,
  Injector,
  inject,
  signal,
} from '@angular/core';

/**
 * Moves focus to a rendered feedback surface and keeps it visible.
 * The owner decides when the surface is blocking enough to request focus.
 */
@Directive({
  selector: '[tchFeedbackFocus]',
  standalone: true,
  host: {
    '[attr.tabindex]': 'focusRequested() ? -1 : null',
  },
})
export class TchFeedbackFocusDirective {
  private readonly element = inject(ElementRef<HTMLElement>);
  private readonly injector = inject(Injector);
  readonly focusRequested = signal(false);
  private readonly focusVersion = signal<unknown>(undefined);

  @Input({ transform: booleanAttribute })
  set tchFeedbackFocus(value: boolean) {
    this.focusRequested.set(value);
  }

  @Input()
  set tchFeedbackFocusKey(value: unknown) {
    this.focusVersion.set(value);
  }

  constructor() {
    effect(() => {
      if (!this.focusRequested()) return;
      this.focusVersion();

      afterNextRender(() => this.focusTarget(), { injector: this.injector });
    });
  }

  private focusTarget(): void {
    const element = this.element.nativeElement;
    if (!element.isConnected) return;

    element.scrollIntoView?.({ block: 'center', behavior: 'smooth' });
    element.focus({ preventScroll: true });
  }
}
