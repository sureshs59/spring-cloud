// src/app/services/auth.service.ts
import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  roles: string[];
  jwtHeader: string;
  jwtPayload: string;
  jwtSignature: string;
  tokenInfo: Record<string, any>;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http   = inject(HttpClient);
  private router = inject(Router);

  // ── Signals — reactive state ─────────────────────────────────
  authResponse = signal<AuthResponse | null>(null);
  isLoading    = signal(false);
  errorMsg     = signal<string | null>(null);

  // Computed
  isLoggedIn  = computed(() => !!this.authResponse());
  currentUser = computed(() => this.authResponse()?.username ?? null);
  userRoles   = computed(() => this.authResponse()?.roles ?? []);
  accessToken = computed(() => this.authResponse()?.accessToken ?? null);
  isAdmin     = computed(() => this.userRoles().includes('ROLE_ADMIN'));

  // ── Login ────────────────────────────────────────────────────
  login(username: string, password: string) {
    this.isLoading.set(true);
    this.errorMsg.set(null);

    return this.http.post<AuthResponse>('/api/auth/login', { username, password }).pipe(
      tap(res => {
        this.authResponse.set(res);
        // Store tokens in localStorage
        localStorage.setItem('accessToken', res.accessToken);
        localStorage.setItem('refreshToken', res.refreshToken);
        this.isLoading.set(false);
      }),
      catchError(err => {
        this.errorMsg.set(err.error?.error || 'Login failed');
        this.isLoading.set(false);
        return of(null);
      })
    );
  }

  // ── Refresh token ────────────────────────────────────────────
  refreshToken() {
    const rt = localStorage.getItem('refreshToken');
    if (!rt) return of(null);
    return this.http.post<any>('/api/auth/refresh', { refreshToken: rt }).pipe(
      tap(res => {
        if (res?.accessToken) {
          localStorage.setItem('accessToken', res.accessToken);
          // Update the stored auth response with new token
          const current = this.authResponse();
          if (current) {
            this.authResponse.set({ ...current, accessToken: res.accessToken, tokenInfo: res.tokenInfo });
          }
        }
      })
    );
  }

  // ── Inspect token ────────────────────────────────────────────
  inspectToken(token: string) {
    return this.http.post<any>('/api/auth/inspect', { token });
  }

  // ── Call protected endpoints ─────────────────────────────────
  getDashboard() {
    return this.http.get<any>('/api/dashboard');
  }

  getProfile() {
    return this.http.get<any>('/api/profile');
  }

  getAdminData() {
    return this.http.get<any>('/api/admin/data');
  }

  // ── Logout ───────────────────────────────────────────────────
  logout() {
    this.authResponse.set(null);
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this.router.navigate(['/']);
  }

  // ── Get stored token ─────────────────────────────────────────
  getStoredToken(): string | null {
    return localStorage.getItem('accessToken');
  }
}
