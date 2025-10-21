// typescript
import {
  Directive,
  effect,
  EffectRef,
  inject,
  Input,
  OnDestroy,
  signal,
  TemplateRef,
  ViewContainerRef,
} from '@angular/core';
import { FeatureService } from '@tchl/feature';

@Directive({
  selector: '[tchFeature]',
})
export class FeatureDirective implements OnDestroy {
  private thenTemplateRef = signal<TemplateRef<any> | null>(inject(TemplateRef<any>));
  private elseTemplateRef = signal<TemplateRef<any> | null>(null);
  private flag = signal<string | boolean | undefined>(undefined);

  private features = inject(FeatureService);
  private vcr = inject(ViewContainerRef);
  private effectRef: EffectRef | null = null;

  constructor() {
    this.effectRef = effect(() => {
      const rawFlag = this.flag();
      const enabled = typeof rawFlag === 'string' ? this.features.isEnabled(rawFlag) : !!rawFlag;

      this.vcr.clear();
      if (enabled && this.thenTemplateRef()) {
        this.vcr.createEmbeddedView(this.thenTemplateRef()!);
      } else if (!enabled && this.elseTemplateRef()) {
        this.vcr.createEmbeddedView(this.elseTemplateRef()!);
      }
    });
  }

  @Input()
  set tchFeature(value: string | boolean) {
    this.flag.set(value);
  }

  @Input('tchFeatureThen')
  set tchFeatureThen(template: TemplateRef<any> | null) {
    this.thenTemplateRef.set(template);
  }

  @Input('tchFeatureElse')
  set tchFeatureElse(template: TemplateRef<any> | null) {
    this.elseTemplateRef.set(template);
  }

  ngOnDestroy(): void {
    if (this.effectRef) {
      this.effectRef.destroy();
      this.effectRef = null;
    }
    this.vcr.clear();
  }
}
