import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { PublicContactFormComponent } from '../contact/public-contact-form.component';

@Component({
  selector: 'tch-public-support-page',
  imports: [TranslatePipe, PublicContactFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-support.page.html',
  styleUrls: ['./public-support.page.scss'],
})
export class PublicSupportPage implements OnInit {
  private readonly route = inject(ActivatedRoute);

  readonly prefillMessage = signal('');

  ngOnInit(): void {
    const ctx = this.route.snapshot.queryParamMap.get('ctx');
    if (ctx) {
      try {
        const decoded = JSON.parse(decodeURIComponent(escape(atob(ctx)))) as Record<string, unknown>;
        const message = typeof decoded['message'] === 'string' ? decoded['message'] : '';
        const stack = typeof decoded['stack'] === 'string' ? decoded['stack'] : '';
        this.prefillMessage.set(stack ? `${message}\n\n---\n${stack}` : message);
      } catch {
        // ignore invalid ctx
      }
    }
  }
}
