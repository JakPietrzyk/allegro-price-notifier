import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

import { errorInterceptor } from './error.interceptor';
import {GlobalErrorService} from '../../services/error/global-error.service';
import {ERROR_MESSAGES, ErrorCode} from '../../models/errors/error-codes';

describe('ErrorInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;

  const errorServiceMock = {
    showError: vi.fn()
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: GlobalErrorService, useValue: errorServiceMock }
      ]
    });

    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);

    vi.clearAllMocks();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should handle known error code and call ErrorService with translated message', () => {
    const knownCode = ErrorCode.AUTH_USER_ALREADY_EXISTS;

    httpClient.get('/test').subscribe({
      next: () => {},
      error: () => {}
    });

    const req = httpMock.expectOne('/test');
    req.flush({ code: knownCode }, { status: 409, statusText: 'Conflict' });

    // Asercja
    expect(errorServiceMock.showError).toHaveBeenCalledWith(ERROR_MESSAGES[knownCode]);
  });

  it('should handle unknown error and call ErrorService with generic message', () => {
    httpClient.get('/test').subscribe({
      next: () => {},
      error: () => {}
    });

    const req = httpMock.expectOne('/test');
    req.flush('Server Error', { status: 500, statusText: 'Error' });

    expect(errorServiceMock.showError).toHaveBeenCalledWith(ERROR_MESSAGES[ErrorCode.INTERNAL_SERVER_ERROR]);
  });
});
