import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { PublicContactFormComponent } from '../contact/public-contact-form.component';

@Component({
  selector: 'tch-public-data-deletion-page',
  imports: [TranslatePipe, PublicContactFormComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-data-deletion.page.html',
  styleUrls: ['./public-data-deletion.page.scss'],
})
export class PublicDataDeletionPage {}
