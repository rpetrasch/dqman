import { Component, inject } from '@angular/core';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { AsyncPipe, CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-main-layout',
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css',
  standalone: true,
  imports: [
    MatToolbarModule,
    MatButtonModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatBadgeModule,
    MatTooltipModule,
    FormsModule,
    AsyncPipe,
    CommonModule,
    RouterLink,
    RouterOutlet
  ]
})
export class MainLayoutComponent {
  private breakpointObserver = inject(BreakpointObserver);

  isHandset$: Observable<boolean> = this.breakpointObserver.observe(Breakpoints.Handset)
    .pipe(
      map(result => result.matches),
      shareReplay()
    );

  globalSearchText = '';
  alertCount = 3; // Example alert count

  onHelp(): void {
    console.log('Help clicked');
    // TODO: Implement help functionality
  }

  onAlerts(): void {
    console.log('Alerts clicked');
    // TODO: Navigate to alerts or show alerts panel
  }

  onSettings(): void {
    console.log('Settings clicked');
    // TODO: Navigate to settings
  }

  onAIAssistant(): void {
    console.log('AI Assistant clicked');
    // TODO: Open AI assistant panel/chat
  }

  onProfile(): void {
    console.log('Profile clicked');
    // TODO: Navigate to profile page
  }

  onAccountSettings(): void {
    console.log('Account Settings clicked');
    // TODO: Navigate to account settings
  }

  onLogout(): void {
    console.log('Logout clicked');
    // TODO: Implement logout functionality
  }
}
