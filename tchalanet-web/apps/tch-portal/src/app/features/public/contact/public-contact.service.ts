import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiResponse } from '@tch/api';
import { TchBackendClient } from '@tch/api';
import { ContactRequestSubmittedResponse, SubmitContactRequest } from './public-contact.model';

@Injectable({ providedIn: 'root' })
export class PublicContactService {
  private readonly backend = inject(TchBackendClient);

  submit(request: SubmitContactRequest): Observable<ApiResponse<ContactRequestSubmittedResponse>> {
    return this.backend.postApiResponse<ContactRequestSubmittedResponse, SubmitContactRequest>(
      '/public/contact-requests',
      request,
      { suppressShellFeedback: true },
    );
  }
}
