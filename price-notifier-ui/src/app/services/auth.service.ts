import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import {environment} from '../../environments/environment';

interface AuthResponse {
  access_token: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/auth`;
  private cookieName = 'jwt_token';

  private http = inject(HttpClient);
  private router = inject(Router);

  login(credentials: {email: string, password: string}) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/authenticate`, credentials)
      .pipe(
        tap(response => {
          this.setCookie(response.access_token, 1);
        })
      );
  }

  register(user: {firstName: string, lastName: string, email: string, password: string}) {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, user)
      .pipe(
        tap(response => {
          this.setCookie(response.access_token, 1);
        })
      );
  }

  logout() {
    this.deleteCookie();
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this.getCookie(this.cookieName);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  private setCookie(token: string, days: number) {
    const d = new Date();
    d.setTime(d.getTime() + (days * 24 * 60 * 60 * 1000));
    const expires = "expires=" + d.toUTCString();
    document.cookie = `${this.cookieName}=${token};${expires};path=/;SameSite=Lax`;
  }

  private getCookie(name: string): string | null {
    const nameEQ = name + "=";
    const ca = document.cookie.split(';');
    for(let i = 0; i < ca.length; i++) {
      let c = ca[i];
      while (c.charAt(0) === ' ') c = c.substring(1, c.length);
      if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
    }
    return null;
  }

  private deleteCookie() {
    document.cookie = `${this.cookieName}=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;`;
  }
}
