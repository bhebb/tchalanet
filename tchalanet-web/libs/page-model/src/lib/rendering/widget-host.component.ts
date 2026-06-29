import {
  ChangeDetectionStrategy,
  Component,
  ComponentRef,
  EnvironmentInjector,
  InjectionToken,
  Type,
  ViewContainerRef,
  computed,
  effect,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { WidgetConfig, WidgetDynamicError } from '../runtime/pagemodel.types';

export type WidgetRegistry = Readonly<Record<string, Type<unknown>>>;
export const WIDGET_REGISTRY = new InjectionToken<WidgetRegistry>('WIDGET_REGISTRY');

type HostState =
  | { readonly kind: 'ok' }
  | { readonly kind: 'invalid' }
  | { readonly kind: 'unsupported'; readonly type: string }
  | { readonly kind: 'error'; readonly error: WidgetDynamicError };

/**
 * Renders a single widget by resolving its component from the registry and binding only its own
 * config + dynamic payload + id. Failures stay local to the widget and never blank the page:
 * - missing id/type → invalid fallback;
 * - unknown type → unsupported fallback;
 * - a `dynamic.errors` entry for this id → widget-local error block;
 * - a thrown instantiation/render error → caught and shown as a local error block.
 */
@Component({
  selector: 'tch-widget-host',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ng-container #outlet />
    @switch (state().kind) {
      @case ('invalid') {
        <div class="widget-fallback widget-fallback--invalid" role="note">
          {{ 'widget.invalid' | translate }}
        </div>
      }
      @case ('unsupported') {
        <div class="widget-fallback" role="note">
          {{ 'widget.unsupported' | translate }}
        </div>
      }
      @case ('error') {
        <div
          class="widget-fallback widget-fallback--error"
          [attr.data-severity]="errorSeverity()"
          role="alert"
        >
          <strong>{{ 'widget.error' | translate }}</strong>
          @if (errorCode()) {
            <span class="widget-fallback__code">{{ errorCode() }}</span>
          }
        </div>
      }
    }
    @if (renderFailed()) {
      <div class="widget-fallback widget-fallback--error" role="alert">
        {{ 'widget.error' | translate }}
      </div>
    }
  `,
  styles: [
    `
      :host {
        display: block;
        min-width: 0;
      }

      .widget-fallback {
        border: 1px dashed var(--tch-color-outline, var(--mat-sys-outline-variant));
        background: var(--tch-color-surface, var(--mat-sys-surface-container));
        color: var(--tch-color-foreground, var(--mat-sys-on-surface));
        border-radius: var(--tch-radius-control, 8px);
        padding: 0.75rem 1rem;
        font-size: 0.875rem;
      }
      .widget-fallback--error {
        border-color: var(--tch-color-error, var(--mat-sys-error));
        color: var(--tch-color-error, var(--mat-sys-error));
      }
      .widget-fallback--error[data-severity='warn'] {
        border-color: var(--tch-color-warning, #9a6700);
        color: var(--tch-color-on-warning-container, #3d2b00);
        background: var(--tch-color-warning-container, #fff4ce);
      }
      .widget-fallback--error[data-severity='info'] {
        border-color: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-on-primary-container, var(--mat-sys-on-primary-container));
        background: var(--tch-color-primary-container, var(--mat-sys-primary-container));
      }
      .widget-fallback__code {
        display: block;
        margin-top: .25rem;
        font-size: .75rem;
        opacity: .75;
      }
    `,
  ],
})
export class WidgetHostComponent {
  readonly widgetId = input.required<string>();
  readonly config = input<WidgetConfig>();
  readonly dynamic = input<unknown>();
  readonly errors = input<readonly WidgetDynamicError[]>([]);

  private readonly outlet = viewChild('outlet', { read: ViewContainerRef });
  private readonly injector = inject(EnvironmentInjector);
  private readonly registry = inject(WIDGET_REGISTRY, { optional: true }) ?? {};

  private readonly localError = computed(() =>
    this.errors().find(e => e.widgetId === this.widgetId()),
  );

  readonly state = computed<HostState>(() => {
    if (this.localError()) {
      return { kind: 'error', error: this.localError() as WidgetDynamicError };
    }
    const config = this.config();
    if (!this.widgetId() || !config?.type) {
      return { kind: 'invalid' };
    }
    if (!this.resolveWidget(config.type)) {
      return { kind: 'unsupported', type: config.type };
    }
    return { kind: 'ok' };
  });

  readonly renderFailed = signal(false);
  readonly errorSeverity = computed(() => this.localError()?.severity ?? 'error');
  readonly errorCode = computed(() => this.localError()?.code ?? '');
  private ref: ComponentRef<unknown> | null = null;

  constructor() {
    effect(() => {
      const outlet = this.outlet();
      const state = this.state();
      if (!outlet) {
        return;
      }
      outlet.clear();
      this.ref = null;
      this.renderFailed.set(false);
      if (state.kind !== 'ok') {
        return;
      }
      const cmp = this.resolveWidget(this.config()?.type);
      if (!cmp) {
        return;
      }
      try {
        const ref = outlet.createComponent(cmp, { environmentInjector: this.injector });
        ref.setInput('config', this.config());
        ref.setInput('dynamic', this.dynamic());
        ref.setInput('widgetId', this.widgetId());
        this.ref = ref;
      } catch {
        // Containment: a widget failure must not blank the page — surface a local error block.
        outlet.clear();
        this.renderFailed.set(true);
      }
    });
  }

  private resolveWidget(type: string | undefined): Type<unknown> | null {
    return type ? this.registry[type] ?? null : null;
  }
}
