import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { PublicContactFormComponent } from './public-contact-form.component';

@Component({
  selector: 'tch-public-contact-page',
  imports: [TranslatePipe, PublicContactFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-contact.page.html',
  styleUrls: ['./public-contact.page.scss'],
})
export class PublicContactPage {}
