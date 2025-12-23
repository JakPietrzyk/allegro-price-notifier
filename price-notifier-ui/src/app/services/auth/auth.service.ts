import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { DOCUMENT } from '@angular/common';
import { tap } from 'rxjs/operators';
import {AuthResponse} from '../../models/auth/auth-response.model';
import {AUTH_CONSTANTS} from '../../core/constants/auth.constants';
import {RegisterRequest} from '../../models/auth/register-request.model';
import {LoginRequest} from '../../models/auth/login-request.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly document = inject(DOCUMENT);

  readonly isAuthenticated = signal<boolean>(this.hasToken());

  login(credentials: LoginRequest) {
    return this.http.post<AuthResponse>(AUTH_CONSTANTS.API.LOGIN, credentials)
      .pipe(
        tap(response => {
          this.setToken(response.access_token);
        })
      );
  }

  register(user: RegisterRequest) {
    return this.http.post<AuthResponse>(AUTH_CONSTANTS.API.REGISTER, user)
      .pipe(
        tap(response => {
          this.setToken(response.access_token);
        })
      );
  }

  logout() {
    this.deleteToken();
    this.router.navigate(['/login']);
  }

  isLoggedIn(): boolean {
    return this.isAuthenticated();
  }

  getToken(): string | null {
    return this.getCookie(AUTH_CONSTANTS.COOKIES.TOKEN_NAME);
  }

  private hasToken(): boolean {
    return !!this.getToken();
  }

  private setToken(token: string) {
    this.setCookie(AUTH_CONSTANTS.COOKIES.TOKEN_NAME, token, AUTH_CONSTANTS.COOKIES.EXPIRY_DAYS);
    this.isAuthenticated.set(true);
  }

  private deleteToken() {
    this.deleteCookie(AUTH_CONSTANTS.COOKIES.TOKEN_NAME);
    this.isAuthenticated.set(false);
  }

  private setCookie(name: string, value: string, days: number) {
    const d = new Date();
    d.setTime(d.getTime() + (days * 24 * 60 * 60 * 1000));

    let cookieString = `${name}=${value};expires=${d.toUTCString()};path=${AUTH_CONSTANTS.COOKIES.PATH};SameSite=Lax`;

    if (this.document.location.protocol === 'https:') {
      cookieString += ';Secure';
    }

    this.document.cookie = cookieString;
  }

  private getCookie(name: string): string | null {
    const nameEQ = name + "=";
    const ca = this.document.cookie.split(';');

    for (const element of ca) {
      let c = element;
      while (c.startsWith(' ')) c = c.substring(1, c.length);
      if (c.startsWith(nameEQ)) return c.substring(nameEQ.length, c.length);
    }
    return null;
  }

  private deleteCookie(name: string) {
    this.document.cookie = `${name}=; Path=${AUTH_CONSTANTS.COOKIES.PATH}; Expires=Thu, 01 Jan 1970 00:00:01 GMT;`;
  }
}
