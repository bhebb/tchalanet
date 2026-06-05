import {
  ChangeDetectionStrategy,
  Component,
  ComponentRef,
  EnvironmentInjector,
  ViewContainerRef,
  computed,
  effect,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { WidgetConfig, WidgetDynamicError } from '../../shared/types';
import { resolveWidget } from './widget-registry';

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
        <div class="widget-fallback widget-fallback--error" role="alert">
          {{ 'widget.error' | translate }}
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
    if (!resolveWidget(config.type)) {
      return { kind: 'unsupported', type: config.type };
    }
    return { kind: 'ok' };
  });

  readonly renderFailed = signal(false);
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
      const cmp = resolveWidget(this.config()?.type);
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
}
