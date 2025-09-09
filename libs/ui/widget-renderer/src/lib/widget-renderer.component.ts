// libs/web/widgets/src/lib/widget-renderer.component.ts
import {
  ChangeDetectionStrategy,
  Component,
  EnvironmentInjector,
  inject,
  input,
  OnInit,
  ViewContainerRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { PageElement } from '@tchl/types';
import { TCH_WIDGET_REGISTRY, WidgetFactory } from '@tchl/web/widgets';

@Component({
  selector: 'tchl-widget-renderer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ng-container #host></ng-container>
    @if (error; ) {
    <div class="widget-error"><strong>Unknown widget:</strong> {{ error }}</div>
    }
  `,
  styles: [
    `
      .widget-error {
        border: 1px dashed var(--color-outline);
        background: color-mix(in srgb, var(--color-surface-container) 85%, white);
        border-radius: var(--radius);
        padding: 12px;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WidgetRendererComponent implements OnInit {
  el = input.required<PageElement>();

  private host = inject(ViewContainerRef);
  private injector = inject(EnvironmentInjector);
  private registries =
    inject<Record<string, WidgetFactory>[]>(TCH_WIDGET_REGISTRY, { optional: true }) ?? [];
  error: string | null = null;

  async ngOnInit() {
    const w = this.el();
    const factory = this.lookup(w.component);
    if (!factory) {
      this.error = w.component;
      return;
    }
    const cmp = await Promise.resolve(factory(w));

    this.host.clear();
    const ref = this.host.createComponent(cmp, { environmentInjector: this.injector });

    if ('props' in ref.instance) ref.setInput('props', (w as any).properties);
    if ('properties' in ref.instance) ref.setInput('properties', (w as any).properties);
  }

  private lookup(name: string): WidgetFactory | null {
    for (const reg of this.registries) if (reg[name]) return reg[name];
    return null;
  }
}
