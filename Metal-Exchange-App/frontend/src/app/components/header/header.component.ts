import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterLink, RouterLinkActive } from '@angular/router';
import { RateService } from '../../services/rate.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink, RouterLinkActive],
  template: `
    <header class="header">
      <div class="header-inner">
        <!-- Brand -->
        <div class="brand">
          <span class="brand-icon">⚖️</span>
          <div>
            <div class="brand-name">GoldExchange</div>
            <div class="brand-sub">Live Indian Metal Rates</div>
          </div>
        </div>

        <!-- Nav -->
        <nav class="nav">
          <a routerLink="/dashboard"
             routerLinkActive="active"
             class="nav-link">
            📊 Dashboard
          </a>
          <a routerLink="/history"
             routerLinkActive="active"
             class="nav-link">
            📅 History
          </a>
        </nav>

        <!-- Live price ticker -->
        <div class="ticker">
          @if (rateService.goldRate()) {
            <div class="tick gold-tick">
              <span class="tick-label">GOLD</span>
              <span class="tick-price">₹{{ rateService.goldRate()?.pricePerGramInr | number:'1.2-2' }}/g</span>
              <span class="tick-change" [class.up]="rateService.goldTrend() === 'up'" [class.down]="rateService.goldTrend() === 'down'">
                {{ rateService.goldTrend() === 'up' ? '▲' : rateService.goldTrend() === 'down' ? '▼' : '—' }}
                {{ rateService.goldRate()?.changePercent | number:'1.2-2' }}%
              </span>
            </div>
          }
          @if (rateService.silverRate()) {
            <div class="tick silver-tick">
              <span class="tick-label">SILVER</span>
              <span class="tick-price">₹{{ rateService.silverRate()?.pricePerGramInr | number:'1.2-2' }}/g</span>
              <span class="tick-change" [class.up]="rateService.silverTrend() === 'up'" [class.down]="rateService.silverTrend() === 'down'">
                {{ rateService.silverTrend() === 'up' ? '▲' : rateService.silverTrend() === 'down' ? '▼' : '—' }}
                {{ rateService.silverRate()?.changePercent | number:'1.2-2' }}%
              </span>
            </div>
          }
        </div>
      </div>
    </header>
  `,
  styles: [`
    .header {
      position: sticky; top: 0; z-index: 100;
      background: rgba(26, 26, 46, 0.97);
      backdrop-filter: blur(10px);
      border-bottom: 1px solid rgba(255,255,255,0.08);
      margin-bottom: 1.5rem;
    }
    .header-inner {
      max-width: 1280px; margin: 0 auto;
      padding: 0 1.5rem;
      display: flex; align-items: center; gap: 2rem;
      height: 64px;
    }
    .brand { display: flex; align-items: center; gap: .75rem; }
    .brand-icon { font-size: 1.75rem; }
    .brand-name { font-size: 1rem; font-weight: 700; color: #f3f4f6; }
    .brand-sub  { font-size: .65rem; color: #9ca3af; }
    .nav { display: flex; gap: .5rem; margin-left: auto; }
    .nav-link {
      padding: .5rem 1rem; border-radius: 8px;
      font-size: .85rem; color: #9ca3af; text-decoration: none;
      transition: all .15s;
    }
    .nav-link:hover { background: rgba(255,255,255,.06); color: #f3f4f6; }
    .nav-link.active { background: rgba(245,158,11,.15); color: #fbbf24; }
    .ticker { display: flex; gap: 1rem; }
    .tick {
      display: flex; flex-direction: column; align-items: flex-end;
      padding: .35rem .7rem; border-radius: 8px; min-width: 90px;
    }
    .gold-tick { background: rgba(245,158,11,0.12); }
    .silver-tick { background: rgba(100,116,139,0.12); }
    .tick-label { font-size: .6rem; font-weight: 700; letter-spacing: .08em; color: #9ca3af; }
    .tick-price { font-size: .875rem; font-weight: 700; color: #f3f4f6; }
    .tick-change { font-size: .7rem; font-weight: 600; }
    .tick-change.up { color: #4ade80; }
    .tick-change.down { color: #f87171; }
    @media (max-width: 768px) { .ticker { display: none; } }
  `]
})
export class HeaderComponent {
  rateService = inject(RateService);
}
