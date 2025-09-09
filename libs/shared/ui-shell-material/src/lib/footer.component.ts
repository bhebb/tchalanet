import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FooterModel } from 'shared/types';
import { UpperCasePipe } from '@angular/common';

@Component({
  selector: 'lib-footer',
  standalone: true,
  template: `
    <footer role="contentinfo" class="footer">
      <div class="container top">
        @if (model?.logoUrl) {
          <img [src]="model!.logoUrl!" alt="" class="logo" />
        }
        <div class="cols">
          @for (c of model?.columns ?? []; track c.title) {
            <section>
              <h3>{{ c.title }}</h3>
              <ul>
                @for (l of c.links; track l.label) {
                  <li><a [href]="l.href">{{ l.label }}</a></li>
                }
              </ul>
            </section>
          }
        </div>
      </div>
      <div class="container bottom">
        <div class="social">
          @for (s of model?.social ?? []; track s.type) {
            <a [href]="s.url" target="_blank" rel="noopener">{{ s.type | uppercase }}</a>
          }
        </div>
        <div class="legal">
          @for (l of model?.legal ?? []; track l.label) {
            <a [href]="l.href">{{ l.label }}</a>
          }
          @if (model?.note) {
            <span class="note">{{ model!.note }}</span>
          }
        </div>
      </div>
    </footer>
  `,
  styles: [`
      .footer {
          background: var(--mat-sys-surface);
          color: var(--mat-sys-on-surface);
          border-top: 1px solid var(--mat-sys-outline-variant);
      }

      .container {
          max-width: 1200px;
          margin: 0 auto;
          padding: 16px;
      }

      .top {
          display: grid;
          gap: 16px;
          grid-template-columns:1fr;
          border-bottom: 1px solid var(--mat-sys-outline-variant);
      }

      .cols {
          display: grid;
          gap: 16px;
          grid-template-columns:repeat(1, minmax(0, 1fr));
      }

      .logo {
          height: 28px;
          opacity: .85
      }

      .bottom {
          display: flex;
          gap: 16px;
          align-items: center;
          justify-content: space-between;
          flex-wrap: wrap
      }

      .social a, .legal a {
          color: var(--mat-sys-primary);
          text-decoration: none;
          font-size: 12px
      }

      .social a:hover, .legal a:hover {
          text-decoration: underline
      }

      .note {
          color: var(--mat-sys-outline);
          font-size: 12px
      }

      @media (min-width: 768px) {
          .top {
              grid-template-columns:1fr 3fr
          }

          .cols {
              grid-template-columns:repeat(3, minmax(0, 1fr))
          }
      }
  `],
  imports: [
    UpperCasePipe
  ],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FooterComponent {
  @Input({ required: true }) model!: FooterModel;
}
