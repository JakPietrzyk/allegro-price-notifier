import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { DOCUMENT } from '@angular/common';
import { of } from 'rxjs';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import {AUTH_CONSTANTS} from '../../core/constants/auth.constants';

describe('AuthService', () => {
  let service: AuthService;
  let httpClientMock: any;
  let routerMock: any;

  let mockCookieStore: { [key: string]: string } = {};
  const documentMock = {
    location: { protocol: 'http:' },
    get cookie() {
      return Object.entries(mockCookieStore)
        .map(([k, v]) => `${k}=${v}`)
        .join('; ');
    },
    set cookie(value: string) {
      const parts = value.split(';');
      const [key, val] = parts[0].split('=');
      if (value.includes('Expires=Thu, 01 Jan 1970')) {
        delete mockCookieStore[key.trim()];
      } else {
        mockCookieStore[key.trim()] = val;
      }
    }
  };

  beforeEach(() => {
    mockCookieStore = {}; // Reset cookies

    httpClientMock = {
      post: vi.fn()
    };

    routerMock = {
      navigate: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: HttpClient, useValue: httpClientMock },
        { provide: Router, useValue: routerMock },
        { provide: DOCUMENT, useValue: documentMock }
      ]
    });

    service = TestBed.inject(AuthService);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should initialize isAuthenticated signal based on cookie presence', () => {
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should set cookie and update signal on successful login', () => {
    // Arrange
    const mockResponse = { access_token: 'fake-jwt-token' };
    const credentials = { email: 'test@test.pl', password: '123' };
    httpClientMock.post.mockReturnValue(of(mockResponse));

    // Act
    service.login(credentials).subscribe();

    // Assert
    expect(httpClientMock.post).toHaveBeenCalledWith(AUTH_CONSTANTS.API.LOGIN, credentials);
    expect(mockCookieStore[AUTH_CONSTANTS.COOKIES.TOKEN_NAME]).toBe('fake-jwt-token');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('should remove cookie, update signal and navigate on logout', () => {
    // Arrange
    mockCookieStore[AUTH_CONSTANTS.COOKIES.TOKEN_NAME] = 'some-token';

    // Act
    service.logout();

    // Assert
    expect(mockCookieStore[AUTH_CONSTANTS.COOKIES.TOKEN_NAME]).toBeUndefined();
    expect(service.isAuthenticated()).toBe(false);
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should return token if cookie exists', () => {
    mockCookieStore[AUTH_CONSTANTS.COOKIES.TOKEN_NAME] = 'abc-123';
    expect(service.getToken()).toBe('abc-123');
  });

  it('should return null if cookie does not exist', () => {
    expect(service.getToken()).toBeNull();
  });
});
