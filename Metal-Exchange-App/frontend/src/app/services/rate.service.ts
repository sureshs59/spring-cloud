import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  Observable, interval, switchMap, startWith,
  catchError, of, shareReplay, map, EMPTY
} from 'rxjs';
import { MetalRate, TodayRates, TimeRange } from '../models/metal-rate.model';

@Injectable({ providedIn: 'root' })
export class RateService {

  private http = inject(HttpClient);
  private platformId = inject(PLATFORM_ID);
  private readonly BASE = '/api/rates';

  // ── Writable Signals (local state) ─────────────────────────────
  selectedTimeRange = signal<TimeRange>('30D');
  isLoading         = signal(false);
  lastError         = signal<string | null>(null);

  // ── Live today rates — auto-refreshes every 5 minutes ──────────
  private todayRates$ = isPlatformBrowser(this.platformId)
    ? interval(5 * 60 * 1000).pipe(
        startWith(0),
        switchMap(() =>
          this.http.get<TodayRates>(`${this.BASE}/today`).pipe(
            catchError(err => {
              this.lastError.set('Unable to fetch live rates. Showing cached data.');
              return of({ gold: null, silver: null, timestamp: 0 } as TodayRates);
            })
          )
        ),
        shareReplay(1)
      )
    : EMPTY;

  /** Signal: today's gold and silver rates */
  todayRates = toSignal(this.todayRates$, {
    initialValue: { gold: null, silver: null, timestamp: 0 } as TodayRates
  });

  /** Signal: gold rate computed from today */
  goldRate = computed(() => this.todayRates()?.gold ?? null);

  /** Signal: silver rate computed from today */
  silverRate = computed(() => this.todayRates()?.silver ?? null);

  /** Signal: gold change indicator — 'up' | 'down' | 'neutral' */
  goldTrend = computed(() => {
    const c = this.goldRate()?.changePercent ?? 0;
    return c > 0 ? 'up' : c < 0 ? 'down' : 'neutral';
  });

  /** Signal: silver change indicator */
  silverTrend = computed(() => {
    const c = this.silverRate()?.changePercent ?? 0;
    return c > 0 ? 'up' : c < 0 ? 'down' : 'neutral';
  });

  // ── History data for charts ─────────────────────────────────────

  getGoldHistory(days: number): Observable<MetalRate[]> {
    return isPlatformBrowser(this.platformId)
      ? this.http.get<MetalRate[]>(`${this.BASE}/gold/history?days=${days}`).pipe(
          catchError(() => of([]))
        )
      : of([]);
  }

  getSilverHistory(days: number): Observable<MetalRate[]> {
    return isPlatformBrowser(this.platformId)
      ? this.http.get<MetalRate[]>(`${this.BASE}/silver/history?days=${days}`).pipe(
          catchError(() => of([]))
        )
      : of([]);
  }

  getAllHistory(days: number): Observable<MetalRate[]> {
    return isPlatformBrowser(this.platformId)
      ? this.http.get<MetalRate[]>(`${this.BASE}/all?days=${days}`).pipe(
          catchError(() => of([]))
        )
      : of([]);
  }

  /** Maps TimeRange enum to number of days */
  getDaysForRange(range: TimeRange): number {
    const map: Record<TimeRange, number> = {
      '7D': 7, '30D': 30, '90D': 90, '1Y': 365
    };
    return map[range];
  }

  /** Manual rate refresh (for the refresh button) */
  triggerFetch(): Observable<any> {
    return isPlatformBrowser(this.platformId)
      ? this.http.post(`${this.BASE}/fetch`, {}).pipe(
          catchError(err => of({ error: err.message }))
        )
      : of({ error: 'Not available on server' });
  }

  // ── Format helpers ──────────────────────────────────────────────

  formatInr(amount: number | null | undefined): string {
    if (amount == null) return '—';
    return new Intl.NumberFormat('en-IN', {
      style: 'currency', currency: 'INR',
      minimumFractionDigits: 2, maximumFractionDigits: 2
    }).format(amount);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  }
}
