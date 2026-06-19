import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { authBearerInterceptor } from './auth-bearer.interceptor';
import { AUTH_CLIENT, AuthClient } from './auth-client';

describe('authBearerInterceptor', () => {
  const auth: AuthClient = {
    isAuthenticated: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    getAccessToken: vi.fn(),
    getTokenExpiresAt: vi.fn(),
  };

  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authBearerInterceptor])),
        provideHttpClientTesting(),
        { provide: AUTH_CLIENT, useValue: auth },
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('attaches the configured provider access token to application API requests', async () => {
    vi.mocked(auth.getAccessToken).mockResolvedValue('access-token');

    const url = `${environment.apiBaseUrl}${environment.apiBasePath}/tenant/runtime/bootstrap`;
    const response = firstValueFrom(client.get(url));
    await flushPromises();

    const request = http.expectOne(url);
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-token');
    request.flush({});
    await response;
  });

  it('does not ask for or attach a token to external requests', async () => {
    const response = firstValueFrom(client.get('https://example.com/public.json'));
    await flushPromises();

    const request = http.expectOne('https://example.com/public.json');
    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(auth.getAccessToken).not.toHaveBeenCalled();
    request.flush({});
    await response;
  });

  it('refreshes the provider token once after a 401 response', async () => {
    vi.mocked(auth.getAccessToken)
      .mockResolvedValueOnce('access-token')
      .mockResolvedValueOnce('refreshed-token');

    const url = `${environment.apiBaseUrl}${environment.apiBasePath}/tenant/runtime/bootstrap`;
    const response = firstValueFrom(client.get(url));
    await flushPromises();

    const firstRequest = http.expectOne(url);
    expect(firstRequest.request.headers.get('Authorization')).toBe('Bearer access-token');
    firstRequest.flush({}, { status: 401, statusText: 'Unauthorized' });
    await flushPromises();

    const retryRequest = http.expectOne(url);
    expect(retryRequest.request.headers.get('Authorization')).toBe('Bearer refreshed-token');
    retryRequest.flush({});
    await response;

    expect(auth.getAccessToken).toHaveBeenNthCalledWith(1);
    expect(auth.getAccessToken).toHaveBeenNthCalledWith(2, true);
  });
});

async function flushPromises(): Promise<void> {
  await Promise.resolve();
  await Promise.resolve();
}
