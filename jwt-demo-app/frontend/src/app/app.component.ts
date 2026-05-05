import { Component, signal, computed, inject, OnInit, OnDestroy } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { HttpClient, HttpClientModule, HTTP_INTERCEPTORS, HttpRequest, HttpHandlerFn, HttpInterceptorFn } from '@angular/common/http';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { tap, catchError, interval, Subscription } from 'rxjs';
import { of } from 'rxjs';

// ── JWT Interceptor ──────────────────────────────────────────────
// Automatically adds Authorization: Bearer <token> to every request
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('accessToken');
  if (token && !req.url.includes('/api/auth/login')) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }
  return next(req);
};

// ── Types ────────────────────────────────────────────────────────
interface AuthResponse {
  accessToken: string; refreshToken: string; tokenType: string;
  expiresIn: number; username: string; roles: string[];
  jwtHeader: string; jwtPayload: string; jwtSignature: string;
  tokenInfo: Record<string, any>; message: string;
}
interface ApiResult { label: string; data: any; error?: string; timestamp: string; }

// ════════════════════════════════════════════════════════════════
// MAIN APP COMPONENT
// ════════════════════════════════════════════════════════════════
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
<div class="app">

  <!-- ── HEADER ───────────────────────────────────────── -->
  <header class="header">
    <div class="header-inner">
      <div class="brand">
        <span class="brand-icon">🔐</span>
        <div>
          <div class="brand-name">JWT Auth Demo</div>
          <div class="brand-sub">Spring Boot + Angular 18</div>
        </div>
      </div>
      @if (isLoggedIn()) {
        <div class="user-pill">
          <span class="user-dot"></span>
          <span>{{ username() }}</span>
          @if (isAdmin()) { <span class="badge-admin">ADMIN</span> }
          <button class="btn-logout" (click)="logout()">Sign out</button>
        </div>
      }
    </div>
  </header>

  <main class="main">

    <!-- ── FLOW DIAGRAM ─────────────────────────────────── -->
    <section class="flow-section">
      <div class="flow-title">JWT Authentication Flow</div>
      <div class="flow-steps">
        <div class="flow-step" [class.active]="flowStep() >= 1">
          <div class="flow-num">1</div>
          <div class="flow-label">Login<br><span>username + password</span></div>
        </div>
        <div class="flow-arrow" [class.active]="flowStep() >= 2">→</div>
        <div class="flow-step" [class.active]="flowStep() >= 2">
          <div class="flow-num">2</div>
          <div class="flow-label">Server issues<br><span>JWT token</span></div>
        </div>
        <div class="flow-arrow" [class.active]="flowStep() >= 3">→</div>
        <div class="flow-step" [class.active]="flowStep() >= 3">
          <div class="flow-num">3</div>
          <div class="flow-label">Client stores<br><span>in localStorage</span></div>
        </div>
        <div class="flow-arrow" [class.active]="flowStep() >= 4">→</div>
        <div class="flow-step" [class.active]="flowStep() >= 4">
          <div class="flow-num">4</div>
          <div class="flow-label">Request + Bearer token<br><span>Authorization header</span></div>
        </div>
        <div class="flow-arrow" [class.active]="flowStep() >= 5">→</div>
        <div class="flow-step" [class.active]="flowStep() >= 5">
          <div class="flow-num">5</div>
          <div class="flow-label">Server validates<br><span>signature + expiry</span></div>
        </div>
        <div class="flow-arrow" [class.active]="flowStep() >= 6">→</div>
        <div class="flow-step" [class.active]="flowStep() >= 6">
          <div class="flow-num">6</div>
          <div class="flow-label">Access granted<br><span>protected resource</span></div>
        </div>
      </div>
    </section>

    <div class="grid-layout">

      <!-- ── LEFT COLUMN ──────────────────────────────── -->
      <div class="left-col">

        <!-- LOGIN CARD -->
        @if (!isLoggedIn()) {
          <div class="card">
            <div class="card-title">Step 1 — Login</div>
            <div class="card-sub">Authenticate to receive your JWT token</div>

            @if (loginError()) {
              <div class="alert-error">{{ loginError() }}</div>
            }

            <div class="field">
              <label>Username</label>
              <input [(ngModel)]="loginForm.username" placeholder="e.g. suresh or admin" />
            </div>
            <div class="field">
              <label>Password</label>
              <input type="password" [(ngModel)]="loginForm.password" placeholder="password or admin123" />
            </div>

            <div class="users-hint">
              <div class="hint-title">Available users:</div>
              <div class="hint-users">
                <span class="user-tag" (click)="fillUser('admin','admin123')">admin / admin123 (ADMIN)</span>
                <span class="user-tag" (click)="fillUser('suresh','password')">suresh / password (USER)</span>
                <span class="user-tag" (click)="fillUser('priya','password')">priya / password (USER)</span>
              </div>
            </div>

            <button class="btn-primary" (click)="login()" [disabled]="loading()">
              {{ loading() ? 'Authenticating...' : 'Login & Get JWT' }}
            </button>
          </div>
        }

        <!-- TOKEN DISPLAY (after login) -->
        @if (isLoggedIn() && authData()) {
          <div class="card">
            <div class="card-title">Your JWT Token</div>

            <!-- Token countdown -->
            <div class="token-timer">
              <span class="timer-label">Expires in:</span>
              <span class="timer-value" [class.warning]="tokenSeconds() < 60">
                {{ formatSeconds(tokenSeconds()) }}
              </span>
              <div class="timer-bar">
                <div class="timer-fill" [style.width]="timerPct() + '%'" [class.warning]="tokenSeconds() < 60"></div>
              </div>
            </div>

            <!-- JWT 3 parts visual -->
            <div class="jwt-visual">
              <div class="jwt-part header-part">
                <div class="part-label">HEADER</div>
                <div class="part-value">{{ authData()!.jwtHeader | json }}</div>
                <div class="part-desc">Algorithm + token type</div>
              </div>
              <div class="jwt-dot">.</div>
              <div class="jwt-part payload-part">
                <div class="part-label">PAYLOAD</div>
                <div class="part-value">{{ authData()!.jwtPayload | json }}</div>
                <div class="part-desc">Claims: username, roles, expiry</div>
              </div>
              <div class="jwt-dot">.</div>
              <div class="jwt-part sig-part">
                <div class="part-label">SIGNATURE</div>
                <div class="part-value">{{ authData()!.jwtSignature }}</div>
                <div class="part-desc">HMAC-SHA256 — proves authenticity</div>
              </div>
            </div>

            <!-- Token info table -->
            <div class="info-grid">
              @for (entry of tokenInfoEntries(); track entry[0]) {
                <div class="info-row">
                  <span class="info-key">{{ entry[0] }}</span>
                  <span class="info-val">{{ entry[1] }}</span>
                </div>
              }
            </div>

            <!-- Raw token (truncated) -->
            <div class="raw-token-box">
              <div class="raw-token-label">Raw JWT (Authorization: Bearer ...)</div>
              <div class="raw-token">{{ truncate(authData()!.accessToken) }}</div>
            </div>

            <button class="btn-secondary" (click)="refreshToken()">
              ↻ Refresh access token
            </button>
          </div>
        }

        <!-- INSPECT ANY TOKEN -->
        <div class="card">
          <div class="card-title">Token Inspector</div>
          <div class="card-sub">Paste any JWT to decode and explain it</div>
          <textarea [(ngModel)]="inspectInput" rows="3"
            placeholder="Paste a JWT token here..."></textarea>
          <button class="btn-secondary" (click)="inspectToken()">Inspect Token</button>

          @if (inspectResult()) {
            <div class="inspect-result">
              <pre>{{ inspectResult() | json }}</pre>
            </div>
          }
        </div>

      </div>

      <!-- ── RIGHT COLUMN ─────────────────────────────── -->
      <div class="right-col">

        <!-- API TESTER -->
        @if (isLoggedIn()) {
          <div class="card">
            <div class="card-title">Step 4–6 — Call Protected APIs</div>
            <div class="card-sub">See how the JWT is sent and validated on each request</div>

            <div class="api-buttons">
              <button class="api-btn user" (click)="callApi('dashboard')">
                <span class="api-method">GET</span>
                <span>/api/dashboard</span>
                <span class="api-badge">Requires JWT</span>
              </button>
              <button class="api-btn user" (click)="callApi('profile')">
                <span class="api-method">GET</span>
                <span>/api/profile</span>
                <span class="api-badge">Requires JWT</span>
              </button>
              <button class="api-btn admin" (click)="callApi('admin')">
                <span class="api-method">GET</span>
                <span>/api/admin/data</span>
                <span class="api-badge admin-badge">ADMIN only</span>
              </button>
            </div>

            <!-- Request flow visualised -->
            <div class="request-flow">
              <div class="flow-box client">Angular</div>
              <div class="flow-conn">
                <div class="flow-arrow-h">→</div>
                <div class="flow-header-label">Authorization: Bearer &lt;JWT&gt;</div>
              </div>
              <div class="flow-box filter">JwtAuthFilter</div>
              <div class="flow-conn">
                <div class="flow-arrow-h">→</div>
                <div class="flow-header-label">validates → sets SecurityContext</div>
              </div>
              <div class="flow-box controller">Controller</div>
            </div>

            <!-- API Results -->
            @for (result of apiResults(); track result.timestamp) {
              <div class="api-result" [class.error]="result.error">
                <div class="result-label">{{ result.label }} — {{ result.timestamp }}</div>
                @if (result.error) {
                  <div class="result-error">{{ result.error }}</div>
                } @else {
                  <pre>{{ result.data | json }}</pre>
                }
              </div>
            }
          </div>
        }

        <!-- HOW JWT WORKS — EXPLANATION CARDS -->
        <div class="card explain-card">
          <div class="card-title">How JWT Works — Key Concepts</div>

          <div class="concept">
            <div class="concept-icon">🏗️</div>
            <div>
              <div class="concept-title">Structure: Header.Payload.Signature</div>
              <div class="concept-body">Three Base64URL-encoded parts joined by dots. Header = algorithm. Payload = claims (data). Signature = HMAC-SHA256 proof that YOU issued this token.</div>
            </div>
          </div>

          <div class="concept">
            <div class="concept-icon">✍️</div>
            <div>
              <div class="concept-title">Signature = Trust</div>
              <div class="concept-body">The server signs the header+payload with a secret key. If ANYONE changes even one character of the token, the signature becomes invalid. The server rejects it.</div>
            </div>
          </div>

          <div class="concept">
            <div class="concept-icon">⏱️</div>
            <div>
              <div class="concept-title">Stateless = No Session</div>
              <div class="concept-body">The server stores NOTHING. Every token contains all needed information (username, roles, expiry). No database lookup needed on every request.</div>
            </div>
          </div>

          <div class="concept">
            <div class="concept-icon">🔄</div>
            <div>
              <div class="concept-title">Access + Refresh Token Pattern</div>
              <div class="concept-body">Access token: short-lived (15 min), sent on every request. Refresh token: long-lived (24h), stored safely, used only to get a new access token.</div>
            </div>
          </div>

          <div class="concept">
            <div class="concept-icon">⚠️</div>
            <div>
              <div class="concept-title">JWT Payload is NOT encrypted</div>
              <div class="concept-body">Anyone can decode the Base64 payload and read the claims. Never put passwords or sensitive data in JWT. The SIGNATURE ensures it cannot be tampered with — but it is readable.</div>
            </div>
          </div>

          <div class="concept">
            <div class="concept-icon">🚪</div>
            <div>
              <div class="concept-title">Logout = Delete the Token</div>
              <div class="concept-body">Since the server is stateless, logout = delete the JWT from localStorage. In production, add the token's JTI to a Redis denylist until it naturally expires.</div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </main>
</div>
  `,
  styles: [`
    :host { display: block; }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Segoe UI', system-ui, sans-serif; }

    .app { min-height: 100vh; background: #0f0f1a; color: #e2e8f0; }

    /* Header */
    .header { background: #1a1a2e; border-bottom: 1px solid rgba(99,102,241,.3); padding: .75rem 1.5rem; position: sticky; top: 0; z-index: 100; }
    .header-inner { max-width: 1400px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; }
    .brand { display: flex; align-items: center; gap: .75rem; }
    .brand-icon { font-size: 1.5rem; }
    .brand-name { font-size: 1rem; font-weight: 700; color: #a78bfa; }
    .brand-sub { font-size: .7rem; color: #64748b; }
    .user-pill { display: flex; align-items: center; gap: .6rem; background: rgba(99,102,241,.15); padding: .35rem .75rem; border-radius: 20px; font-size: .85rem; }
    .user-dot { width: 8px; height: 8px; border-radius: 50%; background: #4ade80; }
    .badge-admin { background: #7c3aed; color: white; font-size: .65rem; padding: 1px 6px; border-radius: 10px; font-weight: 600; }
    .btn-logout { background: transparent; border: 1px solid rgba(255,255,255,.2); color: #94a3b8; padding: .25rem .6rem; border-radius: 6px; font-size: .75rem; cursor: pointer; }
    .btn-logout:hover { border-color: #ef4444; color: #ef4444; }

    /* Flow diagram */
    .flow-section { background: #1a1a2e; border-bottom: 1px solid rgba(99,102,241,.2); padding: 1rem 1.5rem; }
    .flow-title { text-align: center; font-size: .7rem; font-weight: 600; text-transform: uppercase; letter-spacing: .1em; color: #6366f1; margin-bottom: .75rem; }
    .flow-steps { display: flex; align-items: center; justify-content: center; gap: .5rem; flex-wrap: wrap; }
    .flow-step { display: flex; flex-direction: column; align-items: center; gap: .25rem; opacity: .3; transition: opacity .4s; }
    .flow-step.active { opacity: 1; }
    .flow-num { width: 28px; height: 28px; border-radius: 50%; background: #6366f1; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 600; color: white; }
    .flow-label { font-size: 10px; text-align: center; color: #94a3b8; line-height: 1.3; }
    .flow-label span { color: #6366f1; font-size: 9px; }
    .flow-arrow { font-size: 18px; color: #6366f1; opacity: .2; transition: opacity .4s; }
    .flow-arrow.active { opacity: 1; }

    /* Layout */
    .main { max-width: 1400px; margin: 0 auto; padding: 1.5rem; }
    .grid-layout { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; margin-top: 1.25rem; }
    @media (max-width: 900px) { .grid-layout { grid-template-columns: 1fr; } }
    .left-col, .right-col { display: flex; flex-direction: column; gap: 1rem; }

    /* Cards */
    .card { background: #1e1e30; border: 1px solid rgba(99,102,241,.2); border-radius: 12px; padding: 1.25rem; }
    .card-title { font-size: 1rem; font-weight: 600; color: #a78bfa; margin-bottom: .25rem; }
    .card-sub { font-size: .8rem; color: #64748b; margin-bottom: 1rem; }

    /* Login form */
    .field { margin-bottom: .75rem; }
    .field label { display: block; font-size: .75rem; color: #94a3b8; margin-bottom: .3rem; font-weight: 500; }
    input, textarea { width: 100%; background: #0f0f1a; border: 1px solid rgba(99,102,241,.3); border-radius: 8px; padding: .6rem .9rem; color: #e2e8f0; font-size: .875rem; font-family: inherit; resize: vertical; }
    input:focus, textarea:focus { outline: none; border-color: #6366f1; }
    .users-hint { background: rgba(99,102,241,.1); border-radius: 8px; padding: .75rem; margin-bottom: .75rem; }
    .hint-title { font-size: .7rem; color: #6366f1; font-weight: 600; margin-bottom: .4rem; }
    .hint-users { display: flex; flex-wrap: wrap; gap: .4rem; }
    .user-tag { background: rgba(99,102,241,.2); color: #a78bfa; padding: .25rem .6rem; border-radius: 12px; font-size: .72rem; cursor: pointer; border: 1px solid rgba(99,102,241,.3); }
    .user-tag:hover { background: rgba(99,102,241,.35); }
    .alert-error { background: rgba(239,68,68,.15); border: 1px solid rgba(239,68,68,.4); color: #fca5a5; border-radius: 8px; padding: .6rem .9rem; font-size: .8rem; margin-bottom: .75rem; }

    /* Buttons */
    .btn-primary { width: 100%; background: #6366f1; color: white; border: none; border-radius: 8px; padding: .75rem; font-size: .9rem; font-weight: 600; cursor: pointer; transition: background .15s; }
    .btn-primary:hover { background: #5254cc; }
    .btn-primary:disabled { opacity: .5; cursor: not-allowed; }
    .btn-secondary { width: 100%; background: transparent; color: #6366f1; border: 1px solid rgba(99,102,241,.4); border-radius: 8px; padding: .6rem; font-size: .85rem; cursor: pointer; margin-top: .6rem; }
    .btn-secondary:hover { background: rgba(99,102,241,.1); }

    /* Token timer */
    .token-timer { display: flex; align-items: center; gap: .75rem; margin-bottom: 1rem; flex-wrap: wrap; }
    .timer-label { font-size: .75rem; color: #64748b; }
    .timer-value { font-size: 1.1rem; font-weight: 700; color: #4ade80; font-family: 'Courier New', monospace; }
    .timer-value.warning { color: #f59e0b; }
    .timer-bar { flex: 1; height: 6px; background: rgba(255,255,255,.1); border-radius: 3px; min-width: 100px; overflow: hidden; }
    .timer-fill { height: 100%; background: #4ade80; border-radius: 3px; transition: width 1s linear; }
    .timer-fill.warning { background: #f59e0b; }

    /* JWT visual 3 parts */
    .jwt-visual { display: flex; align-items: stretch; gap: 0; margin-bottom: 1rem; border-radius: 8px; overflow: hidden; }
    .jwt-part { flex: 1; padding: .6rem; }
    .header-part { background: rgba(239,68,68,.15); border: 1px solid rgba(239,68,68,.3); }
    .payload-part { background: rgba(99,102,241,.15); border-top: 1px solid rgba(99,102,241,.3); border-bottom: 1px solid rgba(99,102,241,.3); }
    .sig-part { background: rgba(34,197,94,.15); border: 1px solid rgba(34,197,94,.3); }
    .jwt-dot { display: flex; align-items: center; font-size: 1.5rem; color: #64748b; padding: 0 .1rem; font-weight: 700; }
    .part-label { font-size: .6rem; font-weight: 700; text-transform: uppercase; letter-spacing: .08em; margin-bottom: .3rem; }
    .header-part .part-label { color: #f87171; }
    .payload-part .part-label { color: #818cf8; }
    .sig-part .part-label { color: #4ade80; }
    .part-value { font-size: .72rem; font-family: 'Courier New', monospace; color: #cbd5e1; word-break: break-all; margin-bottom: .25rem; }
    .part-desc { font-size: .65rem; color: #64748b; }

    /* Token info table */
    .info-grid { background: rgba(0,0,0,.3); border-radius: 8px; padding: .6rem; margin-bottom: .75rem; }
    .info-row { display: flex; gap: .75rem; padding: .3rem 0; border-bottom: 1px solid rgba(255,255,255,.05); font-size: .78rem; }
    .info-row:last-child { border-bottom: none; }
    .info-key { color: #6366f1; font-weight: 500; min-width: 120px; }
    .info-val { color: #94a3b8; word-break: break-all; }
    .raw-token-box { background: rgba(0,0,0,.4); border-radius: 8px; padding: .75rem; margin-bottom: .75rem; }
    .raw-token-label { font-size: .65rem; color: #64748b; margin-bottom: .3rem; }
    .raw-token { font-family: 'Courier New', monospace; font-size: .72rem; color: #a78bfa; word-break: break-all; }

    /* API tester */
    .api-buttons { display: flex; flex-direction: column; gap: .5rem; margin-bottom: 1rem; }
    .api-btn { display: flex; align-items: center; gap: .75rem; background: rgba(99,102,241,.08); border: 1px solid rgba(99,102,241,.2); border-radius: 8px; padding: .65rem .9rem; cursor: pointer; color: #e2e8f0; font-size: .85rem; text-align: left; transition: all .15s; }
    .api-btn:hover { background: rgba(99,102,241,.2); }
    .api-btn.admin { border-color: rgba(245,158,11,.3); }
    .api-btn.admin:hover { background: rgba(245,158,11,.1); }
    .api-method { background: #6366f1; color: white; font-size: .65rem; font-weight: 700; padding: 2px 6px; border-radius: 4px; }
    .api-btn.admin .api-method { background: #f59e0b; }
    .api-badge { margin-left: auto; font-size: .65rem; background: rgba(99,102,241,.2); color: #818cf8; padding: 2px 6px; border-radius: 10px; }
    .admin-badge { background: rgba(245,158,11,.2) !important; color: #fbbf24 !important; }

    /* Request flow */
    .request-flow { display: flex; align-items: center; gap: .5rem; padding: .75rem; background: rgba(0,0,0,.3); border-radius: 8px; margin-bottom: 1rem; flex-wrap: wrap; }
    .flow-box { padding: .35rem .7rem; border-radius: 6px; font-size: .75rem; font-weight: 600; }
    .flow-box.client { background: rgba(99,102,241,.25); color: #818cf8; }
    .flow-box.filter { background: rgba(245,158,11,.2); color: #fbbf24; }
    .flow-box.controller { background: rgba(34,197,94,.2); color: #4ade80; }
    .flow-conn { display: flex; flex-direction: column; align-items: center; gap: 2px; }
    .flow-arrow-h { color: #6366f1; font-size: 1rem; }
    .flow-header-label { font-size: .6rem; color: #64748b; text-align: center; white-space: nowrap; }

    /* API results */
    .api-result { background: rgba(0,0,0,.4); border: 1px solid rgba(99,102,241,.2); border-radius: 8px; padding: .75rem; margin-top: .5rem; }
    .api-result.error { border-color: rgba(239,68,68,.3); }
    .result-label { font-size: .7rem; color: #64748b; margin-bottom: .4rem; }
    .result-error { color: #f87171; font-size: .8rem; }
    .api-result pre { font-size: .75rem; font-family: 'Courier New', monospace; color: #94a3b8; white-space: pre-wrap; word-break: break-all; max-height: 200px; overflow-y: auto; }

    /* Inspect */
    .inspect-result { background: rgba(0,0,0,.4); border-radius: 8px; padding: .75rem; margin-top: .5rem; max-height: 300px; overflow-y: auto; }
    .inspect-result pre { font-size: .72rem; font-family: 'Courier New', monospace; color: #94a3b8; white-space: pre-wrap; word-break: break-all; }

    /* Concepts */
    .explain-card { border-color: rgba(139,92,246,.2); }
    .concept { display: flex; gap: .75rem; padding: .65rem 0; border-bottom: 1px solid rgba(255,255,255,.05); align-items: flex-start; }
    .concept:last-child { border-bottom: none; }
    .concept-icon { font-size: 1.1rem; flex-shrink: 0; margin-top: .1rem; }
    .concept-title { font-size: .85rem; font-weight: 600; color: #a78bfa; margin-bottom: .2rem; }
    .concept-body { font-size: .78rem; color: #94a3b8; line-height: 1.5; }
  `]
})
export class AppComponent implements OnInit, OnDestroy {
  private http = inject(HttpClient);

  // State
  loginForm   = { username: '', password: '' };
  loading     = signal(false);
  loginError  = signal<string | null>(null);
  authData    = signal<AuthResponse | null>(null);
  apiResults  = signal<ApiResult[]>([]);
  inspectInput = '';
  inspectResult = signal<any>(null);
  tokenSeconds  = signal(900);
  flowStep      = signal(0);

  // Computed
  isLoggedIn  = computed(() => !!this.authData());
  username    = computed(() => this.authData()?.username ?? '');
  isAdmin     = computed(() => this.authData()?.roles.includes('ROLE_ADMIN') ?? false);
  timerPct    = computed(() => Math.min(100, (this.tokenSeconds() / 900) * 100));
  tokenInfoEntries = computed(() => {
    const info = this.authData()?.tokenInfo;
    if (!info) return [];
    return Object.entries(info).filter(([k]) => !['error'].includes(k));
  });

  private timerSub?: Subscription;

  ngOnInit() {}
  ngOnDestroy() { this.timerSub?.unsubscribe(); }

  fillUser(u: string, p: string) {
    this.loginForm.username = u;
    this.loginForm.password = p;
  }

  login() {
    if (!this.loginForm.username || !this.loginForm.password) return;
    this.loading.set(true);
    this.loginError.set(null);
    this.flowStep.set(1);

    this.http.post<AuthResponse>('/api/auth/login', this.loginForm).subscribe({
      next: (res) => {
        this.authData.set(res);
        localStorage.setItem('accessToken', res.accessToken);
        localStorage.setItem('refreshToken', res.refreshToken);
        this.loading.set(false);
        this.tokenSeconds.set(res.expiresIn);
        this.startTimer();
        this.flowStep.set(3);
      },
      error: (err) => {
        this.loginError.set(err.error?.error || 'Login failed. Check username/password.');
        this.loading.set(false);
        this.flowStep.set(0);
      }
    });
  }

  logout() {
    this.authData.set(null);
    this.apiResults.set([]);
    this.flowStep.set(0);
    this.timerSub?.unsubscribe();
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  }

  refreshToken() {
    const rt = localStorage.getItem('refreshToken');
    if (!rt) return;
    this.http.post<any>('/api/auth/refresh', { refreshToken: rt }).subscribe({
      next: (res) => {
        const current = this.authData();
        if (current && res.accessToken) {
          this.authData.set({ ...current, accessToken: res.accessToken, tokenInfo: res.tokenInfo });
          localStorage.setItem('accessToken', res.accessToken);
          this.tokenSeconds.set(900);
        }
      }
    });
  }

  callApi(type: 'dashboard' | 'profile' | 'admin') {
    this.flowStep.set(4);
    const token = localStorage.getItem('accessToken');
    const urlMap = { dashboard: '/api/dashboard', profile: '/api/profile', admin: '/api/admin/data' };
    const url = urlMap[type];
    const headers: any = token ? { Authorization: `Bearer ${token}` } : {};
    const label = `${type.toUpperCase()} ${url}`;

    this.http.get<any>(url, { headers }).subscribe({
      next: (data) => {
        this.flowStep.set(6);
        this.apiResults.update(r => [
          { label, data, timestamp: new Date().toLocaleTimeString() },
          ...r.slice(0, 4)
        ]);
      },
      error: (err) => {
        this.flowStep.set(4);
        this.apiResults.update(r => [
          { label, data: null, error: `${err.status} ${err.error?.message || err.error?.error || 'Access denied'}`, timestamp: new Date().toLocaleTimeString() },
          ...r.slice(0, 4)
        ]);
      }
    });
  }

  inspectToken() {
    const token = this.inspectInput.trim();
    if (!token) return;
    this.http.post<any>('/api/auth/inspect', { token }).subscribe({
      next: res => this.inspectResult.set(res),
      error: err => this.inspectResult.set({ error: err.error?.error || 'Inspection failed' })
    });
  }

  truncate(s: string, n = 80) {
    return s.length > n ? s.substring(0, n) + '...' : s;
  }

  formatSeconds(s: number) {
    const m = Math.floor(s / 60), sec = s % 60;
    return `${m}m ${sec.toString().padStart(2,'0')}s`;
  }

  private startTimer() {
    this.timerSub?.unsubscribe();
    this.timerSub = interval(1000).subscribe(() => {
      this.tokenSeconds.update(s => Math.max(0, s - 1));
    });
  }
}

// ── Bootstrap ────────────────────────────────────────────────────
bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(withInterceptors([jwtInterceptor]))
  ]
}).catch(console.error);
